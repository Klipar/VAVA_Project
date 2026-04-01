from pydantic import BaseModel


class PastTask(BaseModel):
    description: str


class Worker(BaseModel):
    id: int
    name: str
    skills: list[str]
    active_tasks: int
    max_tasks: int = 5
    past_tasks: list[PastTask] = []


class TaskRequest(BaseModel):
    description: str
    required_skills: list[str]
    workers: list[Worker]


class ScoreBreakdown(BaseModel):
    text_similarity: float
    most_similar_task: str | None
    skill_overlap: float
    matched_skills: list[str]
    missing_skills: list[str]
    match_ratio: str
    workload_score: float
    active_tasks: int


class WorkerResult(BaseModel):
    worker_id: int
    worker_name: str
    final_score: float
    explanation: str
    breakdown: ScoreBreakdown


class SuggestResponse(BaseModel):
    ranked_workers: list[WorkerResult]