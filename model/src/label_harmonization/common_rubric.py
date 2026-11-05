"""
Common Relevance Rubric Definition (Milestone 3)

Defines the 0-3 relevance scale, annotation rules,
missing-information handling, and creates annotation examples.
"""
import sys
import json
import pandas as pd
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR, CONFIGS_DIR
from src.utils.helpers import generate_report_header


# ═══════════════════════════════════════════════════════════════════════════
# Common Relevance Rubric (language-independent)
# ═══════════════════════════════════════════════════════════════════════════

COMMON_RUBRIC = {
    0: {
        "name": "Not Relevant",
        "definition": (
            "The CV has no meaningful alignment with the core requirements of the JD."
        ),
        "criteria": [
            "No overlap in required skills",
            "Completely different industry and role with no transferable experience",
            "No relevant experience or qualifications",
        ],
        "examples": [
            "A chef's CV applied to a software engineer position",
            "A fresh graduate with no IT background for a Senior DBA role",
        ],
    },
    1: {
        "name": "Low Relevance",
        "definition": (
            "The CV has some related skills or experience but does not "
            "satisfy important requirements."
        ),
        "criteria": [
            "Some skill overlap but missing most core requirements",
            "Related industry but different role specialization",
            "Entry-level experience for a position requiring significant experience",
            "Transferable soft skills but lacking technical requirements",
        ],
        "examples": [
            "A junior web developer for a Senior Data Scientist position",
            "A sales person with some tech exposure for an IT Manager role",
        ],
    },
    2: {
        "name": "Relevant",
        "definition": (
            "The CV satisfies most core requirements, with some gaps in "
            "preferred skills or experience."
        ),
        "criteria": [
            "Meets majority of must-have skills",
            "Relevant role experience in similar domain",
            "Appropriate seniority level (within one level)",
            "May lack some preferred/nice-to-have qualifications",
        ],
        "examples": [
            "A mid-level Python developer for a Senior Python role (strong skills, less experience)",
            "An accountant with 3 years experience for a 5-year requirement role",
        ],
    },
    3: {
        "name": "Highly Relevant",
        "definition": (
            "The CV demonstrates strong alignment with the required skills, "
            "responsibilities, and relevant experience."
        ),
        "criteria": [
            "Meets all or nearly all must-have skills",
            "Strong relevant experience in the same or closely related role",
            "Appropriate or exceeding seniority level",
            "Has many preferred/nice-to-have qualifications",
            "Industry and domain experience aligned",
        ],
        "examples": [
            "A Senior Java Developer with microservices experience for a similar role",
            "A real estate sales lead for a real estate sales manager position",
        ],
    },
}


# ═══════════════════════════════════════════════════════════════════════════
# Relevance Criteria Definitions
# ═══════════════════════════════════════════════════════════════════════════

RELEVANCE_CRITERIA = {
    "required_skills": {
        "name": "Required Skills",
        "weight": "Primary",
        "description": "Whether the candidate demonstrates the skills required by the JD.",
        "matching_levels": {
            "exact_match": "Skill name matches exactly",
            "equivalent": "Different name but same skill (e.g., PostgreSQL = Postgres)",
            "synonym": "Industry-recognized synonym",
            "related": "Related or transferable skill in the same family",
            "insufficient": "No clear evidence of the skill",
        },
    },
    "role_responsibilities": {
        "name": "Role and Responsibilities",
        "weight": "Primary",
        "description": "Whether the candidate's previous work experience is relevant to the target job.",
    },
    "experience_seniority": {
        "name": "Experience and Seniority",
        "weight": "Secondary",
        "description": "Years of experience and seniority level alignment.",
        "note": (
            "Do not automatically reject candidates for missing years "
            "when the JD does not define a strict minimum."
        ),
    },
    "industry_domain": {
        "name": "Industry and Domain",
        "weight": "Secondary",
        "description": "Relevant domain or industry experience.",
        "note": "Industry mismatch should not automatically imply irrelevance.",
    },
    "preferred_skills": {
        "name": "Preferred Skills",
        "weight": "Tertiary",
        "description": "Additional skills listed as preferred or nice-to-have.",
        "note": "Should not have the same penalty as missing mandatory requirements.",
    },
}


