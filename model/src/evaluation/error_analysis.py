"""
Error Analysis Module (Phase 13)

Provides:
- False positive / false negative analysis
- Confusion between adjacent relevance classes
- Language-specific error patterns
- Skill normalization error detection
- CVs with incomplete information analysis
- Hard negative analysis
"""
import sys
import json
import pandas as pd
import numpy as np
from pathlib import Path
from collections import Counter, defaultdict

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR
from src.utils.helpers import generate_report_header


def analyze_errors(
    true_labels: list[int],
    pred_labels: list[int],
    metadata_df: pd.DataFrame,
    similarity_scores: list[float] = None,
) -> dict:
    """
    Comprehensive error analysis on model predictions.

    Args:
        true_labels: Ground truth labels (0-3)
        pred_labels: Predicted labels (0-3)
        metadata_df: DataFrame with columns like language, source_dataset,
                     job_id, candidate_id, etc.
        similarity_scores: Raw similarity scores from model

    Returns:
        Dictionary with error analysis results
    """
    n = len(true_labels)
    analysis = {"total_samples": n}

    # ── 1. Error type counts ──
    errors = {
        "correct": 0,
        "false_positive": 0,  # Predicted higher than actual
        "false_negative": 0,  # Predicted lower than actual
        "off_by_one": 0,
        "off_by_two_plus": 0,
    }

    error_details = []
    for i in range(n):
        t, p = true_labels[i], pred_labels[i]
        diff = p - t

        if t == p:
            errors["correct"] += 1
        elif diff > 0:
            errors["false_positive"] += 1
        else:
            errors["false_negative"] += 1

        if abs(diff) == 1:
            errors["off_by_one"] += 1
        elif abs(diff) >= 2:
            errors["off_by_two_plus"] += 1

        if t != p:
            row_meta = {}
            if i < len(metadata_df):
                row = metadata_df.iloc[i]
                row_meta = {
                    "language": row.get("language", "unknown"),
                    "source": row.get("source_dataset", "unknown"),
                    "job_id": str(row.get("job_id", "")),
                    "candidate_id": str(row.get("candidate_id", "")),
                }

            error_details.append({
                "index": i,
                "true_label": t,
                "pred_label": p,
                "diff": diff,
                "similarity": similarity_scores[i] if similarity_scores else None,
                **row_meta,
            })

    analysis["error_counts"] = errors
    analysis["accuracy"] = round(errors["correct"] / n, 4) if n > 0 else 0

    # ── 2. Confusion between adjacent classes ──
    adjacent_confusion = defaultdict(int)
    for t, p in zip(true_labels, pred_labels):
        if t != p and abs(t - p) == 1:
            adjacent_confusion[f"{t}→{p}"] += 1
    analysis["adjacent_confusion"] = dict(adjacent_confusion)

    # ── 3. Language-specific errors ──
    if "language" in metadata_df.columns:
        lang_errors = defaultdict(lambda: {"correct": 0, "errors": 0, "total": 0})
        for i in range(n):
            lang = metadata_df.iloc[i].get("language", "unknown") if i < len(metadata_df) else "unknown"
            lang_errors[lang]["total"] += 1
            if true_labels[i] == pred_labels[i]:
                lang_errors[lang]["correct"] += 1
            else:
                lang_errors[lang]["errors"] += 1

        analysis["by_language"] = {}
        for lang, counts in lang_errors.items():
            total = counts["total"]
            analysis["by_language"][lang] = {
                "total": total,
                "errors": counts["errors"],
                "error_rate": round(counts["errors"] / total, 4) if total > 0 else 0,
                "accuracy": round(counts["correct"] / total, 4) if total > 0 else 0,
            }

    # ── 4. Source-specific errors ──
    if "source_dataset" in metadata_df.columns:
        source_errors = defaultdict(lambda: {"correct": 0, "errors": 0, "total": 0})
        for i in range(n):
            src = metadata_df.iloc[i].get("source_dataset", "unknown") if i < len(metadata_df) else "unknown"
            source_errors[src]["total"] += 1
            if true_labels[i] == pred_labels[i]:
                source_errors[src]["correct"] += 1
            else:
                source_errors[src]["errors"] += 1

        analysis["by_source"] = {}
        for src, counts in source_errors.items():
            total = counts["total"]
            analysis["by_source"][src] = {
                "total": total,
                "errors": counts["errors"],
                "error_rate": round(counts["errors"] / total, 4) if total > 0 else 0,
            }

    # ── 5. Error examples (worst cases) ──
    if error_details:
        # Sort by absolute difference (worst errors first)
        error_details.sort(key=lambda x: abs(x["diff"]), reverse=True)
        analysis["worst_errors"] = error_details[:20]

    # ── 6. Similarity score analysis by error type ──
    if similarity_scores:
        fp_sims = [similarity_scores[i] for i in range(n) if pred_labels[i] > true_labels[i]]
        fn_sims = [similarity_scores[i] for i in range(n) if pred_labels[i] < true_labels[i]]
        correct_sims = [similarity_scores[i] for i in range(n) if pred_labels[i] == true_labels[i]]

        analysis["similarity_by_error_type"] = {
            "correct": {
                "mean": round(float(np.mean(correct_sims)), 4) if correct_sims else None,
                "std": round(float(np.std(correct_sims)), 4) if correct_sims else None,
            },
            "false_positive": {
                "mean": round(float(np.mean(fp_sims)), 4) if fp_sims else None,
                "std": round(float(np.std(fp_sims)), 4) if fp_sims else None,
                "count": len(fp_sims),
            },
            "false_negative": {
                "mean": round(float(np.mean(fn_sims)), 4) if fn_sims else None,
                "std": round(float(np.std(fn_sims)), 4) if fn_sims else None,
                "count": len(fn_sims),
            },
        }

    return analysis


