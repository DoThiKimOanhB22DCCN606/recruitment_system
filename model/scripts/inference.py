"""
inference.py -- Inference API for CV-JD matching.

Provides predict_match(cv_text, job_text) -> matching result dict.
Can be imported as a module or run as a standalone demo.
"""

import os
import json
import numpy as np
from sentence_transformers import CrossEncoder

# ── Paths ────────────────────────────────────────────────────────────────────
BASE_DIR  = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(BASE_DIR, "output", "cv_jd_matcher_v1")

LABEL_MAP = {
    0: "Not suitable",
    1: "Partially suitable",
    2: "Highly suitable",
}

# ── Model Singleton ──────────────────────────────────────────────────────────
_model = None

def get_model():
    """Load model (singleton pattern for reuse)."""
    global _model
    if _model is None:
        print(f"Loading CV-JD matcher from {MODEL_DIR}...")
        _model = CrossEncoder(MODEL_DIR, max_length=512)
        print("Model loaded successfully.")
    return _model


def predict_match(cv_text: str, job_text: str) -> dict:
    """
    Predict matching score between a CV and a Job Description.

    Args:
        cv_text:  Candidate CV text (plain text or structured template)
        job_text: Job description text (plain text or structured template)

    Returns:
        dict with keys:
            - label (int): 0, 1, or 2
            - label_name (str): human-readable label
            - score (float): confidence score [0, 1]
            - scores (list[float]): per-class probabilities [p0, p1, p2]
    """
    model = get_model()

    raw_output = model.predict([(cv_text, job_text)])

    if len(raw_output.shape) > 1:
        logits = raw_output[0]
        # Softmax
        exp_logits = np.exp(logits - np.max(logits))
        probs = exp_logits / exp_logits.sum()
        label = int(np.argmax(probs))
        score = float(probs[label])
        scores = probs.tolist()
    else:
        score = float(raw_output[0])
        label = 2 if score > 0.66 else (1 if score > 0.33 else 0)
        scores = [0.0, 0.0, 0.0]
        scores[label] = score

    return {
        "label": label,
        "label_name": LABEL_MAP.get(label, "Unknown"),
        "score": round(score, 4),
        "scores": [round(s, 4) for s in scores],
    }


def predict_batch(pairs: list) -> list:
    """
    Predict matching scores for multiple CV-JD pairs.

    Args:
        pairs: list of (cv_text, job_text) tuples

    Returns:
        list of result dicts (same format as predict_match)
    """
    model = get_model()

    raw_output = model.predict(pairs, show_progress_bar=True)

    results = []
    if len(raw_output.shape) > 1:
        for logits in raw_output:
            exp_logits = np.exp(logits - np.max(logits))
            probs = exp_logits / exp_logits.sum()
            label = int(np.argmax(probs))
            score = float(probs[label])
            results.append({
                "label": label,
                "label_name": LABEL_MAP.get(label, "Unknown"),
                "score": round(score, 4),
                "scores": [round(s, 4) for s in probs.tolist()],
            })
    else:
        for s in raw_output:
            score = float(s)
            label = 2 if score > 0.66 else (1 if score > 0.33 else 0)
            results.append({
                "label": label,
                "label_name": LABEL_MAP.get(label, "Unknown"),
                "score": round(score, 4),
                "scores": [0.0, 0.0, 0.0],
            })

    return results


# ── Demo ─────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("=" * 60)
    print("CV-JD MATCHING INFERENCE DEMO")
    print("=" * 60)

    # Example 1: Strong match
    cv1 = """Candidate Profile

Skills:
Java, Spring Boot, PostgreSQL, Docker, Kubernetes, REST API

Experience:
- Backend Developer (3 years) | Skills: Java, Spring Boot
- Developed microservices architecture with Docker and Kubernetes

Education:
- Bachelor in Computer Science"""

    jd1 = """Job Title:
Java Backend Developer

Required Skills:
Java, Spring Boot, PostgreSQL

Preferred Skills:
Docker, Kubernetes

Minimum Experience:
2 years

Education:
Computer Science"""

    # Example 2: Partial match
    cv2 = """Candidate Profile

Skills:
Python, Django, MySQL, Git

Experience:
- Web Developer (1 year) | Skills: Python, Django

Education:
- Bachelor in Information Technology"""

    jd2 = """Job Title:
Java Backend Developer

Required Skills:
Java, Spring Boot, PostgreSQL

Preferred Skills:
Docker, Kafka

Minimum Experience:
2 years"""

    # Example 3: No match
    cv3 = """Candidate Profile

Skills:
Photoshop, Illustrator, Figma, UI Design

Experience:
- Graphic Designer (5 years) | Skills: Photoshop, Illustrator

Education:
- Bachelor in Fine Arts"""

    print("\n--- Example 1: Strong match (Java dev -> Java job) ---")
    result1 = predict_match(cv1, jd1)
    print(json.dumps(result1, indent=2))

    print("\n--- Example 2: Partial match (Python dev -> Java job) ---")
    result2 = predict_match(cv2, jd2)
    print(json.dumps(result2, indent=2))

    print("\n--- Example 3: No match (Designer -> Java job) ---")
    result3 = predict_match(cv3, jd2)
    print(json.dumps(result3, indent=2))

    print("\n--- Batch prediction ---")
    batch_results = predict_batch([(cv1, jd1), (cv2, jd2), (cv3, jd2)])
    for i, r in enumerate(batch_results):
        print(f"  Pair {i+1}: {r['label_name']} (score={r['score']})")
