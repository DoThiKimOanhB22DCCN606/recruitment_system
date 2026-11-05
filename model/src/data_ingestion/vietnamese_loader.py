"""
Data ingestion module for the Vietnamese CV-JD dataset.

Loads:
- Vietnamese jobs (topcv_balanced_1300.csv)
- Vietnamese CVs (evaluation_cvs_130_canonical.json)
- Vietnamese labels (labels_2450_compact.csv)
- Vietnamese annotations (4 LLM annotator files)
- Agreement summary
"""
import sys
import json
import pandas as pd
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import (
    VN_JOBS_CSV, VN_CVS_JSON, VN_LABELS_CSV,
    VN_ANNOTATION_FILES, VN_AGREEMENT_JSON,
)


def load_vn_jobs(path: Path = VN_JOBS_CSV) -> pd.DataFrame:
    """Load Vietnamese job descriptions from CSV."""
    df = pd.read_csv(path, encoding="utf-8")
    # Use content_hash as job_id if available
    if "content_hash" in df.columns:
        df = df.rename(columns={"content_hash": "job_id"})
    return df


def load_vn_cvs(path: Path = VN_CVS_JSON) -> pd.DataFrame:
    """Load Vietnamese CVs from canonical JSON."""
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    cvs_list = data["cvs"]
    if isinstance(cvs_list, dict):
        cvs_list = list(cvs_list.values())

    df = pd.DataFrame(cvs_list)
    return df


def load_vn_labels(path: Path = VN_LABELS_CSV) -> pd.DataFrame:
    """Load Vietnamese labels with LLM consensus scores."""
    df = pd.read_csv(path, encoding="utf-8")
    return df


def load_vn_annotations(
    annotation_files: dict = None,
) -> dict[str, pd.DataFrame]:
    """Load all 4 LLM annotator files and return dict of DataFrames."""
    if annotation_files is None:
        annotation_files = VN_ANNOTATION_FILES

    results = {}
    for model_name, path in annotation_files.items():
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        annotations = data["annotations"]
        df = pd.DataFrame(annotations)
        results[model_name] = df

    return results


def load_vn_agreement(path: Path = VN_AGREEMENT_JSON) -> dict:
    """Load agreement summary JSON."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_all_vietnamese() -> dict:
    """Load all Vietnamese data into a structured dict."""
    return {
        "jobs": load_vn_jobs(),
        "cvs": load_vn_cvs(),
        "labels": load_vn_labels(),
        "annotations": load_vn_annotations(),
        "agreement": load_vn_agreement(),
    }


if __name__ == "__main__":
    data = load_all_vietnamese()
    print(f"Jobs: {len(data['jobs'])} records, {list(data['jobs'].columns)}")
    print(f"CVs: {len(data['cvs'])} records, {list(data['cvs'].columns)}")
    print(f"Labels: {len(data['labels'])} records, {list(data['labels'].columns)}")
    for m, df in data["annotations"].items():
        print(f"Annotations ({m}): {len(df)} records")
