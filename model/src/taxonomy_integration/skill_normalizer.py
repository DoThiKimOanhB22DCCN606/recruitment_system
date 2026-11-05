"""
ESCO and O*NET Taxonomy Integration Module (Phase 8)

Provides:
- Skill synonym normalization using ESCO taxonomy
- Occupation normalization
- Skill-to-occupation relationship lookup
- English-Vietnamese skill mapping (where available)
- Related skill identification via broader/narrower relations
- Ablation support for experiments with/without taxonomy features

Design principles:
- Keep both original skill text AND normalized identifier
- Distinguish: exact match, synonym, broader, narrower, related, unmapped
- ESCO/O*NET supports relevance evaluation — it does NOT replace it
"""
import sys
import re
import json
import pandas as pd
import numpy as np
from pathlib import Path
from collections import defaultdict
from typing import Optional

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import (
    ESCO_DIR, ONET_DIR, VN_DATA_DIR, PROCESSED_DIR, REPORTS_DIR,
)
from src.data_ingestion.taxonomy_loader import (
    load_esco_occupations, load_esco_occupation_skills,
    load_esco_skill_broader, load_onet_tech_skills, load_onet_crosswalk,
)
from src.utils.helpers import generate_report_header


# ═══════════════════════════════════════════════════════════════════════════
# Skill Normalizer
# ═══════════════════════════════════════════════════════════════════════════

