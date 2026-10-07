# CV-JD Matching Model — Urgent Fixes

> **Version:** v1.1
> **Mục tiêu:** Xử lý các vấn đề nghiêm trọng phát hiện từ evaluation của model v1 trước khi train lại.
> **Ưu tiên:** Data quality → Label quality → Leakage → Class imbalance → Error analysis → Retraining → Re-evaluation

---

## 1. Tình trạng hiện tại

Model v1 đạt:

| Metric             | Kết quả |
| ------------------ | ------: |
| Accuracy           |  93.39% |
| Weighted Precision |  93.02% |
| Weighted Recall    |  93.39% |
| Weighted F1        |  93.07% |
| Macro Precision    |  76.44% |
| Macro Recall       |  64.83% |
| Macro F1           |  66.36% |

Confusion Matrix:

```text
                    Predicted
                 0      1      2
True 0        2901     12    296
True 1          26     10     84
True 2         355      0   8011
```

Phân bố test:

```text
Not suitable       3209  (~27.4%)
Partially suitable   120  (~1.0%)
Highly suitable    8366  (~71.5%)
```

### Vấn đề nghiêm trọng nhất

Class `Partially Suitable`:

```text
Precision = 0.45
Recall    = 0.08
F1        = 0.14
```

Trong 120 mẫu:

```text
Correctly predicted: 10
Predicted as class 0: 26
Predicted as class 2: 84
```

Như vậy model đang **bỏ sót phần lớn trường hợp phù hợp một phần**.

Không được đánh giá model chỉ dựa trên Accuracy hoặc Weighted-F1.

---

# 2. Mục tiêu của v1.1

Trước khi tối ưu model architecture, phải cải thiện chất lượng dữ liệu và quy trình đánh giá.

### Mục tiêu bắt buộc

* [ ] Kiểm tra cách tạo label.
* [ ] Kiểm tra nguyên nhân class 1 chỉ chiếm ~1%.
* [ ] Bổ sung dữ liệu `Partially Suitable`.
* [ ] Bổ sung borderline/hard samples.
* [ ] Kiểm tra train/test leakage.
* [ ] Tạo error analysis dataset.
* [ ] Kiểm tra lỗi giữa class 0 và class 2.
* [ ] Kiểm tra lỗi theo ngôn ngữ.
* [ ] Train lại model.
* [ ] Đánh giá lại bằng Macro-F1 và per-class metrics.

### Chưa ưu tiên

Không cần triển khai ngay:

* Vector Database.
* Bi-Encoder.
* RAG.
* LLM.
* Agent.
* Knowledge Graph.
* Microservice optimization.
* Model architecture phức tạp.

Nếu dữ liệu chưa tốt thì thay đổi model sẽ không giải quyết được vấn đề chính.

---

# 3. PRIORITY 1 — Kiểm tra Label Generation

## 3.1. Kiểm tra phân bố label trên toàn bộ dataset

Thực hiện trên:

```text
train
validation
test
```

Không chỉ kiểm tra test.

Ví dụ:

```python
for name, df in datasets.items():
    print(name)
    print(df["label"].value_counts())
    print(df["label"].value_counts(normalize=True))
```

Expected output:

```text
0 = Not suitable
1 = Partially suitable
2 = Highly suitable
```

### Cần xác định

Nếu class 1 thực sự chỉ chiếm khoảng 1% trong raw data:

> Đây là vấn đề của dataset/labeling, không đơn thuần là vấn đề của model.

---

# 4. Kiểm tra quy tắc tạo label

Phải có guideline rõ ràng.

## Label 0 — Not Suitable

CV không đáp ứng phần lớn yêu cầu quan trọng của JD.

Ví dụ:

```text
JD:
Java
Spring Boot
PostgreSQL
2 years experience

CV:
Graphic Designer
Photoshop
Illustrator
UI Design
```

→ `0`

---

## Label 1 — Partially Suitable

CV có một phần năng lực phù hợp nhưng thiếu một hoặc nhiều yêu cầu quan trọng.

Ví dụ:

```text
JD:
Java
Spring Boot
PostgreSQL
Docker
2 years experience

CV:
Java
Spring Boot
MySQL
1 year experience
```

→ `1`

---

## Label 2 — Highly Suitable

