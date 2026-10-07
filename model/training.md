# CV–JD Matching Model — Training Specification

## 1. Overview

This document defines the data format, preprocessing pipeline, dataset construction, model training, evaluation, and inference process for the Vietnamese CV–Job Description (CV–JD) Matching model.

The standardized dataset is stored in **Apache Parquet** format.

### Objective

Given a candidate CV and a Job Description (JD), the model predicts the degree of compatibility between the candidate and the job.

```text
CV + Job Description
        ↓
   CV-JD Matcher
        ↓
 Matching Score
        ↓
Candidate Ranking / Job Recommendation
```

The matching score represents the model-estimated relevance between a CV and a JD. It does not represent the probability that a candidate will be hired.

---

# 2. Data flow

```text
Raw CV
Raw JD
   ↓
Cleaning
   ↓
Normalization
   ↓
Skill Normalization
   ↓
cv.parquet + jd.parquet
   ↓
CV-JD Pair Construction
   ↓
cv_jd_pairs.parquet
   ↓
Entity-based Split
   ↓
train.parquet
validation.parquet
test.parquet
```

---


# 4. Standardized CV Dataset

File:

```text
data/processed/cv.parquet
```

Each row represents one candidate.

## Schema

| Column           | Type         | Required | Description                      |
| ---------------- | ------------ | -------- | -------------------------------- |
| `candidate_id`   | string       | Yes      | Unique candidate identifier      |
| `skills`         | list[string] | Yes      | Normalized skills                |
| `experience`     | list[struct] | Yes      | Work experience                  |
| `education`      | list[struct] | No       | Education information            |
| `certifications` | list[string] | No       | Certifications                   |
| `languages`      | list[string] | No       | Languages                        |
| `cv_text`        | string       | Yes      | Canonical textual representation |

Example record:

```text
candidate_id:
C001

skills:
["Java", "Spring Boot", "PostgreSQL", "Docker"]

experience:
[
    {
        "title": "Backend Developer",
        "years": 2.5,
        "skills": ["Java", "Spring Boot"]
    }
]

education:
[
    {
        "degree": "Bachelor",
        "major": "Computer Science"
    }
]

certifications:
[]

languages:
["Vietnamese", "English"]

cv_text:
"Candidate Profile...
 Skills: Java, Spring Boot, PostgreSQL, Docker..."
```

`candidate_id` must never be used as a model feature.

---

# 5. Standardized JD Dataset

File:

```text
data/processed/jd.parquet
```

Each row represents one job posting.

## Schema

| Column                 | Type         | Required | Description                      |
| ---------------------- | ------------ | -------- | -------------------------------- |
| `job_id`               | string       | Yes      | Unique job identifier            |
| `title`                | string       | Yes      | Job title                        |
| `required_skills`      | list[string] | Yes      | Required skills                  |
| `preferred_skills`     | list[string] | No       | Preferred skills                 |
| `min_experience_years` | float        | No       | Minimum required experience      |
| `education`            | list[string] | No       | Required education               |
| `location`             | string       | No       | Job location                     |
| `job_text`             | string       | Yes      | Canonical textual representation |

Example:

```text
job_id:
J001

title:
"Java Backend Developer"

required_skills:
["Java", "Spring Boot", "PostgreSQL"]

preferred_skills:
["Docker", "Kafka"]

min_experience_years:
2.0

education:
["Computer Science", "Software Engineering"]

location:
"Hanoi"

job_text:
"Job Title:
Java Backend Developer

Required Skills:
Java, Spring Boot, PostgreSQL

Preferred Skills:
Docker, Kafka

Minimum Experience:
2 years"
```

`job_id` must never be used as a model feature.

---

# 6. CV Text and JD Text

The model requires textual input.

Therefore the preprocessing pipeline generates:

```text
cv_text
job_text
```

from the structured fields.

## CV text template

```text
Candidate Profile

Skills:
{skills}

Experience:
{experience}

Education:
{education}

Certifications:
{certifications}

Languages:
{languages}
```

## JD text template

```text
Job Title:
{title}

Required Skills:
{required_skills}

Preferred Skills:
{preferred_skills}

Minimum Experience:
{min_experience_years}

Education:
{education}

Location:
{location}
```

The template must remain unchanged between training and inference.

---

# 7. Skill Normalization

Skills must be normalized before the Parquet dataset is generated.

Examples:

```text
JS                   → JavaScript
Node                  → Node.js
Postgres              → PostgreSQL
Spring Framework      → Spring
```

