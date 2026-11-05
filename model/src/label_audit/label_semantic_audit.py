"""
Label Semantic Audit Module (Milestone 2)

Performs a thorough audit of label definitions from both datasets:
- Vietnamese: LLM-annotated relevance scores (1-9 scale, 4 models)
- English: Positive-only match relationships (implicit binary)

Produces:
- Label audit table
- Label compatibility report
- Semantic incompatibility analysis
"""
import sys
import json
import pandas as pd
import numpy as np
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from configs.paths import REPORTS_DIR
from src.data_ingestion.vietnamese_loader import (
    load_vn_labels, load_vn_annotations, load_vn_agreement,
)
from src.data_ingestion.english_loader import (
    load_en_jobs, load_en_resumes, load_en_matches, explode_matches,
)
from src.utils.helpers import generate_report_header


def audit_vietnamese_labels() -> dict:
    """Audit Vietnamese label semantics."""
    labels = load_vn_labels()
    annotations = load_vn_annotations()
    agreement = load_vn_agreement()

    audit = {
        "source_dataset": "Vietnamese CV-JD (turn1300_v3_union_final)",
        "language": "Vietnamese",
        "label_format": "Continuous 1-9 scale (from LLM consensus)",
        "original_label_columns": {
            "relevance_score": "Per-annotator relevance score (1-9)",
            "skills_match": "Per-annotator skills match score (1-9)",
            "experience_match": "Per-annotator experience match score (1-9)",
            "education_match": "Per-annotator education match score (1-9)",
            "justification": "Free-text justification in Vietnamese",
        },
        "consensus_columns": {
            "relevance_median_4model": "Median of 4 LLM relevance scores",
            "relevance_median_no_cohere": "Median of 3 LLM scores (excl. Cohere)",
            "is_relevant_median_4model_ge5": "Binary: median ≥ 5",
            "is_relevant_median_no_cohere_ge5": "Binary: median ≥ 5 (excl. Cohere)",
        },
    }

    # Labeling method analysis
    audit["labeling_method"] = {
        "type": "LLM-generated (automated)",
        "annotators": ["claude", "cohere", "gptoss", "llama"],
        "no_cohere_annotators": ["claude", "gptoss", "llama"],
        "consensus_method": "Median of annotator scores",
        "relevant_threshold": 5,
        "human_annotations": False,
        "annotation_guidelines": "Not explicitly documented; each LLM used its own interpretation",
    }

    # CV generation method
    audit["cv_generation"] = {
        "method": "LLM-generated (llama-3.3-70b-versatile)",
        "total_cvs": 130,
        "categories": 13,
        "levels": ["L1", "L1b", "L2", "L2b", "L3", "L3b", "L4", "L4b", "P", "P2"],
        "is_synthetic": True,
        "implication": (
            "CVs are synthetic, not real candidate resumes. "
            "This means the relevance judgments are about synthetic CV vs. real JD pairs."
        ),
    }

    # Score distribution analysis
    vals = labels["relevance_median_no_cohere"]
    audit["score_distribution"] = {
        "scale": "1-9 continuous",
        "min": float(vals.min()),
        "max": float(vals.max()),
        "mean": round(float(vals.mean()), 2),
        "median": round(float(vals.median()), 2),
        "std": round(float(vals.std()), 2),
        "below_threshold_count": int((vals < 5).sum()),
        "at_or_above_threshold_count": int((vals >= 5).sum()),
        "below_threshold_pct": round(float((vals < 5).sum() / len(vals) * 100), 1),
    }

    # Inter-annotator analysis
    model_means = agreement.get("model_means", {})
    audit["annotator_tendencies"] = {}
    for model, means in model_means.items():
        rel = means.get("relevance_score", 0)
        audit["annotator_tendencies"][model] = {
            "mean_relevance": round(rel, 2),
            "tendency": "strict" if rel < 5 else "lenient",
        }

    # Disagreement analysis
    range_col = "range_relevance_no_cohere"
    if range_col in labels.columns:
        ranges = labels[range_col]
        audit["disagreement"] = {
            "mean_range": round(float(ranges.mean()), 2),
            "median_range": round(float(ranges.median()), 2),
            "high_disagreement_count": int((ranges > 4).sum()),
            "high_disagreement_pct": round(float((ranges > 4).sum() / len(ranges) * 100), 2),
            "max_range": float(ranges.max()),
        }

    # Relevance definition analysis
    audit["relevance_definition"] = {
        "stated_definition": "Not formally documented",
        "inferred_criteria": [
            "Skills match between CV and JD",
            "Experience match (years and domain)",
            "Education match",
            "Overall relevance score",
        ],
        "scoring_dimensions": [
            "relevance_score (overall)",
            "skills_match",
            "experience_match",
            "education_match",
        ],
        "positive_definition": "Median relevance score ≥ 5 on 1-9 scale",
        "negative_definition": "Median relevance score < 5 on 1-9 scale",
        "relevance_scope": "Overall suitability (multi-dimensional: skills, experience, education)",
    }

    return audit