CV đáp ứng phần lớn các yêu cầu quan trọng của JD.

Ví dụ:

```text
JD:
Java
Spring Boot
PostgreSQL
Docker
2 years experience

CV:
Java
Spring Boot
PostgreSQL
Docker
3 years experience
```

→ `2`

---

# 5. Không tạo label chỉ bằng keyword matching đơn giản

Không nên dùng:

```python
if matched_skills >= 80%:
    label = 2
elif matched_skills >= 40%:
    label = 1
else:
    label = 0
```

nếu đây là quy tắc duy nhất.

Lý do:

* Không phản ánh mức độ quan trọng của skill.
* Không xét experience.
* Không xét education.
* Không xét required/preferred.
* Có thể tạo label giả quá dễ đoán.
* Model có thể học lại chính quy tắc labeling thay vì học semantic matching.

---

# 6. Bổ sung Partially Suitable Samples

Đây là nhiệm vụ **ưu tiên cao nhất**.

Class 1 hiện tại:

```text
120 / 11695 ≈ 1.0%
```

Tỷ lệ này quá thấp để model học tốt ranh giới:

```text
Not Suitable
        ↕
Partially Suitable
        ↕
Highly Suitable
```

## Mục tiêu đề xuất

Không cần cố ép dataset thành 33% / 33% / 33%.

Có thể bắt đầu với:

```text
Class 0: 30–40%
Class 1: 15–25%
Class 2: 40–55%
```

Tỷ lệ cuối cùng phụ thuộc dữ liệu thực tế.

Quan trọng hơn tỷ lệ là **độ đa dạng của class 1**.

---

# 7. Tạo Borderline Samples

Cần bổ sung những CV-JD pairs có mức độ phù hợp nằm giữa hai class.

### Borderline 0 ↔ 1

Ví dụ:

```text
JD yêu cầu:
Java
Spring Boot
PostgreSQL
Docker

CV có:
Java
Spring Boot
MySQL
```

Không nên gán ngay:

```text
0
```

Nếu CV có nền tảng tương đối phù hợp nhưng thiếu requirement quan trọng, có thể là:

```text
1
```

---

### Borderline 1 ↔ 2

Ví dụ:

```text
JD:
Java
Spring Boot
PostgreSQL
Docker
Kafka

CV:
Java
Spring Boot
PostgreSQL
Docker
```

→ Có thể là `1` hoặc `2` tùy guideline.

Phải định nghĩa guideline trước khi labeling.

---

# 8. Hard Negative Samples

Bổ sung các CV có nhiều keyword giống JD nhưng thực chất không phù hợp.

Ví dụ:

```text
JD:
Java Backend Developer

CV:
Java
Spring
Spring Boot
```

nhưng:

```text
Experience:
Java Tester
```

hoặc:

```text
Java Instructor
```

Các trường hợp này giúp model không chỉ học:

```text
keyword overlap → suitable
```

mà phải học ngữ cảnh.

---

# 9. Hard Positive / Difficult Positive

Không chỉ tạo negative khó.

Cần có positive khó:

```text
CV:
Backend Developer
Java
Spring Boot
Microservices

JD:
Java Developer
Spring Boot
Docker
Kafka
```

Có thể khác wording nhưng vẫn phù hợp.

Điều này giúp model học:

```text
semantic similarity
```

thay vì:

```text
exact keyword matching
```

---

# 10. Kiểm tra Label Consistency

Lấy một sample ngẫu nhiên:

```text
100–300 CV-JD pairs
```

và kiểm tra thủ công.

Tạo bảng:

| CV    | JD    | Existing Label | Human Label | Reason                 |
| ----- | ----- | -------------: | ----------: | ---------------------- |
| CV001 | JD001 |              2 |           2 | Strong match           |
| CV002 | JD002 |              2 |           1 | Missing required skill |
| CV003 | JD003 |              0 |           1 | Partial experience     |

Tính:

```text
Label Agreement
```

Nếu nhiều mẫu disagreement:

> Cần sửa labeling guideline trước khi train lại.

---

# 11. PRIORITY 2 — Kiểm tra Data Leakage

Đây là bước bắt buộc trước khi tin tưởng kết quả 93.39%.

Nếu cùng một candidate xuất hiện trong train và test:

```text
Candidate C001
    ├── train
    └── test
```

