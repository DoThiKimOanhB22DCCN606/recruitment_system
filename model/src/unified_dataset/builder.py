"""
Unified Dataset Construction (Milestone 5)

Builds a unified schema combining Vietnamese and English data.
Constructs the candidate_text and job_text fields.
Generates candidate pairs with negative sampling.
"""
import sys
import json
import pandas as pd
import numpy as np
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR, PROCESSED_DIR
from src.data_ingestion.vietnamese_loader import load_vn_cvs, load_vn_jobs
from src.data_ingestion.english_loader import (
    load_en_jobs, load_en_resumes, load_en_matches, explode_matches,
)
from src.utils.helpers import generate_report_header


# ═══════════════════════════════════════════════════════════════════════════
# Text Construction Helpers
# ═══════════════════════════════════════════════════════════════════════════

def build_vn_cv_text(cv_row: pd.Series) -> str:
    """Build CV text from Vietnamese CV structured fields."""
    parts = []
    if pd.notna(cv_row.get("search_text")):
        return str(cv_row["search_text"])

    # Fallback: build from fields
    if pd.notna(cv_row.get("experience_description")):
        parts.append(f"Kinh nghiệm: {cv_row['experience_description']}")
    if pd.notna(cv_row.get("education_level")):
        edu = cv_row["education_level"]
        field = cv_row.get("education_field", "")
        parts.append(f"Học vấn: {edu} {field}".strip())
    if isinstance(cv_row.get("technical_skills"), list):
        parts.append(f"Kỹ năng: {', '.join(cv_row['technical_skills'])}")
    if isinstance(cv_row.get("soft_skills"), list) and cv_row["soft_skills"]:
        parts.append(f"Kỹ năng mềm: {', '.join(cv_row['soft_skills'])}")
    return ". ".join(parts) if parts else ""


def build_vn_jd_text(job_row: pd.Series) -> str:
    """Build JD text from Vietnamese job structured fields."""
    parts = []
    if pd.notna(job_row.get("title")):
        parts.append(f"Vị trí: {job_row['title']}")
    if pd.notna(job_row.get("job_description")):
        parts.append(str(job_row["job_description"]))
    if pd.notna(job_row.get("job_requirements")):
        parts.append(str(job_row["job_requirements"]))
    return " ".join(parts) if parts else ""


def build_en_cv_text(resume_row: pd.Series) -> str:
    """Build CV text from English resume structured fields."""
    parts = []
    if pd.notna(resume_row.get("role")):
        parts.append(f"Role: {resume_row['role']}")
    if pd.notna(resume_row.get("seniority")):
        parts.append(f"Seniority: {resume_row['seniority']}")
    if pd.notna(resume_row.get("summary")):
        parts.append(str(resume_row["summary"]))
    if isinstance(resume_row.get("skills"), (list, np.ndarray)):
        skills = [s for s in resume_row["skills"] if isinstance(s, str)]
        if skills:
            parts.append(f"Skills: {', '.join(skills)}")
    if isinstance(resume_row.get("experience_bullets"), (list, np.ndarray)):
        bullets = [b for b in resume_row["experience_bullets"] if isinstance(b, str)]
        if bullets:
            parts.append("Experience: " + " | ".join(bullets))
    if pd.notna(resume_row.get("education")):
        parts.append(f"Education: {resume_row['education']}")
    return " ".join(parts)


