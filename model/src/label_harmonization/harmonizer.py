"""
Label Harmonization Module (Milestone 4)

Transforms independently labeled datasets into a common relevance framework.
- Maps Vietnamese 1-9 LLM consensus scores → common 0-3 rubric
- Maps English positive matches → common labels using structured features
- Preserves all original labels and provenance
- Generates harmonization report
"""
import sys
import json
import pandas as pd
import numpy as np
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR, OUTPUTS_DIR, PROCESSED_DIR
from src.data_ingestion.vietnamese_loader import (
    load_vn_labels, load_vn_annotations, load_vn_cvs, load_vn_jobs,
)
from src.data_ingestion.english_loader import (
    load_en_jobs, load_en_resumes, load_en_matches, explode_matches,
)
from src.label_harmonization.common_rubric import (
    map_vn_score_to_common, COMMON_RUBRIC, VN_TO_COMMON_THRESHOLDS,
)
from src.utils.helpers import generate_report_header, format_pct


# ═══════════════════════════════════════════════════════════════════════════
# Vietnamese Label Harmonization
# ═══════════════════════════════════════════════════════════════════════════

def harmonize_vietnamese_labels() -> pd.DataFrame:
    """
    Harmonize Vietnamese LLM-annotated labels to common 0-3 rubric.

    Uses relevance_median_no_cohere (median of Claude, GPToss, Llama)
    as the primary score, mapped to 0-3 via calibrated thresholds.
    """
    labels = load_vn_labels()
    annotations = load_vn_annotations()

    # Primary score column
    score_col = "relevance_median_no_cohere"
    range_col = "range_relevance_no_cohere"

    # Map to common rubric
    labels["common_relevance_label"] = labels[score_col].apply(map_vn_score_to_common)

    # Determine mapping confidence
    def compute_confidence(row):
        score = row[score_col]
        rng = row.get(range_col, 0)

        # Check if score is near a boundary
        boundaries = [2.5, 4.5, 6.5]
        near_boundary = any(abs(score - b) < 0.5 for b in boundaries)

        # High disagreement among annotators
        high_disagreement = rng > 4 if pd.notna(rng) else False

        if high_disagreement and near_boundary:
            return "low"
        elif high_disagreement or near_boundary:
            return "medium"
        else:
            return "high"

    labels["label_confidence"] = labels.apply(compute_confidence, axis=1)

    # Determine mapping status
    def compute_mapping_status(row):
        conf = row["label_confidence"]
        if conf == "high":
            return "direct_mapping"
        elif conf == "medium":
            return "conditional_mapping"
        else:
            return "human_relabeling_required"

    labels["mapping_status"] = labels.apply(compute_mapping_status, axis=1)

    # Add provenance columns
    labels["source_dataset"] = "vietnamese"
    labels["original_label"] = labels[score_col]
    labels["original_label_definition"] = (
        f"Median of 3 LLM relevance scores (Claude, GPToss, Llama) on 1-9 scale"
    )
    labels["label_source"] = "llm_consensus"
    labels["language"] = "vi"

    # Add per-annotator scores for traceability
    for model_name, ann_df in annotations.items():
        ann_subset = ann_df[["cv_id", "job_id", "relevance_score"]].copy()
        ann_subset = ann_subset.rename(
            columns={"relevance_score": f"annotator_{model_name}_score"}
        )
        labels = labels.merge(ann_subset, on=["cv_id", "job_id"], how="left")

    return labels


# ═══════════════════════════════════════════════════════════════════════════
# English Label Harmonization
# ═══════════════════════════════════════════════════════════════════════════

def _compute_skill_overlap(resume_skills, job_must_skills, job_nice_skills):
    """Compute skill overlap metrics between a resume and job."""
    if not isinstance(resume_skills, (list, np.ndarray)):
        resume_skills = []
    if not isinstance(job_must_skills, (list, np.ndarray)):
        job_must_skills = []
    if not isinstance(job_nice_skills, (list, np.ndarray)):
        job_nice_skills = []

    # Normalize to lowercase for comparison
    r_skills = {s.lower().strip() for s in resume_skills if isinstance(s, str)}
    must_skills = {s.lower().strip() for s in job_must_skills if isinstance(s, str)}
    nice_skills = {s.lower().strip() for s in job_nice_skills if isinstance(s, str)}

    must_overlap = len(r_skills & must_skills)
    nice_overlap = len(r_skills & nice_skills)
    must_total = len(must_skills)
    nice_total = len(nice_skills)

    must_coverage = must_overlap / must_total if must_total > 0 else 0.0
    nice_coverage = nice_overlap / nice_total if nice_total > 0 else 0.0

    return {
        "must_overlap": must_overlap,
        "must_total": must_total,
        "must_coverage": round(must_coverage, 3),
        "nice_overlap": nice_overlap,
        "nice_total": nice_total,
        "nice_coverage": round(nice_coverage, 3),
    }