class SkillNormalizer:
    """
    Normalizes raw skill strings to canonical ESCO/O*NET identifiers.

    Matching hierarchy:
    1. Exact title match (case-insensitive)
    2. Normalized form match (lowercase, stripped punctuation)
    3. Token-overlap match (configurable threshold)

    Does NOT assume that related skills are equivalent.
    """

    def __init__(self):
        self._esco_skill_index: dict[str, dict] = {}  # normalized_name → {uri, title, type}
        self._esco_occ_index: dict[str, dict] = {}     # normalized_name → {uri, title}
        self._onet_tech_index: dict[str, dict] = {}    # normalized_name → {code, title, example}
        self._broader_map: dict[str, list[str]] = {}   # skill_uri → [broader_uris]
        self._narrower_map: dict[str, list[str]] = {}  # skill_uri → [narrower_uris]
        self._occ_skill_map: dict[str, list[dict]] = {}  # occ_uri → [{skill_uri, relation}]
        self._uri_to_title: dict[str, str] = {}        # uri → canonical title
        self._loaded = False

    def load(self):
        """Load and index all taxonomy data."""
        if self._loaded:
            return

        # ── ESCO occupation-skill relations ──
        try:
            occ_skills = load_esco_occupation_skills()
            for _, row in occ_skills.iterrows():
                occ_uri = row.get("occupation_uri", "")
                skill_uri = row.get("skill_uri", "")
                skill_title = str(row.get("skill_title", ""))
                relation = row.get("relation_type", "essential")

                # Index skill by normalized name
                norm_name = self._normalize(skill_title)
                if norm_name and skill_uri:
                    self._esco_skill_index[norm_name] = {
                        "uri": skill_uri,
                        "title": skill_title,
                        "type": "skill",
                    }
                    self._uri_to_title[skill_uri] = skill_title

                # Index occupation-skill relationships
                if occ_uri not in self._occ_skill_map:
                    self._occ_skill_map[occ_uri] = []
                self._occ_skill_map[occ_uri].append({
                    "skill_uri": skill_uri,
                    "skill_title": skill_title,
                    "relation_type": relation,
                })
        except Exception as e:
            print(f"Warning: Could not load ESCO occupation-skills: {e}")

        # ── ESCO occupations ──
        try:
            occupations = load_esco_occupations()
            for _, row in occupations.iterrows():
                occ_uri = row.get("uri", "")
                occ_title = str(row.get("title", ""))
                norm_name = self._normalize(occ_title)
                if norm_name:
                    self._esco_occ_index[norm_name] = {
                        "uri": occ_uri,
                        "title": occ_title,
                    }
                    self._uri_to_title[occ_uri] = occ_title
        except Exception as e:
            print(f"Warning: Could not load ESCO occupations: {e}")

        # ── ESCO skill hierarchy ──
        try:
            broader = load_esco_skill_broader()
            for _, row in broader.iterrows():
                skill_uri = row.get("skill_uri", "")
                broader_uri = row.get("broader_uri", "")
                skill_title = str(row.get("skill_title", ""))
                broader_title = str(row.get("broader_title", ""))

                if skill_uri and broader_uri:
                    if skill_uri not in self._broader_map:
                        self._broader_map[skill_uri] = []
                    self._broader_map[skill_uri].append(broader_uri)

                    if broader_uri not in self._narrower_map:
                        self._narrower_map[broader_uri] = []
                    self._narrower_map[broader_uri].append(skill_uri)

                # Also index broader/narrower titles
                for title, uri in [(skill_title, skill_uri), (broader_title, broader_uri)]:
                    norm = self._normalize(title)
                    if norm and uri and norm not in self._esco_skill_index:
                        self._esco_skill_index[norm] = {
                            "uri": uri,
                            "title": title,
                            "type": "skill",
                        }
                        self._uri_to_title[uri] = title
        except Exception as e:
            print(f"Warning: Could not load ESCO skill hierarchy: {e}")

        # ── O*NET technology skills ──
        try:
            tech_skills = load_onet_tech_skills()
            for _, row in tech_skills.iterrows():
                example = str(row.get("Example", ""))
                norm_name = self._normalize(example)
                if norm_name:
                    self._onet_tech_index[norm_name] = {
                        "onet_code": row.get("O*NET-SOC Code", ""),
                        "title": row.get("Title", ""),
                        "example": example,
                        "commodity": row.get("Commodity Title", ""),
                        "hot_tech": row.get("Hot Technology", ""),
                    }
        except Exception as e:
            print(f"Warning: Could not load O*NET tech skills: {e}")

        self._loaded = True
        print(
            f"Taxonomy loaded: {len(self._esco_skill_index)} ESCO skills, "
            f"{len(self._esco_occ_index)} ESCO occupations, "
            f"{len(self._onet_tech_index)} O*NET tech skills, "
            f"{len(self._broader_map)} broader relations"
        )

    @staticmethod
    def _normalize(text: str) -> str:
        """Normalize a skill/occupation name for matching."""
        if not text or not isinstance(text, str):
            return ""
        text = text.lower().strip()
        text = re.sub(r'[^\w\s]', ' ', text)  # Remove punctuation
        text = re.sub(r'\s+', ' ', text).strip()
        return text

    def normalize_skill(self, raw_skill: str) -> dict:
        """
        Normalize a single skill string.

        Returns dict with:
        - original: raw input
        - canonical: canonical name (if found)
        - uri: ESCO/O*NET identifier (if found)
        - match_type: exact | normalized | token_overlap | onet | unmapped
        - source: esco | onet | unmapped
        """
        self.load()

        result = {
            "original": raw_skill,
            "canonical": None,
            "uri": None,
            "match_type": "unmapped",
            "source": "unmapped",
        }

        if not raw_skill or not isinstance(raw_skill, str):
            return result

        norm = self._normalize(raw_skill)
        if not norm:
            return result

        # 1. Exact match in ESCO skills
        if norm in self._esco_skill_index:
            info = self._esco_skill_index[norm]
            result["canonical"] = info["title"]
            result["uri"] = info["uri"]
            result["match_type"] = "exact"
            result["source"] = "esco"
            return result

        # 2. Exact match in ESCO occupations
        if norm in self._esco_occ_index:
            info = self._esco_occ_index[norm]
            result["canonical"] = info["title"]
            result["uri"] = info["uri"]
            result["match_type"] = "exact"
            result["source"] = "esco_occupation"
            return result

        # 3. Check O*NET technology skills
        if norm in self._onet_tech_index:
            info = self._onet_tech_index[norm]
            result["canonical"] = info["example"]
            result["uri"] = info["onet_code"]
            result["match_type"] = "exact"
            result["source"] = "onet"
            return result

        # 4. Token overlap match (for multi-word skills)
        norm_tokens = set(norm.split())
        if len(norm_tokens) >= 2:
            best_match = None
            best_overlap = 0

            for index_name, info in self._esco_skill_index.items():
                index_tokens = set(index_name.split())
                if not index_tokens:
                    continue
                overlap = len(norm_tokens & index_tokens)
                union = len(norm_tokens | index_tokens)
                jaccard = overlap / union if union > 0 else 0

                if jaccard > 0.6 and jaccard > best_overlap:
                    best_overlap = jaccard
                    best_match = info

            if best_match:
                result["canonical"] = best_match["title"]
                result["uri"] = best_match["uri"]
                result["match_type"] = "token_overlap"
                result["source"] = "esco"
                return result

        return result

    def normalize_skills_list(self, skills: list[str]) -> list[dict]:
        """Normalize a list of skills."""
        return [self.normalize_skill(s) for s in skills]

    def get_related_skills(self, skill_uri: str) -> dict:
        """
        Get related skills via ESCO hierarchy.

        Returns broader (parent) and narrower (child) skills.
        Does NOT assume related skills are equivalent.
        """
        self.load()
        result = {"broader": [], "narrower": []}

        # Broader (parent) skills
        for broader_uri in self._broader_map.get(skill_uri, []):
            title = self._uri_to_title.get(broader_uri, broader_uri)
            result["broader"].append({
                "uri": broader_uri,
                "title": title,
                "relation": "broader",
            })

        # Narrower (child) skills
        for narrower_uri in self._narrower_map.get(skill_uri, []):
            title = self._uri_to_title.get(narrower_uri, narrower_uri)
            result["narrower"].append({
                "uri": narrower_uri,
                "title": title,
                "relation": "narrower",
            })

        return result

    def get_occupation_skills(self, occupation_uri: str) -> list[dict]:
        """Get essential and optional skills for an occupation."""
        self.load()
        return self._occ_skill_map.get(occupation_uri, [])

    def get_coverage_stats(self, skills: list[str]) -> dict:
        """Compute normalization coverage statistics for a list of skills."""
        normalized = self.normalize_skills_list(skills)
        total = len(normalized)
        if total == 0:
            return {"total": 0, "mapped": 0, "unmapped": 0, "coverage": 0}

        mapped = sum(1 for n in normalized if n["match_type"] != "unmapped")
        by_source = defaultdict(int)
        by_type = defaultdict(int)
        for n in normalized:
            by_source[n["source"]] += 1
            by_type[n["match_type"]] += 1

        return {
            "total": total,
            "mapped": mapped,
            "unmapped": total - mapped,
            "coverage": round(mapped / total, 3),
            "by_source": dict(by_source),
            "by_match_type": dict(by_type),
        }


