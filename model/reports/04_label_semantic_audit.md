# Label Semantic Audit Report

**Dataset:** Vietnamese & English Datasets  
**Generated:** 2026-10-31 12:42:10  
**Pipeline:** Multilingual CV-JD Matching System  

---

## 1. Vietnamese Dataset Label Audit

### 1.1 Label Format

- **Scale:** Continuous 1-9 scale (from LLM consensus)
- **Consensus method:** Median of annotator scores
- **Relevant threshold:** ≥ 5
- **Annotators:** claude, cohere, gptoss, llama

### 1.2 Labeling Method

- **Type:** LLM-generated (automated)
- **Human annotations:** False
- **Guidelines:** Not explicitly documented; each LLM used its own interpretation

### 1.3 CV Generation

- **Method:** LLM-generated (llama-3.3-70b-versatile)
- **Total CVs:** 130
- **Is synthetic:** True
- **Implication:** CVs are synthetic, not real candidate resumes. This means the relevance judgments are about synthetic CV vs. real JD pairs.

### 1.4 Score Distribution

- Range: 1.0 - 9.0
- Mean: 5.19, Median: 5.0, Std: 1.96
- Below threshold (<5): 905 (36.9%)
- At/above threshold (≥5): 1545

### 1.5 Annotator Tendencies

| Model | Mean Relevance | Tendency |
|---|---|---|
| claude | 4.64 | strict |
| cohere | 5.8 | lenient |
| gptoss | 5.78 | lenient |
| llama | 5.22 | lenient |

### 1.6 Inter-Annotator Disagreement

- Mean range: 1.95
- Median range: 2.0
- High disagreement (>4): 44 (1.8%)
- Max range: 7.0

### 1.7 Relevance Definition

- **Stated:** Not formally documented
- **Positive:** Median relevance score ≥ 5 on 1-9 scale
- **Negative:** Median relevance score < 5 on 1-9 scale
- **Scope:** Overall suitability (multi-dimensional: skills, experience, education)
- **Scoring dimensions:** relevance_score (overall), skills_match, experience_match, education_match

---

## 2. English Dataset Label Audit

### 2.1 Label Format

- **Format:** Implicit binary (presence in relevant_resume_ids = positive)

### 2.2 Labeling Method

- **Type:** Unknown (likely rule-based or LLM-generated)
- **Human annotations:** False
- **Evidence of synthetic generation:**
  - Perfectly balanced seniority distribution (625 each for Jr/Mid/Sr/Lead)
  - Perfectly balanced industry distribution (100 each for 25 industries)
  - Exactly 30 relevant resumes per job (uniform)
  - All data appears synthetically generated

### 2.3 Label Structure

- **Type:** Positive-only (no explicit negatives)
- **Total positive pairs:** 75,000
- **Resumes per job:** 30 (uniform)
- **Negative pairs:** None explicitly provided

### 2.4 Relevance Definition

- **Stated:** Not formally documented
- **Inferred:** A resume is 'relevant' to a job if it appears in the job's relevant_resume_ids list. The criteria for relevance are unknown.
- **Scope:** Unknown (could be skill-based, role-based, or overall suitability)

### 2.5 Synthetic Assessment

- **Is synthetic:** True (confidence: Very high)
- **Implication:** Both the data AND the match labels are likely synthetically generated. Labels may reflect rule-based matching (e.g., industry/seniority/skill overlap) rather than genuine human relevance judgments.

---

## 3. Label Audit Table

| source_dataset | original_label | original_definition | labeling_method | positive_definition | negative_definition | relevance_scope | label_reliability | mapping_status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Vietnamese (turn1300_v3_union_final) | relevance_median_no_cohere (1-9 scale) | Median of 3 LLM relevance scores (Claude, GPToss, Llama) | LLM-generated (automated, 4 models, consensus by median) | Median score ≥ 5 on 1-9 scale | Median score < 5 on 1-9 scale | Multi-dimensional: skills, experience, education, overall | Partially verified (LLM consensus, no human validation) | Conditional mapping (1-9 → 0-3 requires calibration) |
| Vietnamese (per-annotator) | relevance_score per model (1-9 scale) | Individual LLM model's relevance assessment | LLM-generated (individual model) | Score ≥ 5 | Score < 5 | Overall suitability with justification | Unknown (no inter-model calibration guarantee) | Conditional mapping (individual scores less reliable than consensus) |
| English (matches) | Presence in relevant_resume_ids (binary) | Resume is listed as relevant to job | Unknown (likely rule-based or synthetic generation) | Resume ID appears in job's relevant_resume_ids | Not available (unlisted ≠ irrelevant) | Unknown (criteria not documented) | Unknown (synthetic data, no annotation methodology) | Conditional mapping (positive → label ≥ 1, but grade unknown) |


---

## 4. Semantic Incompatibilities

| # | Type | Severity | Description | Resolution |
|---|---|---|---|---|
| 1 | Different label scales | 🔴 HIGH | Vietnamese uses continuous 1-9 scale (from LLM median). English uses implicit binary (present/absent in match list). These cannot be directly aligned. | Map Vietnamese 1-9 to common 0-3 rubric via calibrated thresholds. Map English positives to common rubric using structured feature analysis. |
| 2 | Different matching criteria | 🔴 HIGH | Vietnamese: multi-dimensional (skills + experience + education) with justifications. English: unknown criteria (possibly rule-based on metadata similarity). | Use Vietnamese justifications to understand scoring criteria. For English, analyze matches to reverse-engineer matching criteria. |
| 3 | Different annotation quality | 🟠 MEDIUM | Vietnamese: 4 LLM annotators with consensus mechanism and justifications. English: unknown annotation source with no quality assessment. | Vietnamese labels have better provenance but still lack human validation. Both require human-validated test set. |
| 4 | Different data authenticity | 🟠 MEDIUM | Vietnamese: real JDs from TopCV + synthetic CVs from LLM. English: both jobs and resumes appear fully synthetic. | Document data provenance. Consider that model may learn different patterns from synthetic vs. semi-real data. |
| 5 | Different definitions of positive and negative | 🔴 HIGH | Vietnamese: explicit positive and negative pairs (above/below threshold). English: positive-only (no confirmed negatives). Absent pairs are unlabeled, not negative. | Do not automatically assign label 0 to unlisted English pairs. Use PU-learning techniques or careful negative sampling. |
| 6 | Different relevance granularity | 🟠 MEDIUM | Vietnamese provides fine-grained 1-9 scores enabling graded relevance. English provides only binary (relevant/not-in-list) with no granularity. | For the common rubric (0-3), Vietnamese can be mapped using score thresholds. English positives need scoring based on structured feature analysis. |

---

## 5. Label Compatibility Summary

### Can we directly merge labels?

**No.** The datasets have fundamentally different:

1. **Label scales** (1-9 continuous vs. implicit binary)
2. **Relevance definitions** (multi-dimensional scored vs. binary list membership)
3. **Negative sampling** (explicit vs. none)
4. **Data authenticity** (semi-real vs. fully synthetic)
5. **Annotation methodology** (4-model LLM consensus vs. unknown)

### Recommended approach

1. Define a common 0-3 relevance rubric (Phase 3)
2. Map Vietnamese 1-9 scores to 0-3 using calibrated thresholds
3. For English, assign common labels using structured feature analysis
4. Mark all mappings with confidence levels
5. Create human-validated test set for both languages
6. Track original labels alongside harmonized labels

