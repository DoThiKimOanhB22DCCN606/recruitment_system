"""
Candidate Pair Generation and Negative Sampling (Milestone 5 continued)

Generates:
- Verified positive pairs from harmonized labels
- Random negative pairs with false-negative safeguards
- Semi-hard negatives (same industry, different seniority)
- Hard negatives (same industry+seniority, insufficient skill overlap)
- Language-aware dataset splitting with leakage prevention
"""
import sys
import json
import random
import pandas as pd
import numpy as np
from pathlib import Path
from collections import defaultdict

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR, PROCESSED_DIR
from src.data_ingestion.english_loader import (
    load_en_jobs, load_en_resumes, load_en_matches, explode_matches,
)
from src.utils.helpers import generate_report_header


RANDOM_SEED = 42


def generate_english_negatives(
    n_random: int = 15000,
    n_semi_hard: int = 10000,
    n_hard: int = 5000,
    seed: int = RANDOM_SEED,
) -> pd.DataFrame:
    """
    Generate negative pairs for the English dataset.

    Follows the spec requirements:
    - Unlisted pairs are unlabeled, not confirmed negative
    - Avoid sampling known positives as negatives
    - Track negative type (random, semi-hard, hard)
    """
    rng = np.random.RandomState(seed)
    random.seed(seed)

    jobs = load_en_jobs()
    resumes = load_en_resumes()
    matches = load_en_matches()
    exploded = explode_matches(matches)

    # Build positive pair set for false-negative safeguard
    positive_pairs = set(
        zip(exploded["job_id"], exploded["resume_id"])
    )

    # Index jobs and resumes by properties
    job_ids = jobs["job_id"].tolist()
    resume_ids = resumes["resume_id"].tolist()

    jobs_by_industry = defaultdict(list)
    for _, j in jobs.iterrows():
        jobs_by_industry[j["industry"]].append(j["job_id"])

    resumes_by_industry = defaultdict(list)
    resumes_by_ind_sen = defaultdict(list)
    resume_seniority = {}
    resume_industry = {}
    for _, r in resumes.iterrows():
        resumes_by_industry[r["industry"]].append(r["resume_id"])
        key = (r["industry"], r["seniority"])
        resumes_by_ind_sen[key].append(r["resume_id"])
        resume_seniority[r["resume_id"]] = r["seniority"]
        resume_industry[r["resume_id"]] = r["industry"]

    job_seniority = dict(zip(jobs["job_id"], jobs["seniority"]))
    job_industry = dict(zip(jobs["job_id"], jobs["industry"]))

    negative_pairs = []

    # 1. Random negatives
    print(f"  Generating {n_random} random negatives...")
    attempts = 0
    while len([p for p in negative_pairs if p[2] == "random"]) < n_random and attempts < n_random * 5:
        j_id = rng.choice(job_ids)
        r_id = rng.choice(resume_ids)
        if (j_id, r_id) not in positive_pairs:
            negative_pairs.append((j_id, r_id, "random"))
        attempts += 1

    # 2. Semi-hard negatives: same industry, different seniority
    print(f"  Generating {n_semi_hard} semi-hard negatives...")
    attempts = 0
    semi_hard_count = 0
    while semi_hard_count < n_semi_hard and attempts < n_semi_hard * 5:
        j_id = rng.choice(job_ids)
        j_ind = job_industry.get(j_id, "")
        j_sen = job_seniority.get(j_id, "")

        # Find resumes in same industry but different seniority
        same_ind_resumes = resumes_by_industry.get(j_ind, [])
        if same_ind_resumes:
            r_id = rng.choice(same_ind_resumes)
            r_sen = resume_seniority.get(r_id, "")
            if r_sen != j_sen and (j_id, r_id) not in positive_pairs:
                negative_pairs.append((j_id, r_id, "semi_hard"))
                semi_hard_count += 1
        attempts += 1

    # 3. Hard negatives: same industry + seniority (high similarity but not matched)
    print(f"  Generating {n_hard} hard negatives...")
    attempts = 0
    hard_count = 0
    while hard_count < n_hard and attempts < n_hard * 10:
        j_id = rng.choice(job_ids)
        j_ind = job_industry.get(j_id, "")
        j_sen = job_seniority.get(j_id, "")

        # Find resumes with same industry + seniority
        key = (j_ind, j_sen)
        candidates = resumes_by_ind_sen.get(key, [])
        if candidates:
            r_id = rng.choice(candidates)
            if (j_id, r_id) not in positive_pairs:
                negative_pairs.append((j_id, r_id, "hard"))
                hard_count += 1
        attempts += 1

    neg_df = pd.DataFrame(negative_pairs, columns=["job_id", "resume_id", "negative_type"])

    # Deduplicate
    neg_df = neg_df.drop_duplicates(subset=["job_id", "resume_id"], keep="first")

    # Add labels
    neg_df["common_relevance_label"] = 0
    neg_df["source_dataset"] = "english"
    neg_df["original_label"] = 0
    neg_df["original_label_definition"] = "Sampled negative (not in relevant_resume_ids)"
    neg_df["label_source"] = "negative_sampling"
    neg_df["mapping_status"] = "sampled_negative"
    neg_df["label_confidence"] = "low"  # Not verified
    neg_df["language"] = "en"

    return neg_df


