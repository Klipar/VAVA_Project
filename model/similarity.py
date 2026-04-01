import numpy as np
from embeddings import get_embedding, get_embeddings_batch


# Function to calculate cosine similarity between two vectors
def cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    dot_product = np.dot(vec1, vec2)
    # Calculate the magnitude of the vectors
    norm = np.linalg.norm(vec1) * np.linalg.norm(vec2)
    if norm == 0:
        return 0.0
    
    return dot_product / norm


# Function to compute similarity between a new task and a list of user tasks
def compute_similarity(new_task_text: str, user_tasks_text: list[dict]) -> list[dict]:
    if not user_tasks_text:
        return {
            "max_similarity": 0.0,
            "avg_similarity": 0.0,
            "most_similar_task": None
        }

    new_task_embedding = get_embedding(new_task_text)

    # Batch compute embeddings for user tasks
    user_tasks_text = [task['description'] for task in user_tasks_text]
    user_tasks_embeddings = get_embeddings_batch(user_tasks_text)

    similarities = [
        cosine_similarity(new_task_embedding, user_embedding)
        for user_embedding in user_tasks_embeddings
    ]

    max_index = int(np.argmax(similarities))

    return {
        "max_similarity": round(float(similarities[max_index]), 4),
        "avg_similarity": round(float(np.mean(similarities)), 4),
        "most_similar_task": user_tasks_text[max_index]
    }