def _compute_seniority_alignment(resume_seniority, job_seniority):
    """Compute seniority alignment score."""
    levels = {"Junior": 1, "Mid": 2, "Senior": 3, "Lead": 4}
    r_level = levels.get(resume_seniority, 0)
    j_level = levels.get(job_seniority, 0)
    if r_level == 0 or j_level == 0:
        return "unknown"
    diff = r_level - j_level
    if diff == 0:
        return "exact_match"
    elif abs(diff) == 1:
        return "close"
    else:
        return "mismatch"


def _compute_industry_match(resume_industry, job_industry):
    """Check if industries match."""
    if pd.isna(resume_industry) or pd.isna(job_industry):
        return "unknown"
    return "match" if resume_industry == job_industry else "mismatch"


def estimate_english_relevance(row) -> int:
    """
    Estimate common relevance label for English positive pairs
    based on structured feature analysis.

    Since English matches are positive-only and the criteria are unknown,
    we use available structured features to estimate the relevance grade.
    All English matched pairs receive at least label 1 (Low Relevance).
    """
    must_coverage = row.get("must_coverage", 0)
    nice_coverage = row.get("nice_coverage", 0)
    seniority_align = row.get("seniority_alignment", "unknown")
    industry_match = row.get("industry_match", "unknown")

    score = 0

    # Skills coverage is primary
    if must_coverage >= 0.7:
        score += 2
    elif must_coverage >= 0.4:
        score += 1

    # Seniority alignment
    if seniority_align == "exact_match":
        score += 1
    elif seniority_align == "close":
        score += 0.5

    # Industry match
    if industry_match == "match":
        score += 0.5

    # Nice-to-have bonus
    if nice_coverage >= 0.5:
        score += 0.5

    # Map to 0-3
    if score >= 3:
        return 3
    elif score >= 2:
        return 2
    elif score >= 1:
        return 1
    else:
        return 1  # Minimum 1 for known positive pairs


def harmonize_english_labels() -> pd.DataFrame:
    """
    Harmonize English positive-only labels to common 0-3 rubric.

    Uses structured feature analysis to estimate relevance grade.
    Only processes positive pairs (from matches).
    """
    jobs = load_en_jobs()
    resumes = load_en_resumes()
    matches = load_en_matches()

    # Explode to get positive pairs
    pairs = explode_matches(matches)

    # Merge job features
    pairs = pairs.merge(
        jobs[["job_id", "job_title", "seniority", "industry",
              "must_have_skills", "nice_to_have_skills"]],
        on="job_id",
        how="left",
        suffixes=("", "_job"),
    )

    # Merge resume features
    pairs = pairs.merge(
        resumes[["resume_id", "role", "seniority", "industry",
                 "years_experience", "skills"]],
        on="resume_id",
        how="left",
        suffixes=("_job", "_resume"),
    )

    # Handle column naming after merge
    if "seniority_job" in pairs.columns and "seniority_resume" in pairs.columns:
        pass  # Already have both
    elif "seniority" in pairs.columns:
        # Need to handle this case
        pass

    # Compute structured features
    skill_metrics = pairs.apply(
        lambda r: _compute_skill_overlap(
            r.get("skills", []),
            r.get("must_have_skills", []),
            r.get("nice_to_have_skills", []),
        ),
        axis=1,
    )
    skill_df = pd.DataFrame(skill_metrics.tolist())
    pairs = pd.concat([pairs, skill_df], axis=1)

    # Seniority alignment
    sen_job_col = "seniority_job" if "seniority_job" in pairs.columns else "seniority"
    sen_res_col = "seniority_resume" if "seniority_resume" in pairs.columns else "seniority"
    pairs["seniority_alignment"] = pairs.apply(
        lambda r: _compute_seniority_alignment(
            r.get(sen_res_col, ""), r.get(sen_job_col, "")
        ),
        axis=1,
    )

    # Industry match
    ind_job_col = "industry_job" if "industry_job" in pairs.columns else "industry"
    ind_res_col = "industry_resume" if "industry_resume" in pairs.columns else "industry"
    pairs["industry_match"] = pairs.apply(
        lambda r: _compute_industry_match(
            r.get(ind_res_col, ""), r.get(ind_job_col, "")
        ),
        axis=1,
    )

    # Estimate common relevance label
    pairs["common_relevance_label"] = pairs.apply(estimate_english_relevance, axis=1)

    # Add provenance columns
    pairs["source_dataset"] = "english"
    pairs["original_label"] = 1  # All are positive matches
    pairs["original_label_definition"] = "Present in relevant_resume_ids (positive match)"
    pairs["label_source"] = "original_match"
    pairs["mapping_status"] = "conditional_mapping"
    pairs["label_confidence"] = "medium"  # All conditional since method is unknown
    pairs["language"] = "en"

    return pairs


