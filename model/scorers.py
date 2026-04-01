# Function to compute the skill overlap between required skills and worker skills
def compute_skill_overlap(required_skills: list[str], worker_skills: list[str]) -> dict:
    if not required_skills:
        return {
            "skill_overlap_score": 1.0, # If there are no required skills - everyone matches perfectly
            "missing_skills": [],
            "match_ratio": "0/0"
        }
    
    required_normalized = [skill.lower().strip() for skill in required_skills]
    worker_normalized = [skill.lower().strip() for skill in worker_skills]

    matched_skills = [skill for skill in required_skills if skill.lower().strip() in worker_normalized]
    missing_skills = [skill for skill in required_skills if skill.lower().strip() not in worker_normalized]

    score = len(matched_skills) / len(required_skills)

    return {
        "skill_overlap_score": round(score, 4),
        "matched_skills": matched_skills,
        "missing_skills": missing_skills,
        "match_ratio": f"{len(matched_skills)}/{len(required_skills)}"
    }

# Penalize workers based on their current workload. 
# The more active tasks they have, the lower their score.
def compute_workload_score(active_tasks: int, max_tasks: int = 5) -> dict:
    if max_tasks <= 0:
        return {
            "workload_score": 0.0,
            "active_tasks": active_tasks,
        }
    
    score = max(0.0, 1.0 - (active_tasks / max_tasks))

    return {
        "workload_score": round(score, 4),
        "active_tasks": active_tasks,
    }


# Generate a human-readable explanation based on the breakdown of scores and factors
def generate_explanation(breakdown: dict) -> str:
    parts = []

    # Text similarity
    sim = breakdown["text_similarity"]
    if sim >= 0.8:
        parts.append(f"Their past work is very similar to this task (\"{breakdown['most_similar_task']}\").")
    elif sim >= 0.5:
        parts.append(f"They have some relevant past experience (\"{breakdown['most_similar_task']}\").")
    elif breakdown["most_similar_task"] is None:
        parts.append("They have no recorded past tasks to compare against.")
    else:
        parts.append("Their past work is not closely related to this task.")

    # Skill overlap
    ratio = breakdown["match_ratio"]
    matched = breakdown["matched_skills"]
    missing = breakdown["missing_skills"]

    if not missing:
        parts.append(f"They have all required skills ({', '.join(matched)}).")
    elif matched:
        parts.append(f"They match {ratio} required skills ({', '.join(matched)}), but are missing {', '.join(missing)}.")
    else:
        parts.append(f"They have none of the required skills.")

    # Workload
    active = breakdown["active_tasks"]
    workload = breakdown["workload_score"]

    if workload == 1.0:
        parts.append("They currently have no active tasks.")
    elif workload >= 0.6:
        parts.append(f"Their current workload is low ({active} active tasks).")
    elif workload >= 0.3:
        parts.append(f"Their current workload is moderate ({active} active tasks).")
    else:
        parts.append(f"They are heavily loaded ({active} active tasks).")

    # Overall verdict
    score = breakdown.get("final_score", 0)
    if score >= 0.8:
        verdict = "Strong match."
    elif score >= 0.6:
        verdict = "Good match."
    elif score >= 0.4:
        verdict = "Partial match."
    else:
        verdict = "Weak match."

    return f"{verdict} " + " ".join(parts)