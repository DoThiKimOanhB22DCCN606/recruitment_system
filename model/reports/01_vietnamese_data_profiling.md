# Vietnamese CV-JD Dataset Profiling Report

**Dataset:** Vietnamese Data (turn1300_v3_union_final)  
**Generated:** 2026-10-31 12:37:15  
**Pipeline:** Multilingual CV-JD Matching System  

---

## 1. Jobs Dataset

**Source:** `topcv_balanced_1300.csv`  
**Total Records:** 1,300  
**Columns:** 31  

### Schema

| Column | Type | Missing |
|---|---|---|
| `title` | object | 0 (0.0%) |
| `url` | object | 0 (0.0%) |
| `company` | object | 0 (0.0%) |
| `company_address` | object | 65 (5.0%) |
| `crawled_date` | object | 0 (0.0%) |
| `category` | object | 0 (0.0%) |
| `job_id` | object | 0 (0.0%) |
| `deadline` | object | 65 (5.0%) |
| `is_expired` | bool | 0 (0.0%) |
| `job_salary` | object | 0 (0.0%) |
| `salary_min` | float64 | 430 (33.08%) |
| `salary_max` | float64 | 450 (34.62%) |
| `salary_type` | object | 0 (0.0%) |
| `salary_note` | object | 1299 (99.92%) |
| `has_commission` | bool | 0 (0.0%) |
| `job_location` | object | 62 (4.77%) |
| `education_level` | object | 11 (0.85%) |
| `education_field` | object | 1297 (99.77%) |
| `technical_skills` | object | 30 (2.31%) |
| `soft_skills` | object | 1131 (87.0%) |
| `languages` | object | 1215 (93.46%) |
| `certificates` | float64 | 1300 (100.0%) |
| `gender_requirement` | object | 0 (0.0%) |
| `experience` | object | 0 (0.0%) |
| `requirements_tags` | object | 65 (5.0%) |
| `specializations` | object | 65 (5.0%) |
| `job_description` | object | 0 (0.0%) |
| `job_requirements` | object | 0 (0.0%) |
| `job_benefits` | object | 0 (0.0%) |
| `working_time` | object | 194 (14.92%) |
| `category_key` | object | 0 (0.0%) |

### Category Distribution

| Category | Count |
|---|---|
| Bất động sản | 100 |
| Chăm sóc khách hàng | 100 |
| Giáo dục | 100 |
| Hành chính nhân sự | 100 |
| IT | 100 |
| Kế toán | 100 |
| Kỹ sư xây dựng | 100 |
| Lao động phổ thông | 100 |
| Marketing | 100 |
| Ngân hàng | 100 |
| Nhân viên kinh doanh | 100 |
| Telesales | 100 |
| Thiết kế đồ họa | 100 |

### Experience Distribution

| Experience | Count |
|---|---|
| 3 năm | 198 |
| Không yêu cầu | 184 |
| 2 năm | 182 |
| Dưới 1 năm | 176 |
| 5 năm | 165 |
| 1 năm | 159 |
| 4 năm | 125 |
| Trên 5 năm | 111 |

### job_description Text Length

- Min: 6 chars
- Max: 4776 chars
- Mean: 839.2 chars
- Median: 694.0 chars

### job_requirements Text Length

- Min: 6 chars
- Max: 3401 chars
- Mean: 516.1 chars
- Median: 452.0 chars

### title Text Length

- Min: 6 chars
- Max: 238 chars
- Mean: 57.1 chars
- Median: 48.0 chars

---

## 2. CVs Dataset

**Source:** `evaluation_cvs_130_canonical.json`  
**Total CVs:** 130  
**Generation Model:** llama-3.3-70b-versatile (LLM-generated, synthetic)  

### Schema

Columns: `cv_id`, `name`, `category_target`, `level`, `experience_years`, `experience_description`, `education_level`, `education_field`, `technical_skills`, `soft_skills`, `languages`, `certificates`, `gender`, `search_text`  

### Category Distribution

| Category | Count |
|---|---|
| Bất động sản | 10 |
| Chăm sóc khách hàng | 10 |
| Giáo dục | 10 |
| Hành chính nhân sự | 10 |
| IT | 10 |
| Kế toán | 10 |
| Kỹ sư xây dựng | 10 |
| Lao động phổ thông | 10 |
| Marketing | 10 |
| Ngân hàng | 10 |
| Nhân viên kinh doanh | 10 |
| Telesales | 10 |
| Thiết kế đồ họa | 10 |

### Level Distribution

| Level | Count |
|---|---|
| L1 | 13 |
| L1b | 13 |
| L2 | 13 |
| L2b | 13 |
| L3 | 13 |
| L3b | 13 |
| L4 | 13 |
| L4b | 13 |
| P | 13 |
| P2 | 13 |

---

## 3. Labels Dataset (LLM Consensus)

**Source:** `labels_2450_compact.csv`  
**Total Pairs:** 2,450  
**Unique CVs:** 130  
**Unique Jobs:** 675  
**Scale:** 1-9 continuous (LLM relevance scores)  

### Relevance Score Statistics

| Metric | Min | Max | Mean | Median | Std |
|---|---|---|---|---|---|
| `relevance_median_4model` | 1.0 | 9.0 | 5.38 | 5.5 | 1.88 |
| `relevance_median_no_cohere` | 1.0 | 9.0 | 5.19 | 5.0 | 1.96 |
| `relevance_mean_4model` | 1.0 | 9.0 | 5.36 | 5.5 | 1.78 |
| `relevance_mean_no_cohere` | 1.0 | 9.0 | 5.21 | 5.33 | 1.81 |

### Relevance Score Distribution (binned)

**relevance_median_no_cohere:**

