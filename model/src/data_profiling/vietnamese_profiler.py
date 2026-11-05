"""
Comprehensive data profiling for the Vietnamese CV-JD dataset.

Generates:
- Record counts & schema
- Label distribution analysis (LLM consensus scores on 1-9 scale)
- CV and JD statistics
- Duplicate detection
- Missing value analysis
- Text length distributions
- Inter-annotator agreement analysis
- Data quality issues summary
"""
import sys
import json
import pandas as pd
import numpy as np
from pathlib import Path
from collections import Counter

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR, VN_DATA_DIR
from src.data_ingestion.vietnamese_loader import (
    load_vn_jobs, load_vn_cvs, load_vn_labels,
    load_vn_annotations, load_vn_agreement,
)
from src.utils.helpers import generate_report_header, format_pct, format_number


def profile_vn_jobs(jobs: pd.DataFrame) -> dict:
    """Profile Vietnamese jobs dataset."""
    profile = {
        "total_records": len(jobs),
        "columns": list(jobs.columns),
        "dtypes": {c: str(jobs[c].dtype) for c in jobs.columns},
        "missing_values": jobs.isna().sum().to_dict(),
        "missing_pct": (jobs.isna().sum() / len(jobs) * 100).round(2).to_dict(),
    }

    # Category distribution
    if "category" in jobs.columns:
        profile["category_distribution"] = jobs["category"].value_counts().to_dict()
        profile["num_categories"] = jobs["category"].nunique()

    # Experience distribution
    if "experience" in jobs.columns:
        profile["experience_distribution"] = jobs["experience"].value_counts().to_dict()

    # Education level
    if "education_level" in jobs.columns:
        profile["education_level_distribution"] = (
            jobs["education_level"].value_counts().to_dict()
        )

    # Text length stats
    for col in ["job_description", "job_requirements", "title"]:
        if col in jobs.columns:
            lengths = jobs[col].dropna().str.len()
            profile[f"{col}_length"] = {
                "min": int(lengths.min()),
                "max": int(lengths.max()),
                "mean": round(float(lengths.mean()), 1),
                "median": round(float(lengths.median()), 1),
            }

    # Duplicates
    if "content_hash" in jobs.columns or "job_id" in jobs.columns:
        id_col = "content_hash" if "content_hash" in jobs.columns else "job_id"
        profile["duplicate_ids"] = int(len(jobs) - jobs[id_col].nunique())
        profile["unique_ids"] = int(jobs[id_col].nunique())

    # Check for duplicate job descriptions
    if "job_description" in jobs.columns:
        desc_dupes = jobs["job_description"].dropna().duplicated().sum()
        profile["duplicate_descriptions"] = int(desc_dupes)

    # Technical skills coverage
    if "technical_skills" in jobs.columns:
        non_null = jobs["technical_skills"].notna().sum()
        profile["technical_skills_coverage"] = format_pct(int(non_null), len(jobs))

    return profile


def profile_vn_cvs(cvs: pd.DataFrame) -> dict:
    """Profile Vietnamese CVs dataset."""
    profile = {
        "total_cvs": len(cvs),
        "columns": list(cvs.columns),
    }

    # Category distribution
    if "category_target" in cvs.columns:
        profile["category_distribution"] = cvs["category_target"].value_counts().to_dict()
        profile["num_categories"] = int(cvs["category_target"].nunique())

    # Level distribution
    if "level" in cvs.columns:
        profile["level_distribution"] = cvs["level"].value_counts().to_dict()
        profile["num_levels"] = int(cvs["level"].nunique())

    # Experience years
    if "experience_years" in cvs.columns:
        profile["experience_years"] = {
            "min": int(cvs["experience_years"].min()),
            "max": int(cvs["experience_years"].max()),
            "mean": round(float(cvs["experience_years"].mean()), 1),
            "distribution": cvs["experience_years"].value_counts().sort_index().to_dict(),
        }

    # Skills stats
    if "technical_skills" in cvs.columns:
        skill_counts = cvs["technical_skills"].apply(
            lambda x: len(x) if isinstance(x, list) else 0
        )
        profile["technical_skills_per_cv"] = {
            "min": int(skill_counts.min()),
            "max": int(skill_counts.max()),
            "mean": round(float(skill_counts.mean()), 1),
        }

    # Search text length
    if "search_text" in cvs.columns:
        lengths = cvs["search_text"].str.len()
        profile["search_text_length"] = {
            "min": int(lengths.min()),
            "max": int(lengths.max()),
            "mean": round(float(lengths.mean()), 1),
        }

    return profile