model có thể đã nhìn thấy thông tin candidate trong training.

Tương tự với Job.

---

## 11.1. Candidate leakage

```python
train_candidates = set(train_df["candidate_id"])
val_candidates = set(val_df["candidate_id"])
test_candidates = set(test_df["candidate_id"])

print("Train/Test:",
      len(train_candidates & test_candidates))

print("Train/Val:",
      len(train_candidates & val_candidates))
```

Expected:

```text
Train/Test: 0
Train/Val: 0
```

---

## 11.2. Job leakage

```python
train_jobs = set(train_df["job_id"])
val_jobs = set(val_df["job_id"])
test_jobs = set(test_df["job_id"])

print("Train/Test:",
      len(train_jobs & test_jobs))

print("Train/Val:",
      len(train_jobs & val_jobs))
```

Expected:

```text
Train/Test: 0
Train/Val: 0
```

---

# 12. Split Dataset theo Entity

Không nên split random pair nếu một candidate/job xuất hiện trong nhiều pair.

Không tốt:

```text
Random pair split
```

Nên:

```text
Candidate/Job entity split
```

Ví dụ:

```text
Train:
Candidate C001-C7000

Validation:
Candidate C7001-C8000

Test:
Candidate C8001-C10000
```

Hoặc áp dụng group split phù hợp với cấu trúc dataset.

---

# 13. PRIORITY 3 — Tạo Error Analysis Dataset

Sau mỗi evaluation, lưu toàn bộ prediction.

Schema:

```text
candidate_id
job_id
cv_text
job_text

true_label
predicted_label

prob_0
prob_1
prob_2

confidence
```

Lưu:

```text
evaluation/error_cases.parquet
```

---

# 14. Phân loại Error Cases

Tập trung vào:

### Error type 1

```text
True 0 → Predicted 2
```

Hiện tại:

```text
296 cases
```

Đây là false positive nghiêm trọng.

---

### Error type 2

```text
True 2 → Predicted 0
```

Hiện tại:

```text
355 cases
```

Đây là false negative nghiêm trọng.

---

### Error type 3

```text
True 1 → Predicted 0
```

Hiện tại:

```text
26 cases
```

---

### Error type 4

```text
True 1 → Predicted 2
```

Hiện tại:

```text
84 cases
```

Đây là lỗi lớn nhất của class 1.

---

# 15. Phân tích nguyên nhân lỗi

Mỗi error case nên được phân loại:

```text
skill_mismatch
experience_mismatch
education_mismatch
semantic_mismatch
keyword_overlap
missing_required_skill
synonym_problem
language_problem
truncation
label_error
ambiguous_case
other
```

Ví dụ:

```text
True:
Partially Suitable

Pred:
Highly Suitable

Reason:
Missing required skill: Docker
```

---

# 16. Kiểm tra Text Truncation

Code hiện tại có:

```python
cv_text = str(row["cv_text"])[:1500]
job_text = str(row["job_text"])[:1500]
```

Đây là:

```text
1500 characters
```

không phải:

```text
1500 tokens
```

Có thể làm mất:

* skills
* experience
* education
* certifications
* important requirements

---

## Cách cải thiện

Không nối toàn bộ text một cách tùy tiện.

Ưu tiên:

```text
[CV]
Skills: ...
Experience: ...
Education: ...
Certifications: ...

[JOB]
Title: ...
Required Skills: ...
Preferred Skills: ...
Experience: ...
Education: ...
```

Ví dụ:

```python
cv_text = f"""
Skills: {skills}
Experience: {experience}
Education: {education}
Certifications: {certifications}
"""

job_text = f"""
Title: {title}
Required Skills: {required_skills}
Preferred Skills: {preferred_skills}
Minimum Experience: {min_experience}
Education: {education}
"""
```

Sau đó mới xử lý giới hạn token.

---

# 17. PRIORITY 4 — Xử lý Class Imbalance

Không nên chỉ dùng:

```text
oversampling class 1
```

mà không kiểm tra chất lượng sample.

Ưu tiên:

```text
1. Improve labels
2. Add diverse class 1
3. Add borderline samples
4. Add hard negatives
5. Oversample carefully
```

---

# 18. Oversampling Class 1

Sau khi bổ sung dữ liệu thật:

