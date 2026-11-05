# Label Harmonization Report

**Dataset:** Vietnamese & English Datasets → Common 0-3 Rubric  
**Generated:** 2026-10-31 12:43:33  
**Pipeline:** Multilingual CV-JD Matching System  

---

## 1. Vietnamese Label Harmonization

**Total pairs:** 2,450

### Original Score Distribution (relevance_median_no_cohere)

- Mean: 5.19, Median: 5.00
- Min: 1.0, Max: 9.0

### Harmonized Label Distribution (0-3)

| Label | Name | Count | Percentage |
|---|---|---|---|
| 0 | Not Relevant | 291 | 11.9% |
| 1 | Low Relevance | 614 | 25.1% |
| 2 | Relevant | 848 | 34.6% |
| 3 | Highly Relevant | 697 | 28.4% |

### Mapping Status

| Status | Count | Percentage |
|---|---|---|
| direct_mapping | 2,406 | 98.2% |
| conditional_mapping | 44 | 1.8% |

### Label Confidence

| Confidence | Count | Percentage |
|---|---|---|
| high | 2,406 | 98.2% |
| medium | 44 | 1.8% |

---

## 2. English Label Harmonization

**Total positive pairs:** 75,000

### Harmonized Label Distribution (0-3)

| Label | Name | Count | Percentage |
|---|---|---|---|
| 0 | Not Relevant | 0 | 0.0% |
| 1 | Low Relevance | 9,893 | 13.2% |
| 2 | Relevant | 34,868 | 46.5% |
| 3 | Highly Relevant | 30,239 | 40.3% |

### Structured Feature Analysis

| Feature | Mean | Median |
|---|---|---|
| must_coverage | 0.775 | 0.750 |
| nice_coverage | 0.621 | 1.000 |

**Seniority alignment:**

- close: 33,051 (44.1%)
- exact_match: 25,190 (33.6%)
- mismatch: 16,759 (22.3%)

**Industry match:**

- mismatch: 67,503 (90.0%)
- match: 7,497 (10.0%)

---

## 3. Combined Summary

**Total harmonized pairs:** 77,450

| Source | Pairs | % of Total |
|---|---|---|
| Vietnamese | 2,450 | 3.2% |
| English | 75,000 | 96.8% |

### Combined Label Distribution

| Label | Name | Vietnamese | English | Total |
|---|---|---|---|---|
| 0 | Not Relevant | 291 | 0 | 291 |
| 1 | Low Relevance | 614 | 9,893 | 10,507 |
| 2 | Relevant | 848 | 34,868 | 35,716 |
| 3 | Highly Relevant | 697 | 30,239 | 30,936 |

### Potential Sources of Label Noise

1. **Vietnamese:** LLM annotators may have inconsistent relevance definitions
2. **Vietnamese:** Boundary scores (near 2.5, 4.5, 6.5) may be misclassified
3. **Vietnamese:** CVs are synthetic — relevance may not reflect real-world patterns
4. **English:** Unknown match criteria — feature-based grading may not match intent
5. **English:** Exact string matching for skills understates actual overlap
6. **Both:** No human validation performed yet

