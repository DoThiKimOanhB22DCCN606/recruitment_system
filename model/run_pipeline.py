"""
Main Pipeline Runner

Executes the full multilingual CV-JD matching pipeline step by step.
Each milestone can be run independently.

Usage:
    python run_pipeline.py --milestone 1    # Data profiling
    python run_pipeline.py --milestone 2    # Label audit
    python run_pipeline.py --milestone 3    # Common rubric
    python run_pipeline.py --milestone 4    # Label harmonization
    python run_pipeline.py --milestone 5    # Unified dataset + negatives + splitting
    python run_pipeline.py --milestone 6    # Training + evaluation
    python run_pipeline.py --milestone 7    # API (starts server)
    python run_pipeline.py --all            # Run milestones 1-6
"""
import sys
import argparse
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))


def run_milestone_1():
    """Milestone 1: Dataset Inspection & Profiling."""
    print("\n" + "=" * 70)
    print("MILESTONE 1: Dataset Inspection & Profiling")
    print("=" * 70)

    from src.data_profiling.vietnamese_profiler import generate_vn_profiling_report
    from src.data_profiling.english_profiler import generate_en_profiling_report
    from src.data_profiling.taxonomy_profiler import generate_taxonomy_profiling_report
    from configs.paths import REPORTS_DIR
    import json

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)

    print("\n[1/3] Vietnamese data profiling...")
    vn_report, vn_data = generate_vn_profiling_report()
    with open(REPORTS_DIR / "01_vietnamese_data_profiling.md", "w", encoding="utf-8") as f:
        f.write(vn_report)
    with open(REPORTS_DIR / "01_vietnamese_data_profiling.json", "w", encoding="utf-8") as f:
        json.dump(vn_data, f, ensure_ascii=False, indent=2, default=str)
    print("  ✓ Report: reports/01_vietnamese_data_profiling.md")

    print("\n[2/3] English data profiling...")
    en_report, en_data = generate_en_profiling_report()
    with open(REPORTS_DIR / "02_english_data_profiling.md", "w", encoding="utf-8") as f:
        f.write(en_report)
    with open(REPORTS_DIR / "02_english_data_profiling.json", "w", encoding="utf-8") as f:
        json.dump(en_data, f, ensure_ascii=False, indent=2, default=str)
    print("  ✓ Report: reports/02_english_data_profiling.md")

    print("\n[3/3] Taxonomy profiling...")
    tax_report, tax_data = generate_taxonomy_profiling_report()
    with open(REPORTS_DIR / "03_taxonomy_profiling.md", "w", encoding="utf-8") as f:
        f.write(tax_report)
    with open(REPORTS_DIR / "03_taxonomy_profiling.json", "w", encoding="utf-8") as f:
        json.dump(tax_data, f, ensure_ascii=False, indent=2, default=str)
    print("  ✓ Report: reports/03_taxonomy_profiling.md")

    print("\n✅ Milestone 1 complete.")


def run_milestone_2():
    """Milestone 2: Label Semantic Audit."""
    print("\n" + "=" * 70)
    print("MILESTONE 2: Label Semantic Audit")
    print("=" * 70)

    from src.label_audit.label_semantic_audit import generate_label_audit_report
    from configs.paths import REPORTS_DIR
    import json

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)

    report_text, audit_data = generate_label_audit_report()
    with open(REPORTS_DIR / "04_label_semantic_audit.md", "w", encoding="utf-8") as f:
        f.write(report_text)
    with open(REPORTS_DIR / "04_label_semantic_audit.json", "w", encoding="utf-8") as f:
        json.dump(audit_data, f, ensure_ascii=False, indent=2, default=str)
    print("  ✓ Report: reports/04_label_semantic_audit.md")

    print("\n✅ Milestone 2 complete.")


def run_milestone_3():
    """Milestone 3: Common Relevance Rubric."""
    print("\n" + "=" * 70)
    print("MILESTONE 3: Common Relevance Rubric")
    print("=" * 70)

    from src.label_harmonization.common_rubric import generate_rubric_report, save_rubric_config
    from configs.paths import REPORTS_DIR

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)

    report_text = generate_rubric_report()
    with open(REPORTS_DIR / "05_common_relevance_rubric.md", "w", encoding="utf-8") as f:
        f.write(report_text)
    print("  ✓ Report: reports/05_common_relevance_rubric.md")

    config_path = save_rubric_config()
    print(f"  ✓ Config: {config_path}")

    print("\n✅ Milestone 3 complete.")


def run_milestone_4():
    """Milestone 4: Label Harmonization."""
    print("\n" + "=" * 70)
    print("MILESTONE 4: Label Harmonization")
    print("=" * 70)

    from src.label_harmonization.harmonizer import run_harmonization
    run_harmonization()

    print("\n✅ Milestone 4 complete.")


