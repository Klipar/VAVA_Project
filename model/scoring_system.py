from similarity import compute_similarity
from scorers import compute_skill_overlap, compute_workload_score, generate_explanation



# Compute an overall score for a worker based on similarity, skill overlap, and workload
# The new_task and worker dictionaries should have the following structure:
#     new_task: {
#         "description": str,
#         "required_skills": list[str]
#     }

#     worker: {
#         "id": any,
#         "name": str,
#         "skills": list[str],
#         "active_tasks": int,
#         "max_tasks": int,
#         "past_tasks": list[dict]  # each has at least "description"
#     }
def score_worker(new_task: dict, worker: dict, weights: dict=None) -> dict:
    if weights is None:
        weights = {
            "similarity": 0.5,
            "skill_overlap": 0.3,
            "workload": 0.2
        }

    # Compute similarity score
    similarity_result = compute_similarity(new_task["description"], worker["past_tasks"])
    # If worker doesn't have past tasks - make the neutral score of 0.5
    # This will prevent the problem of Cold Start
    text_similarity = similarity_result["max_similarity"] if worker["past_tasks"] else 0.5

    # Compute skill overlap score
    skill_overlap_result = compute_skill_overlap(new_task["required_skills"], worker["skills"])

    # Compute workload score
    workload_result = compute_workload_score(worker["active_tasks"], worker.get("max_tasks", 5))

    final_score = (weights["similarity"] * text_similarity +
                   weights["skill_overlap"] * skill_overlap_result["skill_overlap_score"] +
                   weights["workload"] * workload_result["workload_score"])

    breakdown = {
        "text_similarity": round(text_similarity, 4),
        "most_similar_task": similarity_result.get("most_similar_task"),
        "skill_overlap": skill_overlap_result["skill_overlap_score"],
        "matched_skills": skill_overlap_result["matched_skills"],
        "missing_skills": skill_overlap_result["missing_skills"],
        "match_ratio": skill_overlap_result["match_ratio"],
        "workload_score": workload_result["workload_score"],
        "active_tasks": workload_result["active_tasks"],
        "final_score": round(final_score, 4),
    }

    return {
        "worker_id":   worker["id"],
        "worker_name": worker["name"],
        "final_score": round(final_score, 4),
        "explanation": generate_explanation(breakdown),
        "breakdown":   breakdown
    }

def rank_workers(new_task: dict, workers: list[dict], weights: dict=None) -> list[dict]:
    scored_workers = [score_worker(new_task, worker, weights) for worker in workers]
    ranked_workers = sorted(scored_workers, key=lambda x: x["final_score"], reverse=True)
    return ranked_workers