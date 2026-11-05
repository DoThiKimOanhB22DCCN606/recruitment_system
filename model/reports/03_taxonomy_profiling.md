# Taxonomy Resources Profiling Report

**Dataset:** ESCO & O*NET  
**Generated:** 2026-10-31 12:37:29  
**Pipeline:** Multilingual CV-JD Matching System  

---

## 1. ESCO Taxonomy

### Occupations: 237 records
Columns: `uri`, `title`

### Occupation-Skill Relations: 9,964 records
- Unique occupations: 234
- Unique skills: 3479
- Relation types: {'optional': 5662, 'essential': 4302}

### Skill Broader Relations (Hierarchy): 14,833 records
- Unique skills: 13181
- Unique broader concepts: 2224
- Skill types: {}

### ⚠️ Empty Files: skills_list.csv, skill_relations.csv

---

## 2. O*NET Taxonomy

### Technology Skills: 32,773 records
- Unique O*NET-SOC codes: 923
- Unique job titles: 923
- Hot Technology: {'N': 21247, 'Y': 11526}

### ESCO-O*NET Crosswalk: 8,632 records
Columns: `O*NET-SOC 2019 Crosswalks`, `Unnamed: 1`, `Unnamed: 2`, `Unnamed: 3`

### Additional Excel Files

| File | Size |
|---|---|
| Alternate_Titles.xlsx | 1,293,637 bytes |
| ESCO_to_ONET-SOC.xlsx | 213,061 bytes |
| Occupation_Data.xlsx | 102,540 bytes |
| Technology_Skills.xlsx | 1,239,112 bytes |

---

## 3. Additional Taxonomy Resources

### `skills.csv`
- total_rows: 13939
- columns: ['id', 'description']
- size_bytes: 6223383

### `skills_with_names.csv`
- total_rows: 13939
- columns: ['uri', 'skill_id', 'preferred_label', 'alt_labels', 'skill_type', 'description_en']
- size_bytes: 6643787

### `job_term_taxonomy`
- total_entries: 3684
- type: list

### `job_terms_unique`
- total_rows: 3684
- columns: ['term', 'frequency']