def run_milestone_5():
    """Milestone 5: Unified Dataset + Negatives + Splitting."""
    print("\n" + "=" * 70)
    print("MILESTONE 5: Unified Dataset Construction")
    print("=" * 70)

    from src.unified_dataset.builder import build_unified_dataset
    from src.pair_generation.negative_sampling import (
        generate_english_negatives, split_dataset,
    )
    from src.evaluation.human_validation import (
        sample_for_validation, create_annotation_template,
    )
    from configs.paths import PROCESSED_DIR
    import pandas as pd
    import json

    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    # Build unified
    print("\n[1/4] Building unified dataset...")
    unified, vn_u, en_u = build_unified_dataset()
    unified.to_csv(PROCESSED_DIR / "unified_dataset.csv", index=False, encoding="utf-8")
    print(f"  ✓ Saved: outputs/processed/unified_dataset.csv ({len(unified):,} pairs)")

    # Generate negatives
    print("\n[2/4] Generating negative pairs...")
    neg_df = generate_english_negatives(n_random=15000, n_semi_hard=10000, n_hard=5000)
    neg_df.to_csv(PROCESSED_DIR / "english_negatives.csv", index=False, encoding="utf-8")
    print(f"  ✓ Saved: outputs/processed/english_negatives.csv ({len(neg_df):,} pairs)")

    # Combine and split
    print("\n[3/4] Splitting dataset...")
    from src.data_ingestion.english_loader import load_en_jobs, load_en_resumes
    from src.unified_dataset.builder import build_en_cv_text, build_en_jd_text
    en_jobs = load_en_jobs()
    en_resumes = load_en_resumes()
    jd_text_cache = {row["job_id"]: build_en_jd_text(row) for _, row in en_jobs.iterrows()}
    cv_text_cache = {row["resume_id"]: build_en_cv_text(row) for _, row in en_resumes.iterrows()}

    neg_subset = neg_df[["job_id", "common_relevance_label", "source_dataset",
                          "original_label", "label_source", "mapping_status",
                          "label_confidence", "language"]].copy()
    neg_subset["candidate_id"] = neg_df["resume_id"]
    neg_subset["candidate_text"] = neg_df["resume_id"].map(cv_text_cache).fillna("")
    neg_subset["job_text"] = neg_df["job_id"].map(jd_text_cache).fillna("")

    unified_with_neg = pd.concat([unified, neg_subset], ignore_index=True)
    splits = split_dataset(unified_with_neg, strategy="job_level")
    for split_name, split_df in splits.items():
        split_df.to_csv(PROCESSED_DIR / f"split_{split_name}.csv", index=False, encoding="utf-8")
    print("  ✓ Saved: split_train.csv, split_dev.csv, split_test.csv")

    # Human validation templates
    print("\n[4/4] Creating human validation templates...")
    samples = sample_for_validation(unified, n_total=200)
    templates = create_annotation_template(samples)
    with open(PROCESSED_DIR / "human_validation_templates.json", "w", encoding="utf-8") as f:
        json.dump(templates, f, ensure_ascii=False, indent=2)
    print(f"  ✓ Saved: human_validation_templates.json ({len(templates)} samples)")

    print("\n✅ Milestone 5 complete.")


def run_milestone_6():
    """Milestone 6: Training & Evaluation."""
    print("\n" + "=" * 70)
    print("MILESTONE 6: Training & Evaluation")
    print("=" * 70)

    from src.training.run_experiments import run_all_experiments

    results = run_all_experiments(
        model_name="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        num_epochs=2,
        batch_size=16,
        max_train_samples=5000,
        loss_type="cosent",
    )

    print("\n✅ Milestone 6 complete.")
    return results


def run_milestone_7():
    """Milestone 7: API Server."""
    print("\n" + "=" * 70)
    print("MILESTONE 7: API Deployment")
    print("=" * 70)
    print("Starting FastAPI server...")
    print("Access API at: http://localhost:8000")
    print("API docs at:   http://localhost:8000/docs")

    import uvicorn
    uvicorn.run("src.api.main:app", host="0.0.0.0", port=8000, reload=False)


def main():
    parser = argparse.ArgumentParser(
        description="Multilingual CV-JD Matching Pipeline Runner"
    )
    parser.add_argument(
        "--milestone", "-m", type=int, choices=[1, 2, 3, 4, 5, 6, 7],
        help="Run a specific milestone (1-7)"
    )
    parser.add_argument(
        "--all", action="store_true",
        help="Run all milestones (1-6, excluding API server)"
    )
    args = parser.parse_args()

    milestone_funcs = {
        1: run_milestone_1,
        2: run_milestone_2,
        3: run_milestone_3,
        4: run_milestone_4,
        5: run_milestone_5,
        6: run_milestone_6,
        7: run_milestone_7,
    }

    if args.all:
        for m in range(1, 7):  # 1-6, skip API
            milestone_funcs[m]()
        print("\n" + "=" * 70)
        print("ALL MILESTONES (1-6) COMPLETE")
        print("=" * 70)
        print("To start the API: python run_pipeline.py --milestone 7")
    elif args.milestone:
        milestone_funcs[args.milestone]()
    else:
        parser.print_help()
        print("\nExamples:")
        print("  python run_pipeline.py --milestone 1   # Run profiling only")
        print("  python run_pipeline.py --milestone 6   # Run training only")
        print("  python run_pipeline.py --all           # Run everything (1-6)")


if __name__ == "__main__":
    main()
