"""
build_training_data.py — Re-process normalized datasets into training.md spec format.

Generates:
  - data/processed/cv.parquet
  - data/processed/jd.parquet
  - data/processed/cv_jd_pairs.parquet
  - data/splits/train.parquet, validation.parquet, test.parquet
  - data/dataset_metadata.json

Follows training.md specification:
  - 3-level labels (0=Not suitable, 1=Partially suitable, 2=Highly suitable)
  - pair_type (positive, negative, hard_negative)
  - Entity-based splitting (no candidate leaks across splits)
  - Skill normalization via skill_aliases.json
  - All validation checks from §11
"""

import os
import re
import json
import numpy as np
import pandas as pd
from datetime import datetime

# ── Paths ────────────────────────────────────────────────────────────────────
BASE_DIR   = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # model/
ROOT_DIR   = os.path.dirname(BASE_DIR)                                    # graduation-project/
NORM_DIR   = os.path.join(BASE_DIR, "normalized")
PROC_DIR   = os.path.join(BASE_DIR, "data", "processed")
SPLIT_DIR  = os.path.join(BASE_DIR, "data", "splits")
ALIAS_PATH = os.path.join(BASE_DIR, "preprocessing", "skill_aliases.json")

os.makedirs(PROC_DIR, exist_ok=True)
os.makedirs(SPLIT_DIR, exist_ok=True)

DATASET_VERSION = "dataset-v1.0"

# ── Utilities ────────────────────────────────────────────────────────────────

def load_skill_aliases():
    """Load skill normalization dictionary."""
    with open(ALIAS_PATH, "r", encoding="utf-8") as f:
        aliases = json.load(f)
    # Remove metadata key
    aliases.pop("_metadata", None)
    return aliases

def normalize_skill(skill, aliases):
    """Normalize a single skill string using the alias dict."""
    s = skill.strip()
    if s in aliases:
        return aliases[s]
    return s

def normalize_skill_list(skills, aliases):
    """Normalize and deduplicate a list of skills."""
    if not isinstance(skills, (list, np.ndarray)):
        return []
    normalized = []
    seen = set()
    for s in skills:
        if isinstance(s, str) and s.strip():
            ns = normalize_skill(s, aliases)
            if ns not in seen:
                normalized.append(ns)
                seen.add(ns)
    return normalized