Different technologies must not be incorrectly merged.

Examples:

```text
Java          ≠ JavaScript
React         ≠ React Native
AWS           ≠ AWS Lambda
```

Store the normalization dictionary separately:

```text
preprocessing/
└── skill_aliases.json
```

Record the version of the normalization dictionary in the dataset metadata.

---

# 8. CV–JD Matching Pair Dataset

File:

```text
data/processed/cv_jd_pairs.parquet
```

Each row represents one CV–JD training example.

## Schema

| Column            | Type   | Description                         |
| ----------------- | ------ | ----------------------------------- |
| `candidate_id`    | string | Candidate identifier                |
| `job_id`          | string | Job identifier                      |
| `cv_text`         | string | Standardized CV text                |
| `job_text`        | string | Standardized JD text                |
| `label`           | int8   | Relevance label                     |
| `pair_type`       | string | Positive / Negative / Hard Negative |
| `dataset_version` | string | Dataset version                     |

Example:

```text
candidate_id:
C001

job_id:
J001

cv_text:
"Skills: Java, Spring Boot, PostgreSQL..."

job_text:
"Title: Java Backend Developer..."

label:
2

pair_type:
"positive"

dataset_version:
"dataset-v1.0"
```

---

# 9. Matching Labels

Initial training uses three relevance levels:

```text
0 = Not suitable
1 = Partially suitable
2 = Highly suitable
```

## Label 2

```text
CV:
Java
Spring Boot
PostgreSQL
2.5 years

JD:
Java
Spring Boot
PostgreSQL
2+ years

Label:
2
```

## Label 1

```text
CV:
Java
Spring Boot
1 year

JD:
Java
Spring Boot
PostgreSQL
2+ years

Label:
1
```

## Label 0

```text
CV:
Python
Django

JD:
Java
Spring Boot
PostgreSQL

Label:
0
```

The exact labeling rules must be defined in:

```text
LABELING_GUIDELINE.md
```

---

# 10. Negative Sampling

The pair dataset must contain:

```text
Positive samples
Negative samples
Hard negative samples
```

## Positive

CV and JD have strong compatibility.

```text
pair_type = "positive"
label = 2
```

## Negative

CV and JD have very low compatibility.

```text
pair_type = "negative"
label = 0
```

## Hard Negative

CV and JD have substantial overlap but important differences.

```text
pair_type = "hard_negative"
label = 0 or 1
```

Example:

```text
CV:
Java
Spring Boot
MySQL
2 years

JD:
Java
Spring Boot
PostgreSQL
5 years
Kafka required
```

Hard negatives should be retained because they force the model to distinguish semantic relevance from simple keyword overlap.

---

# 11. Data Validation

Before saving Parquet files, perform validation.

Required checks:

```text
[ ] candidate_id is not null
[ ] job_id is not null
[ ] candidate_id is unique in cv.parquet
[ ] job_id is unique in jd.parquet
[ ] cv_text is not empty
[ ] job_text is not empty
[ ] label ∈ {0, 1, 2}
[ ] no duplicated CV-JD pair
[ ] no invalid skill values
[ ] no corrupted records
```

Example validation:

```python
assert df["candidate_id"].notna().all()
assert df["cv_text"].str.len().gt(0).all()
assert df["label"].isin([0, 1, 2]).all()
```

---

# 12. Saving Data as Parquet

Use PyArrow as the Parquet engine.

```python
import pandas as pd

df.to_parquet(
    "data/processed/cv.parquet",
    engine="pyarrow",
    index=False
)
```

For JD:

```python
jd_df.to_parquet(
    "data/processed/jd.parquet",
    engine="pyarrow",
    index=False
)
```

For matching pairs:

```python
pairs_df.to_parquet(
    "data/processed/cv_jd_pairs.parquet",
    engine="pyarrow",
    index=False
)
```

---

# 13. Recommended Data Types

Use explicit data types whenever possible.

Example:

```python
df["candidate_id"] = df["candidate_id"].astype("string")
df["job_id"] = df["job_id"].astype("string")
df["label"] = df["label"].astype("int8")
```

For numeric experience:

```python
df["min_experience_years"] = (
    df["min_experience_years"]
    .astype("float32")
)
```

Avoid storing numeric fields as strings.

Incorrect:

```text
"2 years"
"3 years"
"5+ years"
```

Preferred:

```text
2.0
3.0
5.0
```

