# English CV-JD Dataset Profiling Report

**Dataset:** English Data (HuggingFace Parquet)  
**Generated:** 2026-10-31 12:37:26  
**Pipeline:** Multilingual CV-JD Matching System  

---

## 1. Jobs Dataset

**Source:** `jobs/train-00000-of-00001.parquet`  
**Total Records:** 2,500  
**Unique Job IDs:** 2,500  
**Duplicate Job IDs:** 0  

### Seniority Distribution

| Seniority | Count |
|---|---|
| Junior | 855 |
| Mid | 825 |
| Senior | 820 |

### Industry Distribution (10 industries)

| Industry | Count |
|---|---|
| Healthcare | 290 |
| FinTech | 279 |
| SaaS | 271 |
| Logistics | 248 |
| Cybersecurity | 247 |
| Travel | 243 |
| Gaming | 241 |
| Retail | 237 |
| EdTech | 225 |
| E-commerce | 219 |
| ... | (all 10 have equal counts) |

### Skills & Content Statistics

| Metric | Min | Max | Mean | Median |
|---|---|---|---|---|
| Must-have skills/job | 3 | 5 | 4.0 | 4.0 |
| Nice-to-have skills/job | 1 | 1 | 1.0 | 1.0 |
| Responsibilities/job | 3 | 3 | 3.0 | 3.0 |
| Requirements/job | 3 | 3 | 3.0 | 3.0 |
| Description length (chars) | 52 | 79 | 64.49 | 64.0 |

---

## 2. Resumes Dataset

**Source:** `resumes/train-00000-of-00001.parquet`  
**Total Records:** 10,000  
**Unique Resume IDs:** 10,000  

### Years of Experience

- Range: 0 - 12 years
- Mean: 4.63, Median: 4.0, Std: 3.6

### Education Distribution

| Education | Count |
|---|---|
| BSc | 2071 |
| MBA | 2031 |
| MSc | 1980 |
| BA | 1965 |
| High School | 1953 |

### Content Statistics

| Metric | Min | Max | Mean | Median |
|---|---|---|---|---|
| Skills/resume | 5 | 8 | 6.49 | 6.0 |
| Experience bullets/resume | 3 | 3 | 3.0 | 3.0 |
| Summary length (chars) | 46 | 74 | 58.56 | 58.0 |

---

## 3. Matches Dataset

**Source:** `matches/train-00000-of-00001.parquet`  
**Total Match Records:** 2,500  
**Total Positive Pairs:** 75,000  
**Unique Resume IDs Referenced:** 9,971  
**Duplicate Relationships:** 0  

### Relevant Resumes per Job

- Min: 30, Max: 30, Mean: 30.0, Median: 30.0
- All same count: **True**

---

## 4. Foreign Key Validation

- All job FK valid: **True**
- All resume FK valid: **True**
- Orphan job IDs: 0
- Orphan resume IDs: 0
- Unreferenced jobs: 0
- Unreferenced resumes: 29

---

## 5. Synthetic Data Assessment

| Indicator | Jobs | Resumes |
|---|---|---|
| Seniority perfectly balanced | False | False |
| Industry perfectly balanced | False | False |
| Uniform match count (30 each) | — | — |

**Conclusion:** The English dataset is almost certainly **synthetically generated**.

---

## 6. Data Quality Issues

| # | Severity | Category | Description | Recommendation |
|---|---|---|---|---|
| 1 | 🟡 WARNING | Synthetic Data | Every job has exactly 30 relevant resumes. This is unrealistic and confirms synthetic generation. | Document this limitation. The uniformity may affect negative sampling and evaluation. |
| 2 | 🟠 MEDIUM | Label Coverage | Only positive match relationships exist. No verified negative pairs. Unlisted pairs are unlabeled, not confirmed negative. | Use positive-unlabeled learning approach. Generate negatives carefully with false-negative safeguards. |
| 3 | 🔵 LOW | Coverage | 29 resumes are never referenced in any match. | Investigate whether these should be excluded or are valid negatives. |
| 4 | 🟠 MEDIUM | Label Quality | Match annotation methodology is unknown. The synthetic data patterns suggest rule-based or LLM-based generation. | Treat labels as weak supervision. Validate a sample against the common relevance rubric. |

