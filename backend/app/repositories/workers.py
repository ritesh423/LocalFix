from typing import Protocol
from uuid import UUID

from app.domain.tickets import Worker


class WorkerRepository(Protocol):
    def save(self, worker: Worker) -> Worker: ...

    def get(self, property_id: UUID, worker_id: UUID) -> Worker | None: ...

    def list_active(self, property_id: UUID) -> list[Worker]: ...


class InMemoryWorkerRepository:
    def __init__(self, workers: tuple[Worker, ...] = ()) -> None:
        self._workers = {worker.id: worker for worker in workers}

    def save(self, worker: Worker) -> Worker:
        self._workers[worker.id] = worker
        return worker

    def get(self, property_id: UUID, worker_id: UUID) -> Worker | None:
        worker = self._workers.get(worker_id)
        return (
            worker if worker is not None and worker.property_id == property_id else None
        )

    def list_active(self, property_id: UUID) -> list[Worker]:
        return sorted(
            [
                worker
                for worker in self._workers.values()
                if worker.property_id == property_id and worker.is_active
            ],
            key=lambda worker: worker.name.casefold(),
        )
