"""
Profiling for ESCO and O*NET taxonomy resources.

Analyzes:
- Available taxonomy files and coverage
- Skill hierarchy depth
- Occupation-skill relationships
- Cross-reference capabilities (ESCO ↔ O*NET)
- Vietnamese translation coverage
"""
import sys
import json
import pandas as pd
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR, ESCO_DIR, ONET_DIR, VN_DATA_DIR
from src.data_ingestion.taxonomy_loader import (
    load_esco_occupations, load_esco_occupation_skills,
    load_esco_skill_broader, load_onet_tech_skills, load_onet_crosswalk,
)
from src.utils.helpers import generate_report_header, format_number


def profile_esco(esco_occ, esco_occ_skills, esco_broader) -> dict:
    """Profile ESCO taxonomy data."""
    profile = {}

    # Occupations
    profile["occupations"] = {
        "total": len(esco_occ),
        "columns": list(esco_occ.columns),
        "sample_titles": esco_occ["title"].head(5).tolist() if "title" in esco_occ.columns else [],
    }

    # Occupation-skill relationships
    profile["occupation_skills"] = {
        "total_relations": len(esco_occ_skills),
        "columns": list(esco_occ_skills.columns),
        "unique_occupations": int(esco_occ_skills["occupation_uri"].nunique()) if "occupation_uri" in esco_occ_skills.columns else 0,
        "unique_skills": int(esco_occ_skills["skill_uri"].nunique()) if "skill_uri" in esco_occ_skills.columns else 0,
    }
    if "relation_type" in esco_occ_skills.columns:
        profile["occupation_skills"]["relation_type_distribution"] = (
            esco_occ_skills["relation_type"].value_counts().to_dict()
        )

    # Skill broader relations (hierarchy)
    profile["skill_broader"] = {
        "total_relations": len(esco_broader),
        "columns": list(esco_broader.columns),
        "unique_skills": int(esco_broader["skill_uri"].nunique()) if "skill_uri" in esco_broader.columns else 0,
        "unique_broader": int(esco_broader["broader_uri"].nunique()) if "broader_uri" in esco_broader.columns else 0,
    }
    if "skill_type" in esco_broader.columns:
        profile["skill_broader"]["skill_type_distribution"] = (
            esco_broader["skill_type"].value_counts().to_dict()
        )

    # Check empty files
    skills_list_path = ESCO_DIR / "skills_list.csv"
    skill_relations_path = ESCO_DIR / "skill_relations.csv"
    profile["empty_files"] = []
    for p in [skills_list_path, skill_relations_path]:
        if p.exists():
            try:
                df = pd.read_csv(p)
                if len(df) == 0:
                    profile["empty_files"].append(p.name)
            except Exception:
                profile["empty_files"].append(f"{p.name} (read error)")

    return profile


def profile_onet(onet_tech, onet_crosswalk) -> dict:
    """Profile O*NET taxonomy data."""
    profile = {}

    # Technology skills
    profile["technology_skills"] = {
        "total": len(onet_tech),
        "columns": list(onet_tech.columns),
        "unique_onet_codes": int(onet_tech["O*NET-SOC Code"].nunique()) if "O*NET-SOC Code" in onet_tech.columns else 0,
        "unique_titles": int(onet_tech["Title"].nunique()) if "Title" in onet_tech.columns else 0,
    }
    if "Hot Technology" in onet_tech.columns:
        profile["technology_skills"]["hot_technology_distribution"] = (
            onet_tech["Hot Technology"].value_counts().to_dict()
        )

    # ESCO-O*NET crosswalk
    profile["crosswalk"] = {
        "total": len(onet_crosswalk),
        "columns": list(onet_crosswalk.columns),
    }

    # Available xlsx files
    profile["xlsx_files"] = []
    for f in ONET_DIR.glob("*.xlsx"):
        profile["xlsx_files"].append({
            "name": f.name,
            "size_bytes": f.stat().st_size,
        })

    return profile