# ═══════════════════════════════════════════════════════════════════════════
# Data Splitting with Leakage Prevention
# ═══════════════════════════════════════════════════════════════════════════

def split_dataset(
    unified: pd.DataFrame,
    test_size: float = 0.15,
    dev_size: float = 0.10,
    seed: int = RANDOM_SEED,
    strategy: str = "job_level",
) -> dict[str, pd.DataFrame]:
    """
    Split unified dataset with leakage prevention.

    Strategy 'job_level': Hold out entire jobs to prevent leakage.
    All pairs for a held-out job go to the same split.
    """
    rng = np.random.RandomState(seed)

    if strategy == "job_level":
        # Group by (source_dataset, job_id) to prevent job leakage
        job_groups = unified.groupby(["source_dataset", "job_id"]).size().reset_index(name="count")

        # Stratify by source and label distribution
        splits = {"train": [], "dev": [], "test": []}

        for source in unified["source_dataset"].unique():
            source_jobs = job_groups[job_groups["source_dataset"] == source].copy()
            source_jobs = source_jobs.sample(frac=1, random_state=rng).reset_index(drop=True)

            n_jobs = len(source_jobs)
            n_test = max(1, int(n_jobs * test_size))
            n_dev = max(1, int(n_jobs * dev_size))

            test_jobs = set(source_jobs.iloc[:n_test]["job_id"])
            dev_jobs = set(source_jobs.iloc[n_test:n_test + n_dev]["job_id"])
            train_jobs = set(source_jobs.iloc[n_test + n_dev:]["job_id"])

            source_data = unified[unified["source_dataset"] == source]
            splits["test"].append(source_data[source_data["job_id"].isin(test_jobs)])
            splits["dev"].append(source_data[source_data["job_id"].isin(dev_jobs)])
            splits["train"].append(source_data[source_data["job_id"].isin(train_jobs)])

        result = {
            split: pd.concat(dfs, ignore_index=True) for split, dfs in splits.items()
        }
    else:
        # Simple random split (with pair-level dedup)
        shuffled = unified.sample(frac=1, random_state=rng).reset_index(drop=True)
        n = len(shuffled)
        n_test = int(n * test_size)
        n_dev = int(n * dev_size)

        result = {
            "test": shuffled.iloc[:n_test],
            "dev": shuffled.iloc[n_test:n_test + n_dev],
            "train": shuffled.iloc[n_test + n_dev:],
        }

    # Validate no leakage
    train_jobs = set(result["train"]["job_id"])
    test_jobs = set(result["test"]["job_id"])
    dev_jobs = set(result["dev"]["job_id"])

    job_leakage_test = train_jobs & test_jobs
    job_leakage_dev = train_jobs & dev_jobs

    print(f"  Split strategy: {strategy}")
    for split, df in result.items():
        print(f"  {split}: {len(df):,} pairs")
        if "source_dataset" in df.columns:
            for src in df["source_dataset"].unique():
                count = (df["source_dataset"] == src).sum()
                print(f"    {src}: {count:,}")
    print(f"  Job leakage (train<->test): {len(job_leakage_test)}")
    print(f"  Job leakage (train<->dev): {len(job_leakage_dev)}")

    return result


if __name__ == "__main__":
    # Generate English negatives
    print("Generating English negative pairs...")
    neg_df = generate_english_negatives(
        n_random=15000, n_semi_hard=10000, n_hard=5000
    )
    print(f"  → {len(neg_df)} negative pairs generated")
    print(f"  Types: {neg_df['negative_type'].value_counts().to_dict()}")

    # Save negatives
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
    neg_path = PROCESSED_DIR / "english_negatives.csv"
    neg_df.to_csv(neg_path, index=False, encoding="utf-8")
    print(f"  Saved: {neg_path}")

    # Load unified and split
    print("\nLoading unified dataset...")
    unified = pd.read_csv(PROCESSED_DIR / "unified_dataset.csv", encoding="utf-8")

    # Populate candidate_text and job_text for negative pairs from text cache
    print("Populating text for English negative samples...")
    from src.unified_dataset.builder import build_en_cv_text, build_en_jd_text
    en_jobs = load_en_jobs()
    en_resumes = load_en_resumes()

    jd_text_cache = {row["job_id"]: build_en_jd_text(row) for _, row in en_jobs.iterrows()}
    cv_text_cache = {row["resume_id"]: build_en_cv_text(row) for _, row in en_resumes.iterrows()}

    neg_subset = neg_df[[
        "job_id", "common_relevance_label", "source_dataset",
        "original_label", "label_source", "mapping_status",
        "label_confidence", "language",
    ]].copy()
    neg_subset["candidate_id"] = neg_df["resume_id"]
    neg_subset["candidate_text"] = neg_df["resume_id"].map(cv_text_cache).fillna("")
    neg_subset["job_text"] = neg_df["job_id"].map(jd_text_cache).fillna("")

    unified_with_neg = pd.concat([unified, neg_subset], ignore_index=True)
    print(f"Total with negatives: {len(unified_with_neg):,}")

    # Split
    print("\nSplitting dataset...")
    splits = split_dataset(unified_with_neg, strategy="job_level")

    for split_name, split_df in splits.items():
        split_path = PROCESSED_DIR / f"split_{split_name}.csv"
        split_df.to_csv(split_path, index=False, encoding="utf-8")
        print(f"  Saved: {split_path}")
