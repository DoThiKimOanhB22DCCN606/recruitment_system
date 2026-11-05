"""
Data ingestion module for the English CV-JD dataset.

Loads:
- English jobs (parquet)
- English resumes (parquet)
- English matches (parquet)
- Pre-computed embeddings metadata
"""
import sys
import json
import numpy as np
import pandas as pd
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import (
    EN_JOBS_PARQUET, EN_RESUMES_PARQUET, EN_MATCHES_PARQUET,
    EN_EMBEDDINGS_DIR,
)


def load_en_jobs(path: Path = EN_JOBS_PARQUET) -> pd.DataFrame:
    """Load English jobs from Parquet."""
    return pd.read_parquet(path)


def load_en_resumes(path: Path = EN_RESUMES_PARQUET) -> pd.DataFrame:
    """Load English resumes from Parquet."""
    return pd.read_parquet(path)


def load_en_matches(path: Path = EN_MATCHES_PARQUET) -> pd.DataFrame:
    """Load English match relationships from Parquet."""
    return pd.read_parquet(path)


def load_en_embeddings_meta(path: Path = None) -> dict:
    """Load embeddings metadata."""
    if path is None:
        meta_path = EN_EMBEDDINGS_DIR / "embedding_model_comparison.csv"
    else:
        meta_path = path
    if meta_path.exists():
        df = pd.read_csv(meta_path)
        return {"comparison": df.to_dict(orient="records")}
    return {}


def explode_matches(matches_df: pd.DataFrame) -> pd.DataFrame:
    """Explode relevant_resume_ids to get one row per positive pair."""
    exploded = matches_df.explode("relevant_resume_ids").rename(
        columns={"relevant_resume_ids": "resume_id"}
    )
    return exploded.reset_index(drop=True)


def load_all_english() -> dict:
    """Load all English data into a structured dict."""
    jobs = load_en_jobs()
    resumes = load_en_resumes()
    matches = load_en_matches()

    return {
        "jobs": jobs,
        "resumes": resumes,
        "matches": matches,
        "embeddings_meta": load_en_embeddings_meta(),
    }


if __name__ == "__main__":
    data = load_all_english()
    print(f"Jobs: {len(data['jobs'])} records, {list(data['jobs'].columns)}")
    print(f"Resumes: {len(data['resumes'])} records, {list(data['resumes'].columns)}")
    print(f"Matches: {len(data['matches'])} records")
    exploded = explode_matches(data["matches"])
    print(f"Exploded positive pairs: {len(exploded)}")
