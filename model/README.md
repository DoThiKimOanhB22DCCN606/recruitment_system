# CV–Job Description (CV–JD) Matching Model

Hệ thống mô hình AI chấm điểm và xếp hạng mức độ tương thích giữa Hồ sơ ứng viên (CV) và Bản mô tả công việc (Job Description - JD) phục vụ cho hệ thống tuyển dụng thông minh.

---

## 📌 1. Tổng quan (Overview)

Mô hình nhận đầu vào gồm văn bản thông tin hồ sơ ứng viên (**CV Text**) và văn bản mô tả công việc (**JD Text**), sau đó dự đoán mức độ phù hợp theo 3 thang điểm nhãn phân loại:

```text
       CV (Ứng viên)  +  JD (Tuyển dụng)
                    │
                    ▼
           ┌─────────────────┐
           │  Cross-Encoder  │
           │     Matcher     │
           └─────────────────┘
                    │
                    ▼
    [Nhãn & Xác suất tương thích]
  0: Không phù hợp (Not suitable)
  1: Phù hợp một phần (Partially suitable)
  2: Rất phù hợp (Highly suitable)
                    │
                    ▼
     Gợi ý & Xếp hạng ứng viên
```

> **Lưu ý:** Điểm số tương thích phản ánh mức độ phù hợp năng lực/kinh nghiệm/kỹ năng giữa CV và JD dựa trên dữ liệu học, không thay thế cho quyết định tuyển dụng cuối cùng của con người.

---

## 📂 2. Cấu trúc thư mục (Directory Structure)

```text
model/
├── README.md                      # Tài liệu hướng dẫn này
├── training.md                    # Bản đặc tả kỹ thuật chi tiết dữ liệu & huấn luyện
├── data/
│   ├── dataset_metadata.json      # Metadata chi tiết về phiên bản và số lượng dữ liệu
│   ├── processed/                 # Dữ liệu chuẩn hóa dạng Apache Parquet
│   │   ├── cv.parquet             # Danh sách ứng viên đã chuẩn hóa
│   │   ├── jd.parquet             # Danh sách JD đã chuẩn hóa
│   │   └── cv_jd_pairs.parquet    # Tất cả các cặp (CV, JD) đã gán nhãn
│   └── splits/                    # Dữ liệu chia tập (Entity-based split)
│       ├── train.parquet          # 94,938 mẫu (80%)
│       ├── validation.parquet     # 11,316 mẫu (10%)
│       └── test.parquet           # 11,695 mẫu (10%)
├── preprocessing/
│   └── skill_aliases.json         # Từ điển ánh xạ và chuẩn hóa kỹ năng CNTT
├── scripts/
│   ├── normalize_pipeline.py      # Tiền xử lý dữ liệu từ nguồn gốc thô
│   ├── build_training_data.py     # Chuẩn hóa Parquet & chia tập dữ liệu theo spec
│   ├── train_model.py             # Huấn luyện mô hình Cross-Encoder
│   ├── evaluate_model.py          # Đánh giá mô hình trên tập Test
│   └── inference.py               # API suy luận / demo khớp CV-JD
└── output/
    └── cv_jd_matcher_v1/          # Checkpoint mô hình sau huấn luyện
        ├── config.json
        ├── model.safetensors
        ├── tokenizer.json
        ├── vocab.txt
        └── README.md              # Model card sinh bởi sentence-transformers
```

---

## 📊 3. Dữ liệu & Quy trình xử lý (Data Pipeline)

### 3.1. Nguồn dữ liệu & Quy mô
- **CVs**: 14,817 hồ sơ ứng viên.
- **JDs**: 2,500 tin tuyển dụng.
- **Tổng cặp ghép nối (CV-JD Pairs)**: 117,949 cặp.
- **Phân bổ nhãn**:
  - `Nhãn 0` (Không phù hợp / Negative): **32,808**
  - `Nhãn 1` (Phù hợp một phần / Hard Negative): **1,207**
  - `Nhãn 2` (Rất phù hợp / Positive): **83,934**

### 3.2. Chuẩn hóa dữ liệu (Preprocessing & Normalization)
1. **Làm sạch văn bản**: Loại bỏ mã HTML, chuẩn hóa khoảng trắng, ký tự đặc biệt và Unicode tiếng Việt.
2. **Chuẩn hóa kỹ năng**: Ánh xạ kỹ năng đồng nghĩa về một tên quy chuẩn thông qua [skill_aliases.json](preprocessing/skill_aliases.json) (ví dụ: `react.js`, `reactjs` ➔ `React`; `spring-boot`, `springboot` ➔ `Spring Boot`).
3. **Phân chia không rò rỉ (Entity-based Split)**: Đảm bảo cùng một ứng viên hoặc tin tuyển dụng không xuất hiện đồng thời ở cả tập `Train`, `Validation` và `Test` nhằm chống overfitting và đánh giá khách quan.

