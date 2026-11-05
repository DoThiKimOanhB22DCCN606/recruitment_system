"""
FastAPI Inference Service (Milestone 7)

Provides:
- CV parsing and preprocessing
- CV-JD matching and ranking
- Relevance scoring with explanation
- Health check endpoint
"""
import sys
import json
import os
import numpy as np
from pathlib import Path
from typing import Optional

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

app = FastAPI(
    title="Multilingual CV-JD Matching API",
    description=(
        "AI-powered candidate-job matching system supporting Vietnamese and English. "
        "Uses a Bi-Encoder architecture with multilingual sentence embeddings."
    ),
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ═══════════════════════════════════════════════════════════════════════════
# Models
# ═══════════════════════════════════════════════════════════════════════════

class CVInput(BaseModel):
    """Candidate resume input."""
    text: str = Field(..., description="Full CV/resume text")
    skills: list[str] = Field(default=[], description="List of skills")
    experience_years: Optional[int] = Field(None, description="Years of experience")
    seniority: Optional[str] = Field(None, description="Seniority level")
    industry: Optional[str] = Field(None, description="Industry/domain")
    education: Optional[str] = Field(None, description="Education level")
    language: Optional[str] = Field("auto", description="Language: vi, en, or auto")


class JDInput(BaseModel):
    """Job description input."""
    text: str = Field(..., description="Full JD text")
    must_have_skills: list[str] = Field(default=[], description="Required skills")
    nice_to_have_skills: list[str] = Field(default=[], description="Preferred skills")
    seniority: Optional[str] = Field(None, description="Required seniority")
    industry: Optional[str] = Field(None, description="Industry")
    language: Optional[str] = Field("auto", description="Language: vi, en, or auto")


class MatchRequest(BaseModel):
    """Request for matching a CV against a JD."""
    cv: CVInput
    jd: JDInput


class RankRequest(BaseModel):
    """Request for ranking multiple CVs against a JD."""
    cvs: list[CVInput]
    jd: JDInput
    top_k: int = Field(default=10, description="Number of top candidates to return")


class MatchResult(BaseModel):
    """Match result for a single CV-JD pair."""
    relevance_score: float = Field(..., description="Relevance score (0-1)")
    relevance_label: int = Field(..., description="Relevance label (0-3)")
    relevance_name: str = Field(..., description="Relevance category name")
    explanation: dict = Field(default={}, description="Matching explanation")


class RankResult(BaseModel):
    """Ranked candidate result."""
    rank: int
    cv_index: int
    relevance_score: float
    relevance_label: int
    relevance_name: str
    explanation: dict = {}


# ═══════════════════════════════════════════════════════════════════════════
# Model Loading
# ═══════════════════════════════════════════════════════════════════════════

_model = None
_label_names = {0: "Not Relevant", 1: "Low Relevance", 2: "Relevant", 3: "Highly Relevant"}


def get_model():
    """Lazy-load the sentence transformer model."""
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        model_path = os.environ.get(
            "MODEL_PATH",
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        )
        print(f"Loading model: {model_path}")
        _model = SentenceTransformer(model_path)
    return _model


def compute_similarity(cv_text: str, jd_text: str) -> float:
    """Compute cosine similarity between CV and JD embeddings."""
    model = get_model()
    embeddings = model.encode([cv_text, jd_text])
    sim = np.dot(embeddings[0], embeddings[1]) / (
        np.linalg.norm(embeddings[0]) * np.linalg.norm(embeddings[1])
    )
    return float(sim)


def score_to_label(score: float) -> tuple[int, str]:
    """Map similarity score to relevance label."""
    # These thresholds should be calibrated against validation data
    if score >= 0.75:
        return 3, _label_names[3]
    elif score >= 0.55:
        return 2, _label_names[2]
    elif score >= 0.35:
        return 1, _label_names[1]
    else:
        return 0, _label_names[0]


def generate_explanation(cv: CVInput, jd: JDInput) -> dict:
    """Generate matching explanation based on structured fields."""
    explanation = {}

    # Skill overlap
    if cv.skills and jd.must_have_skills:
        cv_skills_lower = {s.lower() for s in cv.skills}
        must_lower = {s.lower() for s in jd.must_have_skills}
        matched = cv_skills_lower & must_lower
        missing = must_lower - cv_skills_lower

        explanation["required_skills"] = {
            "matched": list(matched),
            "missing": list(missing),
            "coverage": round(len(matched) / len(must_lower), 2) if must_lower else 0,
        }

    if cv.skills and jd.nice_to_have_skills:
        cv_skills_lower = {s.lower() for s in cv.skills}
        nice_lower = {s.lower() for s in jd.nice_to_have_skills}
        matched = cv_skills_lower & nice_lower
        explanation["preferred_skills"] = {
            "matched": list(matched),
            "coverage": round(len(matched) / len(nice_lower), 2) if nice_lower else 0,
        }

    # Seniority alignment
    if cv.seniority and jd.seniority:
        explanation["seniority"] = {
            "candidate": cv.seniority,
            "required": jd.seniority,
            "aligned": cv.seniority.lower() == jd.seniority.lower(),
        }

    # Industry match
    if cv.industry and jd.industry:
        explanation["industry"] = {
            "candidate": cv.industry,
            "required": jd.industry,
            "matched": cv.industry.lower() == jd.industry.lower(),
        }

    return explanation


# ═══════════════════════════════════════════════════════════════════════════
# Endpoints
# ═══════════════════════════════════════════════════════════════════════════

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "model_loaded": _model is not None}