| Range | Count |
|---|---|
| 1-2 | 291 |
| 3-4 | 614 |
| 5-6 | 848 |
| 7-8 | 656 |
| 9-10 | 41 |

**relevance_median_4model:**

| Range | Count |
|---|---|
| 1-2 | 178 |
| 3-4 | 516 |
| 5-6 | 849 |
| 7-8 | 862 |
| 9-10 | 45 |

### Binary Relevance (threshold ≥ 5)

- **is_relevant_median_4model_ge5:** True=1563, False=887
- **is_relevant_median_no_cohere_ge5:** True=1545, False=905

### Inter-Annotator Disagreement (score range)

| Metric | Mean Range | Median Range | High Disagree (>4) |
|---|---|---|---|
| `range_relevance_4model` | 2.26 | 2.0 | 92 (3.76%) |
| `range_relevance_no_cohere` | 1.95 | 2.0 | 44 (1.8%) |

### Pairs by CV Category

| Category | Count |
|---|---|
| Chăm sóc khách hàng | 200 |
| Hành chính nhân sự | 200 |
| Kế toán | 197 |
| IT | 196 |
| Kỹ sư xây dựng | 196 |
| Bất động sản | 195 |
| Nhân viên kinh doanh | 194 |
| Giáo dục | 191 |
| Telesales | 182 |
| Marketing | 180 |
| Ngân hàng | 176 |
| Lao động phổ thông | 173 |
| Thiết kế đồ họa | 170 |

---

## 4. Per-Model Annotation Statistics

| Model | Annotations | Relevance Mean | Relevance Median | Skills Mean | Exp. Mean |
|---|---|---|---|---|---|
| claude | 2450 | 4.64 | 5.0 | 4.74 | 5.36 |
| cohere | 2450 | 5.8 | 7.0 | 5.4 | 6.79 |
| gptoss | 2450 | 5.78 | 6.0 | 5.43 | 5.63 |
| llama | 2450 | 5.22 | 6.0 | 5.02 | 5.68 |

### Per-Model Relevance Score Distribution

**claude:**

| Score | Count |
|---|---|
| 1 | 112 |
| 2 | 288 |
| 3 | 428 |
| 4 | 372 |
| 5 | 347 |
| 6 | 396 |
| 7 | 295 |
| 8 | 175 |
| 9 | 37 |

**cohere:**

| Score | Count |
|---|---|
| 1 | 55 |
| 2 | 115 |
| 3 | 222 |
| 4 | 236 |
| 5 | 391 |
| 6 | 181 |
| 7 | 756 |
| 8 | 465 |
| 9 | 29 |

**gptoss:**

| Score | Count |
|---|---|
| 1 | 7 |
| 2 | 117 |
| 3 | 134 |
| 4 | 269 |
| 5 | 642 |
| 6 | 383 |
| 7 | 384 |
| 8 | 382 |
| 9 | 132 |

**llama:**

| Score | Count |
|---|---|
| 0 | 3 |
| 1 | 62 |
| 2 | 383 |
| 3 | 149 |
| 4 | 341 |
| 5 | 221 |
| 6 | 509 |
| 7 | 326 |
| 8 | 401 |
| 9 | 55 |

---

## 5. Inter-Annotator Agreement Analysis

**Models:** claude, cohere, gptoss, llama  
**Recommended Label:** `consensus_4model.relevance_score.median`  
**Relevant Threshold:** ≥ 5  

### Model Mean Scores

| Model | Relevance | Skills | Experience | Education |
|---|---|---|---|---|
| claude | 4.64 | 4.74 | 5.36 | 8.20 |
| cohere | 5.80 | 5.40 | 6.79 | 7.27 |
| gptoss | 5.78 | 5.43 | 5.63 | 8.44 |
| llama | 5.22 | 5.02 | 5.68 | 7.66 |

### 4-Model Agreement

| Metric | Avg Range | Median Range | High Disagree (>4) % |
|---|---|---|---|
| relevance_score | 2.2563 | 2.0 | 3.76% |
| skills_match | 2.6041 | 2.0 | 10.12% |
| experience_match | 2.9931 | 3.0 | 19.31% |
| education_match | 2.5302 | 2.0 | 10.0% |

---

## 6. Data Quality Issues

| # | Severity | Category | Description | Recommendation |
|---|---|---|---|---|
| 1 | ℹ️ INFO | Coverage | Labels cover 675/1300 jobs (51.9%) | Document which jobs lack labels and why |
| 2 | 🟡 WARNING | Data Generation | Vietnamese CVs were generated by LLM (llama-3.3-70b-versatile). They are synthetic, not real candidate resumes. | Document this as a limitation. Real CV data would improve model reliability. |
| 3 | 🟡 WARNING | Label Quality | All 4 annotators are LLM models (Claude, Cohere, GPToss, Llama). No human annotations exist. | Create a human-validated test set as specified in the project requirements. |
| 4 | 🟠 MEDIUM | Agreement | range_relevance_4model: 3.76% of pairs have range > 4 (high inter-annotator disagreement) | Review high-disagreement samples for human relabeling |
| 5 | 🟠 MEDIUM | Agreement | range_relevance_no_cohere: 1.8% of pairs have range > 4 (high inter-annotator disagreement) | Review high-disagreement samples for human relabeling |
| 6 | 🔵 LOW | Missing Data | Jobs have significant missing data: certificates=1300, salary_note=1299, education_field=1297, languages=1215, soft_skills=1131 | Assess impact on relevance evaluation |
| 7 | ℹ️ INFO | Taxonomy | ESCO skills_list.csv and skill_relations.csv are empty (0 rows) | Use occupation_skills.csv and skill_broader_relations.csv instead |