def audit_english_labels() -> dict:
    """Audit English label semantics."""
    jobs = load_en_jobs()
    resumes = load_en_resumes()
    matches = load_en_matches()
    exploded = explode_matches(matches)

    audit = {
        "source_dataset": "English CV-JD (HuggingFace Parquet)",
        "language": "English",
        "label_format": "Implicit binary (presence in relevant_resume_ids = positive)",
        "original_label_columns": {
            "relevant_resume_ids": "List of resume IDs relevant to each job",
        },
    }

    # Labeling method analysis
    audit["labeling_method"] = {
        "type": "Unknown (likely rule-based or LLM-generated)",
        "evidence": [
            "Perfectly balanced seniority distribution (625 each for Jr/Mid/Sr/Lead)",
            "Perfectly balanced industry distribution (100 each for 25 industries)",
            "Exactly 30 relevant resumes per job (uniform)",
            "All data appears synthetically generated",
        ],
        "human_annotations": False,
        "annotation_guidelines": "Not available",
    }

    # Label structure
    audit["label_structure"] = {
        "type": "Positive-only (no explicit negatives)",
        "total_positive_pairs": len(exploded),
        "resumes_per_job": 30,
        "uniform_match_count": True,
        "negative_pairs": "None explicitly provided",
        "unlabeled_pairs": "All non-matched pairs are unlabeled (not confirmed negative)",
    }

    # Relevance definition analysis
    audit["relevance_definition"] = {
        "stated_definition": "Not formally documented",
        "inferred_definition": (
            "A resume is 'relevant' to a job if it appears in the job's relevant_resume_ids list. "
            "The criteria for relevance are unknown."
        ),
        "positive_definition": "Presence in relevant_resume_ids",
        "negative_definition": "Not available (absence ≠ confirmed negative)",
        "relevance_scope": "Unknown (could be skill-based, role-based, or overall suitability)",
    }

    # Synthetic data assessment
    audit["synthetic_assessment"] = {
        "is_synthetic": True,
        "confidence": "Very high",
        "indicators": [
            "Perfect balance in seniority across jobs and resumes",
            "Perfect balance in industry across jobs and resumes",
            "Uniform 30 relevant resumes per job",
            "Structured text patterns in descriptions",
        ],
        "implication": (
            "Both the data AND the match labels are likely synthetically generated. "
            "Labels may reflect rule-based matching (e.g., industry/seniority/skill overlap) "
            "rather than genuine human relevance judgments."
        ),
    }

    return audit