@app.post("/match", response_model=MatchResult)
async def match_cv_jd(request: MatchRequest):
    """
    Match a single CV against a JD.

    Returns relevance score, label, and explanation.
    """
    try:
        cv_text = request.cv.text[:2048]
        jd_text = request.jd.text[:2048]

        similarity = compute_similarity(cv_text, jd_text)
        label, name = score_to_label(similarity)
        explanation = generate_explanation(request.cv, request.jd)

        return MatchResult(
            relevance_score=round(similarity, 4),
            relevance_label=label,
            relevance_name=name,
            explanation=explanation,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/rank", response_model=list[RankResult])
async def rank_candidates(request: RankRequest):
    """
    Rank multiple CVs against a single JD.

    Returns candidates sorted by relevance score (descending).
    """
    try:
        model = get_model()
        jd_text = request.jd.text[:2048]

        # Encode all texts
        cv_texts = [cv.text[:2048] for cv in request.cvs]
        all_texts = cv_texts + [jd_text]
        embeddings = model.encode(all_texts)

        jd_emb = embeddings[-1]
        cv_embs = embeddings[:-1]

        # Compute similarities
        results = []
        for i, cv_emb in enumerate(cv_embs):
            sim = float(np.dot(cv_emb, jd_emb) / (
                np.linalg.norm(cv_emb) * np.linalg.norm(jd_emb)
            ))
            label, name = score_to_label(sim)
            explanation = generate_explanation(request.cvs[i], request.jd)

            results.append(RankResult(
                rank=0,  # Will be set after sorting
                cv_index=i,
                relevance_score=round(sim, 4),
                relevance_label=label,
                relevance_name=name,
                explanation=explanation,
            ))

        # Sort by score descending
        results.sort(key=lambda x: x.relevance_score, reverse=True)
        for rank, result in enumerate(results[:request.top_k], 1):
            result.rank = rank

        return results[:request.top_k]

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/model-info")
async def model_info():
    """Get information about the loaded model."""
    model = get_model()
    return {
        "model_name": str(model._model_config.get("model_name_or_path", "unknown")),
        "max_seq_length": model.max_seq_length,
        "embedding_dimension": model.get_sentence_embedding_dimension(),
        "relevance_labels": _label_names,
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