def profile_vn_labels(labels: pd.DataFrame) -> dict:
    """Profile Vietnamese labels dataset with LLM consensus scores."""
    profile = {
        "total_pairs": len(labels),
        "columns": list(labels.columns),
        "unique_cvs": int(labels["cv_id"].nunique()),
        "unique_jobs": int(labels["job_id"].nunique()),
        "missing_values": labels.isna().sum().to_dict(),
    }

    # Relevance score distributions (1-9 scale from LLM annotators)
    for col in [
        "relevance_median_4model",
        "relevance_mean_4model",
        "relevance_median_no_cohere",
        "relevance_mean_no_cohere",
    ]:
        if col in labels.columns:
            vals = labels[col].dropna()
            profile[f"{col}_stats"] = {
                "min": round(float(vals.min()), 2),
                "max": round(float(vals.max()), 2),
                "mean": round(float(vals.mean()), 2),
                "median": round(float(vals.median()), 2),
                "std": round(float(vals.std()), 2),
            }
            # Bin into ranges for distribution
            bins = [0, 2, 4, 6, 8, 10]
            bin_labels = ["1-2", "3-4", "5-6", "7-8", "9-10"]
            binned = pd.cut(vals, bins=bins, labels=bin_labels)
            profile[f"{col}_distribution"] = binned.value_counts().sort_index().to_dict()

    # Binary relevance thresholds
    for col in ["is_relevant_median_4model_ge5", "is_relevant_median_no_cohere_ge5"]:
        if col in labels.columns:
            profile[f"{col}_distribution"] = labels[col].value_counts().to_dict()

    # Range/agreement columns
    for col in [
        "range_relevance_4model",
        "range_relevance_no_cohere",
        "range_experience_4model",
        "range_experience_no_cohere",
    ]:
        if col in labels.columns:
            vals = labels[col].dropna()
            profile[f"{col}_stats"] = {
                "mean": round(float(vals.mean()), 2),
                "median": round(float(vals.median()), 2),
                "max": round(float(vals.max()), 2),
                "high_disagreement_count": int((vals > 4).sum()),
                "high_disagreement_pct": round(float((vals > 4).sum() / len(vals) * 100), 2),
            }

    # CV category distribution in labels
    if "cv_category" in labels.columns:
        profile["cv_category_distribution"] = labels["cv_category"].value_counts().to_dict()

    # Job category distribution in labels
    if "job_category" in labels.columns:
        profile["job_category_distribution"] = labels["job_category"].value_counts().to_dict()

    # CV level distribution
    if "cv_level" in labels.columns:
        profile["cv_level_distribution"] = labels["cv_level"].value_counts().to_dict()

    # Duplicate pairs
    if "cv_id" in labels.columns and "job_id" in labels.columns:
        dupes = labels.duplicated(subset=["cv_id", "job_id"], keep=False)
        profile["duplicate_cv_jd_pairs"] = int(dupes.sum())

    return profile


def profile_vn_annotations(annotations: dict[str, pd.DataFrame]) -> dict:
    """Profile individual LLM annotator outputs."""
    profile = {}

    for model_name, df in annotations.items():
        model_profile = {
            "total_annotations": len(df),
            "columns": list(df.columns),
        }

        # Relevance score stats
        if "relevance_score" in df.columns:
            vals = df["relevance_score"].dropna()
            model_profile["relevance_score"] = {
                "min": int(vals.min()),
                "max": int(vals.max()),
                "mean": round(float(vals.mean()), 2),
                "median": round(float(vals.median()), 2),
                "std": round(float(vals.std()), 2),
                "distribution": vals.value_counts().sort_index().to_dict(),
            }

        # Skills match stats
        if "skills_match" in df.columns:
            vals = df["skills_match"].dropna()
            model_profile["skills_match"] = {
                "mean": round(float(vals.mean()), 2),
                "median": round(float(vals.median()), 2),
            }

        # Experience match stats
        if "experience_match" in df.columns:
            vals = df["experience_match"].dropna()
            model_profile["experience_match"] = {
                "mean": round(float(vals.mean()), 2),
                "median": round(float(vals.median()), 2),
            }

        # Education match stats
        if "education_match" in df.columns:
            vals = df["education_match"].dropna()
            model_profile["education_match"] = {
                "mean": round(float(vals.mean()), 2),
                "median": round(float(vals.median()), 2),
            }

        # Justification stats
        if "justification" in df.columns:
            just_lens = df["justification"].dropna().str.len()
            model_profile["justification_length"] = {
                "min": int(just_lens.min()) if len(just_lens) > 0 else 0,
                "max": int(just_lens.max()) if len(just_lens) > 0 else 0,
                "mean": round(float(just_lens.mean()), 1) if len(just_lens) > 0 else 0,
                "missing": int(df["justification"].isna().sum()),
            }

        profile[model_name] = model_profile

    return profile