# ═══════════════════════════════════════════════════════════════════════════
# Harmonization Report
# ═══════════════════════════════════════════════════════════════════════════

def generate_harmonization_report(
    vn_harmonized: pd.DataFrame,
    en_harmonized: pd.DataFrame,
) -> str:
    """Generate the label harmonization report."""
    report = generate_report_header(
        "Label Harmonization Report",
        "Vietnamese & English Datasets → Common 0-3 Rubric"
    )

    # ── Vietnamese ──
    report += "## 1. Vietnamese Label Harmonization\n\n"
    report += f"**Total pairs:** {len(vn_harmonized):,}\n\n"

    # Original distribution
    report += "### Original Score Distribution (relevance_median_no_cohere)\n\n"
    orig_stats = vn_harmonized["original_label"].describe()
    report += f"- Mean: {orig_stats['mean']:.2f}, Median: {orig_stats['50%']:.2f}\n"
    report += f"- Min: {orig_stats['min']:.1f}, Max: {orig_stats['max']:.1f}\n\n"

    # Harmonized distribution
    report += "### Harmonized Label Distribution (0-3)\n\n"
    report += "| Label | Name | Count | Percentage |\n|---|---|---|---|\n"
    vn_dist = vn_harmonized["common_relevance_label"].value_counts().sort_index()
    for label in range(4):
        count = vn_dist.get(label, 0)
        pct = 100 * count / len(vn_harmonized)
        name = COMMON_RUBRIC[label]["name"]
        report += f"| {label} | {name} | {count:,} | {pct:.1f}% |\n"
    report += "\n"

    # Mapping status
    report += "### Mapping Status\n\n"
    report += "| Status | Count | Percentage |\n|---|---|---|\n"
    ms_dist = vn_harmonized["mapping_status"].value_counts()
    for status, count in ms_dist.items():
        pct = 100 * count / len(vn_harmonized)
        report += f"| {status} | {count:,} | {pct:.1f}% |\n"
    report += "\n"

    # Confidence
    report += "### Label Confidence\n\n"
    report += "| Confidence | Count | Percentage |\n|---|---|---|\n"
    conf_dist = vn_harmonized["label_confidence"].value_counts()
    for conf, count in conf_dist.items():
        pct = 100 * count / len(vn_harmonized)
        report += f"| {conf} | {count:,} | {pct:.1f}% |\n"
    report += "\n"

    # ── English ──
    report += "---\n\n## 2. English Label Harmonization\n\n"
    report += f"**Total positive pairs:** {len(en_harmonized):,}\n\n"

    # Harmonized distribution
    report += "### Harmonized Label Distribution (0-3)\n\n"
    report += "| Label | Name | Count | Percentage |\n|---|---|---|---|\n"
    en_dist = en_harmonized["common_relevance_label"].value_counts().sort_index()
    for label in range(4):
        count = en_dist.get(label, 0)
        pct = 100 * count / len(en_harmonized)
        name = COMMON_RUBRIC[label]["name"]
        report += f"| {label} | {name} | {count:,} | {pct:.1f}% |\n"
    report += "\n"

    # Feature analysis
    report += "### Structured Feature Analysis\n\n"
    report += "| Feature | Mean | Median |\n|---|---|---|\n"
    for feat in ["must_coverage", "nice_coverage"]:
        if feat in en_harmonized.columns:
            report += f"| {feat} | {en_harmonized[feat].mean():.3f} | {en_harmonized[feat].median():.3f} |\n"

    if "seniority_alignment" in en_harmonized.columns:
        report += "\n**Seniority alignment:**\n\n"
        sa_dist = en_harmonized["seniority_alignment"].value_counts()
        for val, count in sa_dist.items():
            report += f"- {val}: {count:,} ({100*count/len(en_harmonized):.1f}%)\n"

    if "industry_match" in en_harmonized.columns:
        report += "\n**Industry match:**\n\n"
        im_dist = en_harmonized["industry_match"].value_counts()
        for val, count in im_dist.items():
            report += f"- {val}: {count:,} ({100*count/len(en_harmonized):.1f}%)\n"
    report += "\n"

    # ── Combined Summary ──
    report += "---\n\n## 3. Combined Summary\n\n"
    total = len(vn_harmonized) + len(en_harmonized)
    report += f"**Total harmonized pairs:** {total:,}\n\n"
    report += "| Source | Pairs | % of Total |\n|---|---|---|\n"
    report += f"| Vietnamese | {len(vn_harmonized):,} | {100*len(vn_harmonized)/total:.1f}% |\n"
    report += f"| English | {len(en_harmonized):,} | {100*len(en_harmonized)/total:.1f}% |\n\n"

    # Combined label distribution
    all_labels = pd.concat([
        vn_harmonized["common_relevance_label"],
        en_harmonized["common_relevance_label"],
    ])
    report += "### Combined Label Distribution\n\n"
    report += "| Label | Name | Vietnamese | English | Total |\n|---|---|---|---|---|\n"
    for label in range(4):
        vn_count = vn_dist.get(label, 0)
        en_count = en_dist.get(label, 0)
        total_count = vn_count + en_count
        name = COMMON_RUBRIC[label]["name"]
        report += f"| {label} | {name} | {vn_count:,} | {en_count:,} | {total_count:,} |\n"
    report += "\n"

    # Label noise sources
    report += "### Potential Sources of Label Noise\n\n"
    report += "1. **Vietnamese:** LLM annotators may have inconsistent relevance definitions\n"
    report += "2. **Vietnamese:** Boundary scores (near 2.5, 4.5, 6.5) may be misclassified\n"
    report += "3. **Vietnamese:** CVs are synthetic — relevance may not reflect real-world patterns\n"
    report += "4. **English:** Unknown match criteria — feature-based grading may not match intent\n"
    report += "5. **English:** Exact string matching for skills understates actual overlap\n"
    report += "6. **Both:** No human validation performed yet\n\n"

    return report


