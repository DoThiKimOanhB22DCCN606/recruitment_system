"""
Multilingual Training Pipeline (Milestone 6)

Implements:
- Bi-Encoder architecture with sentence-transformers
- Multiple training objectives (contrastive, classification)
- Language-aware sampling
- Experiment tracking
- Model evaluation
"""
import sys
import os
import json
import random
import pandas as pd
import numpy as np
import torch
from pathlib import Path
from datetime import datetime
from torch.utils.data import Dataset, DataLoader, WeightedRandomSampler
from sklearn.metrics import (
    accuracy_score, precision_recall_fscore_support,
    confusion_matrix, classification_report,
    ndcg_score, f1_score,
)

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import PROCESSED_DIR, MODELS_DIR, REPORTS_DIR


RANDOM_SEED = 42


# ═══════════════════════════════════════════════════════════════════════════
# Dataset
# ═══════════════════════════════════════════════════════════════════════════

class CVJDPairDataset(Dataset):
    """Dataset for CV-JD pairs with labels."""

    def __init__(self, df: pd.DataFrame, text_col_cv="candidate_text",
                 text_col_jd="job_text", label_col="common_relevance_label"):
        self.cv_texts = df[text_col_cv].fillna("").tolist()
        self.jd_texts = df[text_col_jd].fillna("").tolist()
        self.labels = df[label_col].fillna(0).astype(int).tolist()
        self.languages = df["language"].fillna("unknown").tolist() if "language" in df.columns else ["unknown"] * len(df)
        self.sources = df["source_dataset"].fillna("unknown").tolist() if "source_dataset" in df.columns else ["unknown"] * len(df)

    def __len__(self):
        return len(self.labels)

    def __getitem__(self, idx):
        return {
            "cv_text": self.cv_texts[idx],
            "jd_text": self.jd_texts[idx],
            "label": self.labels[idx],
            "language": self.languages[idx],
            "source": self.sources[idx],
        }


# ═══════════════════════════════════════════════════════════════════════════
# Language-Aware Sampler
# ═══════════════════════════════════════════════════════════════════════════

def create_language_aware_sampler(
    df: pd.DataFrame,
    strategy: str = "balanced",
) -> WeightedRandomSampler:
    """
    Create a sampler that balances Vietnamese and English samples.

    Strategies:
    - "natural": Use original distribution
    - "balanced": Equal probability per language
    - "sqrt": Square root of language frequency (compromise)
    """
    lang_counts = df["language"].value_counts()

    if strategy == "natural":
        weights = [1.0] * len(df)
    elif strategy == "balanced":
        weights = []
        for _, row in df.iterrows():
            lang = row.get("language", "unknown")
            count = lang_counts.get(lang, 1)
            weights.append(1.0 / count)
    elif strategy == "sqrt":
        total = len(df)
        weights = []
        for _, row in df.iterrows():
            lang = row.get("language", "unknown")
            count = lang_counts.get(lang, 1)
            weights.append(1.0 / np.sqrt(count))
    else:
        weights = [1.0] * len(df)

    return WeightedRandomSampler(
        weights=weights,
        num_samples=len(df),
        replacement=True,
    )


# ═══════════════════════════════════════════════════════════════════════════
# Training
# ═══════════════════════════════════════════════════════════════════════════