```python
class_1 = train_df[train_df["label"] == 1]
```

Có thể oversample class 1.

Ví dụ:

```python
from sklearn.utils import resample

class_1_up = resample(
    class_1,
    replace=True,
    n_samples=len(class_0) // 3,
    random_state=42
)
```

Không nên oversample quá mức đến khi:

```text
class 1 ≈ class 0 ≈ class 2
```

nếu dữ liệu thực tế không có phân bố như vậy.

---

# 19. Class Weight

Có thể thử weighted loss:

```text
weight(class 0)
weight(class 1)
weight(class 2)
```

Nhưng phải đánh giá lại.

Không nên đặt class 1 weight quá lớn vì có thể làm:

```text
class 0 → class 1
class 2 → class 1
```

tăng mạnh.

---

# 20. PRIORITY 5 — Bổ sung Evaluation Metrics

Không dùng Accuracy làm metric chính.

Bắt buộc báo cáo:

```text
Accuracy
Precision per class
Recall per class
F1 per class
Macro-F1
Weighted-F1
Confusion Matrix
```

Metric chính nên là:

```text
Macro-F1
```

vì dataset mất cân bằng.

---

# 21. Ranking Metrics

Hệ thống thực tế cần:

```text
Candidate Ranking
```

nên cần thêm:

```text
NDCG@5
NDCG@10
MRR
Precision@5
Recall@5
```

Ví dụ:

```text
Job J001

Candidate A → 0.91
Candidate B → 0.87
Candidate C → 0.75
Candidate D → 0.61
```

Cần đánh giá:

> Candidate phù hợp có nằm ở top ranking hay không?

Classification accuracy không trả lời được câu hỏi này.

---

# 22. Đánh giá theo ngôn ngữ

Nếu hệ thống hỗ trợ:

```text
Vietnamese
English
Vietnamese ↔ English
```

cần chia evaluation:

```text
VI → VI
EN → EN
VI → EN
EN → VI
```

Mỗi nhóm:

```text
Accuracy
Macro-F1
F1 class 0
F1 class 1
F1 class 2
```

Điều này giúp phát hiện model tốt ở English nhưng yếu ở Vietnamese.

---

# 23. So sánh Model

Không nên chỉ báo cáo model hiện tại.

Tối thiểu cần:

### Baseline 1

```text
TF-IDF + Cosine Similarity
```

### Baseline 2

```text
Pretrained Multilingual Embedding
```

### Model 3

```text
Fine-tuned Cross-Encoder
```

So sánh:

| Model                    | Accuracy | Macro-F1 | Weighted-F1 | NDCG@10 |
| ------------------------ | -------: | -------: | ----------: | ------: |
| TF-IDF                   |        - |        - |           - |       - |
| Embedding                |        - |        - |           - |       - |
| Fine-tuned Cross-Encoder |   93.39% |   66.36% |      93.07% |       - |
| Cross-Encoder v1.1       |        - |        - |           - |       - |

---

# 24. Multilingual Model

Model hiện tại:

```text
cross-encoder/ms-marco-MiniLM-L-6-v2
```

chủ yếu được thiết kế cho English.

Nếu mục tiêu là:

```text
Vietnamese + English
```

cần benchmark thêm multilingual model.

Một ứng viên:

```text
BAAI/bge-reranker-v2-m3
```

Không được mặc định kết luận model này tốt hơn.

Cần thực nghiệm:

```text
Model A
vs
Model B
```

trên cùng:

```text
train
validation
test
```

và cùng metrics.

---

# 25. Không thay model ngay lập tức

Thứ tự thực hiện:

```text
Data quality
      ↓
Label quality
      ↓
Leakage check
      ↓
Error analysis
      ↓
Class balancing
      ↓
Retrain
      ↓
Evaluate
      ↓
Multilingual benchmark
      ↓
Model selection
```

Nếu sau khi cải thiện dataset mà model vẫn yếu:

```text
Partially Suitable
```

mới xem xét thay model.

---

# 26. Retraining v1.1

Sau khi hoàn thành các bước trên:

```text
data/
├── processed/
│   ├── cv.parquet
│   ├── jd.parquet
│   └── cv_jd_pairs.parquet
│
├── splits/
│   ├── train.parquet
│   ├── validation.parquet
│   └── test.parquet
│
└── metadata/
    ├── dataset_metadata.json
    └── labeling_guideline.md
```