def run_harmonization():
    """Run the full harmonization pipeline."""
    print("Harmonizing Vietnamese labels...")
    vn_harmonized = harmonize_vietnamese_labels()
    print(f"  → {len(vn_harmonized)} Vietnamese pairs harmonized")

    print("Harmonizing English labels...")
    en_harmonized = harmonize_english_labels()
    print(f"  → {len(en_harmonized)} English pairs harmonized")

    # Save processed datasets
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    vn_path = PROCESSED_DIR / "vietnamese_harmonized.csv"
    vn_harmonized.to_csv(vn_path, index=False, encoding="utf-8")
    print(f"  Saved: {vn_path}")

    # For English, save a smaller version (without full text) as CSV
    en_cols_to_save = [
        "job_id", "resume_id", "common_relevance_label",
        "source_dataset", "original_label", "original_label_definition",
        "label_source", "mapping_status", "label_confidence", "language",
        "must_coverage", "nice_coverage", "seniority_alignment", "industry_match",
    ]
    en_save_cols = [c for c in en_cols_to_save if c in en_harmonized.columns]
    en_path = PROCESSED_DIR / "english_harmonized.csv"
    en_harmonized[en_save_cols].to_csv(en_path, index=False, encoding="utf-8")
    print(f"  Saved: {en_path}")

    # Generate report
    print("Generating harmonization report...")
    report_text = generate_harmonization_report(vn_harmonized, en_harmonized)

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "06_label_harmonization.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report_text)
    print(f"  Report: {report_path}")

    return vn_harmonized, en_harmonized


if __name__ == "__main__":
    vn_h, en_h = run_harmonization()
    print("\nDone!")