# ═══════════════════════════════════════════════════════════════════════════
# Dataset Enrichment
# ═══════════════════════════════════════════════════════════════════════════

def enrich_unified_dataset(
    unified_path: Path = None,
    output_path: Path = None,
) -> pd.DataFrame:
    """
    Enrich unified dataset with normalized skill identifiers.

    Adds columns:
    - candidate_skills_normalized: JSON list of normalized skill dicts
    - job_skills_normalized: JSON list of normalized skill dicts
    - skill_coverage_candidate: fraction of candidate skills mapped
    - skill_coverage_job: fraction of job must-have skills mapped
    """
    if unified_path is None:
        unified_path = PROCESSED_DIR / "unified_dataset.csv"
    if output_path is None:
        output_path = PROCESSED_DIR / "unified_dataset_enriched.csv"

    df = pd.read_csv(unified_path, encoding="utf-8")
    normalizer = SkillNormalizer()

    print(f"Enriching {len(df)} pairs with taxonomy data...")

    candidate_norm = []
    job_norm = []
    cand_coverage = []
    job_coverage = []

    for idx, row in df.iterrows():
        if idx % 5000 == 0 and idx > 0:
            print(f"  Processed {idx}/{len(df)}...")

        # Candidate skills
        try:
            c_skills = json.loads(str(row.get("candidate_skills", "[]")))
            if not isinstance(c_skills, list):
                c_skills = []
        except (json.JSONDecodeError, TypeError):
            c_skills = []

        c_normalized = normalizer.normalize_skills_list(c_skills)
        candidate_norm.append(json.dumps(c_normalized, ensure_ascii=False))
        c_stats = normalizer.get_coverage_stats(c_skills)
        cand_coverage.append(c_stats["coverage"])

        # Job skills
        try:
            j_skills = json.loads(str(row.get("job_must_have_skills", "[]")))
            if not isinstance(j_skills, list):
                j_skills = []
        except (json.JSONDecodeError, TypeError):
            j_skills = []

        j_normalized = normalizer.normalize_skills_list(j_skills)
        job_norm.append(json.dumps(j_normalized, ensure_ascii=False))
        j_stats = normalizer.get_coverage_stats(j_skills)
        job_coverage.append(j_stats["coverage"])

    df["candidate_skills_normalized"] = candidate_norm
    df["job_skills_normalized"] = job_norm
    df["skill_coverage_candidate"] = cand_coverage
    df["skill_coverage_job"] = job_coverage

    df.to_csv(output_path, index=False, encoding="utf-8")
    print(f"Enriched dataset saved to: {output_path}")

    # Print coverage summary
    print(f"\nSkill Normalization Coverage:")
    print(f"  Candidate skills: mean={np.mean(cand_coverage):.3f}, "
          f"median={np.median(cand_coverage):.3f}")
    print(f"  Job must-have skills: mean={np.mean(job_coverage):.3f}, "
          f"median={np.median(job_coverage):.3f}")

    return df


