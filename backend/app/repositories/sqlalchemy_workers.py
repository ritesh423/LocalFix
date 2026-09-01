from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import PropertyWorkerRecord
from app.domain.tickets import ServiceCategory, Worker


class SqlAlchemyWorkerRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save(self, worker: Worker) -> Worker:
        with self._session_factory.begin() as session:
            session.merge(self._to_record(worker))
        return worker

    def get(self, property_id: UUID, worker_id: UUID) -> Worker | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(PropertyWorkerRecord).where(
                    PropertyWorkerRecord.id == worker_id,
                    PropertyWorkerRecord.property_id == property_id,
                )
            )
            return self._to_domain(record) if record is not None else None

    def list_active(self, property_id: UUID) -> list[Worker]:
        with self._session_factory() as session:
            records = session.scalars(
                select(PropertyWorkerRecord)
                .where(
                    PropertyWorkerRecord.property_id == property_id,
                    PropertyWorkerRecord.is_active.is_(True),
                )
                .order_by(PropertyWorkerRecord.name)
            ).all()
            return [self._to_domain(record) for record in records]

    @staticmethod
    def _to_record(worker: Worker) -> PropertyWorkerRecord:
        return PropertyWorkerRecord(
            id=worker.id,
            property_id=worker.property_id,
            name=worker.name,
            specialty=worker.specialty.value,
            is_active=worker.is_active,
        )

    @staticmethod
    def _to_domain(record: PropertyWorkerRecord) -> Worker:
        return Worker(
            id=record.id,
            property_id=record.property_id,
            name=record.name,
            specialty=ServiceCategory(record.specialty),
            is_active=record.is_active,
        )