# ═══════════════════════════════════════════════════════════════════════════
# Evidence States for Missing Information
# ═══════════════════════════════════════════════════════════════════════════

EVIDENCE_STATES = {
    "present": "The CV contains evidence of the skill or qualification.",
    "absent": "There is sufficient evidence that the candidate does not meet the criterion.",
    "unknown": (
        "The CV does not provide enough information to determine "
        "whether the candidate meets the criterion. "
        "Absence of a skill in the CV does not necessarily prove "
        "the candidate does not possess it."
    ),
}


# ═══════════════════════════════════════════════════════════════════════════
# Hard Constraints vs Soft Criteria
# ═══════════════════════════════════════════════════════════════════════════

CONSTRAINT_TYPES = {
    "hard_constraint": {
        "definition": (
            "Explicit mandatory conditions that cannot reasonably be substituted, "
            "such as legally required certifications or clearly non-negotiable conditions."
        ),
        "examples": [
            "Medical license for a physician position",
            "Active security clearance for classified work",
            "Professional engineering license (PE) where required by law",
        ],
        "note": "Do not treat every must_have_skills entry as an absolute hard constraint.",
    },
    "soft_criterion": {
        "definition": (
            "Skills, responsibilities, experience, seniority, industry, and preferred "
            "qualifications that can be assessed by degree of alignment."
        ),
        "examples": [
            "3 years of Python experience (flexible based on quality)",
            "Experience with Agile methodologies",
            "Bachelor's degree in related field",
        ],
    },
}


# ═══════════════════════════════════════════════════════════════════════════
# Vietnamese 1-9 to Common 0-3 Mapping Thresholds
# ═══════════════════════════════════════════════════════════════════════════

VN_TO_COMMON_THRESHOLDS = {
    "description": (
        "Map Vietnamese LLM consensus scores (1-9 scale, median of 3 models) "
        "to the common 0-3 relevance rubric."
    ),
    "mapping": {
        0: {"vn_range": (1.0, 2.5), "label": "Not Relevant"},
        1: {"vn_range": (2.5, 4.5), "label": "Low Relevance"},
        2: {"vn_range": (4.5, 6.5), "label": "Relevant"},
        3: {"vn_range": (6.5, 9.0), "label": "Highly Relevant"},
    },
    "boundary_handling": (
        "Boundary values (2.5, 4.5, 6.5) are assigned to the higher category. "
        "This is a conditional mapping that should be validated against "
        "human-annotated samples."
    ),
    "confidence": "conditional",
    "validation_required": True,
}


def map_vn_score_to_common(score: float) -> int:
    """Map a Vietnamese 1-9 score to the common 0-3 rubric."""
    if score < 2.5:
        return 0
    elif score < 4.5:
        return 1
    elif score < 6.5:
        return 2
    else:
        return 3


