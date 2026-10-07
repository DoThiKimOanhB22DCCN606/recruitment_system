"""
train_model.py -- Train a Cross-Encoder for CV-JD matching.

Uses sentence-transformers CrossEncoder with 3-class classification.
Loads data from data/splits/train.parquet and data/splits/validation.parquet.
Saves best model to output/cv_jd_matcher_v1/.

Designed to work on CPU (with GPU auto-detection).
"""

import os
import json
import time
import numpy as np
import pandas as pd
import torch
from datetime import datetime

from sentence_transformers import CrossEncoder, InputExample
from sentence_transformers.cross_encoder.evaluation import CESoftmaxAccuracyEvaluator
from torch.utils.data import DataLoader

# -- Paths -----------------------------------------------------------------
BASE_DIR   = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPLIT_DIR  = os.path.join(BASE_DIR, "data", "splits")
OUTPUT_DIR = os.path.join(BASE_DIR, "output", "cv_jd_matcher_v1")

os.makedirs(OUTPUT_DIR, exist_ok=True)

# -- Configuration ---------------------------------------------------------
CONFIG = {
    "model_name": "cross-encoder/ms-marco-MiniLM-L-6-v2",
    "num_labels": 3,
    "max_length": 512,
    "batch_size": 16,
    "num_epochs": 3,
    "learning_rate": 2e-5,
    "warmup_ratio": 0.1,
    "weight_decay": 0.01,
    "evaluation_steps": 500,
    "save_best_model": True,
    "show_progress_bar": True,
}

def load_data():
    """Load train and validation splits."""
    print("Loading training data...")
    train_df = pd.read_parquet(os.path.join(SPLIT_DIR, "train.parquet"))
    val_df   = pd.read_parquet(os.path.join(SPLIT_DIR, "validation.parquet"))

    print(f"  Train: {train_df.shape}")
    print(f"  Val:   {val_df.shape}")
    print(f"  Train labels: {train_df['label'].value_counts().sort_index().to_dict()}")
    print(f"  Val labels:   {val_df['label'].value_counts().sort_index().to_dict()}")

    return train_df, val_df

def prepare_input_examples(df, max_length_chars=1500):
    """Convert DataFrame rows to InputExample instances for CrossEncoder."""
    examples = []
    for _, row in df.iterrows():
        cv_text  = str(row["cv_text"])[:max_length_chars]
        job_text = str(row["job_text"])[:max_length_chars]
        label    = int(row["label"])
        examples.append(InputExample(texts=[cv_text, job_text], label=label))
    return examples

def train():
    """Main training function."""
    print("=" * 60)
    print("CV-JD MATCHING MODEL TRAINING")
    print("=" * 60)

    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Device: {device}")
    print(f"Model:  {CONFIG['model_name']}")
    print(f"Config: {json.dumps(CONFIG, indent=2)}")

    # Load data
    train_df, val_df = load_data()

    # Prepare training samples
    print("\nPreparing training samples...")
    train_examples = prepare_input_examples(train_df)
    val_examples   = prepare_input_examples(val_df)

    print(f"  Train examples: {len(train_examples)}")
    print(f"  Val examples:   {len(val_examples)}")

    # Create DataLoader
    train_dataloader = DataLoader(
        train_examples,
        shuffle=True,
        batch_size=CONFIG["batch_size"],
    )

    # Initialize CrossEncoder
    # The base model has num_labels=1 (ranking), we need 3 (classification).
    # ignore_mismatched_sizes=True reinitializes the classifier head.
    print(f"\nInitializing CrossEncoder: {CONFIG['model_name']}...")
    model = CrossEncoder(
        CONFIG["model_name"],
        num_labels=CONFIG["num_labels"],
        max_length=CONFIG["max_length"],
        device=device,
        automodel_args={"ignore_mismatched_sizes": True},
    )

    # Create evaluator for multi-class (softmax accuracy)
    val_sentence_pairs = [e.texts for e in val_examples]
    val_labels         = [e.label for e in val_examples]

    evaluator = CESoftmaxAccuracyEvaluator(
        sentence_pairs=val_sentence_pairs,
        labels=val_labels,
        name="val_eval",
    )

    # Calculate warmup steps
    steps_per_epoch = len(train_dataloader)
    num_train_steps = steps_per_epoch * CONFIG["num_epochs"]
    warmup_steps    = int(num_train_steps * CONFIG["warmup_ratio"])
    print(f"  Steps per epoch: {steps_per_epoch}")
    print(f"  Total training steps: {num_train_steps}")
    print(f"  Warmup steps: {warmup_steps}")

    # Train
    print("\n" + "=" * 60)
    print("STARTING TRAINING...")
    print("=" * 60)
    start_time = time.time()

    model.fit(
        train_dataloader=train_dataloader,
        evaluator=evaluator,
        epochs=CONFIG["num_epochs"],
        warmup_steps=warmup_steps,
        evaluation_steps=CONFIG["evaluation_steps"],
        output_path=OUTPUT_DIR,
        save_best_model=CONFIG["save_best_model"],
        show_progress_bar=CONFIG["show_progress_bar"],
        optimizer_params={"lr": CONFIG["learning_rate"]},
        weight_decay=CONFIG["weight_decay"],
    )

    elapsed = time.time() - start_time
    print(f"\nTraining completed in {elapsed/60:.1f} minutes")

    # Save training config
    config_path = os.path.join(OUTPUT_DIR, "training_config.json")
    save_config = {
        **CONFIG,
        "device": device,
        "train_samples": len(train_examples),
        "val_samples": len(val_examples),
        "steps_per_epoch": steps_per_epoch,
        "total_steps": num_train_steps,
        "warmup_steps": warmup_steps,
        "training_time_seconds": round(elapsed, 2),
        "training_time_minutes": round(elapsed / 60, 1),
        "completed_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(save_config, f, indent=2)
    print(f"Saved training config -> {config_path}")

    print("\n" + "=" * 60)
    print(f"MODEL SAVED TO: {OUTPUT_DIR}")
    print("=" * 60)

if __name__ == "__main__":
    train()