# ═══════════════════════════════════════════════════════════════════════════
# Taxonomy Profiling Report
# ═══════════════════════════════════════════════════════════════════════════

def generate_integration_report(normalizer: SkillNormalizer = None) -> str:
    """Generate a report on taxonomy integration capabilities."""
    if normalizer is None:
        normalizer = SkillNormalizer()
        normalizer.load()

    report = generate_report_header(
        "Taxonomy Integration Report",
        "ESCO & O*NET Skill Normalization"
    )

    report += "## 1. Available Resources\n\n"
    report += f"- ESCO skills indexed: {len(normalizer._esco_skill_index):,}\n"
    report += f"- ESCO occupations indexed: {len(normalizer._esco_occ_index):,}\n"
    report += f"- O*NET tech skills indexed: {len(normalizer._onet_tech_index):,}\n"
    report += f"- Broader skill relations: {len(normalizer._broader_map):,}\n"
    report += f"- Narrower skill relations: {len(normalizer._narrower_map):,}\n\n"

    # Test with sample skills
    test_skills_en = [
        "Python", "Java", "Machine Learning", "Project Management",
        "SQL", "Docker", "Agile", "Communication",
        "React", "Data Analysis", "Leadership",
    ]
    test_skills_vi = [
        "quản lý dự án", "kế toán", "thiết kế đồ họa",
        "bán hàng", "marketing", "lập trình",
    ]

    report += "## 2. Sample Normalization Results\n\n"

    report += "### English Skills\n\n"
    report += "| Raw Skill | Canonical | Match Type | Source |\n|---|---|---|---|\n"
    for skill in test_skills_en:
        n = normalizer.normalize_skill(skill)
        report += (
            f"| {n['original']} | {n['canonical'] or '—'} | "
            f"{n['match_type']} | {n['source']} |\n"
        )
    report += "\n"

    report += "### Vietnamese Skills\n\n"
    report += "| Raw Skill | Canonical | Match Type | Source |\n|---|---|---|---|\n"
    for skill in test_skills_vi:
        n = normalizer.normalize_skill(skill)
        report += (
            f"| {n['original']} | {n['canonical'] or '—'} | "
            f"{n['match_type']} | {n['source']} |\n"
        )
    report += "\n"

    # Coverage stats
    en_stats = normalizer.get_coverage_stats(test_skills_en)
    vi_stats = normalizer.get_coverage_stats(test_skills_vi)

    report += "## 3. Coverage Summary\n\n"
    report += f"- English sample coverage: {en_stats['coverage']*100:.0f}% "
    report += f"({en_stats['mapped']}/{en_stats['total']})\n"
    report += f"- Vietnamese sample coverage: {vi_stats['coverage']*100:.0f}% "
    report += f"({vi_stats['mapped']}/{vi_stats['total']})\n\n"

    report += "## 4. Limitations\n\n"
    report += "- ESCO `skills_list.csv` and `skill_relations.csv` are empty (0 rows)\n"
    report += "- Vietnamese skill terms have low direct match rate against English ESCO titles\n"
    report += "- Token-overlap matching may produce false positives for short skill names\n"
    report += "- O*NET provides technology-level skills, not competency-level skills\n"
    report += "- Run ablation experiment (E6) to verify whether taxonomy improves matching\n\n"

    return report


if __name__ == "__main__":
    # Generate integration report
    normalizer = SkillNormalizer()
    normalizer.load()

    report = generate_integration_report(normalizer)
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "08_taxonomy_integration.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"Report saved to: {report_path}")

    # Optionally enrich the unified dataset
    unified_path = PROCESSED_DIR / "unified_dataset.csv"
    if unified_path.exists():
        enrich_unified_dataset(unified_path)
    else:
        print(f"Unified dataset not found at {unified_path}. "
              "Run unified_dataset.builder first.")