The original raw value can be retained separately when needed for traceability.

---

# 14. Dataset Splitting

Final datasets:

```text
data/splits/
├── train.parquet
├── validation.parquet
└── test.parquet
```

Recommended initial ratio:

```text
Train       80%
Validation  10%
Test        10%
```

The split must be based on entity IDs rather than individual CV–JD pairs.

## Incorrect

```text
Train:
C001 + J001
C001 + J002

Test:
C001 + J003
```

## Correct

```text
Train:
C001 ... C800

Validation:
C801 ... C900

Test:
C901 ... C1000
```

This reduces data leakage caused by the same candidate appearing in multiple splits.

---

# 15. Creating Train / Validation / Test Parquet

Example:

```python
train_df.to_parquet(
    "data/splits/train.parquet",
    engine="pyarrow",
    index=False
)

val_df.to_parquet(
    "data/splits/validation.parquet",
    engine="pyarrow",
    index=False
)

test_df.to_parquet(
    "data/splits/test.parquet",
    engine="pyarrow",
    index=False
)
```

The test dataset must not be modified based on test results.

---

# 16. Loading Parquet for Training

Pandas:

```python
import pandas as pd

train_df = pd.read_parquet(
    "data/splits/train.parquet"
)

val_df = pd.read_parquet(
    "data/splits/validation.parquet"
)

test_df = pd.read_parquet(
    "data/splits/test.parquet"
)
```

Hugging Face Datasets:

```python
from datasets import load_dataset

dataset = load_dataset(
    "parquet",
    data_files={
        "train": "data/splits/train.parquet",
        "validation": "data/splits/validation.parquet",
        "test": "data/splits/test.parquet"
    }
)
```

The resulting dataset can then be passed to the tokenizer and training pipeline.

---

# 17. Dataset Versioning

Every processed dataset should have a version.

Example:

```text
dataset-v1.0
dataset-v1.1
dataset-v2.0
```

Recommended metadata:

```text
Dataset version
Source datasets
Number of CV records
Number of JD records
Number of CV-JD pairs
Number of positive samples
Number of negative samples
Number of hard negatives
Normalization version
Labeling version
Creation date
```

Example:

```json
{
  "dataset_version": "dataset-v1.0",
  "cv_count": 10000,
  "jd_count": 5000,
  "pair_count": 50000,
  "positive_count": 18000,
  "negative_count": 22000,
  "hard_negative_count": 10000,
  "normalization_version": "skill-normalizer-v1.0",
  "labeling_version": "labeling-v1.0"
}
```

Store the metadata in:

```text
data/
└── dataset_metadata.json
```

---

# 18. Recommended Final Data Flow

```text
Raw CSV / JSON / Other Sources
              ↓
        Data Cleaning
              ↓
       CV Normalization
       JD Normalization
              ↓
       Skill Normalization
              ↓
       ┌───────────────┐
       │   Parquet     │
       │               │
       │ cv.parquet    │
       │ jd.parquet    │
       └───────┬───────┘
               ↓
       CV-JD Pair Generation
               ↓
        Label Generation
               ↓
        Negative Sampling
               ↓
     cv_jd_pairs.parquet
               ↓
       Entity-based Split
               ↓
     ┌─────────┼──────────┐
     ↓         ↓          ↓
 train.parquet val.parquet test.parquet
     │         │          │
     └─────────┼──────────┘
               ↓
        Model Training
```

---

# 19. Definition of Done for Data Preparation

The dataset preparation phase is complete when:

* [ ] Raw CV data has been cleaned.
* [ ] Raw JD data has been cleaned.
* [ ] CV schema has been standardized.
* [ ] JD schema has been standardized.
* [ ] Skills have been normalized.
* [ ] `cv.parquet` has been generated.
* [ ] `jd.parquet` has been generated.
* [ ] CV–JD pairs have been generated.
* [ ] Matching labels have been assigned.
* [ ] Positive samples exist.
* [ ] Negative samples exist.
* [ ] Hard negative samples exist.
* [ ] Duplicate pairs have been removed.
* [ ] Data leakage has been checked.
* [ ] `train.parquet` has been generated.
* [ ] `validation.parquet` has been generated.
* [ ] `test.parquet` has been generated.
* [ ] Dataset metadata has been recorded.
* [ ] Dataset version has been assigned.
* [ ] Parquet schema and data types have been validated.
* [ ] The training script can load the Parquet datasets successfully.
