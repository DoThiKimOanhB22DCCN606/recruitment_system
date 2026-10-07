import os
import re
import json
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split

# Directory paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) # model/ directory
ROOT_DIR = os.path.dirname(BASE_DIR) # d:\graduation-project
OUTPUT_DIR = os.path.join(BASE_DIR, "normalized")

os.makedirs(OUTPUT_DIR, exist_ok=True)

def clean_text(text):
    """Clean text by removing html tags, unescaped characters, extra spaces, and normalizing unicode."""
    if not text or pd.isna(text) or not isinstance(text, str):
        return ""
    # Strip HTML tags
    text = re.sub(r'<[^>]+>', ' ', text)
    # Remove non-printable / weird unicode spacing
    text = re.sub(r'[\r\n\t]+', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def clean_list(items):
    """Convert numpy array, string representation, or list into clean list of strings."""
    if items is None or (isinstance(items, float) and pd.isna(items)):
        return []
    if isinstance(items, np.ndarray):
        items = items.tolist()
    if isinstance(items, str):
        if items.startswith('[') and items.endswith(']'):
            try:
                items = json.loads(items.replace("'", '"'))
            except Exception:
                items = [x.strip(" '\"[]") for x in items.split(',')]
        else:
            items = [items]
    if not isinstance(items, list):
        items = [str(items)]
    
    cleaned = [clean_text(str(x)) for x in items if clean_text(str(x))]
    return cleaned

def parse_label(val):
    """Convert various label representations (Accepted/Rejected, 1/0, True/False) to int 0 or 1."""
    if isinstance(val, (int, np.integer)):
        return 1 if val > 0 else 0
    if isinstance(val, bool):
        return 1 if val else 0
    if isinstance(val, str):
        v = val.strip().lower()
        if v in ['accepted', '1', 'true', 'yes', 'match', 'relevant']:
            return 1
        if v in ['rejected', '0', 'false', 'no', 'unmatched', 'irrelevant']:
            return 0
    return 0

def build_job_full_text(row):
    """Combine job fields into unified text representation for model embedding/tokenization."""
    parts = []
    if row.get('job_title'):
        parts.append(f"Job Title: {row['job_title']}")
    if row.get('seniority'):
        parts.append(f"Seniority: {row['seniority']}")
    if row.get('industry'):
        parts.append(f"Industry: {row['industry']}")
    
    skills = row.get('must_have_skills', [])
    if isinstance(skills, (list, np.ndarray)) and len(skills) > 0:
        parts.append(f"Must-Have Skills: {', '.join(skills)}")
    
    nice_skills = row.get('nice_to_have_skills', [])
    if isinstance(nice_skills, (list, np.ndarray)) and len(nice_skills) > 0:
        parts.append(f"Nice-To-Have Skills: {', '.join(nice_skills)}")
        
    if row.get('description'):
        parts.append(f"Description: {row['description']}")
        
    reqs = row.get('requirements', [])
    if isinstance(reqs, (list, np.ndarray)) and len(reqs) > 0:
        parts.append(f"Requirements: {' '.join(reqs)}")
    elif isinstance(reqs, str) and reqs:
        parts.append(f"Requirements: {reqs}")
        
    resps = row.get('responsibilities', [])
    if isinstance(resps, (list, np.ndarray)) and len(resps) > 0:
        parts.append(f"Responsibilities: {' '.join(resps)}")
        
    return " | ".join(parts)

def build_resume_full_text(row):
    """Combine resume fields into unified text representation for model embedding/tokenization."""
    parts = []
    if row.get('role'):
        parts.append(f"Candidate Role: {row['role']}")
    if row.get('seniority'):
        parts.append(f"Seniority: {row['seniority']}")
    if row.get('years_experience') is not None and not pd.isna(row.get('years_experience')):
        parts.append(f"Years of Experience: {row['years_experience']}")
    if row.get('education'):
        parts.append(f"Education: {row['education']}")
        
    skills = row.get('skills', [])
    if isinstance(skills, (list, np.ndarray)) and len(skills) > 0:
        parts.append(f"Skills: {', '.join(skills)}")
        
    if row.get('summary'):
        parts.append(f"Summary: {row['summary']}")
        
    exp = row.get('experience_bullets', [])
    if isinstance(exp, (list, np.ndarray)) and len(exp) > 0:
        parts.append(f"Experience Details: {' '.join(exp)}")
        
    return " | ".join(parts)

def process_benchmark_dataset():
    """Process model/dataset/dataset (Jobs, Resumes, Matches)."""
    print("\n--- Processing Benchmark Structured Matching Dataset ---")
    jobs_path = os.path.join(BASE_DIR, "dataset", "dataset", "jobs", "train-00000-of-00001.parquet")
    resumes_path = os.path.join(BASE_DIR, "dataset", "dataset", "resumes", "train-00000-of-00001.parquet")
    matches_path = os.path.join(BASE_DIR, "dataset", "dataset", "matches", "train-00000-of-00001.parquet")

    jobs_df = pd.read_parquet(jobs_path)
    resumes_df = pd.read_parquet(resumes_path)
    matches_df = pd.read_parquet(matches_path)

    # Clean Jobs
    jobs_df['job_title'] = jobs_df['job_title'].apply(clean_text)
    jobs_df['seniority'] = jobs_df['seniority'].apply(clean_text)
    jobs_df['industry'] = jobs_df['industry'].apply(clean_text)
    jobs_df['must_have_skills'] = jobs_df['must_have_skills'].apply(clean_list)
    jobs_df['nice_to_have_skills'] = jobs_df['nice_to_have_skills'].apply(clean_list)
    jobs_df['description'] = jobs_df['description'].apply(clean_text)
    jobs_df['responsibilities'] = jobs_df['responsibilities'].apply(clean_list)
    jobs_df['requirements'] = jobs_df['requirements'].apply(clean_list)
    jobs_df['full_text'] = jobs_df.apply(build_job_full_text, axis=1)
    jobs_df['language'] = 'en'

    # Clean Resumes
    resumes_df['role'] = resumes_df['role'].apply(clean_text)
    resumes_df['seniority'] = resumes_df['seniority'].apply(clean_text)
    resumes_df['industry'] = resumes_df['industry'].apply(clean_text)
    resumes_df['education'] = resumes_df['education'].apply(clean_text)
    resumes_df['skills'] = resumes_df['skills'].apply(clean_list)
    resumes_df['summary'] = resumes_df['summary'].apply(clean_text)
    resumes_df['experience_bullets'] = resumes_df['experience_bullets'].apply(clean_list)
    resumes_df['full_text'] = resumes_df.apply(build_resume_full_text, axis=1)
    resumes_df['language'] = 'en'

    # Save normalized Jobs & Resumes
    jobs_out = os.path.join(OUTPUT_DIR, "jobs_normalized.parquet")
    resumes_out = os.path.join(OUTPUT_DIR, "resumes_normalized.parquet")
    jobs_df.to_parquet(jobs_out, index=False)
    resumes_df.to_parquet(resumes_out, index=False)
    print(f"Saved normalized jobs: {jobs_df.shape} -> {jobs_out}")
    print(f"Saved normalized resumes: {resumes_df.shape} -> {resumes_out}")

    # Build Paired Matching Dataset
    job_map = dict(zip(jobs_df['job_id'], jobs_df['full_text']))
    resume_map = dict(zip(resumes_df['resume_id'], resumes_df['full_text']))
    all_resumes_set = set(resumes_df['resume_id'])
    
    pairs = []
    pair_counter = 0

    for _, row in matches_df.iterrows():
        jid = row['job_id']
        pos_rids = set(clean_list(row['relevant_resume_ids']))
        job_text = job_map.get(jid, "")

        # Positive pairs
        for rid in pos_rids:
            if rid in resume_map:
                pair_counter += 1
                pairs.append({
                    'pair_id': f"PAIR_{pair_counter:07d}",
                    'job_id': jid,
                    'resume_id': rid,
                    'job_text': job_text,
                    'resume_text': resume_map[rid],
                    'label': 1,
                    'source': 'benchmark_dataset'
                })

        # Negative sampling (1:1 ratio for balanced contrastive/binary training)
        neg_rids = list(all_resumes_set - pos_rids)
        if len(neg_rids) > 0 and len(pos_rids) > 0:
            sample_size = min(len(pos_rids), len(neg_rids))
            sampled_negs = np.random.choice(neg_rids, size=sample_size, replace=False)
            for rid in sampled_negs:
                pair_counter += 1
                pairs.append({
                    'pair_id': f"PAIR_{pair_counter:07d}",
                    'job_id': jid,
                    'resume_id': rid,
                    'job_text': job_text,
                    'resume_text': resume_map[rid],
                    'label': 0,
                    'source': 'benchmark_dataset'
                })

    pairs_df = pd.DataFrame(pairs)
    print(f"Generated benchmark paired matching dataset: {pairs_df.shape} pairs")
    return jobs_df, resumes_df, pairs_df

def process_applicant_csv():
    """Process model/dataset (1)/job_applicant_dataset.csv into matching pairs (without bias fields)."""
    csv_path = os.path.join(BASE_DIR, "dataset (1)", "job_applicant_dataset.csv")
    if not os.path.exists(csv_path):
        return pd.DataFrame()

    print("\n--- Processing Job Applicant Dataset (CSV) ---")
    df = pd.read_csv(csv_path)

    pairs = []
    for idx, row in df.iterrows():
        resume_clean = clean_text(str(row['Resume']))
        role_clean = clean_text(str(row['Job Roles']))
        jd_clean = clean_text(str(row['Job Description']))
        label = parse_label(row['Best Match'])

        job_text = f"Job Role: {role_clean} | Description: {jd_clean}" if role_clean else f"Description: {jd_clean}"

        pairs.append({
            'pair_id': f"CSV_PAIR_{idx:06d}",
            'job_id': f"CSV_JOB_{idx:06d}",
            'resume_id': f"CSV_RES_{idx:06d}",
            'job_text': job_text,
            'resume_text': resume_clean,
            'label': label,
            'source': 'applicant_csv'
        })

    applicant_df = pd.DataFrame(pairs)
    print(f"Generated applicant CSV matching dataset: {applicant_df.shape} pairs")
    return applicant_df

def process_train_json():
    """Process train.json into matching pairs."""
    train_json_path = os.path.join(ROOT_DIR, "train.json")
    if not os.path.exists(train_json_path):
        return pd.DataFrame()

    print("\n--- Processing train.json ---")
    with open(train_json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    pairs = []
    for idx, item in enumerate(data):
        pos = clean_text(item.get('opening_position', ''))
        desc = clean_text(item.get('description', ''))
        req = clean_text(item.get('requirement', ''))
        kw_job = clean_list(item.get('keywords_for_recruitment', []))
        
        cv_text = clean_text(item.get('cv', ''))
        exp_years = item.get('experience_years', '')
        kw_cv = clean_list(item.get('keywords_from_applicants', []))
        label = parse_label(item.get('result'))

        job_parts = []
        if pos: job_parts.append(f"Position: {pos}")
        if desc: job_parts.append(f"Description: {desc}")
        if req: job_parts.append(f"Requirement: {req}")
        if kw_job: job_parts.append(f"Keywords: {', '.join(kw_job)}")
        job_text = " | ".join(job_parts)

        cv_parts = []
        if exp_years: cv_parts.append(f"Experience Years: {exp_years}")
        if kw_cv: cv_parts.append(f"Keywords: {', '.join(kw_cv)}")
        if cv_text: cv_parts.append(f"CV Content: {cv_text}")
        resume_text = " | ".join(cv_parts)

        pairs.append({
            'pair_id': f"TJ_PAIR_{idx:06d}",
            'job_id': f"TJ_JOB_{idx:06d}",
            'resume_id': f"TJ_RES_{idx:06d}",
            'job_text': job_text,
            'resume_text': resume_text,
            'label': label,
            'source': 'train_json'
        })

    tj_df = pd.DataFrame(pairs)
    print(f"Generated train.json matching dataset: {tj_df.shape} pairs")
    return tj_df

def process_master_resumes():
    """Process master_resumes.jsonl into normalized JSONL/Parquet."""
    master_path = os.path.join(ROOT_DIR, "master_resumes.jsonl")
    if not os.path.exists(master_path):
        return pd.DataFrame()

    print("\n--- Processing master_resumes.jsonl ---")
    records = []
    with open(master_path, "r", encoding="utf-8") as f:
        for idx, line in enumerate(f):
            line_str = line.strip()
            if not line_str: continue
            try:
                item = json.loads(line_str)
                pinfo = item.get('personal_info', {})
                summary = clean_text(pinfo.get('summary', ''))
                
                # Extract role & exp
                exp_list = item.get('experience', [])
                role = ""
                exp_bullets = []
                if isinstance(exp_list, list) and len(exp_list) > 0:
                    first_exp = exp_list[0]
                    if isinstance(first_exp, dict):
                        role = clean_text(first_exp.get('title', ''))
                    for exp_item in exp_list:
                        if isinstance(exp_item, dict):
                            t = exp_item.get('title', '')
                            c = exp_item.get('company', '')
                            d = exp_item.get('description', '')
                            exp_bullets.append(f"{t} at {c}: {d}".strip())

                # Extract education
                edu_list = item.get('education', [])
                edu_str = ""
                if isinstance(edu_list, list) and len(edu_list) > 0:
                    first_edu = edu_list[0]
                    if isinstance(first_edu, dict):
                        deg = first_edu.get('degree', {})
                        inst = first_edu.get('institution', {})
                        deg_lvl = deg.get('level', '') if isinstance(deg, dict) else str(deg)
                        inst_name = inst.get('name', '') if isinstance(inst, dict) else str(inst)
                        edu_str = f"{deg_lvl} from {inst_name}".strip()

                # Extract skills
                skills_obj = item.get('skills', {})
                extracted_skills = []
                if isinstance(skills_obj, dict):
                    for k, v in skills_obj.items():
                        if isinstance(v, list):
                            for s in v:
                                if isinstance(s, dict):
                                    extracted_skills.append(s.get('name', ''))
                                elif isinstance(s, str):
                                    extracted_skills.append(s)
                        elif isinstance(v, dict):
                            for subk, subv in v.items():
                                if isinstance(subv, list):
                                    for s in subv:
                                        if isinstance(s, dict):
                                            extracted_skills.append(s.get('name', ''))
                                        elif isinstance(s, str):
                                            extracted_skills.append(s)
                elif isinstance(skills_obj, list):
                    extracted_skills = [str(x) for x in skills_obj]

                extracted_skills = clean_list(extracted_skills)

                row_dict = {
                    'resume_id': f"MRES_{idx:06d}",
                    'role': role,
                    'education': edu_str,
                    'skills': extracted_skills,
                    'summary': summary,
                    'experience_bullets': exp_bullets,
                    'language': 'en'
                }
                row_dict['full_text'] = build_resume_full_text(row_dict)
                records.append(row_dict)
            except Exception:
                continue

    mres_df = pd.DataFrame(records)
    mres_out = os.path.join(OUTPUT_DIR, "master_resumes_normalized.parquet")
    mres_df.to_parquet(mres_out, index=False)
    print(f"Saved master resumes normalized: {mres_df.shape} -> {mres_out}")
    return mres_df

def process_vietnamese_jobs():
    """Process model/vietnamese-job-descriptions/data.parquet into normalized Vietnamese job database."""
    vn_path = os.path.join(BASE_DIR, "vietnamese-job-descriptions", "data.parquet")
    if not os.path.exists(vn_path):
        return pd.DataFrame()

    print("\n--- Processing Vietnamese Job Descriptions ---")
    df = pd.read_parquet(vn_path)

    # Clean text columns
    df['job_title'] = df['job_title'].apply(clean_text)
    df['company_name'] = df['company_name'].apply(clean_text)
    df['salary'] = df['salary'].apply(clean_text)
    df['location'] = df['location'].apply(clean_text)
    df['job_type'] = df['job_type'].apply(clean_text)
    df['job_industry'] = df['job_industry'].apply(clean_text)
    df['experience_level'] = df['experience_level'].apply(clean_text)
    df['education_level'] = df['education_level'].apply(clean_text)
    df['job_position'] = df['job_position'].apply(clean_text)
    df['job_description'] = df['job_description'].apply(clean_text)
    df['benefits'] = df['benefits'].apply(clean_text)
    df['requirements'] = df['requirements'].apply(clean_text)
    df['language'] = 'vi'

    # Build full_text for Vietnamese jobs
    def build_vn_full_text(row):
        parts = []
        if row['job_title']: parts.append(f"Tiêu đề: {row['job_title']}")
        if row['job_industry']: parts.append(f"Ngành nghề: {row['job_industry']}")
        if row['job_position']: parts.append(f"Vị trí: {row['job_position']}")
        if row['experience_level']: parts.append(f"Kinh nghiệm: {row['experience_level']}")
        if row['education_level']: parts.append(f"Học vấn: {row['education_level']}")
        if row['job_description']: parts.append(f"Mô tả: {row['job_description']}")
        if row['requirements']: parts.append(f"Yêu cầu: {row['requirements']}")
        if row['benefits']: parts.append(f"Quyền lợi: {row['benefits']}")
        return " | ".join(parts)

    df['full_text'] = df.apply(build_vn_full_text, axis=1)

    vn_out = os.path.join(OUTPUT_DIR, "vn_jobs_normalized.parquet")
    df.to_parquet(vn_out, index=False)
    print(f"Saved normalized Vietnamese jobs: {df.shape} -> {vn_out}")
    return df

def run_pipeline():
    np.random.seed(42)
    print("==================================================")
    print("STARTING DATA NORMALIZATION PIPELINE")
    print("==================================================")

    # 1. Benchmark structured dataset
    jobs_df, resumes_df, benchmark_pairs = process_benchmark_dataset()

    # 2. Applicant CSV dataset
    applicant_pairs = process_applicant_csv()

    # 3. Train JSON dataset
    tj_pairs = process_train_json()

    # 4. Master Resumes JSONL
    mres_df = process_master_resumes()

    # 5. Vietnamese jobs dataset
    vn_jobs_df = process_vietnamese_jobs()

    # Combine all matching pairs for model training
    print("\n--- Combining All Matching Datasets ---")
    all_pairs = pd.concat([benchmark_pairs, applicant_pairs, tj_pairs], ignore_index=True)
    
    # Clean up empty text rows if any
    all_pairs = all_pairs[(all_pairs['job_text'].str.strip() != "") & (all_pairs['resume_text'].str.strip() != "")]
    
    print(f"Total unified matching pairs: {len(all_pairs)}")
    print(f"Label distribution:\n{all_pairs['label'].value_counts()}")

    # Stratified Train/Val/Test Split (80% Train, 10% Val, 10% Test)
    train_df, test_val_df = train_test_split(all_pairs, test_size=0.20, random_state=42, stratify=all_pairs['label'])
    val_df, test_df = train_test_split(test_val_df, test_size=0.50, random_state=42, stratify=test_val_df['label'])

    train_out = os.path.join(OUTPUT_DIR, "matching_pairs_train.parquet")
    val_out = os.path.join(OUTPUT_DIR, "matching_pairs_val.parquet")
    test_out = os.path.join(OUTPUT_DIR, "matching_pairs_test.parquet")

    train_df.to_parquet(train_out, index=False)
    val_df.to_parquet(val_out, index=False)
    test_df.to_parquet(test_out, index=False)

    print(f"Saved Train set: {train_df.shape} -> {train_out}")
    print(f"Saved Val set:   {val_df.shape} -> {val_out}")
    print(f"Saved Test set:  {test_df.shape} -> {test_out}")

    # Generate dataset summary report
    summary = {
        'status': 'SUCCESS',
        'jobs_normalized_count': len(jobs_df),
        'resumes_normalized_count': len(resumes_df),
        'master_resumes_count': len(mres_df) if len(mres_df) > 0 else 0,
        'vn_jobs_normalized_count': len(vn_jobs_df) if len(vn_jobs_df) > 0 else 0,
        'total_matching_pairs': len(all_pairs),
        'train_pairs': len(train_df),
        'val_pairs': len(val_df),
        'test_pairs': len(test_df),
        'sources': {
            'benchmark_dataset': len(benchmark_pairs),
            'applicant_csv': len(applicant_pairs),
            'train_json': len(tj_pairs)
        },
        'label_distribution': {
            '0_no_match': int((all_pairs['label'] == 0).sum()),
            '1_match': int((all_pairs['label'] == 1).sum())
        }
    }

    summary_path = os.path.join(OUTPUT_DIR, "dataset_summary.json")
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)

    print(f"\nSaved Dataset Summary Report -> {summary_path}")
    print("==================================================")
    print("DATA NORMALIZATION PIPELINE COMPLETED SUCCESSFULLY")
    print("==================================================")

if __name__ == "__main__":
    run_pipeline()
