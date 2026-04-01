from fastapi import FastAPI
from schemas import TaskRequest, SuggestResponse
from scoring_system import rank_workers

app = FastAPI(title="Smart Task Assignment API")

@app.post("/suggest", response_model=SuggestResponse)
def suggest_workers(task_request: TaskRequest) -> SuggestResponse:
    task = {
        "description": task_request.description,
        "required_skills": task_request.required_skills
    }

    workers = [{
          "id": worker.id,
            "name": worker.name,
            "skills": worker.skills,
            "active_tasks": worker.active_tasks,
            "max_tasks": worker.max_tasks,
            "past_tasks": [{"description": pt.description} for pt in worker.past_tasks],
        }
        for worker in task_request.workers
    ]

    ranked_workers = rank_workers(task, workers)

    return SuggestResponse(ranked_workers=ranked_workers)