def clean_text(text):
    """Clean text: strip HTML, normalize whitespace."""
    if not text or not isinstance(text, str) or pd.isna(text):
        return ""
    text = re.sub(r'<[^>]+>', ' ', text)
    text = re.sub(r'[\r\n\t]+', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def safe_list(val):
    """Ensure value is a Python list."""
    if isinstance(val, np.ndarray):
        return val.tolist()
    if isinstance(val, list):
        return val
    if pd.isna(val) if isinstance(val, float) else not val:
        return []
    return [val]

# ── CV Text Template (training.md §6) ────────────────────────────────────────

def build_cv_text(row):
    """Build cv_text from structured fields using the spec template."""
    parts = ["Candidate Profile", ""]

    skills = row.get("skills", [])
    if skills:
        parts.append(f"Skills:\n{', '.join(skills)}")
    else:
        parts.append("Skills:\n(none)")

    exp = row.get("experience", [])
    if exp:
        exp_lines = []
        for e in exp:
            if isinstance(e, dict):
                title = e.get("title", "")
                years = e.get("years", "")
                eskills = e.get("skills", [])
                line = f"- {title}"
                if years:
                    line += f" ({years} years)"
                if eskills:
                    line += f" | Skills: {', '.join(eskills)}"
                exp_lines.append(line)
            elif isinstance(e, str):
                exp_lines.append(f"- {e}")
        parts.append(f"Experience:\n" + "\n".join(exp_lines))
    else:
        parts.append("Experience:\n(none)")

    edu = row.get("education", [])
    if edu:
        if isinstance(edu, str):
            parts.append(f"Education:\n{edu}")
        elif isinstance(edu, list):
            edu_lines = []
            for ed in edu:
                if isinstance(ed, dict):
                    deg = ed.get("degree", "")
                    major = ed.get("major", "")
                    edu_lines.append(f"- {deg} in {major}" if major else f"- {deg}")
                elif isinstance(ed, str):
                    edu_lines.append(f"- {ed}")
            parts.append(f"Education:\n" + "\n".join(edu_lines))
    else:
        parts.append("Education:\n(none)")

    certs = row.get("certifications", [])
    if certs:
        parts.append(f"Certifications:\n{', '.join(certs)}")

    langs = row.get("languages", [])
    if langs:
        parts.append(f"Languages:\n{', '.join(langs)}")

    return "\n\n".join(parts)

# ── JD Text Template (training.md §6) ────────────────────────────────────────

def build_jd_text(row):
    """Build job_text from structured fields using the spec template."""
    parts = []

    title = row.get("title", "")
    parts.append(f"Job Title:\n{title}" if title else "Job Title:\n(not specified)")

    req_skills = row.get("required_skills", [])
    if req_skills:
        parts.append(f"Required Skills:\n{', '.join(req_skills)}")

    pref_skills = row.get("preferred_skills", [])
    if pref_skills:
        parts.append(f"Preferred Skills:\n{', '.join(pref_skills)}")

    min_exp = row.get("min_experience_years")
    if min_exp is not None and not pd.isna(min_exp):
        parts.append(f"Minimum Experience:\n{int(min_exp)} years")

    edu = row.get("education", [])
    if edu:
        if isinstance(edu, list):
            parts.append(f"Education:\n{', '.join(edu)}")
        elif isinstance(edu, str) and edu:
            parts.append(f"Education:\n{edu}")

    loc = row.get("location", "")
    if loc:
        parts.append(f"Location:\n{loc}")

    return "\n\n".join(parts)

# ── Phase 1: Build cv.parquet ────────────────────────────────────────────────

def build_cv_dataset(aliases):
    """Build standardized CV dataset from normalized resumes."""
    print("\n=== Building cv.parquet ===")

    # Load benchmark resumes
    resumes_df = pd.read_parquet(os.path.join(NORM_DIR, "resumes_normalized.parquet"))
    master_df  = pd.read_parquet(os.path.join(NORM_DIR, "master_resumes_normalized.parquet"))

    cv_records = []

    # Process benchmark resumes
    for _, row in resumes_df.iterrows():
        skills = normalize_skill_list(safe_list(row.get("skills", [])), aliases)
        exp_bullets = safe_list(row.get("experience_bullets", []))
        years_exp = row.get("years_experience", 0)
        role = clean_text(row.get("role", ""))

        experience = []
        if role:
            experience.append({
                "title": role,
                "years": float(years_exp) if years_exp else 0.0,
                "skills": skills[:5]  # top skills for the role
            })
        for bullet in exp_bullets:
            if isinstance(bullet, str) and bullet.strip():
                experience.append({
                    "title": bullet.strip()[:100],
                    "years": 0.0,
                    "skills": []
                })

        edu_raw = clean_text(row.get("education", ""))
        education = [{"degree": edu_raw, "major": clean_text(row.get("industry", ""))}] if edu_raw else []

        record = {
            "candidate_id": str(row["resume_id"]),
            "skills": skills,
            "experience": experience,
            "education": education,
            "certifications": [],
            "languages": [],
        }
        record["cv_text"] = build_cv_text(record)
        cv_records.append(record)

    # Process master resumes
    for _, row in master_df.iterrows():
        skills = normalize_skill_list(safe_list(row.get("skills", [])), aliases)
        exp_bullets = safe_list(row.get("experience_bullets", []))
        role = clean_text(row.get("role", ""))

        experience = []
        if role:
            experience.append({"title": role, "years": 0.0, "skills": skills[:5]})
        for bullet in exp_bullets:
            if isinstance(bullet, str) and bullet.strip():
                experience.append({"title": bullet.strip()[:100], "years": 0.0, "skills": []})

        edu_raw = clean_text(row.get("education", ""))
        education = [{"degree": edu_raw, "major": ""}] if edu_raw else []

        record = {
            "candidate_id": str(row["resume_id"]),
            "skills": skills,
            "experience": experience,
            "education": education,
            "certifications": [],
            "languages": [],
        }
        record["cv_text"] = build_cv_text(record)
        cv_records.append(record)

    cv_df = pd.DataFrame(cv_records)
    cv_df["candidate_id"] = cv_df["candidate_id"].astype("string")

    # Deduplicate by candidate_id
    cv_df = cv_df.drop_duplicates(subset=["candidate_id"], keep="first")

    out_path = os.path.join(PROC_DIR, "cv.parquet")
    cv_df.to_parquet(out_path, engine="pyarrow", index=False)
    print(f"  Saved {cv_df.shape} -> {out_path}")
    return cv_df

# ── Phase 2: Build jd.parquet ────────────────────────────────────────────────

def build_jd_dataset(aliases):
    """Build standardized JD dataset from normalized jobs."""
    print("\n=== Building jd.parquet ===")

    jobs_df = pd.read_parquet(os.path.join(NORM_DIR, "jobs_normalized.parquet"))

    jd_records = []
    for _, row in jobs_df.iterrows():
        req_skills  = normalize_skill_list(safe_list(row.get("must_have_skills", [])), aliases)
        pref_skills = normalize_skill_list(safe_list(row.get("nice_to_have_skills", [])), aliases)
        reqs        = safe_list(row.get("requirements", []))

        # Try to extract min_experience_years from requirements text
        min_exp = None
        for r in reqs:
            if isinstance(r, str):
                match = re.search(r'(\d+)\+?\s*(?:years?|năm)', r, re.IGNORECASE)
                if match:
                    min_exp = float(match.group(1))
                    break

        edu_raw = []
        for r in reqs:
            if isinstance(r, str):
                for deg in ["Bachelor", "Master", "PhD", "BSc", "MSc", "MBA", "Associate"]:
                    if deg.lower() in r.lower():
                        edu_raw.append(deg)
                        break

        record = {
            "job_id": str(row["job_id"]),
            "title": clean_text(row.get("job_title", "")),
            "required_skills": req_skills,
            "preferred_skills": pref_skills,
            "min_experience_years": min_exp,
            "education": edu_raw,
            "location": "",
        }
        record["job_text"] = build_jd_text(record)
        jd_records.append(record)

    jd_df = pd.DataFrame(jd_records)
    jd_df["job_id"] = jd_df["job_id"].astype("string")
    jd_df["min_experience_years"] = jd_df["min_experience_years"].astype("float32")

    jd_df = jd_df.drop_duplicates(subset=["job_id"], keep="first")

    out_path = os.path.join(PROC_DIR, "jd.parquet")
    jd_df.to_parquet(out_path, engine="pyarrow", index=False)
    print(f"  Saved {jd_df.shape} -> {out_path}")
    return jd_df

# ── Phase 3: Build cv_jd_pairs.parquet ───────────────────────────────────────

def compute_skill_overlap(cv_skills, jd_skills):
    """Compute fraction of JD required skills found in CV skills."""
    if not jd_skills:
        return 0.0
    cv_set = set(s.lower() for s in cv_skills)
    jd_set = set(s.lower() for s in jd_skills)
    if not jd_set:
        return 0.0
    return len(cv_set & jd_set) / len(jd_set)

def build_pairs_dataset(cv_df, jd_df, aliases):
    """Build CV-JD matching pair dataset with 3-level labels and hard negatives."""
    print("\n=== Building cv_jd_pairs.parquet ===")

    np.random.seed(42)

    # Build lookup maps
    cv_map   = dict(zip(cv_df["candidate_id"], cv_df["cv_text"]))
    cv_skills_map = {}
    for _, row in cv_df.iterrows():
        cv_skills_map[row["candidate_id"]] = row["skills"] if isinstance(row["skills"], list) else []

    jd_map   = dict(zip(jd_df["job_id"], jd_df["job_text"]))
    jd_skills_map = {}
    for _, row in jd_df.iterrows():
        jd_skills_map[row["job_id"]] = row["required_skills"] if isinstance(row["required_skills"], list) else []

    pairs = []
    pair_seen = set()

    # ── Source 1: Benchmark matches (high quality, label=2 positive) ──
    print("  Processing benchmark matches...")
    matches_df = pd.read_parquet(
        os.path.join(BASE_DIR, "dataset", "dataset", "matches", "train-00000-of-00001.parquet")
    )

    all_cv_ids = list(cv_df["candidate_id"])
    all_jd_ids = list(jd_df["job_id"])

    for _, row in matches_df.iterrows():
        jid = str(row["job_id"])
        relevant = row["relevant_resume_ids"]
        if isinstance(relevant, np.ndarray):
            relevant = relevant.tolist()
        if not isinstance(relevant, list):
            continue

        jd_text   = jd_map.get(jid, "")
        jd_skills = jd_skills_map.get(jid, [])

        for rid in relevant:
            rid = str(rid)
            if rid not in cv_map:
                continue
            pair_key = (rid, jid)
            if pair_key in pair_seen:
                continue
            pair_seen.add(pair_key)

            pairs.append({
                "candidate_id": rid,
                "job_id": jid,
                "cv_text": cv_map[rid],
                "job_text": jd_text,
                "label": 2,
                "pair_type": "positive",
                "dataset_version": DATASET_VERSION,
            })

        # Generate hard negatives: CVs with partial skill overlap
        for _ in range(min(5, len(relevant))):
            neg_cid = np.random.choice(all_cv_ids)
            neg_cid = str(neg_cid)
            if neg_cid in set(str(r) for r in relevant):
                continue
            pair_key = (neg_cid, jid)
            if pair_key in pair_seen:
                continue
            pair_seen.add(pair_key)

            cv_skills = cv_skills_map.get(neg_cid, [])
            overlap = compute_skill_overlap(cv_skills, jd_skills)

            if overlap >= 0.3:
                # Hard negative: some skill overlap but not a real match
                pairs.append({
                    "candidate_id": neg_cid,
                    "job_id": jid,
                    "cv_text": cv_map.get(neg_cid, ""),
                    "job_text": jd_text,
                    "label": 1,
                    "pair_type": "hard_negative",
                    "dataset_version": DATASET_VERSION,
                })
            else:
                # Random negative: no meaningful overlap
                pairs.append({
                    "candidate_id": neg_cid,
                    "job_id": jid,
                    "cv_text": cv_map.get(neg_cid, ""),
                    "job_text": jd_text,
                    "label": 0,
                    "pair_type": "negative",
                    "dataset_version": DATASET_VERSION,
                })

        # Also generate pure random negatives
        for _ in range(min(5, len(relevant))):
            neg_cid = np.random.choice(all_cv_ids)
            neg_cid = str(neg_cid)
            pair_key = (neg_cid, jid)
            if pair_key in pair_seen:
                continue
            pair_seen.add(pair_key)

            pairs.append({
                "candidate_id": neg_cid,
                "job_id": jid,
                "cv_text": cv_map.get(neg_cid, ""),
                "job_text": jd_text,
                "label": 0,
                "pair_type": "negative",
                "dataset_version": DATASET_VERSION,
            })

    # ── Source 2: Applicant CSV (Accepted→2, Rejected→0) ──
    print("  Processing applicant CSV...")
    csv_path = os.path.join(BASE_DIR, "dataset (1)", "job_applicant_dataset.csv")
    if os.path.exists(csv_path):
        app_df = pd.read_csv(csv_path)
        for idx, row in app_df.iterrows():
            cid = f"CSV_C_{idx:06d}"
            jid = f"CSV_J_{idx:06d}"
            resume_text = clean_text(str(row.get("Resume", "")))
            role = clean_text(str(row.get("Job Roles", "")))
            jd_desc = clean_text(str(row.get("Job Description", "")))
            best_match = row.get("Best Match", 0)

            # Build texts using spec templates
            cv_text = f"Candidate Profile\n\nSkills:\n{resume_text}"
            job_text = f"Job Title:\n{role}\n\nRequired Skills:\n(see description)\n\nDescription:\n{jd_desc}"

            label = 2 if best_match == 1 else 0
            pair_type = "positive" if best_match == 1 else "negative"

            pair_key = (cid, jid)
            if pair_key in pair_seen:
                continue
            pair_seen.add(pair_key)

            pairs.append({
                "candidate_id": cid,
                "job_id": jid,
                "cv_text": cv_text,
                "job_text": job_text,
                "label": label,
                "pair_type": pair_type,
                "dataset_version": DATASET_VERSION,
            })

    # ── Source 3: train.json (Accepted→2, Rejected→0) ──
    print("  Processing train.json...")
    tj_path = os.path.join(ROOT_DIR, "train.json")
    if os.path.exists(tj_path):
        with open(tj_path, "r", encoding="utf-8") as f:
            train_data = json.load(f)

        for idx, item in enumerate(train_data):
            cid = f"TJ_C_{idx:06d}"
            jid = f"TJ_J_{idx:06d}"

            pos   = clean_text(item.get("opening_position", ""))
            desc  = clean_text(item.get("description", ""))
            req   = clean_text(item.get("requirement", ""))
            kw_job = item.get("keywords_for_recruitment", [])
            if isinstance(kw_job, list):
                kw_job = [clean_text(str(k)) for k in kw_job]

            cv_raw  = clean_text(item.get("cv", ""))
            exp_y   = item.get("experience_years", "")
            kw_cv   = item.get("keywords_from_applicants", [])
            if isinstance(kw_cv, list):
                kw_cv = [clean_text(str(k)) for k in kw_cv]

            result = str(item.get("result", "")).strip().lower()
            label = 2 if result == "accepted" else 0
            pair_type = "positive" if result == "accepted" else "negative"

            # Build using spec templates
            cv_text = f"Candidate Profile\n\nSkills:\n{', '.join(kw_cv)}\n\nExperience:\n- {exp_y} years\n\nCV:\n{cv_raw}"
            job_text = f"Job Title:\n{pos}\n\nRequired Skills:\n{', '.join(kw_job)}\n\nDescription:\n{desc}\n\nRequirements:\n{req}"

            pair_key = (cid, jid)
            if pair_key in pair_seen:
                continue
            pair_seen.add(pair_key)

            pairs.append({
                "candidate_id": cid,
                "job_id": jid,
                "cv_text": cv_text,
                "job_text": job_text,
                "label": label,
                "pair_type": pair_type,
                "dataset_version": DATASET_VERSION,
            })

    # Build DataFrame
    pairs_df = pd.DataFrame(pairs)

    # Clean up: remove empty texts
    pairs_df = pairs_df[
        (pairs_df["cv_text"].str.strip() != "") &
        (pairs_df["job_text"].str.strip() != "")
    ].copy()

    # Set proper types
    pairs_df["candidate_id"]    = pairs_df["candidate_id"].astype("string")
    pairs_df["job_id"]          = pairs_df["job_id"].astype("string")
    pairs_df["label"]           = pairs_df["label"].astype("int8")
    pairs_df["pair_type"]       = pairs_df["pair_type"].astype("string")
    pairs_df["dataset_version"] = pairs_df["dataset_version"].astype("string")

    # Remove exact duplicate pairs
    pairs_df = pairs_df.drop_duplicates(subset=["candidate_id", "job_id"], keep="first")

    out_path = os.path.join(PROC_DIR, "cv_jd_pairs.parquet")
    pairs_df.to_parquet(out_path, engine="pyarrow", index=False)
    print(f"  Saved {pairs_df.shape} -> {out_path}")
    print(f"  Label distribution:\n{pairs_df['label'].value_counts().sort_index()}")
    print(f"  Pair type distribution:\n{pairs_df['pair_type'].value_counts()}")
    return pairs_df

# ── Phase 4: Entity-based Split ──────────────────────────────────────────────

def entity_based_split(pairs_df):
    """Split by candidate_id to prevent data leakage."""
    print("\n=== Entity-based Train/Val/Test Split ===")

    unique_candidates = pairs_df["candidate_id"].unique()
    np.random.seed(42)
    np.random.shuffle(unique_candidates)

    n = len(unique_candidates)
    n_train = int(n * 0.80)
    n_val   = int(n * 0.10)

    train_cids = set(unique_candidates[:n_train])
    val_cids   = set(unique_candidates[n_train:n_train + n_val])
    test_cids  = set(unique_candidates[n_train + n_val:])

    train_df = pairs_df[pairs_df["candidate_id"].isin(train_cids)].copy()
    val_df   = pairs_df[pairs_df["candidate_id"].isin(val_cids)].copy()
    test_df  = pairs_df[pairs_df["candidate_id"].isin(test_cids)].copy()

    # Verify no leakage
    assert len(train_cids & val_cids) == 0, "Leakage: train ∩ val"
    assert len(train_cids & test_cids) == 0, "Leakage: train ∩ test"
    assert len(val_cids & test_cids) == 0, "Leakage: val ∩ test"

    train_out = os.path.join(SPLIT_DIR, "train.parquet")
    val_out   = os.path.join(SPLIT_DIR, "validation.parquet")
    test_out  = os.path.join(SPLIT_DIR, "test.parquet")

    train_df.to_parquet(train_out, engine="pyarrow", index=False)
    val_df.to_parquet(val_out,   engine="pyarrow", index=False)
    test_df.to_parquet(test_out,  engine="pyarrow", index=False)

    print(f"  Unique candidates: {n} (train={len(train_cids)}, val={len(val_cids)}, test={len(test_cids)})")
    print(f"  Train: {train_df.shape} -> {train_out}")
    print(f"    Labels: {train_df['label'].value_counts().sort_index().to_dict()}")
    print(f"  Val:   {val_df.shape} -> {val_out}")
    print(f"    Labels: {val_df['label'].value_counts().sort_index().to_dict()}")
    print(f"  Test:  {test_df.shape} -> {test_out}")
    print(f"    Labels: {test_df['label'].value_counts().sort_index().to_dict()}")
    print(f"  No data leakage (entity-based split verified)")

    return train_df, val_df, test_df

# ── Phase 5: Validation Checks (training.md §11) ────────────────────────────

def run_validation(cv_df, jd_df, pairs_df):
    """Run all validation checks from training.md §11."""
    print("\n=== Running Validation Checks (training.md §11) ===")
    checks = []

    # CV checks
    c = cv_df["candidate_id"].notna().all()
    checks.append(("candidate_id is not null", c))

    c = cv_df["candidate_id"].is_unique
    checks.append(("candidate_id is unique in cv.parquet", c))

    c = cv_df["cv_text"].str.len().gt(0).all()
    checks.append(("cv_text is not empty", c))

    # JD checks
    c = jd_df["job_id"].notna().all()
    checks.append(("job_id is not null", c))

    c = jd_df["job_id"].is_unique
    checks.append(("job_id is unique in jd.parquet", c))

    c = jd_df["job_text"].str.len().gt(0).all()
    checks.append(("job_text is not empty", c))

    # Pairs checks
    c = pairs_df["candidate_id"].notna().all()
    checks.append(("pairs: candidate_id is not null", c))

    c = pairs_df["job_id"].notna().all()
    checks.append(("pairs: job_id is not null", c))

    c = pairs_df["cv_text"].str.len().gt(0).all()
    checks.append(("pairs: cv_text is not empty", c))

    c = pairs_df["job_text"].str.len().gt(0).all()
    checks.append(("pairs: job_text is not empty", c))

    c = pairs_df["label"].isin([0, 1, 2]).all()
    checks.append(("label in {0, 1, 2}", c))

    dup_count = pairs_df.duplicated(subset=["candidate_id", "job_id"]).sum()
    checks.append(("no duplicated CV-JD pair", dup_count == 0))

    all_passed = True
    for name, passed in checks:
        status = "PASS" if passed else "FAIL"
        print(f"  [{status}] {name}")
        if not passed:
            all_passed = False

    if all_passed:
        print("\n  == ALL VALIDATION CHECKS PASSED ==")
    else:
        print("\n  !! SOME VALIDATION CHECKS FAILED !!")

    return all_passed

# ── Phase 6: Dataset Metadata ────────────────────────────────────────────────

def save_metadata(cv_df, jd_df, pairs_df, train_df, val_df, test_df):
    """Save dataset_metadata.json as specified in training.md §17."""
    label_counts = pairs_df["label"].value_counts().to_dict()
    pair_type_counts = pairs_df["pair_type"].value_counts().to_dict()

    metadata = {
        "dataset_version": DATASET_VERSION,
        "cv_count": len(cv_df),
        "jd_count": len(jd_df),
        "pair_count": len(pairs_df),
        "positive_count": int(pair_type_counts.get("positive", 0)),
        "negative_count": int(pair_type_counts.get("negative", 0)),
        "hard_negative_count": int(pair_type_counts.get("hard_negative", 0)),
        "label_distribution": {str(k): int(v) for k, v in sorted(label_counts.items())},
        "split_sizes": {
            "train": len(train_df),
            "validation": len(val_df),
            "test": len(test_df),
        },
        "normalization_version": "skill-normalizer-v1.0",
        "labeling_version": "labeling-v1.0",
        "creation_date": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "sources": ["benchmark_dataset", "applicant_csv", "train_json"],
    }

    meta_path = os.path.join(BASE_DIR, "data", "dataset_metadata.json")
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"\n  Saved metadata -> {meta_path}")
    print(json.dumps(metadata, indent=2))

# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    print("=" * 60)
    print("BUILD TRAINING DATA — training.md Specification")
    print("=" * 60)

    aliases = load_skill_aliases()
    print(f"Loaded {len(aliases)} skill aliases from {ALIAS_PATH}")

    cv_df    = build_cv_dataset(aliases)
    jd_df    = build_jd_dataset(aliases)
    pairs_df = build_pairs_dataset(cv_df, jd_df, aliases)

    run_validation(cv_df, jd_df, pairs_df)

    train_df, val_df, test_df = entity_based_split(pairs_df)

    save_metadata(cv_df, jd_df, pairs_df, train_df, val_df, test_df)

    print("\n" + "=" * 60)
    print("BUILD TRAINING DATA COMPLETED SUCCESSFULLY")
    print("=" * 60)

if __name__ == "__main__":
    main()
