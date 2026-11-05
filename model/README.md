# Multilingual AI-Based Candidate–Job Matching and Recommendation System

A graduation project that builds a system to evaluate relevance between candidate resumes (CVs) and job descriptions (JDs), supporting Vietnamese and English data, with a reproducible Label Harmonization Pipeline.

## Project Structure

```
model v2/
├── configs/                          # Configuration files
│   ├── paths.py                      # Central path configuration
│   └── common_relevance_rubric.json  # 0-3 relevance scale definition
├── src/                              # Source modules
│   ├── data_ingestion/               # Data loading modules
│   │   ├── vietnamese_loader.py      # Vietnamese CV/JD/label loaders
│   │   ├── english_loader.py         # English parquet loaders
│   │   └── taxonomy_loader.py        # ESCO/O*NET loaders
│   ├── data_profiling/               # Dataset profiling & quality audit
│   │   ├── vietnamese_profiler.py    # Vietnamese data profiling
│   │   ├── english_profiler.py       # English data profiling
│   │   └── taxonomy_profiler.py      # Taxonomy resource profiling
│   ├── label_audit/                  # Label semantic audit
│   │   └── label_semantic_audit.py   # Cross-dataset label analysis
│   ├── label_harmonization/          # Label harmonization pipeline
│   │   ├── common_rubric.py          # Common 0-3 relevance rubric
│   │   └── harmonizer.py             # Score mapping & harmonization
│   ├── unified_dataset/              # Unified dataset construction
│   │   └── builder.py                # Text construction & merging
│   ├── pair_generation/              # Pair generation & negative sampling
│   │   └── negative_sampling.py      # Random/semi-hard/hard negatives
│   ├── training/                     # Model training pipeline
│   │   ├── trainer.py                # Bi-Encoder training & evaluation
│   │   └── run_experiments.py        # Controlled experiment runner
│   ├── evaluation/                   # Evaluation modules
│   │   └── human_validation.py       # Human validation sampling & templates
│   ├── api/                          # FastAPI inference service
│   │   └── main.py                   # REST API endpoints
│   └── utils/                        # Shared utilities
│       └── helpers.py                # Report generation, formatting
├── data/                             # Raw datasets (preserved, never modified)
│   ├── vietnamese data/              # Vietnamese CV-JD data
│   │   ├── jobs/                     # TopCV job descriptions
│   │   ├── turn1300_v3_union_final/  # Labeled CV-JD pairs (4 LLM annotators)
│   │   ├── esco/                     # ESCO taxonomy
│   │   └── onet/                     # O*NET taxonomy
│   └── english data/                 # English CV-JD data (parquet)
│       └── dataset/
│           ├── jobs/                 # 2,500 job descriptions
│           ├── resumes/              # 10,000 resumes
│           ├── matches/              # Positive match relationships
│           └── embeddings/           # Pre-computed embeddings
├── reports/                          # Generated reports
├── outputs/                          # Generated outputs
│   ├── processed/                    # Harmonized datasets & splits
│   └── models/                       # Trained model checkpoints
├── Dockerfile                        # Docker deployment
├── docker-compose.yml                # Docker Compose config
└── requirements.txt                  # Python dependencies
```

## Quick Start

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Run Data Profiling (Milestone 1)
```bash
python -m src.data_profiling.vietnamese_profiler
python -m src.data_profiling.english_profiler
python -m src.data_profiling.taxonomy_profiler
```

### 3. Run Label Audit (Milestone 2)
```bash
python -m src.label_audit.label_semantic_audit
```

### 4. Generate Common Rubric (Milestone 3)
```bash
python -m src.label_harmonization.common_rubric
```

### 5. Run Label Harmonization (Milestone 4)
```bash
python -m src.label_harmonization.harmonizer
```

### 6. Build Unified Dataset & Generate Negatives (Milestone 5)
```bash
python -m src.unified_dataset.builder
python -m src.pair_generation.negative_sampling
python -m src.evaluation.human_validation
```

### 7. Train Models (Milestone 6)
```bash
python -m src.training.run_experiments --epochs 2 --batch-size 16 --max-samples 3000
```

### 8. Run API (Milestone 7)
```bash
uvicorn src.api.main:app --host 0.0.0.0 --port 8000
```

Or with Docker:
```bash
docker-compose up --build
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| POST | `/match` | Match single CV vs JD |
| POST | `/rank` | Rank multiple CVs for a JD |
| GET | `/model-info` | Model information |

## Datasets

| Dataset | Language | Records | Labels | Source |
|---------|----------|---------|--------|--------|
| Vietnamese | vi | 2,450 pairs | 1-9 LLM consensus → 0-3 | 130 synthetic CVs × 1,300 real JDs |
| English | en | 75,000 positive pairs | Implicit binary → 0-3 | Synthetic jobs/resumes |

## Label Harmonization

The central contribution is the **Label Harmonization Pipeline**:

1. **Audit** original label semantics from each dataset
2. **Define** a common 4-level relevance rubric (0-3)
3. **Map** Vietnamese 1-9 LLM scores → 0-3 with calibrated thresholds
4. **Estimate** English relevance grades using structured feature analysis
5. **Track** provenance, confidence, and mapping status for every label

## Common Relevance Scale

| Label | Name | Definition |
|-------|------|------------|
| 0 | Not Relevant | No meaningful alignment with core JD requirements |
| 1 | Low Relevance | Some related skills but missing important requirements |
| 2 | Relevant | Satisfies most core requirements with some gaps |
| 3 | Highly Relevant | Strong alignment with skills, responsibilities, experience |

## Reports Generated

| Report | Description |
|--------|-------------|
| `01_vietnamese_data_profiling.md` | Vietnamese dataset statistics & quality issues |
| `02_english_data_profiling.md` | English dataset statistics & quality issues |
| `03_taxonomy_profiling.md` | ESCO/O*NET resource analysis |
| `04_label_semantic_audit.md` | Cross-dataset label compatibility analysis |
| `05_common_relevance_rubric.md` | 0-3 relevance scale definition & guidelines |
| `06_label_harmonization.md` | Label mapping results & distribution analysis |
| `07_experiment_results.md` | Training experiment comparison |