def analyze_agreement(agreement: dict) -> dict:
    """Analyze inter-annotator agreement summary."""
    analysis = {}

    if "metadata" in agreement:
        meta = agreement["metadata"]
        analysis["total_pairs"] = meta.get("total_pairs")
        analysis["total_cvs"] = meta.get("total_cvs")
        analysis["unique_jobs"] = meta.get("unique_jobs")
        analysis["models"] = meta.get("models")
        analysis["recommended_label"] = meta.get("recommended_label")
        analysis["relevant_threshold"] = meta.get("relevant_threshold")

    if "model_means" in agreement:
        analysis["model_means"] = agreement["model_means"]

    if "agreement_4model" in agreement:
        ag = agreement["agreement_4model"]
        analysis["agreement_4model"] = {}
        for metric, vals in ag.items():
            analysis["agreement_4model"][metric] = {
                "avg_range": vals.get("avg_range"),
                "median_range": vals.get("median_range"),
                "range_gt4_pct": vals.get("range_gt4_pct"),
                "max_range": vals.get("max_range"),
            }

    if "agreement_no_cohere" in agreement:
        ag = agreement["agreement_no_cohere"]
        analysis["agreement_no_cohere"] = {}
        for metric, vals in ag.items():
            if isinstance(vals, dict):
                analysis["agreement_no_cohere"][metric] = {
                    "avg_range": vals.get("avg_range"),
                    "median_range": vals.get("median_range"),
                    "range_gt4_pct": vals.get("range_gt4_pct"),
                }

    return analysis


def identify_quality_issues(
    jobs_profile: dict,
    cvs_profile: dict,
    labels_profile: dict,
    annotations_profile: dict,
    agreement_analysis: dict,
) -> list[dict]:
    """Identify data quality issues in the Vietnamese dataset."""
    issues = []

    # 1. Check for duplicate pairs
    dup_pairs = labels_profile.get("duplicate_cv_jd_pairs", 0)
    if dup_pairs > 0:
        issues.append({
            "severity": "HIGH",
            "category": "Duplicates",
            "description": f"{dup_pairs} duplicate (cv_id, job_id) pairs found in labels",
            "recommendation": "Investigate and deduplicate or resolve conflicting labels",
        })

    # 2. Check label coverage
    total_cvs = cvs_profile.get("total_cvs", 0)
    total_jobs = jobs_profile.get("total_records", 0)
    labels_cvs = labels_profile.get("unique_cvs", 0)
    labels_jobs = labels_profile.get("unique_jobs", 0)
    if labels_jobs < total_jobs:
        issues.append({
            "severity": "INFO",
            "category": "Coverage",
            "description": (
                f"Labels cover {labels_jobs}/{total_jobs} jobs "
                f"({100*labels_jobs/total_jobs:.1f}%)"
            ),
            "recommendation": "Document which jobs lack labels and why",
        })

    # 3. CVs are LLM-generated
    issues.append({
        "severity": "WARNING",
        "category": "Data Generation",
        "description": (
            "Vietnamese CVs were generated by LLM (llama-3.3-70b-versatile). "
            "They are synthetic, not real candidate resumes."
        ),
        "recommendation": (
            "Document this as a limitation. Real CV data would improve model reliability."
        ),
    })

    # 4. Annotations are LLM-generated
    issues.append({
        "severity": "WARNING",
        "category": "Label Quality",
        "description": (
            "All 4 annotators are LLM models (Claude, Cohere, GPToss, Llama). "
            "No human annotations exist."
        ),
        "recommendation": (
            "Create a human-validated test set as specified in the project requirements."
        ),
    })

    # 5. High disagreement cases
    for col in ["range_relevance_4model", "range_relevance_no_cohere"]:
        stats = labels_profile.get(f"{col}_stats", {})
        high_dis = stats.get("high_disagreement_pct", 0)
        if high_dis > 0:
            issues.append({
                "severity": "MEDIUM",
                "category": "Agreement",
                "description": (
                    f"{col}: {high_dis}% of pairs have range > 4 "
                    f"(high inter-annotator disagreement)"
                ),
                "recommendation": "Review high-disagreement samples for human relabeling",
            })

    # 6. Missing data in jobs
    missing = jobs_profile.get("missing_values", {})
    high_missing = {k: v for k, v in missing.items() if v > 100}
    if high_missing:
        issues.append({
            "severity": "LOW",
            "category": "Missing Data",
            "description": (
                f"Jobs have significant missing data: "
                + ", ".join(f"{k}={v}" for k, v in sorted(high_missing.items(), key=lambda x: -x[1])[:5])
            ),
            "recommendation": "Assess impact on relevance evaluation",
        })

    # 7. Empty ESCO files
    issues.append({
        "severity": "INFO",
        "category": "Taxonomy",
        "description": "ESCO skills_list.csv and skill_relations.csv are empty (0 rows)",
        "recommendation": "Use occupation_skills.csv and skill_broader_relations.csv instead",
    })

    return issues