---

## 🧠 4. Kiến trúc mô hình (Model Architecture)

- **Mô hình nền tảng**: `cross-encoder/ms-marco-MiniLM-L-6-v2` (Sentence-Transformers).
- **Cơ chế**: **Cross-Encoder** nhận đồng thời cặp chuỗi `(cv_text, job_text)` qua tầng Self-Attention đầy đủ, giúp mô hình nắm bắt ngữ cảnh chéo và sự tương đồng sâu hơn so với mô hình Bi-Encoder độc lập.
- **Đầu ra**: Lớp phân loại 3 nhãn (`num_labels = 3`) với xác suất Softmax.

### Siêu tham số huấn luyện (Hyperparameters)
| Tham số | Giá trị |
| :--- | :--- |
| `max_length` | 512 tokens |
| `batch_size` | 16 |
| `epochs` | 3 |
| `learning_rate` | 2e-5 |
| `warmup_ratio` | 0.1 |
| `weight_decay` | 0.01 |
| `evaluation_steps`| 500 steps |
| `save_best_model`| True (theo Micro/Macro F1 trên Validation) |

---

## 🚀 5. Hướng dẫn chạy (How to Run)

### 5.1. Cài đặt thư viện phụ thuộc
Đảm bảo đã cài đặt Python 3.9+ và các gói cần thiết:
```bash
pip install torch sentence-transformers transformers datasets pandas numpy pyarrow scikit-learn
```

### 5.2. Chạy thử nghiệm suy luận (Inference / Demo)
Nếu muốn kiểm tra thử độ chính xác hoặc dùng thử mô hình trên các cặp CV-JD mẫu:
```bash
python model/scripts/inference.py
```

### 5.3. Đánh giá chất lượng mô hình trên tập Test
Để tính toán Accuracy, Precision, Recall, Macro/Weighted F1 và Confusion Matrix trên 11,695 mẫu kiểm thử:
```bash
python model/scripts/evaluate_model.py
```

### 5.4. Huấn luyện lại mô hình (Train Model)
Để bắt đầu quá trình huấn luyện:
```bash
python model/scripts/train_model.py
```
> *Lưu ý:* Quá trình huấn luyện trên ~95k mẫu dữ liệu sẽ mất khoảng vài giờ nếu chạy bằng CPU; khuyến khích sử dụng GPU (NVIDIA CUDA) để hoàn thành nhanh hơn.

### 5.5. Tái tạo lại tập dữ liệu (Data Re-build)
Nếu có dữ liệu gốc mới và cần chuẩn hóa, sinh lại các file Parquet:
```bash
python model/scripts/build_training_data.py
```

---

## 🔌 6. Hướng dẫn tích hợp vào Backend / Service (API Usage)

Bạn có thể dễ dàng import hàm suy luận từ `model/scripts/inference.py` vào các dịch vụ Backend (FastAPI, Flask hoặc tiến trình Python gọi từ Java Spring Boot):

```python
from model.scripts.inference import predict_match, predict_batch

cv_text = """Candidate Profile
Skills: Java, Spring Boot, PostgreSQL, Docker
Experience:
- Backend Developer (2 years) | Skills: Java, Spring Boot
Education:
- Bachelor of Computer Science"""

job_text = """Job Title: Java Backend Developer
Required Skills: Java, Spring Boot, PostgreSQL
Preferred Skills: Docker, Kubernetes
Minimum Experience: 2 years"""

# 1. Dự đoán cho 1 cặp CV-JD
result = predict_match(cv_text, job_text)
print(result)
# Output:
# {
#   "label": 2,
#   "label_name": "Highly suitable",
#   "score": 0.9412,
#   "scores": [0.0121, 0.0467, 0.9412]
# }

# 2. Dự đoán hàng loạt (Batch Inference)
pairs = [(cv_text, job_text)]
batch_results = predict_batch(pairs)
```

---

## 📈 7. Trạng thái hiện tại & Hướng phát triển (Status & Roadmap)

- [x] Thiết kế xong schema dữ liệu chuẩn hóa Parquet và kiểm tra chống rò rỉ ranh giới (data leakage).
- [x] Xây dựng xong từ điển chuẩn hóa kỹ năng CNTT.
- [x] Đã huấn luyện checkpoint mô hình ban đầu đạt F1 Micro > 0.92, F1 Macro > 0.61.
- [ ] Tiếp tục fine-tune đủ 3 epochs trên máy có GPU để tối ưu hóa nhận diện nhãn 1 (Partially suitable).
- [ ] Tích hợp API serving (gRPC / FastAPI) để kết nối trực tiếp với backend Spring Boot candidate-service.