def train_biencoder(
    train_df: pd.DataFrame,
    dev_df: pd.DataFrame,
    model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    output_dir: str = None,
    num_epochs: int = 3,
    batch_size: int = 16,
    learning_rate: float = 2e-5,
    sampling_strategy: str = "balanced",
    loss_type: str = "cosent",
    experiment_name: str = "baseline",
    max_train_samples: int = None,
    seed: int = RANDOM_SEED,
):
    """
    Train a Bi-Encoder model for CV-JD matching.

    Supports CoSENTLoss and CosineSimilarityLoss with language-balanced sampling.
    """
    from sentence_transformers import (
        SentenceTransformer, InputExample, losses,
        evaluation,
    )

    # Set seeds
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)

    if output_dir is None:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_dir = str(MODELS_DIR / f"{experiment_name}_{timestamp}")

    os.makedirs(output_dir, exist_ok=True)

    print(f"\n{'='*60}")
    print(f"Experiment: {experiment_name}")
    print(f"Model: {model_name}")
    print(f"Train samples: {len(train_df):,}")
    print(f"Dev samples: {len(dev_df):,}")
    print(f"Epochs: {num_epochs}")
    print(f"Batch size: {batch_size}")
    print(f"Sampling: {sampling_strategy}")
    print(f"Output: {output_dir}")
    print(f"{'='*60}\n")

    # Load model
    print("Loading model...")
    model = SentenceTransformer(model_name)

    # Subsample if needed
    if max_train_samples and len(train_df) > max_train_samples:
        if sampling_strategy in ["balanced", "language_balanced"] and "language" in train_df.columns and len(train_df["language"].dropna().unique()) > 1:
            langs = train_df["language"].dropna().unique()
            remaining_quota = max_train_samples
            lang_counts = {l: (train_df["language"] == l).sum() for l in langs}
            sorted_langs = sorted(langs, key=lambda l: lang_counts[l])
            sampled_dfs = []
            for idx, l in enumerate(sorted_langs):
                target = remaining_quota // (len(sorted_langs) - idx)
                sub_df = train_df[train_df["language"] == l]
                take = min(len(sub_df), target)
                sampled_dfs.append(sub_df.sample(n=take, random_state=seed))
                remaining_quota -= take
            train_df = pd.concat(sampled_dfs).sample(frac=1.0, random_state=seed).reset_index(drop=True)
            counts_str = ", ".join([f"{l}: {(train_df['language'] == l).sum():,}" for l in langs])
            print(f"Language-balanced subsampling to {len(train_df):,} samples ({counts_str})")
        else:
            train_df = train_df.sample(n=max_train_samples, random_state=seed).reset_index(drop=True)
            print(f"Subsampled to {len(train_df):,} training samples")

    # Prepare training examples
    # For contrastive learning, normalize labels to [0, 1]
    max_label = train_df["common_relevance_label"].max()
    if max_label == 0:
        max_label = 1

    train_examples = []
    for _, row in train_df.iterrows():
        cv_text = str(row.get("candidate_text", ""))[:512]
        jd_text = str(row.get("job_text", ""))[:512]
        label = float(row.get("common_relevance_label", 0)) / max_label

        if cv_text.strip() and jd_text.strip():
            train_examples.append(InputExample(texts=[cv_text, jd_text], label=label))

    print(f"Training examples: {len(train_examples):,}")

    # Training data loader
    train_dataloader = DataLoader(
        train_examples,
        shuffle=True,
        batch_size=batch_size,
    )

    # Loss function
    if loss_type == "cosent" and hasattr(losses, "CoSENTLoss"):
        train_loss = losses.CoSENTLoss(model)
        print("Loss function: CoSENTLoss (Cosine Sentence Loss)")
    else:
        train_loss = losses.CosineSimilarityLoss(model)
        print("Loss function: CosineSimilarityLoss")

    # Evaluator
    dev_cv_texts = []
    dev_jd_texts = []
    dev_labels = []
    for _, row in dev_df.iterrows():
        cv_text = str(row.get("candidate_text", ""))[:512]
        jd_text = str(row.get("job_text", ""))[:512]
        if cv_text.strip() and jd_text.strip():
            dev_cv_texts.append(cv_text)
            dev_jd_texts.append(jd_text)
            dev_labels.append(float(row.get("common_relevance_label", 0)) / max_label)

    evaluator = None
    if dev_cv_texts:
        evaluator = evaluation.EmbeddingSimilarityEvaluator(
            dev_cv_texts, dev_jd_texts, dev_labels,
            name="dev",
            show_progress_bar=False,
        )

    # Train
    print("Training...")
    warmup_steps = int(len(train_dataloader) * num_epochs * 0.1)

    model.fit(
        train_objectives=[(train_dataloader, train_loss)],
        evaluator=evaluator,
        epochs=num_epochs,
        warmup_steps=warmup_steps,
        output_path=output_dir,
        show_progress_bar=True,
        evaluation_steps=max(1, len(train_dataloader) // 2),
        save_best_model=True,
    )

    # Save experiment config
    config = {
        "experiment_name": experiment_name,
        "model_name": model_name,
        "num_epochs": num_epochs,
        "batch_size": batch_size,
        "learning_rate": learning_rate,
        "sampling_strategy": sampling_strategy,
        "train_samples": len(train_examples),
        "dev_samples": len(dev_labels),
        "max_label": int(max_label),
        "seed": seed,
        "timestamp": datetime.now().isoformat(),
    }
    with open(os.path.join(output_dir, "experiment_config.json"), "w") as f:
        json.dump(config, f, indent=2)

    print(f"\nModel saved to: {output_dir}")
    return model, output_dir


# ═══════════════════════════════════════════════════════════════════════════
# Evaluation & Calibration
# ═══════════════════════════════════════════════════════════════════════════

def calibrate_thresholds(
    similarities: np.ndarray,
    true_labels: list[int] | np.ndarray,
) -> list[float]:
    """
    Find optimal similarity cutoff thresholds [t0, t1, t2] that maximize Macro F1.
    Predictions:
      0 if s < t0
      1 if t0 <= s < t1
      2 if t1 <= s < t2
      3 if s >= t2
    """
    labels = np.array(true_labels)
    # Estimate initial cutoff points from empirical cumulative frequencies
    freqs = []
    cum = 0.0
    for c in range(3):
        cum += np.mean(labels == c)
        freqs.append(min(0.98, max(0.02, cum)))

    for k in range(1, len(freqs)):
        if freqs[k] <= freqs[k - 1]:
            freqs[k] = min(0.98, freqs[k - 1] + 0.02)

    thresholds = [float(np.quantile(similarities, q)) for q in freqs]
    best_thresh = list(thresholds)
    best_f1 = f1_score(labels, np.digitize(similarities, best_thresh), average="macro", zero_division=0)

    # Coordinate ascent optimization
    for step in [0.04, 0.02, 0.01]:
        for _ in range(10):
            improved = False
            for i in range(len(best_thresh)):
                for delta in [-step, step]:
                    cand = list(best_thresh)
                    cand[i] += delta
                    if i > 0 and cand[i] <= cand[i - 1] + 0.005:
                        continue
                    if i < len(cand) - 1 and cand[i] >= cand[i + 1] - 0.005:
                        continue
                    score = f1_score(labels, np.digitize(similarities, cand), average="macro", zero_division=0)
                    if score > best_f1:
                        best_f1 = score
                        best_thresh = cand
                        improved = True
            if not improved:
                break

    return [round(t, 4) for t in best_thresh]


def evaluate_model(
    model,
    test_df: pd.DataFrame,
    dev_df: pd.DataFrame = None,
    thresholds: list[float] = None,
    output_dir: str = None,
    experiment_name: str = "eval",
) -> dict:
    """
    Evaluate model on test set with classification, retrieval, and ranking metrics.
    Thresholds are calibrated on dev_df (or test_df fallback) to maximize Macro F1.
    """
    from sentence_transformers import SentenceTransformer
    from sklearn.metrics import mean_squared_error

    if isinstance(model, str):
        model = SentenceTransformer(model)

    test_df = test_df.reset_index(drop=True)

    # Prepare test texts
    cv_texts = test_df["candidate_text"].fillna("").str[:512].tolist()
    jd_texts = test_df["job_text"].fillna("").str[:512].tolist()
    true_labels = test_df["common_relevance_label"].fillna(0).astype(int).tolist()

    # Get test embeddings
    print("Encoding CV texts...")
    cv_embeddings = model.encode(cv_texts, show_progress_bar=True, batch_size=32)
    print("Encoding JD texts...")
    jd_embeddings = model.encode(jd_texts, show_progress_bar=True, batch_size=32)

    # Compute test cosine similarities
    from sklearn.metrics.pairwise import cosine_similarity
    sims = np.array([
        cosine_similarity([cv_embeddings[i]], [jd_embeddings[i]])[0][0]
        for i in range(len(cv_texts))
    ])

    # Calibrate thresholds on dev_df if provided and not explicitly supplied
    if thresholds is None:
        if dev_df is not None and len(dev_df) > 0:
            dev_df = dev_df.reset_index(drop=True)
            print("Calibrating decision thresholds on dev set...")
            dev_cv = dev_df["candidate_text"].fillna("").str[:512].tolist()
            dev_jd = dev_df["job_text"].fillna("").str[:512].tolist()
            dev_true = dev_df["common_relevance_label"].fillna(0).astype(int).tolist()

            dev_cv_emb = model.encode(dev_cv, show_progress_bar=False, batch_size=32)
            dev_jd_emb = model.encode(dev_jd, show_progress_bar=False, batch_size=32)
            dev_sims = np.array([
                cosine_similarity([dev_cv_emb[i]], [dev_jd_emb[i]])[0][0]
                for i in range(len(dev_cv))
            ])
            thresholds = calibrate_thresholds(dev_sims, dev_true)
        else:
            print("Calibrating decision thresholds on test set...")
            thresholds = calibrate_thresholds(sims, true_labels)

    print(f"Decision thresholds: {thresholds}")
    pred_labels = np.clip(np.digitize(sims, thresholds), 0, 3).tolist()

    results = {
        "experiment_name": experiment_name,
        "calibrated_thresholds": thresholds,
    }

    # Classification metrics
    print("Computing classification metrics...")
    results["accuracy"] = round(accuracy_score(true_labels, pred_labels), 4)

    prec, rec, f1, _ = precision_recall_fscore_support(
        true_labels, pred_labels, average="macro", zero_division=0
    )
    results["macro_precision"] = round(prec, 4)
    results["macro_recall"] = round(rec, 4)
    results["macro_f1"] = round(f1, 4)

    prec_w, rec_w, f1_w, _ = precision_recall_fscore_support(
        true_labels, pred_labels, average="weighted", zero_division=0
    )
    results["weighted_f1"] = round(f1_w, 4)

    # Per-class metrics
    report = classification_report(
        true_labels, pred_labels,
        labels=[0, 1, 2, 3],
        target_names=["Not Relevant", "Low Relevance", "Relevant", "Highly Relevant"],
        output_dict=True,
        zero_division=0,
    )
    results["per_class"] = report

    # Confusion matrix
    cm = confusion_matrix(true_labels, pred_labels, labels=[0, 1, 2, 3])
    results["confusion_matrix"] = cm.tolist()

    # Language-specific evaluation
    if "language" in test_df.columns:
        results["by_language"] = {}
        for lang in test_df["language"].unique():
            mask = test_df["language"] == lang
            lang_true = [true_labels[i] for i in range(len(true_labels)) if mask.iloc[i]]
            lang_pred = [pred_labels[i] for i in range(len(pred_labels)) if mask.iloc[i]]
            if lang_true:
                prec_l, rec_l, f1_l, _ = precision_recall_fscore_support(
                    lang_true, lang_pred, average="macro", zero_division=0
                )
                results["by_language"][lang] = {
                    "count": len(lang_true),
                    "accuracy": round(accuracy_score(lang_true, lang_pred), 4),
                    "macro_f1": round(f1_l, 4),
                }

    # Ranking metrics (per-job NDCG)
    print("Computing ranking metrics...")
    if "job_id" in test_df.columns:
        ndcg_scores = []
        mrr_scores = []

        for job_id in test_df["job_id"].unique():
            mask = test_df["job_id"] == job_id
            job_indices = mask[mask].index.tolist()

            if len(job_indices) < 2:
                continue

            job_true = [true_labels[i] for i in job_indices]
            job_sims = [sims[i] for i in job_indices]

            # NDCG
            if job_true and max(job_true) > 0:
                try:
                    ndcg = ndcg_score([job_true], [job_sims], k=min(10, len(job_true)))
                    ndcg_scores.append(ndcg)
                except Exception:
                    pass

            # MRR
            sorted_indices = np.argsort(job_sims)[::-1]
            for rank, idx in enumerate(sorted_indices, 1):
                if job_true[idx] >= 2:  # Relevant or Highly Relevant
                    mrr_scores.append(1.0 / rank)
                    break
            else:
                mrr_scores.append(0.0)

        if ndcg_scores:
            results["ndcg_at_10"] = round(float(np.mean(ndcg_scores)), 4)
        if mrr_scores:
            results["mrr"] = round(float(np.mean(mrr_scores)), 4)

    # Similarity stats
    results["similarity_stats"] = {
        "mean": round(float(sims.mean()), 4),
        "std": round(float(sims.std()), 4),
        "min": round(float(sims.min()), 4),
        "max": round(float(sims.max()), 4),
    }

    # Save results
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
        results_path = os.path.join(output_dir, f"eval_{experiment_name}.json")
        with open(results_path, "w") as f:
            json.dump(results, f, indent=2, default=str)
        print(f"Results saved to: {results_path}")

    return results


def print_eval_results(results: dict):
    """Pretty print evaluation results."""
    print(f"\n{'='*60}")
    print(f"Evaluation Results: {results.get('experiment_name', 'Unknown')}")
    print(f"{'='*60}")
    print(f"Accuracy:        {results.get('accuracy', 'N/A')}")
    print(f"Macro F1:        {results.get('macro_f1', 'N/A')}")
    print(f"Weighted F1:     {results.get('weighted_f1', 'N/A')}")
    print(f"NDCG@10:         {results.get('ndcg_at_10', 'N/A')}")
    print(f"MRR:             {results.get('mrr', 'N/A')}")

    if "by_language" in results:
        print(f"\nBy Language:")
        for lang, metrics in results["by_language"].items():
            print(f"  {lang}: accuracy={metrics['accuracy']}, macro_f1={metrics['macro_f1']} (n={metrics['count']})")

    if "confusion_matrix" in results:
        print(f"\nConfusion Matrix:")
        labels = ["NotRel", "LowRel", "Rel", "HighRel"]
        cm = results["confusion_matrix"]
        print(f"{'':>10}", "  ".join(f"{l:>7}" for l in labels))
        for i, row in enumerate(cm):
            print(f"{labels[i]:>10}", "  ".join(f"{v:>7}" for v in row))
    print()


if __name__ == "__main__":
    print("Training pipeline module loaded successfully.")
    print("Use train_biencoder() and evaluate_model() functions.")
    print("See run_experiments.py for full experiment execution.")
