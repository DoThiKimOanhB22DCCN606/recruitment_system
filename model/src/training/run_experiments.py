"""
Experiment Runner (Milestone 6)

Runs the controlled experiments specified in the project requirements:
E1: Vietnamese only
E2: English only
E3: Combined (direct mappings only)
E9: Bi-Encoder baseline (full combined)
"""
import sys
import json
import pandas as pd
from pathlib import Path
from datetime import datetime

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import PROCESSED_DIR, MODELS_DIR, REPORTS_DIR
from src.training.trainer import (
    train_biencoder, evaluate_model, print_eval_results,
)
from src.utils.helpers import generate_report_header


def load_splits():
    """Load pre-split datasets."""
    splits = {}
    for split in ["train", "dev", "test"]:
        path = PROCESSED_DIR / f"split_{split}.csv"
        if path.exists():
            splits[split] = pd.read_csv(path, encoding="utf-8")
        else:
            print(f"Warning: {path} not found")
    return splits


def filter_by_source(df: pd.DataFrame, source: str) -> pd.DataFrame:
    """Filter dataset by source_dataset."""
    return df[df["source_dataset"] == source].copy()


def filter_by_confidence(df: pd.DataFrame, min_confidence: str = "high") -> pd.DataFrame:
    """Filter by label confidence."""
    confidence_levels = {"high": 3, "medium": 2, "low": 1}
    min_level = confidence_levels.get(min_confidence, 0)
    df_conf = df.copy()
    df_conf["_conf_level"] = df_conf["label_confidence"].map(confidence_levels).fillna(0)
    result = df_conf[df_conf["_conf_level"] >= min_level].drop(columns=["_conf_level"])
    return result


def run_experiment(
    experiment_id: str,
    train_df: pd.DataFrame,
    dev_df: pd.DataFrame,
    test_df: pd.DataFrame,
    model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    num_epochs: int = 3,
    batch_size: int = 16,
    max_train_samples: int = 5000,
    loss_type: str = "cosent",
    sampling_strategy: str = "balanced",
    **kwargs,
) -> dict:
    """Run a single experiment."""
    print(f"\n{'#'*60}")
    print(f"# Experiment {experiment_id}")
    print(f"{'#'*60}")

    # Filter out rows without text
    train_clean = train_df.dropna(subset=["candidate_text", "job_text"])
    train_clean = train_clean[
        (train_clean["candidate_text"].str.strip() != "") &
        (train_clean["job_text"].str.strip() != "")
    ]

    dev_clean = dev_df.dropna(subset=["candidate_text", "job_text"])
    dev_clean = dev_clean[
        (dev_clean["candidate_text"].str.strip() != "") &
        (dev_clean["job_text"].str.strip() != "")
    ]

    test_clean = test_df.dropna(subset=["candidate_text", "job_text"])
    test_clean = test_clean[
        (test_clean["candidate_text"].str.strip() != "") &
        (test_clean["job_text"].str.strip() != "")
    ]

    print(f"Train: {len(train_clean):,} (from {len(train_df):,})")
    print(f"Dev: {len(dev_clean):,}")
    print(f"Test: {len(test_clean):,}")

    if len(train_clean) < 10:
        print("ERROR: Not enough training samples. Skipping.")
        return {"experiment_id": experiment_id, "error": "insufficient_data"}

    # Train
    model, output_dir = train_biencoder(
        train_df=train_clean,
        dev_df=dev_clean,
        model_name=model_name,
        experiment_name=experiment_id,
        num_epochs=num_epochs,
        batch_size=batch_size,
        max_train_samples=max_train_samples,
        loss_type=loss_type,
        sampling_strategy=sampling_strategy,
        **kwargs,
    )

    # Evaluate (calibrating decision thresholds on dev set)
    results = evaluate_model(
        model=model,
        test_df=test_clean,
        dev_df=dev_clean,
        output_dir=output_dir,
        experiment_name=experiment_id,
    )
    results["experiment_id"] = experiment_id
    results["train_samples"] = len(train_clean)
    results["test_samples"] = len(test_clean)

    print_eval_results(results)

    return results