def generate_vn_profiling_report() -> str:
    """Generate full Vietnamese dataset profiling report as markdown."""
    # Load data
    jobs = load_vn_jobs()
    cvs = load_vn_cvs()
    labels = load_vn_labels()
    annotations = load_vn_annotations()
    agreement = load_vn_agreement()

    # Profile
    jobs_profile = profile_vn_jobs(jobs)
    cvs_profile = profile_vn_cvs(cvs)
    labels_profile = profile_vn_labels(labels)
    annotations_profile = profile_vn_annotations(annotations)
    agreement_analysis = analyze_agreement(agreement)
    quality_issues = identify_quality_issues(
        jobs_profile, cvs_profile, labels_profile, annotations_profile, agreement_analysis
    )

    # Build report
    report = generate_report_header(
        "Vietnamese CV-JD Dataset Profiling Report",
        "Vietnamese Data (turn1300_v3_union_final)"
    )

    # ── Section 1: Jobs ──
    report += "## 1. Jobs Dataset\n\n"
    report += f"**Source:** `topcv_balanced_1300.csv`  \n"
    report += f"**Total Records:** {format_number(jobs_profile['total_records'])}  \n"
    report += f"**Columns:** {len(jobs_profile['columns'])}  \n\n"

    report += "### Schema\n\n"
    report += "| Column | Type | Missing |\n|---|---|---|\n"
    for col in jobs_profile["columns"]:
        dtype = jobs_profile["dtypes"].get(col, "?")
        missing = jobs_profile["missing_values"].get(col, 0)
        missing_pct = jobs_profile["missing_pct"].get(col, 0)
        report += f"| `{col}` | {dtype} | {missing} ({missing_pct}%) |\n"
    report += "\n"

    if "category_distribution" in jobs_profile:
        report += "### Category Distribution\n\n"
        report += "| Category | Count |\n|---|---|\n"
        for cat, count in sorted(jobs_profile["category_distribution"].items(), key=lambda x: -x[1]):
            report += f"| {cat} | {count} |\n"
        report += "\n"

    if "experience_distribution" in jobs_profile:
        report += "### Experience Distribution\n\n"
        report += "| Experience | Count |\n|---|---|\n"
        for exp, count in sorted(jobs_profile["experience_distribution"].items(), key=lambda x: -x[1]):
            report += f"| {exp} | {count} |\n"
        report += "\n"

    for col in ["job_description", "job_requirements", "title"]:
        key = f"{col}_length"
        if key in jobs_profile:
            stats = jobs_profile[key]
            report += f"### {col} Text Length\n\n"
            report += f"- Min: {stats['min']} chars\n"
            report += f"- Max: {stats['max']} chars\n"
            report += f"- Mean: {stats['mean']} chars\n"
            report += f"- Median: {stats['median']} chars\n\n"

    # ── Section 2: CVs ──
    report += "---\n\n## 2. CVs Dataset\n\n"
    report += f"**Source:** `evaluation_cvs_130_canonical.json`  \n"
    report += f"**Total CVs:** {cvs_profile['total_cvs']}  \n"
    report += f"**Generation Model:** llama-3.3-70b-versatile (LLM-generated, synthetic)  \n\n"

    report += "### Schema\n\n"
    report += f"Columns: {', '.join(f'`{c}`' for c in cvs_profile['columns'])}  \n\n"

    if "category_distribution" in cvs_profile:
        report += "### Category Distribution\n\n"
        report += "| Category | Count |\n|---|---|\n"
        for cat, count in sorted(cvs_profile["category_distribution"].items(), key=lambda x: -x[1]):
            report += f"| {cat} | {count} |\n"
        report += "\n"

    if "level_distribution" in cvs_profile:
        report += "### Level Distribution\n\n"
        report += "| Level | Count |\n|---|---|\n"
        for lvl, count in sorted(cvs_profile["level_distribution"].items()):
            report += f"| {lvl} | {count} |\n"
        report += "\n"

    # ── Section 3: Labels ──
    report += "---\n\n## 3. Labels Dataset (LLM Consensus)\n\n"
    report += f"**Source:** `labels_2450_compact.csv`  \n"
    report += f"**Total Pairs:** {format_number(labels_profile['total_pairs'])}  \n"
    report += f"**Unique CVs:** {labels_profile['unique_cvs']}  \n"
    report += f"**Unique Jobs:** {labels_profile['unique_jobs']}  \n"
    report += f"**Scale:** 1-9 continuous (LLM relevance scores)  \n\n"

    report += "### Relevance Score Statistics\n\n"
    report += "| Metric | Min | Max | Mean | Median | Std |\n|---|---|---|---|---|---|\n"
    for col in ["relevance_median_4model", "relevance_median_no_cohere",
                 "relevance_mean_4model", "relevance_mean_no_cohere"]:
        key = f"{col}_stats"
        if key in labels_profile:
            s = labels_profile[key]
            report += f"| `{col}` | {s['min']} | {s['max']} | {s['mean']} | {s['median']} | {s['std']} |\n"
    report += "\n"

    report += "### Relevance Score Distribution (binned)\n\n"
    for col in ["relevance_median_no_cohere", "relevance_median_4model"]:
        key = f"{col}_distribution"
        if key in labels_profile:
            report += f"**{col}:**\n\n"
            report += "| Range | Count |\n|---|---|\n"
            for rng, count in labels_profile[key].items():
                report += f"| {rng} | {count} |\n"
            report += "\n"

    report += "### Binary Relevance (threshold ≥ 5)\n\n"
    for col in ["is_relevant_median_4model_ge5", "is_relevant_median_no_cohere_ge5"]:
        key = f"{col}_distribution"
        if key in labels_profile:
            dist = labels_profile[key]
            report += f"- **{col}:** True={dist.get(True, 0)}, False={dist.get(False, 0)}\n"
    report += "\n"

    report += "### Inter-Annotator Disagreement (score range)\n\n"
    report += "| Metric | Mean Range | Median Range | High Disagree (>4) |\n|---|---|---|---|\n"
    for col in ["range_relevance_4model", "range_relevance_no_cohere"]:
        key = f"{col}_stats"
        if key in labels_profile:
            s = labels_profile[key]
            report += f"| `{col}` | {s['mean']} | {s['median']} | {s['high_disagreement_count']} ({s['high_disagreement_pct']}%) |\n"
    report += "\n"

    if "cv_category_distribution" in labels_profile:
        report += "### Pairs by CV Category\n\n"
        report += "| Category | Count |\n|---|---|\n"
        for cat, count in sorted(labels_profile["cv_category_distribution"].items(), key=lambda x: -x[1]):
            report += f"| {cat} | {count} |\n"
        report += "\n"

    # ── Section 4: Per-Model Annotations ──
    report += "---\n\n## 4. Per-Model Annotation Statistics\n\n"
    report += "| Model | Annotations | Relevance Mean | Relevance Median | Skills Mean | Exp. Mean |\n"
    report += "|---|---|---|---|---|---|\n"
    for model_name, mp in annotations_profile.items():
        rel = mp.get("relevance_score", {})
        sk = mp.get("skills_match", {})
        exp = mp.get("experience_match", {})
        report += (
            f"| {model_name} | {mp['total_annotations']} | "
            f"{rel.get('mean', 'N/A')} | {rel.get('median', 'N/A')} | "
            f"{sk.get('mean', 'N/A')} | {exp.get('mean', 'N/A')} |\n"
        )
    report += "\n"

    # Per-model relevance distribution
    report += "### Per-Model Relevance Score Distribution\n\n"
    for model_name, mp in annotations_profile.items():
        rel = mp.get("relevance_score", {})
        dist = rel.get("distribution", {})
        if dist:
            report += f"**{model_name}:**\n\n"
            report += "| Score | Count |\n|---|---|\n"
            for score, count in sorted(dist.items(), key=lambda x: x[0]):
                report += f"| {score} | {count} |\n"
            report += "\n"

    # ── Section 5: Agreement ──
    report += "---\n\n## 5. Inter-Annotator Agreement Analysis\n\n"
    if agreement_analysis.get("models"):
        report += f"**Models:** {', '.join(agreement_analysis['models'])}  \n"
    report += f"**Recommended Label:** `{agreement_analysis.get('recommended_label')}`  \n"
    report += f"**Relevant Threshold:** ≥ {agreement_analysis.get('relevant_threshold')}  \n\n"

    if "model_means" in agreement_analysis:
        report += "### Model Mean Scores\n\n"
        report += "| Model | Relevance | Skills | Experience | Education |\n|---|---|---|---|---|\n"
        for model, means in agreement_analysis["model_means"].items():
            report += (
                f"| {model} | {means.get('relevance_score', 'N/A'):.2f} | "
                f"{means.get('skills_match', 'N/A'):.2f} | "
                f"{means.get('experience_match', 'N/A'):.2f} | "
                f"{means.get('education_match', 'N/A'):.2f} |\n"
            )
        report += "\n"

    if "agreement_4model" in agreement_analysis:
        report += "### 4-Model Agreement\n\n"
        report += "| Metric | Avg Range | Median Range | High Disagree (>4) % |\n|---|---|---|---|\n"
        for metric, vals in agreement_analysis["agreement_4model"].items():
            report += (
                f"| {metric} | {vals.get('avg_range', 'N/A')} | "
                f"{vals.get('median_range', 'N/A')} | "
                f"{vals.get('range_gt4_pct', 'N/A')}% |\n"
            )
        report += "\n"

    # ── Section 6: Quality Issues ──
    report += "---\n\n## 6. Data Quality Issues\n\n"
    report += "| # | Severity | Category | Description | Recommendation |\n|---|---|---|---|---|\n"
    for i, issue in enumerate(quality_issues, 1):
        sev_icon = {"HIGH": "🔴", "MEDIUM": "🟠", "WARNING": "🟡", "LOW": "🔵", "INFO": "ℹ️"}.get(
            issue["severity"], "❓"
        )
        report += (
            f"| {i} | {sev_icon} {issue['severity']} | {issue['category']} | "
            f"{issue['description']} | {issue['recommendation']} |\n"
        )
    report += "\n"

    return report, {
        "jobs_profile": jobs_profile,
        "cvs_profile": cvs_profile,
        "labels_profile": labels_profile,
        "annotations_profile": annotations_profile,
        "agreement_analysis": agreement_analysis,
        "quality_issues": quality_issues,
    }


if __name__ == "__main__":
    report_text, profile_data = generate_vn_profiling_report()

    # Save report
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "01_vietnamese_data_profiling.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report_text)

    # Save profile data as JSON
    json_path = REPORTS_DIR / "01_vietnamese_data_profiling.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(profile_data, f, ensure_ascii=False, indent=2, default=str)

    print(f"Report saved to: {report_path}")
    print(f"Profile data saved to: {json_path}")