def build_label_audit_table(vn_audit: dict, en_audit: dict) -> pd.DataFrame:
    """Build the label audit table as specified in the project requirements."""
    rows = [
        {
            "source_dataset": "Vietnamese (turn1300_v3_union_final)",
            "original_label": "relevance_median_no_cohere (1-9 scale)",
            "original_definition": "Median of 3 LLM relevance scores (Claude, GPToss, Llama)",
            "labeling_method": "LLM-generated (automated, 4 models, consensus by median)",
            "positive_definition": "Median score ≥ 5 on 1-9 scale",
            "negative_definition": "Median score < 5 on 1-9 scale",
            "relevance_scope": "Multi-dimensional: skills, experience, education, overall",
            "label_reliability": "Partially verified (LLM consensus, no human validation)",
            "mapping_status": "Conditional mapping (1-9 → 0-3 requires calibration)",
        },
        {
            "source_dataset": "Vietnamese (per-annotator)",
            "original_label": "relevance_score per model (1-9 scale)",
            "original_definition": "Individual LLM model's relevance assessment",
            "labeling_method": "LLM-generated (individual model)",
            "positive_definition": "Score ≥ 5",
            "negative_definition": "Score < 5",
            "relevance_scope": "Overall suitability with justification",
            "label_reliability": "Unknown (no inter-model calibration guarantee)",
            "mapping_status": "Conditional mapping (individual scores less reliable than consensus)",
        },
        {
            "source_dataset": "English (matches)",
            "original_label": "Presence in relevant_resume_ids (binary)",
            "original_definition": "Resume is listed as relevant to job",
            "labeling_method": "Unknown (likely rule-based or synthetic generation)",
            "positive_definition": "Resume ID appears in job's relevant_resume_ids",
            "negative_definition": "Not available (unlisted ≠ irrelevant)",
            "relevance_scope": "Unknown (criteria not documented)",
            "label_reliability": "Unknown (synthetic data, no annotation methodology)",
            "mapping_status": "Conditional mapping (positive → label ≥ 1, but grade unknown)",
        },
    ]

    return pd.DataFrame(rows)


def analyze_semantic_incompatibility(vn_audit: dict, en_audit: dict) -> list[dict]:
    """Classify semantic incompatibilities between datasets."""
    incompatibilities = []

    # 1. Different label scales
    incompatibilities.append({
        "type": "Different label scales",
        "severity": "HIGH",
        "description": (
            "Vietnamese uses continuous 1-9 scale (from LLM median). "
            "English uses implicit binary (present/absent in match list). "
            "These cannot be directly aligned."
        ),
        "resolution": (
            "Map Vietnamese 1-9 to common 0-3 rubric via calibrated thresholds. "
            "Map English positives to common rubric using structured feature analysis."
        ),
    })

    # 2. Different relevance definitions
    incompatibilities.append({
        "type": "Different matching criteria",
        "severity": "HIGH",
        "description": (
            "Vietnamese: multi-dimensional (skills + experience + education) with justifications. "
            "English: unknown criteria (possibly rule-based on metadata similarity)."
        ),
        "resolution": (
            "Use Vietnamese justifications to understand scoring criteria. "
            "For English, analyze matches to reverse-engineer matching criteria."
        ),
    })

    # 3. Different annotation quality
    incompatibilities.append({
        "type": "Different annotation quality",
        "severity": "MEDIUM",
        "description": (
            "Vietnamese: 4 LLM annotators with consensus mechanism and justifications. "
            "English: unknown annotation source with no quality assessment."
        ),
        "resolution": (
            "Vietnamese labels have better provenance but still lack human validation. "
            "Both require human-validated test set."
        ),
    })

    # 4. Different data authenticity
    incompatibilities.append({
        "type": "Different data authenticity",
        "severity": "MEDIUM",
        "description": (
            "Vietnamese: real JDs from TopCV + synthetic CVs from LLM. "
            "English: both jobs and resumes appear fully synthetic."
        ),
        "resolution": (
            "Document data provenance. Consider that model may learn "
            "different patterns from synthetic vs. semi-real data."
        ),
    })

    # 5. No explicit negatives in English
    incompatibilities.append({
        "type": "Different definitions of positive and negative",
        "severity": "HIGH",
        "description": (
            "Vietnamese: explicit positive and negative pairs (above/below threshold). "
            "English: positive-only (no confirmed negatives). "
            "Absent pairs are unlabeled, not negative."
        ),
        "resolution": (
            "Do not automatically assign label 0 to unlisted English pairs. "
            "Use PU-learning techniques or careful negative sampling."
        ),
    })

    # 6. Graded vs binary
    incompatibilities.append({
        "type": "Different relevance granularity",
        "severity": "MEDIUM",
        "description": (
            "Vietnamese provides fine-grained 1-9 scores enabling graded relevance. "
            "English provides only binary (relevant/not-in-list) with no granularity."
        ),
        "resolution": (
            "For the common rubric (0-3), Vietnamese can be mapped using score thresholds. "
            "English positives need scoring based on structured feature analysis."
        ),
    })

    return incompatibilities