def run_all_experiments(
    model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    num_epochs: int = 2,
    batch_size: int = 16,
    max_train_samples: int = 5000,
    loss_type: str = "cosent",
):
    """Run all planned experiments."""
    splits = load_splits()
    if "train" not in splits:
        print("ERROR: Split files not found. Run pair_generation first.")
        return

    train = splits["train"]
    dev = splits["dev"]
    test = splits["test"]

    all_results = []

    # E1: Vietnamese only
    print("\n" + "="*60)
    print("E1: Vietnamese dataset only")
    vn_train = filter_by_source(train, "vietnamese")
    vn_dev = filter_by_source(dev, "vietnamese")
    vn_test = filter_by_source(test, "vietnamese")
    if len(vn_train) > 0 and len(vn_test) > 0:
        r = run_experiment("E1_vietnamese_only", vn_train, vn_dev, vn_test,
                          model_name=model_name, num_epochs=num_epochs,
                          batch_size=batch_size, max_train_samples=max_train_samples,
                          loss_type=loss_type)
        all_results.append(r)
    else:
        print("Skipping E1: insufficient Vietnamese data in splits")

    # E2: English only
    print("\n" + "="*60)
    print("E2: English dataset only")
    en_train = filter_by_source(train, "english")
    en_dev = filter_by_source(dev, "english")
    en_test = filter_by_source(test, "english")
    if len(en_train) > 0 and len(en_test) > 0:
        r = run_experiment("E2_english_only", en_train, en_dev, en_test,
                          model_name=model_name, num_epochs=num_epochs,
                          batch_size=batch_size, max_train_samples=max_train_samples,
                          loss_type=loss_type)
        all_results.append(r)
    else:
        print("Skipping E2: insufficient English data in splits")

    # E9: Bi-Encoder baseline (combined with language-balanced sampling)
    print("\n" + "="*60)
    print("E9: Combined Bi-Encoder baseline (Language-Balanced)")
    r = run_experiment("E9_biencoder_combined", train, dev, test,
                      model_name=model_name, num_epochs=num_epochs,
                      batch_size=batch_size, max_train_samples=max_train_samples,
                      loss_type=loss_type, sampling_strategy="language_balanced")
    all_results.append(r)

    # Generate experiment comparison report
    generate_experiment_report(all_results)

    return all_results


def generate_experiment_report(results: list[dict]):
    """Generate experiment comparison report."""
    report = generate_report_header(
        "Experiment Results Report",
        "Multilingual CV-JD Matching"
    )

    report += "## Experiment Comparison\n\n"
    report += "| Experiment | Train | Test | Accuracy | Macro F1 | Weighted F1 | NDCG@10 | MRR |\n"
    report += "|---|---|---|---|---|---|---|---|\n"

    for r in results:
        if "error" in r:
            report += f"| {r.get('experiment_id', '?')} | — | — | ERROR | — | — | — | — |\n"
            continue
        report += (
            f"| {r.get('experiment_id', '?')} | "
            f"{r.get('train_samples', '?'):,} | "
            f"{r.get('test_samples', '?'):,} | "
            f"{r.get('accuracy', 'N/A')} | "
            f"{r.get('macro_f1', 'N/A')} | "
            f"{r.get('weighted_f1', 'N/A')} | "
            f"{r.get('ndcg_at_10', 'N/A')} | "
            f"{r.get('mrr', 'N/A')} |\n"
        )
    report += "\n"

    # Per-language results
    report += "## Language-Specific Results\n\n"
    for r in results:
        if "error" in r or "by_language" not in r:
            continue
        report += f"### {r['experiment_id']}\n\n"
        report += "| Language | Count | Accuracy | Macro F1 |\n|---|---|---|---|\n"
        for lang, metrics in r["by_language"].items():
            report += f"| {lang} | {metrics['count']:,} | {metrics['accuracy']} | {metrics['macro_f1']} |\n"
        report += "\n"

    # Save
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "07_experiment_results.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"\nExperiment report saved to: {report_path}")

    # Save all results as JSON
    json_path = REPORTS_DIR / "07_experiment_results.json"
    with open(json_path, "w") as f:
        json.dump(results, f, indent=2, default=str)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
    parser.add_argument("--epochs", type=int, default=2)
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--max-samples", type=int, default=5000)
    parser.add_argument("--loss", default="cosent", choices=["cosent", "cosine"])
    args = parser.parse_args()

    run_all_experiments(
        model_name=args.model,
        num_epochs=args.epochs,
        batch_size=args.batch_size,
        max_train_samples=args.max_samples,
        loss_type=args.loss,
    )
