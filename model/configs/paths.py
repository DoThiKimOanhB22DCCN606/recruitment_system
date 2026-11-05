"""
Central path configuration for the Multilingual CV-JD Matching project.
All paths are relative to PROJECT_ROOT to ensure reproducibility.
"""
from pathlib import Path

# Project root
PROJECT_ROOT = Path(r"d:\model v2")

# ── Data paths ──────────────────────────────────────────────────────────────
DATA_DIR = PROJECT_ROOT / "data"

# Vietnamese data
VN_DATA_DIR = DATA_DIR / "vietnamese data"
VN_JOBS_DIR = VN_DATA_DIR / "jobs"
VN_JOBS_CSV = VN_JOBS_DIR / "topcv_balanced_1300.csv"
VN_JOBS_REBUILT_CSV = VN_JOBS_DIR / "topcv_balanced_1300_rebuilt_20260602.csv"
VN_LABELS_DIR = VN_DATA_DIR / "turn1300_v3_union_final"
VN_LABELS_CSV = VN_LABELS_DIR / "labels_2450_compact.csv"
VN_LABELS_JSON = VN_LABELS_DIR / "labels_2450_compact.json"
VN_CVS_JSON = VN_LABELS_DIR / "evaluation_cvs_130_canonical.json"
VN_AGREEMENT_JSON = VN_LABELS_DIR / "agreement_summary.json"
VN_EVAL_PAIRS_JSON = VN_LABELS_DIR / "evaluation_pairs_2450_merged_4model.json"
VN_ANNOTATION_FILES = {
    "claude": VN_LABELS_DIR / "annotations_claude_merged_2450.json",
    "cohere": VN_LABELS_DIR / "annotations_cohere_merged_2450.json",
    "gptoss": VN_LABELS_DIR / "annotations_gptoss_merged_2450.json",
    "llama": VN_LABELS_DIR / "annotations_llama_merged_2450.json",
}

# ESCO / O*NET
ESCO_DIR = VN_DATA_DIR / "esco"
ESCO_OCCUPATIONS_CSV = ESCO_DIR / "occupations.csv"
ESCO_OCC_SKILLS_CSV = ESCO_DIR / "occupation_skills.csv"
ESCO_SKILL_BROADER_CSV = ESCO_DIR / "skill_broader_relations.csv"

ONET_DIR = VN_DATA_DIR / "onet"
ONET_TECH_SKILLS_CSV = ONET_DIR / "technology_skills.csv"
ONET_CROSSWALK_CSV = ONET_DIR / "esco_onet_crosswalk.csv"

# English data
EN_DATA_DIR = DATA_DIR / "english data" / "dataset"
EN_JOBS_PARQUET = EN_DATA_DIR / "jobs" / "train-00000-of-00001.parquet"
EN_RESUMES_PARQUET = EN_DATA_DIR / "resumes" / "train-00000-of-00001.parquet"
EN_MATCHES_PARQUET = EN_DATA_DIR / "matches" / "train-00000-of-00001.parquet"
EN_EMBEDDINGS_DIR = EN_DATA_DIR / "embeddings"

# ── Output paths ────────────────────────────────────────────────────────────
REPORTS_DIR = PROJECT_ROOT / "reports"
OUTPUTS_DIR = PROJECT_ROOT / "outputs"
PROCESSED_DIR = OUTPUTS_DIR / "processed"
MODELS_DIR = OUTPUTS_DIR / "models"

# ── Configs ─────────────────────────────────────────────────────────────────
CONFIGS_DIR = PROJECT_ROOT / "configs"