Train:

```text
output/
└── cv_jd_matcher_v1_1/
```

Không ghi đè model v1.

---

# 27. Reproducibility

Cố định:

```python
SEED = 42
```

Cần lưu:

```text
model_name
dataset_version
training_date
random_seed
batch_size
learning_rate
epochs
max_length
class distribution
training samples
validation samples
test samples
```

Ví dụ:

```json
{
  "model_version": "cv_jd_matcher_v1.1",
  "dataset_version": "dataset_v1.1",
  "seed": 42,
  "epochs": 3,
  "batch_size": 16,
  "learning_rate": 0.00002,
  "max_length": 512
}
```

---

# 28. Điều kiện đạt trước khi chuyển sang bước tiếp theo

Model v1.1 chỉ được xem xét tốt hơn v1 nếu:

### Bắt buộc

* [ ] Không có train/test candidate leakage.
* [ ] Không có train/test job leakage.
* [ ] Label guideline rõ ràng.
* [ ] Class 1 có đủ dữ liệu đa dạng.
* [ ] Có error analysis.
* [ ] Có test set độc lập.

### Model

Không đặt mục tiêu Accuracy đơn thuần.

Ưu tiên:

```text
Macro-F1 ↑
F1 class 1 ↑
Recall class 1 ↑
NDCG@10 ↑
```

Đặc biệt:

```text
F1(class 1): 0.14 → cần cải thiện rõ rệt
Recall(class 1): 0.08 → cần cải thiện rõ rệt
```

---

# 29. Checklist thực hiện

## 🔴 P0 — Làm ngay

* [ ] Kiểm tra label distribution toàn bộ dataset.
* [ ] Kiểm tra label generation.
* [ ] Kiểm tra 100–300 samples thủ công.
* [ ] Bổ sung Partially Suitable.
* [ ] Bổ sung borderline samples.
* [ ] Bổ sung hard negatives.
* [ ] Kiểm tra candidate leakage.
* [ ] Kiểm tra job leakage.

## 🟠 P1 — Sau P0

* [ ] Tạo error_cases.parquet.
* [ ] Phân tích 0 → 2.
* [ ] Phân tích 2 → 0.
* [ ] Phân tích 1 → 0.
* [ ] Phân tích 1 → 2.
* [ ] Kiểm tra truncation.
* [ ] Kiểm tra multilingual cases.
* [ ] Xử lý class imbalance.
* [ ] Train v1.1.

## 🟡 P2 — Sau khi có v1.1

* [ ] NDCG@5.
* [ ] NDCG@10.
* [ ] MRR.
* [ ] Precision@K.
* [ ] Recall@K.
* [ ] So sánh TF-IDF.
* [ ] So sánh multilingual embedding.
* [ ] Benchmark multilingual Cross-Encoder.

## 🟢 P3 — Có thể làm sau

* [ ] Confidence calibration.
* [ ] Explainability.
* [ ] Skill gap analysis.
* [ ] Bi-Encoder.
* [ ] Vector retrieval.
* [ ] Cross-Encoder reranking.
* [ ] Production optimization.

---

# 30. Kết luận

Kết quả v1:

```text
Accuracy = 93.39%
Weighted-F1 = 93.07%
Macro-F1 = 66.36%
```

không nên được kết luận là model đã đạt chất lượng cao.

Nguyên nhân chính là dataset mất cân bằng nghiêm trọng:

```text
Highly Suitable      ≈ 71.5%
Not Suitable         ≈ 27.4%
Partially Suitable   ≈ 1.0%
```

và model gần như không nhận diện được `Partially Suitable`:

```text
Recall = 8.33%
F1 = 0.14
```

Do đó, **việc cần làm trước tiên không phải thay model**, mà là:

```text
Improve Labeling
      ↓
Add Partial/Borderline Samples
      ↓
Check Data Leakage
      ↓
Perform Error Analysis
      ↓
Handle Class Imbalance
      ↓
Retrain v1.1
      ↓
Evaluate Macro-F1 + Ranking Metrics
```

Chỉ sau khi hoàn thành chu trình này mới có cơ sở để quyết định có cần thay đổi backbone/model hay không.