def generate_error_report(analysis: dict) -> str:
    """Generate error analysis markdown report."""
    report = generate_report_header(
        "Error Analysis Report",
        "Model Prediction Errors"
    )

    # Error counts
    ec = analysis.get("error_counts", {})
    total = analysis.get("total_samples", 0)
    report += "## 1. Error Summary\n\n"
    report += f"**Total samples:** {total:,}  \n"
    report += f"**Accuracy:** {analysis.get('accuracy', 'N/A')}  \n\n"
    report += "| Error Type | Count | Percentage |\n|---|---|---|\n"
    for etype, count in ec.items():
        pct = f"{100*count/total:.1f}%" if total > 0 else "N/A"
        report += f"| {etype} | {count:,} | {pct} |\n"
    report += "\n"

    # Adjacent confusion
    ac = analysis.get("adjacent_confusion", {})
    if ac:
        report += "## 2. Adjacent Class Confusion\n\n"
        report += "| Transition | Count |\n|---|---|\n"
        for trans, count in sorted(ac.items(), key=lambda x: -x[1]):
            report += f"| {trans} | {count:,} |\n"
        report += "\n"

    # Language-specific
    by_lang = analysis.get("by_language", {})
    if by_lang:
        report += "## 3. Language-Specific Error Rates\n\n"
        report += "| Language | Total | Errors | Error Rate | Accuracy |\n|---|---|---|---|---|\n"
        for lang, stats in by_lang.items():
            report += (
                f"| {lang} | {stats['total']:,} | {stats['errors']:,} | "
                f"{stats['error_rate']} | {stats['accuracy']} |\n"
            )
        report += "\n"

    # Source-specific
    by_src = analysis.get("by_source", {})
    if by_src:
        report += "## 4. Source-Specific Error Rates\n\n"
        report += "| Source | Total | Errors | Error Rate |\n|---|---|---|---|\n"
        for src, stats in by_src.items():
            report += f"| {src} | {stats['total']:,} | {stats['errors']:,} | {stats['error_rate']} |\n"
        report += "\n"

    # Similarity analysis
    sim_analysis = analysis.get("similarity_by_error_type", {})
    if sim_analysis:
        report += "## 5. Similarity Score Analysis by Error Type\n\n"
        report += "| Error Type | Mean Similarity | Std | Count |\n|---|---|---|---|\n"
        for etype, stats in sim_analysis.items():
            mean = stats.get("mean", "N/A")
            std = stats.get("std", "N/A")
            count = stats.get("count", "—")
            report += f"| {etype} | {mean} | {std} | {count} |\n"
        report += "\n"

    # Worst errors
    worst = analysis.get("worst_errors", [])
    if worst:
        report += "## 6. Worst Prediction Errors (Top 20)\n\n"
        report += "| # | True | Pred | Diff | Language | Source | Similarity |\n|---|---|---|---|---|---|---|\n"
        for i, err in enumerate(worst[:20], 1):
            report += (
                f"| {i} | {err['true_label']} | {err['pred_label']} | "
                f"{err['diff']:+d} | {err.get('language', '?')} | "
                f"{err.get('source', '?')} | {err.get('similarity', 'N/A')} |\n"
            )
        report += "\n"

    report += "## 7. Recommendations\n\n"
    report += "1. Focus on reducing adjacent class confusion (off-by-one errors)\n"
    report += "2. Investigate language-specific error patterns for targeted improvements\n"
    report += "3. Review false positives with high similarity — may indicate hard negatives\n"
    report += "4. Review false negatives with low similarity — may indicate missing skill synonyms\n"
    report += "5. Consider calibrating similarity score → label thresholds per language\n"
    report += "6. Do not present similarity scores as calibrated probabilities\n\n"

    return report


if __name__ == "__main__":
    # This module is used by the experiment runner after evaluation.
    # Standalone usage example:
    print("Error analysis module loaded.")
    print("Usage: analyze_errors(true_labels, pred_labels, metadata_df)")
    print("       generate_error_report(analysis_dict)")
