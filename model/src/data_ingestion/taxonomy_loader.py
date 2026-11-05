"""
Data ingestion module for ESCO and O*NET taxonomy resources.
"""
import sys
import pandas as pd
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import (
    ESCO_OCCUPATIONS_CSV, ESCO_OCC_SKILLS_CSV, ESCO_SKILL_BROADER_CSV,
    ONET_TECH_SKILLS_CSV, ONET_CROSSWALK_CSV, ONET_DIR,
)


def load_esco_occupations(path: Path = ESCO_OCCUPATIONS_CSV) -> pd.DataFrame:
    """Load ESCO occupations."""
    return pd.read_csv(path, encoding="utf-8")


def load_esco_occupation_skills(path: Path = ESCO_OCC_SKILLS_CSV) -> pd.DataFrame:
    """Load ESCO occupation-skill relationships."""
    return pd.read_csv(path, encoding="utf-8")


def load_esco_skill_broader(path: Path = ESCO_SKILL_BROADER_CSV) -> pd.DataFrame:
    """Load ESCO skill broader relations (hierarchy)."""
    return pd.read_csv(path, encoding="utf-8")


def load_onet_tech_skills(path: Path = ONET_TECH_SKILLS_CSV) -> pd.DataFrame:
    """Load O*NET technology skills."""
    return pd.read_csv(path, encoding="utf-8")


def load_onet_crosswalk(path: Path = ONET_CROSSWALK_CSV) -> pd.DataFrame:
    """Load ESCO-O*NET crosswalk."""
    return pd.read_csv(path, encoding="utf-8")


def load_onet_xlsx_files() -> dict[str, pd.DataFrame]:
    """Load O*NET Excel files that are available."""
    results = {}
    for name in ["Alternate_Titles", "ESCO_to_ONET-SOC", "Occupation_Data", "Technology_Skills"]:
        path = ONET_DIR / f"{name}.xlsx"
        if path.exists():
            try:
                results[name] = pd.read_excel(path)
            except Exception as e:
                print(f"Warning: Could not read {path}: {e}")
    return results


def load_all_taxonomy() -> dict:
    """Load all taxonomy resources."""
    return {
        "esco_occupations": load_esco_occupations(),
        "esco_occupation_skills": load_esco_occupation_skills(),
        "esco_skill_broader": load_esco_skill_broader(),
        "onet_tech_skills": load_onet_tech_skills(),
        "onet_crosswalk": load_onet_crosswalk(),
    }


if __name__ == "__main__":
    data = load_all_taxonomy()
    for name, df in data.items():
        print(f"{name}: {len(df)} records, {list(df.columns)}")