def build_en_jd_text(job_row: pd.Series) -> str:
    """Build JD text from English job structured fields."""
    parts = []
    if pd.notna(job_row.get("job_title")):
        parts.append(f"Title: {job_row['job_title']}")
    if pd.notna(job_row.get("seniority")):
        parts.append(f"Seniority: {job_row['seniority']}")
    if pd.notna(job_row.get("description")):
        parts.append(str(job_row["description"]))
    if isinstance(job_row.get("must_have_skills"), (list, np.ndarray)):
        skills = [s for s in job_row["must_have_skills"] if isinstance(s, str)]
        if skills:
            parts.append(f"Required Skills: {', '.join(skills)}")
    if isinstance(job_row.get("nice_to_have_skills"), (list, np.ndarray)):
        skills = [s for s in job_row["nice_to_have_skills"] if isinstance(s, str)]
        if skills:
            parts.append(f"Preferred Skills: {', '.join(skills)}")
    if isinstance(job_row.get("responsibilities"), (list, np.ndarray)):
        items = [r for r in job_row["responsibilities"] if isinstance(r, str)]
        if items:
            parts.append("Responsibilities: " + " | ".join(items))
    if isinstance(job_row.get("requirements"), (list, np.ndarray)):
        items = [r for r in job_row["requirements"] if isinstance(r, str)]
        if items:
            parts.append("Requirements: " + " | ".join(items))
    return " ".join(parts)


# ═══════════════════════════════════════════════════════════════════════════
# Unified Dataset Builder
# ═══════════════════════════════════════════════════════════════════════════

def build_vietnamese_unified() -> pd.DataFrame:
    """Build unified Vietnamese pairs with text fields."""
    # Load harmonized labels
    vn_harm = pd.read_csv(PROCESSED_DIR / "vietnamese_harmonized.csv", encoding="utf-8")
    cvs = load_vn_cvs()
    jobs = load_vn_jobs()

    # Build CV texts
    cv_texts = {}
    for _, row in cvs.iterrows():
        cv_texts[row["cv_id"]] = build_vn_cv_text(row)

    # Build JD texts
    jd_texts = {}
    # The job_id in labels is content_hash
    if "content_hash" in jobs.columns:
        id_col = "content_hash"
    elif "job_id" in jobs.columns:
        id_col = "job_id"
    else:
        id_col = jobs.columns[0]

    for _, row in jobs.iterrows():
        jd_texts[row[id_col]] = build_vn_jd_text(row)

    # Build unified DataFrame
    records = []
    for _, row in vn_harm.iterrows():
        cv_id = row["cv_id"]
        job_id = row["job_id"]

        record = {
            "candidate_id": cv_id,
            "job_id": job_id,
            "candidate_text": cv_texts.get(cv_id, ""),
            "job_text": jd_texts.get(job_id, ""),
            "language": "vi",
            "source_dataset": "vietnamese",
            "original_label": row.get("original_label"),
            "common_relevance_label": row.get("common_relevance_label"),
            "label_source": row.get("label_source", "llm_consensus"),
            "label_confidence": row.get("label_confidence", "medium"),
            "mapping_status": row.get("mapping_status", "conditional_mapping"),
        }

        # Add structured fields if available from CVs
        cv_data = cvs[cvs["cv_id"] == cv_id]
        if len(cv_data) > 0:
            cv_row = cv_data.iloc[0]
            record["candidate_skills"] = (
                json.dumps(cv_row.get("technical_skills", []), ensure_ascii=False)
                if isinstance(cv_row.get("technical_skills"), list) else "[]"
            )
            record["candidate_seniority"] = str(cv_row.get("level", ""))
            record["candidate_industry"] = str(cv_row.get("category_target", ""))
            record["education"] = str(cv_row.get("education_level", ""))
            record["experience_years"] = cv_row.get("experience_years")

        # Add job structured fields
        job_data = jobs[jobs[id_col] == job_id]
        if len(job_data) > 0:
            j_row = job_data.iloc[0]
            record["job_must_have_skills"] = str(j_row.get("technical_skills", ""))
            record["job_nice_to_have_skills"] = ""
            record["job_seniority"] = str(j_row.get("experience", ""))
            record["job_industry"] = str(j_row.get("category", ""))

        records.append(record)

    return pd.DataFrame(records)