def generate_rubric_report() -> str:
    """Generate the common relevance rubric report."""
    report = generate_report_header(
        "Common Relevance Rubric",
        "Multilingual CV-JD Matching System"
    )

    # ── Rubric Definition ──
    report += "## 1. Relevance Scale Definition\n\n"
    report += (
        "**CV–JD relevance** measures how well a candidate's demonstrated skills, "
        "experience, qualifications, and responsibilities align with the requirements "
        "and expectations of a specific job description.\n\n"
    )
    report += "| Label | Name | Definition |\n|---|---|---|\n"
    for label, info in COMMON_RUBRIC.items():
        report += f"| {label} | {info['name']} | {info['definition']} |\n"
    report += "\n"

    # Detailed criteria
    for label, info in COMMON_RUBRIC.items():
        report += f"### Label {label}: {info['name']}\n\n"
        report += f"{info['definition']}\n\n"
        report += "**Criteria:**\n"
        for c in info["criteria"]:
            report += f"- {c}\n"
        report += "\n**Examples:**\n"
        for e in info["examples"]:
            report += f"- {e}\n"
        report += "\n"

    # ── Relevance Criteria ──
    report += "---\n\n## 2. Relevance Criteria\n\n"
    report += "| Criterion | Weight | Description |\n|---|---|---|\n"
    for key, info in RELEVANCE_CRITERIA.items():
        report += f"| {info['name']} | {info['weight']} | {info['description']} |\n"
    report += "\n"

    # ── Evidence States ──
    report += "---\n\n## 3. Evidence States\n\n"
    report += "| State | Definition |\n|---|---|\n"
    for state, definition in EVIDENCE_STATES.items():
        report += f"| `{state}` | {definition} |\n"
    report += "\n"
    report += (
        "> **Important:** The absence of a skill in the CV does not necessarily prove "
        "that the candidate does not possess that skill. Do not automatically assign "
        "negative relevance based on missing information.\n\n"
    )

    # ── Hard vs Soft ──
    report += "---\n\n## 4. Hard Constraints vs Soft Criteria\n\n"
    for ctype, info in CONSTRAINT_TYPES.items():
        report += f"### {ctype.replace('_', ' ').title()}\n\n"
        report += f"{info['definition']}\n\n"
        report += "Examples:\n"
        for e in info["examples"]:
            report += f"- {e}\n"
        if "note" in info:
            report += f"\n> {info['note']}\n"
        report += "\n"

    # ── Vietnamese Score Mapping ──
    report += "---\n\n## 5. Vietnamese Score → Common Label Mapping\n\n"
    report += f"{VN_TO_COMMON_THRESHOLDS['description']}\n\n"
    report += "| Common Label | Vietnamese Score Range | Name |\n|---|---|---|\n"
    for label, mapping in VN_TO_COMMON_THRESHOLDS["mapping"].items():
        lo, hi = mapping["vn_range"]
        report += f"| {label} | [{lo}, {hi}) | {mapping['label']} |\n"
    report += f"\n> {VN_TO_COMMON_THRESHOLDS['boundary_handling']}\n\n"
    report += "**Validation required:** This mapping must be validated against human-annotated samples.\n\n"

    # ── Annotation Guidelines ──
    report += "---\n\n## 6. Annotation Guidelines for Human Validation\n\n"
    report += "### Process\n\n"
    report += "1. Read the full JD text, noting required skills, preferred skills, and requirements\n"
    report += "2. Read the full CV text, noting skills, experience, and qualifications\n"
    report += "3. Evaluate each relevance criterion independently\n"
    report += "4. Assign a label (0-3) based on overall alignment\n"
    report += "5. Record evidence for each criterion (present/absent/unknown)\n"
    report += "6. Write a brief justification\n\n"

    report += "### Rules\n\n"
    report += "- Evaluate in context of the specific JD, not general similarity\n"
    report += "- Do not rely on superficial vocabulary overlap\n"
    report += "- Do not penalize for missing preferred/nice-to-have skills as heavily as required skills\n"
    report += "- Distinguish between 'skill not mentioned' and 'skill confirmed absent'\n"
    report += "- Consider transferable skills and equivalent experience\n"
    report += "- Do not use original dataset labels when assigning new labels\n"
    report += "- Apply the same rubric regardless of language\n\n"

    return report


def save_rubric_config():
    """Save rubric configuration as JSON for use by other modules."""
    config = {
        "rubric": {
            str(k): {
                "name": v["name"],
                "definition": v["definition"],
                "criteria": v["criteria"],
            }
            for k, v in COMMON_RUBRIC.items()
        },
        "relevance_criteria": RELEVANCE_CRITERIA,
        "evidence_states": EVIDENCE_STATES,
        "constraint_types": {
            k: {"definition": v["definition"]}
            for k, v in CONSTRAINT_TYPES.items()
        },
        "vn_to_common_thresholds": VN_TO_COMMON_THRESHOLDS,
    }

    CONFIGS_DIR.mkdir(parents=True, exist_ok=True)
    config_path = CONFIGS_DIR / "common_relevance_rubric.json"
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(config, f, ensure_ascii=False, indent=2)

    return config_path


if __name__ == "__main__":
    # Generate report
    report_text = generate_rubric_report()
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "05_common_relevance_rubric.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report_text)
    print(f"Rubric report saved to: {report_path}")

    # Save config
    config_path = save_rubric_config()
    print(f"Rubric config saved to: {config_path}")
