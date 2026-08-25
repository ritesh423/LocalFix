from datetime import datetime
from typing import Protocol

from app.domain.notifications import NotificationJob, NotificationStatus


class NotificationOutboxRepository(Protocol):
    def list_ready(
        self,
        now: datetime,
        limit: int = 100,
    ) -> list[NotificationJob]: ...

    def update(self, job: NotificationJob) -> None: ...


class InMemoryNotificationOutboxRepository:
    def __init__(self, jobs: list[NotificationJob]) -> None:
        self._jobs = jobs

    def list_ready(
        self,
        now: datetime,
        limit: int = 100,
    ) -> list[NotificationJob]:
        ready = [
            job
            for job in self._jobs
            if job.status is NotificationStatus.PENDING and job.available_at <= now
        ]
        return sorted(ready, key=lambda job: job.created_at)[:limit]

    def update(self, job: NotificationJob) -> None:
        for index, existing in enumerate(self._jobs):
            if existing.id == job.id:
                self._jobs[index] = job
                return
        raise LookupError(f"Notification job {job.id} was not found.")
