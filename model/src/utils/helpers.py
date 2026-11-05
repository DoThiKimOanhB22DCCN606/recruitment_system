"""
Utility functions for report generation, text processing, and data validation.
"""
import json
import hashlib
from pathlib import Path
from datetime import datetime
from typing import Any


def generate_report_header(title: str, dataset_name: str) -> str:
    """Generate a standardized markdown report header."""
    return (
        f"# {title}\n\n"
        f"**Dataset:** {dataset_name}  \n"
        f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  \n"
        f"**Pipeline:** Multilingual CV-JD Matching System  \n\n---\n\n"
    )


def safe_json_dump(obj: Any, path: Path, ensure_ascii: bool = False):
    """Write JSON with UTF-8 encoding."""
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=ensure_ascii, indent=2, default=str)


def safe_json_load(path: Path) -> Any:
    """Read JSON with UTF-8 encoding."""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def text_hash(text: str) -> str:
    """Compute a stable hash for text content."""
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def format_pct(n: int, total: int) -> str:
    """Format a count as count (percentage)."""
    if total == 0:
        return f"{n} (N/A)"
    return f"{n} ({100 * n / total:.1f}%)"


def format_number(n) -> str:
    """Format a number with commas."""
    if isinstance(n, float):
        return f"{n:,.2f}"
    return f"{n:,}"
