"""
Human Validation Module (Milestone 5)

Creates:
- Sampling strategy for human validation
- Annotation templates
- Inter-annotator agreement tools
"""
import sys
import json
import random
import pandas as pd
import numpy as np
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import PROCESSED_DIR, REPORTS_DIR
from src.label_harmonization.common_rubric import COMMON_RUBRIC


def sample_for_validation(
    unified_df: pd.DataFrame,
    n_total: int = 200,
    seed: int = 42,
) -> pd.DataFrame:
    """
    Select representative samples for human validation.

    Sampling strategy covers:
    - Every original label range
    - Both source datasets
    - Different job categories
    - Different seniority levels
    - Borderline relevance scores
    - High disagreement samples
    - Random samples
    """
    rng = np.random.RandomState(seed)
    samples = []
    used_indices = set()

    def add_samples(df_subset, n, reason):
        if len(df_subset) == 0:
            return
        available = df_subset[~df_subset.index.isin(used_indices)]
        n_take = min(n, len(available))
        if n_take > 0:
            chosen = available.sample(n=n_take, random_state=rng)
            for idx, row in chosen.iterrows():
                used_indices.add(idx)
                row_dict = row.to_dict()
                row_dict["sampling_reason"] = reason
                samples.append(row_dict)

    # 1. Stratified by source
    for source in unified_df["source_dataset"].unique():
        source_data = unified_df[unified_df["source_dataset"] == source]
        add_samples(source_data, n_total // 4, f"source_{source}")

    # 2. Borderline labels (label confidence = low/medium)
    if "label_confidence" in unified_df.columns:
        borderline = unified_df[unified_df["label_confidence"].isin(["low", "medium"])]
        add_samples(borderline, n_total // 6, "borderline_confidence")

    # 3. Each common label
    for label in range(4):
        label_data = unified_df[unified_df["common_relevance_label"] == label]
        add_samples(label_data, n_total // 8, f"label_{label}")

    # 4. Random samples
    remaining = n_total - len(samples)
    if remaining > 0:
        add_samples(unified_df, remaining, "random")

    result = pd.DataFrame(samples)
    return result


def create_annotation_template(sample_df: pd.DataFrame) -> list[dict]:
    """Create annotation templates for human validators."""
    templates = []

    for _, row in sample_df.iterrows():
        template = {
            "pair_id": f"{row.get('candidate_id', '')}_{row.get('job_id', '')}",
            "candidate_text": row.get("candidate_text", ""),
            "job_text": row.get("job_text", ""),
            "language": row.get("language", "unknown"),
            "source_dataset": row.get("source_dataset", "unknown"),
            "sampling_reason": row.get("sampling_reason", ""),
            # Fields for annotator to fill
            "human_relevance_label": None,  # 0-3
            "required_skills_evidence": None,  # present/absent/unknown
            "experience_evidence": None,
            "seniority_evidence": None,
            "industry_evidence": None,
            "justification": "",
            "annotator_id": "",
            "annotation_date": "",
            # Original labels for comparison (shown after annotation)
            "_original_label": row.get("original_label"),
            "_common_label": row.get("common_relevance_label"),
            "_label_confidence": row.get("label_confidence"),
        }
        templates.append(template)

    return templates


def compute_inter_annotator_agreement(
    annotations_a: list[int],
    annotations_b: list[int],
) -> dict:
    """Compute inter-annotator agreement statistics."""
    from sklearn.metrics import cohen_kappa_score

    if len(annotations_a) != len(annotations_b):
        return {"error": "Annotation lists must be same length"}

    n = len(annotations_a)
    exact_agreement = sum(a == b for a, b in zip(annotations_a, annotations_b)) / n

    # Cohen's Kappa
    try:
        kappa = cohen_kappa_score(annotations_a, annotations_b)
    except Exception:
        kappa = None

    # Weighted Kappa (for ordinal labels)
    try:
        weighted_kappa = cohen_kappa_score(
            annotations_a, annotations_b, weights="quadratic"
        )
    except Exception:
        weighted_kappa = None

    return {
        "n_samples": n,
        "exact_agreement": round(exact_agreement, 4),
        "cohens_kappa": round(kappa, 4) if kappa is not None else None,
        "weighted_kappa": round(weighted_kappa, 4) if weighted_kappa is not None else None,
    }


if __name__ == "__main__":
    # Load unified dataset
    unified_path = PROCESSED_DIR / "unified_dataset.csv"
    if unified_path.exists():
        df = pd.read_csv(unified_path, encoding="utf-8")

        # Sample for validation
        print("Sampling for human validation...")
        samples = sample_for_validation(df, n_total=200)
        print(f"  Selected {len(samples)} samples")

        # Create templates
        templates = create_annotation_template(samples)

        # Save
        PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
        template_path = PROCESSED_DIR / "human_validation_templates.json"
        with open(template_path, "w", encoding="utf-8") as f:
            json.dump(templates, f, ensure_ascii=False, indent=2)
        print(f"  Templates saved to: {template_path}")

        # Summary
        print(f"\n  Sampling reasons:")
        for reason, count in samples["sampling_reason"].value_counts().items():
            print(f"    {reason}: {count}")
    else:
        print(f"Unified dataset not found at {unified_path}")