def build_english_unified() -> pd.DataFrame:
    """Build unified English pairs with text fields."""
    en_harm = pd.read_csv(PROCESSED_DIR / "english_harmonized.csv", encoding="utf-8")
    jobs = load_en_jobs()
    resumes = load_en_resumes()

    # Build text caches
    cv_texts = {}
    for _, row in resumes.iterrows():
        cv_texts[row["resume_id"]] = build_en_cv_text(row)

    jd_texts = {}
    for _, row in jobs.iterrows():
        jd_texts[row["job_id"]] = build_en_jd_text(row)

    # Build unified records
    records = []
    for _, row in en_harm.iterrows():
        job_id = row["job_id"]
        resume_id = row["resume_id"]

        record = {
            "candidate_id": resume_id,
            "job_id": job_id,
            "candidate_text": cv_texts.get(resume_id, ""),
            "job_text": jd_texts.get(job_id, ""),
            "language": "en",
            "source_dataset": "english",
            "original_label": row.get("original_label"),
            "common_relevance_label": row.get("common_relevance_label"),
            "label_source": row.get("label_source", "original_match"),
            "label_confidence": row.get("label_confidence", "medium"),
            "mapping_status": row.get("mapping_status", "conditional_mapping"),
        }

        # Add resume structured fields
        res_data = resumes[resumes["resume_id"] == resume_id]
        if len(res_data) > 0:
            r = res_data.iloc[0]
            record["candidate_skills"] = (
                json.dumps(list(r["skills"]), ensure_ascii=False)
                if isinstance(r.get("skills"), (list, np.ndarray)) else "[]"
            )
            record["candidate_seniority"] = str(r.get("seniority", ""))
            record["candidate_industry"] = str(r.get("industry", ""))
            record["education"] = str(r.get("education", ""))
            record["experience_years"] = r.get("years_experience")

        # Add job structured fields
        job_data = jobs[jobs["job_id"] == job_id]
        if len(job_data) > 0:
            j = job_data.iloc[0]
            record["job_must_have_skills"] = (
                json.dumps(list(j["must_have_skills"]), ensure_ascii=False)
                if isinstance(j.get("must_have_skills"), (list, np.ndarray)) else "[]"
            )
            record["job_nice_to_have_skills"] = (
                json.dumps(list(j["nice_to_have_skills"]), ensure_ascii=False)
                if isinstance(j.get("nice_to_have_skills"), (list, np.ndarray)) else "[]"
            )
            record["job_seniority"] = str(j.get("seniority", ""))
            record["job_industry"] = str(j.get("industry", ""))

        records.append(record)

    return pd.DataFrame(records)


def build_unified_dataset() -> pd.DataFrame:
    """Build the complete unified dataset."""
    print("Building Vietnamese unified pairs...")
    vn_unified = build_vietnamese_unified()
    print(f"  → {len(vn_unified)} Vietnamese pairs")

    print("Building English unified pairs...")
    en_unified = build_english_unified()
    print(f"  → {len(en_unified)} English pairs")

    # Combine
    print("Combining datasets...")
    unified = pd.concat([vn_unified, en_unified], ignore_index=True)
    print(f"  → {len(unified)} total unified pairs")

    return unified, vn_unified, en_unified


if __name__ == "__main__":
    unified, vn_u, en_u = build_unified_dataset()

    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    # Save unified
    unified_path = PROCESSED_DIR / "unified_dataset.csv"
    unified.to_csv(unified_path, index=False, encoding="utf-8")
    print(f"Saved unified dataset: {unified_path}")

    # Report
    print(f"\nUnified Dataset Summary:")
    print(f"  Total pairs: {len(unified):,}")
    print(f"  Vietnamese: {len(vn_u):,}")
    print(f"  English: {len(en_u):,}")
    print(f"  Label distribution:")
    for label in sorted(unified["common_relevance_label"].unique()):
        count = (unified["common_relevance_label"] == label).sum()
        pct = 100 * count / len(unified)
        print(f"    Label {label}: {count:,} ({pct:.1f}%)")