def profile_additional_taxonomy_files() -> dict:
    """Profile additional taxonomy files in the Vietnamese data directory."""
    profile = {}

    # skills.csv and skills_with_names.csv
    for fname in ["skills.csv", "skills_with_names.csv"]:
        path = VN_DATA_DIR / fname
        if path.exists():
            try:
                df = pd.read_csv(path, nrows=5)
                full_df = pd.read_csv(path)
                profile[fname] = {
                    "total_rows": len(full_df),
                    "columns": list(df.columns),
                    "size_bytes": path.stat().st_size,
                }
            except Exception as e:
                profile[fname] = {"error": str(e)}

    # job_term_taxonomy.json
    taxonomy_path = VN_DATA_DIR / "job_term_taxonomy.json"
    if taxonomy_path.exists():
        try:
            with open(taxonomy_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            if isinstance(data, dict):
                profile["job_term_taxonomy"] = {
                    "total_entries": len(data),
                    "type": "dict",
                    "sample_keys": list(data.keys())[:5],
                }
            elif isinstance(data, list):
                profile["job_term_taxonomy"] = {
                    "total_entries": len(data),
                    "type": "list",
                }
        except Exception as e:
            profile["job_term_taxonomy"] = {"error": str(e)}

    # job_terms_unique.csv
    terms_path = VN_DATA_DIR / "job_terms_unique.csv"
    if terms_path.exists():
        try:
            df = pd.read_csv(terms_path)
            profile["job_terms_unique"] = {
                "total_rows": len(df),
                "columns": list(df.columns),
            }
        except Exception as e:
            profile["job_terms_unique"] = {"error": str(e)}

    return profile


def generate_taxonomy_profiling_report() -> str:
    """Generate taxonomy profiling report."""
    esco_occ = load_esco_occupations()
    esco_occ_skills = load_esco_occupation_skills()
    esco_broader = load_esco_skill_broader()
    onet_tech = load_onet_tech_skills()
    onet_crosswalk = load_onet_crosswalk()

    esco_profile = profile_esco(esco_occ, esco_occ_skills, esco_broader)
    onet_profile = profile_onet(onet_tech, onet_crosswalk)
    additional = profile_additional_taxonomy_files()

    report = generate_report_header(
        "Taxonomy Resources Profiling Report",
        "ESCO & O*NET"
    )

    # ESCO
    report += "## 1. ESCO Taxonomy\n\n"

    occ = esco_profile["occupations"]
    report += f"### Occupations: {occ['total']} records\n"
    report += f"Columns: {', '.join(f'`{c}`' for c in occ['columns'])}\n\n"

    os_ = esco_profile["occupation_skills"]
    report += f"### Occupation-Skill Relations: {format_number(os_['total_relations'])} records\n"
    report += f"- Unique occupations: {os_['unique_occupations']}\n"
    report += f"- Unique skills: {os_['unique_skills']}\n"
    if "relation_type_distribution" in os_:
        report += f"- Relation types: {os_['relation_type_distribution']}\n"
    report += "\n"

    sb = esco_profile["skill_broader"]
    report += f"### Skill Broader Relations (Hierarchy): {format_number(sb['total_relations'])} records\n"
    report += f"- Unique skills: {sb['unique_skills']}\n"
    report += f"- Unique broader concepts: {sb['unique_broader']}\n"
    if "skill_type_distribution" in sb:
        report += f"- Skill types: {sb['skill_type_distribution']}\n"
    report += "\n"

    if esco_profile.get("empty_files"):
        report += f"### ⚠️ Empty Files: {', '.join(esco_profile['empty_files'])}\n\n"

    # O*NET
    report += "---\n\n## 2. O*NET Taxonomy\n\n"

    ts = onet_profile["technology_skills"]
    report += f"### Technology Skills: {format_number(ts['total'])} records\n"
    report += f"- Unique O*NET-SOC codes: {ts['unique_onet_codes']}\n"
    report += f"- Unique job titles: {ts['unique_titles']}\n"
    if "hot_technology_distribution" in ts:
        report += f"- Hot Technology: {ts['hot_technology_distribution']}\n"
    report += "\n"

    cw = onet_profile["crosswalk"]
    report += f"### ESCO-O*NET Crosswalk: {format_number(cw['total'])} records\n"
    report += f"Columns: {', '.join(f'`{c}`' for c in cw['columns'])}\n\n"

    if onet_profile.get("xlsx_files"):
        report += "### Additional Excel Files\n\n"
        report += "| File | Size |\n|---|---|\n"
        for f in onet_profile["xlsx_files"]:
            report += f"| {f['name']} | {format_number(f['size_bytes'])} bytes |\n"
        report += "\n"

    # Additional files
    report += "---\n\n## 3. Additional Taxonomy Resources\n\n"
    for name, info in additional.items():
        if isinstance(info, dict) and "error" not in info:
            report += f"### `{name}`\n"
            for k, v in info.items():
                report += f"- {k}: {v}\n"
            report += "\n"
        elif isinstance(info, dict) and "error" in info:
            report += f"### `{name}` — ⚠️ Error: {info['error']}\n\n"

    return report, {
        "esco": esco_profile,
        "onet": onet_profile,
        "additional": additional,
    }


if __name__ == "__main__":
    report_text, profile_data = generate_taxonomy_profiling_report()

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "03_taxonomy_profiling.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report_text)

    json_path = REPORTS_DIR / "03_taxonomy_profiling.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(profile_data, f, ensure_ascii=False, indent=2, default=str)

    print(f"Report saved to: {report_path}")
    print(f"Profile data saved to: {json_path}")
