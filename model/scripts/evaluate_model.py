"""
evaluate_model.py — Evaluate the trained CV–JD matching model on test data.

Computes accuracy, precision, recall, F1, and confusion matrix.
Saves results to output/evaluation_results.json.
"""

import os
import json
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix,
)
from sentence_transformers import CrossEncoder

# ── Paths ────────────────────────────────────────────────────────────────────
BASE_DIR   = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPLIT_DIR  = os.path.join(BASE_DIR, "data", "splits")
MODEL_DIR  = os.path.join(BASE_DIR, "output", "cv_jd_matcher_v1")
OUTPUT_DIR = os.path.join(BASE_DIR, "output")

LABEL_NAMES = {0: "Not suitable", 1: "Partially suitable", 2: "Highly suitable"}

def evaluate():
    print("=" * 60)
    print("CV–JD MATCHING MODEL EVALUATION")
    print("=" * 60)

    # Load test data
    print("\nLoading test data...")
    test_df = pd.read_parquet(os.path.join(SPLIT_DIR, "test.parquet"))
    print(f"  Test samples: {test_df.shape[0]}")
    print(f"  Label distribution: {test_df['label'].value_counts().sort_index().to_dict()}")

    # Load model
    print(f"\nLoading model from {MODEL_DIR}...")
    model = CrossEncoder(MODEL_DIR, max_length=512)

    # Prepare sentence pairs
    sentence_pairs = list(zip(
        test_df["cv_text"].astype(str).tolist(),
        test_df["job_text"].astype(str).tolist(),
    ))
    true_labels = test_df["label"].astype(int).tolist()

    # Predict
    print("Running predictions...")
    predictions_raw = model.predict(sentence_pairs, show_progress_bar=True)

    # For 3-class classification, predictions_raw is (n, 3) logits
    if len(predictions_raw.shape) > 1:
        predicted_labels = np.argmax(predictions_raw, axis=1).tolist()
        confidence_scores = np.max(
            np.exp(predictions_raw) / np.exp(predictions_raw).sum(axis=1, keepdims=True),
            axis=1
        ).tolist()
    else:
        predicted_labels = (predictions_raw > 0.5).astype(int).tolist()
        confidence_scores = predictions_raw.tolist()

    # Compute metrics
    print("\nComputing metrics...")
    accuracy  = accuracy_score(true_labels, predicted_labels)
    precision = precision_score(true_labels, predicted_labels, average="weighted", zero_division=0)
    recall    = recall_score(true_labels, predicted_labels, average="weighted", zero_division=0)
    f1        = f1_score(true_labels, predicted_labels, average="weighted", zero_division=0)

    precision_macro = precision_score(true_labels, predicted_labels, average="macro", zero_division=0)
    recall_macro    = recall_score(true_labels, predicted_labels, average="macro", zero_division=0)
    f1_macro        = f1_score(true_labels, predicted_labels, average="macro", zero_division=0)

    cm = confusion_matrix(true_labels, predicted_labels, labels=[0, 1, 2])
    report = classification_report(
        true_labels, predicted_labels,
        labels=[0, 1, 2],
        target_names=list(LABEL_NAMES.values()),
        zero_division=0,
    )

    # Print results
    print("\n" + "=" * 60)
    print("EVALUATION RESULTS")
    print("=" * 60)
    print(f"\n  Accuracy:           {accuracy:.4f}")
    print(f"  Precision (wt):     {precision:.4f}")
    print(f"  Recall (wt):        {recall:.4f}")
    print(f"  F1 (wt):            {f1:.4f}")
    print(f"  Precision (macro):  {precision_macro:.4f}")
    print(f"  Recall (macro):     {recall_macro:.4f}")
    print(f"  F1 (macro):         {f1_macro:.4f}")
    print(f"\n  Confusion Matrix (rows=true, cols=pred):")
    print(f"  Labels: {list(LABEL_NAMES.values())}")
    for i, row in enumerate(cm):
        print(f"    {LABEL_NAMES[i]:>20s}: {row}")
    print(f"\n  Classification Report:\n{report}")

    # Save results
    results = {
        "test_samples": len(true_labels),
        "accuracy": round(accuracy, 4),
        "weighted": {
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1": round(f1, 4),
        },
        "macro": {
            "precision": round(precision_macro, 4),
            "recall": round(recall_macro, 4),
            "f1": round(f1_macro, 4),
        },
        "confusion_matrix": cm.tolist(),
        "label_names": LABEL_NAMES,
        "classification_report": report,
        "avg_confidence": round(float(np.mean(confidence_scores)), 4),
    }

    results_path = os.path.join(OUTPUT_DIR, "evaluation_results.json")
    with open(results_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nSaved evaluation results -> {results_path}")

if __name__ == "__main__":
    evaluate()