def generate_label_audit_report() -> str:
    """Generate the complete label semantic audit report."""
    vn_audit = audit_vietnamese_labels()
    en_audit = audit_english_labels()
    audit_table = build_label_audit_table(vn_audit, en_audit)
    incompatibilities = analyze_semantic_incompatibility(vn_audit, en_audit)

    report = generate_report_header(
        "Label Semantic Audit Report",
        "Vietnamese & English Datasets"
    )

    # ── Section 1: Vietnamese Labels ──
    report += "## 1. Vietnamese Dataset Label Audit\n\n"

    report += "### 1.1 Label Format\n\n"
    report += f"- **Scale:** {vn_audit['label_format']}\n"
    report += f"- **Consensus method:** Median of annotator scores\n"
    report += f"- **Relevant threshold:** ≥ 5\n"
    report += f"- **Annotators:** {', '.join(vn_audit['labeling_method']['annotators'])}\n\n"

    report += "### 1.2 Labeling Method\n\n"
    lm = vn_audit["labeling_method"]
    report += f"- **Type:** {lm['type']}\n"
    report += f"- **Human annotations:** {lm['human_annotations']}\n"
    report += f"- **Guidelines:** {lm['annotation_guidelines']}\n\n"

    report += "### 1.3 CV Generation\n\n"
    cg = vn_audit["cv_generation"]
    report += f"- **Method:** {cg['method']}\n"
    report += f"- **Total CVs:** {cg['total_cvs']}\n"
    report += f"- **Is synthetic:** {cg['is_synthetic']}\n"
    report += f"- **Implication:** {cg['implication']}\n\n"

    report += "### 1.4 Score Distribution\n\n"
    sd = vn_audit["score_distribution"]
    report += f"- Range: {sd['min']} - {sd['max']}\n"
    report += f"- Mean: {sd['mean']}, Median: {sd['median']}, Std: {sd['std']}\n"
    report += f"- Below threshold (<5): {sd['below_threshold_count']} ({sd['below_threshold_pct']}%)\n"
    report += f"- At/above threshold (≥5): {sd['at_or_above_threshold_count']}\n\n"

    report += "### 1.5 Annotator Tendencies\n\n"
    report += "| Model | Mean Relevance | Tendency |\n|---|---|---|\n"
    for model, info in vn_audit["annotator_tendencies"].items():
        report += f"| {model} | {info['mean_relevance']} | {info['tendency']} |\n"
    report += "\n"

    report += "### 1.6 Inter-Annotator Disagreement\n\n"
    if "disagreement" in vn_audit:
        d = vn_audit["disagreement"]
        report += f"- Mean range: {d['mean_range']}\n"
        report += f"- Median range: {d['median_range']}\n"
        report += f"- High disagreement (>4): {d['high_disagreement_count']} ({d['high_disagreement_pct']}%)\n"
        report += f"- Max range: {d['max_range']}\n\n"

    report += "### 1.7 Relevance Definition\n\n"
    rd = vn_audit["relevance_definition"]
    report += f"- **Stated:** {rd['stated_definition']}\n"
    report += f"- **Positive:** {rd['positive_definition']}\n"
    report += f"- **Negative:** {rd['negative_definition']}\n"
    report += f"- **Scope:** {rd['relevance_scope']}\n"
    report += f"- **Scoring dimensions:** {', '.join(rd['scoring_dimensions'])}\n\n"

    # ── Section 2: English Labels ──
    report += "---\n\n## 2. English Dataset Label Audit\n\n"

    report += "### 2.1 Label Format\n\n"
    report += f"- **Format:** {en_audit['label_format']}\n\n"

    report += "### 2.2 Labeling Method\n\n"
    lm = en_audit["labeling_method"]
    report += f"- **Type:** {lm['type']}\n"
    report += f"- **Human annotations:** {lm['human_annotations']}\n"
    report += "- **Evidence of synthetic generation:**\n"
    for e in lm["evidence"]:
        report += f"  - {e}\n"
    report += "\n"

    report += "### 2.3 Label Structure\n\n"
    ls = en_audit["label_structure"]
    report += f"- **Type:** {ls['type']}\n"
    report += f"- **Total positive pairs:** {ls['total_positive_pairs']:,}\n"
    report += f"- **Resumes per job:** {ls['resumes_per_job']} (uniform)\n"
    report += f"- **Negative pairs:** {ls['negative_pairs']}\n\n"

    report += "### 2.4 Relevance Definition\n\n"
    rd = en_audit["relevance_definition"]
    report += f"- **Stated:** {rd['stated_definition']}\n"
    report += f"- **Inferred:** {rd['inferred_definition']}\n"
    report += f"- **Scope:** {rd['relevance_scope']}\n\n"

    report += "### 2.5 Synthetic Assessment\n\n"
    sa = en_audit["synthetic_assessment"]
    report += f"- **Is synthetic:** {sa['is_synthetic']} (confidence: {sa['confidence']})\n"
    report += f"- **Implication:** {sa['implication']}\n\n"

    # ── Section 3: Label Audit Table ──
    report += "---\n\n## 3. Label Audit Table\n\n"
    # Build markdown table manually to avoid tabulate dependency
    cols = audit_table.columns.tolist()
    report += "| " + " | ".join(cols) + " |\n"
    report += "| " + " | ".join(["---"] * len(cols)) + " |\n"
    for _, row in audit_table.iterrows():
        report += "| " + " | ".join(str(row[c]) for c in cols) + " |\n"
    report += "\n\n"

    # ── Section 4: Semantic Incompatibilities ──
    report += "---\n\n## 4. Semantic Incompatibilities\n\n"
    report += "| # | Type | Severity | Description | Resolution |\n|---|---|---|---|---|\n"
    for i, inc in enumerate(incompatibilities, 1):
        sev_icon = {"HIGH": "🔴", "MEDIUM": "🟠", "LOW": "🔵"}.get(inc["severity"], "❓")
        report += (
            f"| {i} | {inc['type']} | {sev_icon} {inc['severity']} | "
            f"{inc['description']} | {inc['resolution']} |\n"
        )
    report += "\n"

    # ── Section 5: Label Compatibility Summary ──
    report += "---\n\n## 5. Label Compatibility Summary\n\n"
    report += "### Can we directly merge labels?\n\n"
    report += "**No.** The datasets have fundamentally different:\n\n"
    report += "1. **Label scales** (1-9 continuous vs. implicit binary)\n"
    report += "2. **Relevance definitions** (multi-dimensional scored vs. binary list membership)\n"
    report += "3. **Negative sampling** (explicit vs. none)\n"
    report += "4. **Data authenticity** (semi-real vs. fully synthetic)\n"
    report += "5. **Annotation methodology** (4-model LLM consensus vs. unknown)\n\n"

    report += "### Recommended approach\n\n"
    report += "1. Define a common 0-3 relevance rubric (Phase 3)\n"
    report += "2. Map Vietnamese 1-9 scores to 0-3 using calibrated thresholds\n"
    report += "3. For English, assign common labels using structured feature analysis\n"
    report += "4. Mark all mappings with confidence levels\n"
    report += "5. Create human-validated test set for both languages\n"
    report += "6. Track original labels alongside harmonized labels\n\n"

    return report, {
        "vietnamese_audit": vn_audit,
        "english_audit": en_audit,
        "audit_table": audit_table.to_dict(orient="records"),
        "incompatibilities": incompatibilities,
    }


if __name__ == "__main__":
    report_text, audit_data = generate_label_audit_report()

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / "04_label_semantic_audit.md"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report_text)

    json_path = REPORTS_DIR / "04_label_semantic_audit.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(audit_data, f, ensure_ascii=False, indent=2, default=str)

    print(f"Report saved to: {report_path}")
    print(f"Audit data saved to: {json_path}")
