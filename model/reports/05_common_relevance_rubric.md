# Common Relevance Rubric

**Dataset:** Multilingual CV-JD Matching System  
**Generated:** 2026-10-31 12:40:56  
**Pipeline:** Multilingual CV-JD Matching System  

---

## 1. Relevance Scale Definition

**CV–JD relevance** measures how well a candidate's demonstrated skills, experience, qualifications, and responsibilities align with the requirements and expectations of a specific job description.

| Label | Name | Definition |
|---|---|---|
| 0 | Not Relevant | The CV has no meaningful alignment with the core requirements of the JD. |
| 1 | Low Relevance | The CV has some related skills or experience but does not satisfy important requirements. |
| 2 | Relevant | The CV satisfies most core requirements, with some gaps in preferred skills or experience. |
| 3 | Highly Relevant | The CV demonstrates strong alignment with the required skills, responsibilities, and relevant experience. |

### Label 0: Not Relevant

The CV has no meaningful alignment with the core requirements of the JD.

**Criteria:**
- No overlap in required skills
- Completely different industry and role with no transferable experience
- No relevant experience or qualifications

**Examples:**
- A chef's CV applied to a software engineer position
- A fresh graduate with no IT background for a Senior DBA role

### Label 1: Low Relevance

The CV has some related skills or experience but does not satisfy important requirements.

**Criteria:**
- Some skill overlap but missing most core requirements
- Related industry but different role specialization
- Entry-level experience for a position requiring significant experience
- Transferable soft skills but lacking technical requirements

**Examples:**
- A junior web developer for a Senior Data Scientist position
- A sales person with some tech exposure for an IT Manager role

### Label 2: Relevant

The CV satisfies most core requirements, with some gaps in preferred skills or experience.

**Criteria:**
- Meets majority of must-have skills
- Relevant role experience in similar domain
- Appropriate seniority level (within one level)
- May lack some preferred/nice-to-have qualifications

**Examples:**
- A mid-level Python developer for a Senior Python role (strong skills, less experience)
- An accountant with 3 years experience for a 5-year requirement role

### Label 3: Highly Relevant

The CV demonstrates strong alignment with the required skills, responsibilities, and relevant experience.

**Criteria:**
- Meets all or nearly all must-have skills
- Strong relevant experience in the same or closely related role
- Appropriate or exceeding seniority level
- Has many preferred/nice-to-have qualifications
- Industry and domain experience aligned

**Examples:**
- A Senior Java Developer with microservices experience for a similar role
- A real estate sales lead for a real estate sales manager position

---

## 2. Relevance Criteria

| Criterion | Weight | Description |
|---|---|---|
| Required Skills | Primary | Whether the candidate demonstrates the skills required by the JD. |
| Role and Responsibilities | Primary | Whether the candidate's previous work experience is relevant to the target job. |
| Experience and Seniority | Secondary | Years of experience and seniority level alignment. |
| Industry and Domain | Secondary | Relevant domain or industry experience. |
| Preferred Skills | Tertiary | Additional skills listed as preferred or nice-to-have. |

---

## 3. Evidence States

| State | Definition |
|---|---|
| `present` | The CV contains evidence of the skill or qualification. |
| `absent` | There is sufficient evidence that the candidate does not meet the criterion. |
| `unknown` | The CV does not provide enough information to determine whether the candidate meets the criterion. Absence of a skill in the CV does not necessarily prove the candidate does not possess it. |

> **Important:** The absence of a skill in the CV does not necessarily prove that the candidate does not possess that skill. Do not automatically assign negative relevance based on missing information.

---

## 4. Hard Constraints vs Soft Criteria

### Hard Constraint

Explicit mandatory conditions that cannot reasonably be substituted, such as legally required certifications or clearly non-negotiable conditions.

Examples:
- Medical license for a physician position
- Active security clearance for classified work
- Professional engineering license (PE) where required by law

> Do not treat every must_have_skills entry as an absolute hard constraint.

### Soft Criterion

Skills, responsibilities, experience, seniority, industry, and preferred qualifications that can be assessed by degree of alignment.

Examples:
- 3 years of Python experience (flexible based on quality)
- Experience with Agile methodologies
- Bachelor's degree in related field

---

## 5. Vietnamese Score → Common Label Mapping

Map Vietnamese LLM consensus scores (1-9 scale, median of 3 models) to the common 0-3 relevance rubric.

| Common Label | Vietnamese Score Range | Name |
|---|---|---|
| 0 | [1.0, 2.5) | Not Relevant |
| 1 | [2.5, 4.5) | Low Relevance |
| 2 | [4.5, 6.5) | Relevant |
| 3 | [6.5, 9.0) | Highly Relevant |

> Boundary values (2.5, 4.5, 6.5) are assigned to the higher category. This is a conditional mapping that should be validated against human-annotated samples.

**Validation required:** This mapping must be validated against human-annotated samples.

---

## 6. Annotation Guidelines for Human Validation

### Process

1. Read the full JD text, noting required skills, preferred skills, and requirements
2. Read the full CV text, noting skills, experience, and qualifications
3. Evaluate each relevance criterion independently
4. Assign a label (0-3) based on overall alignment
5. Record evidence for each criterion (present/absent/unknown)
6. Write a brief justification

### Rules

- Evaluate in context of the specific JD, not general similarity
- Do not rely on superficial vocabulary overlap
- Do not penalize for missing preferred/nice-to-have skills as heavily as required skills
- Distinguish between 'skill not mentioned' and 'skill confirmed absent'
- Consider transferable skills and equivalent experience
- Do not use original dataset labels when assigning new labels
- Apply the same rubric regardless of language

