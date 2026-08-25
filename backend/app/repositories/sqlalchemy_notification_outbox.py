from datetime import UTC, datetime

from sqlalchemy import select, update
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import NotificationOutboxRecord
from app.domain.notifications import (
    NotificationJob,
    NotificationKind,
    NotificationStatus,
)
from app.domain.ticket_workflow import UserRole


class SqlAlchemyNotificationOutboxRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def list_ready(
        self,
        now: datetime,
        limit: int = 100,
    ) -> list[NotificationJob]:
        statement = (
            select(NotificationOutboxRecord)
            .where(
                NotificationOutboxRecord.status == NotificationStatus.PENDING.value,
                NotificationOutboxRecord.available_at <= now,
            )
            .order_by(NotificationOutboxRecord.created_at.asc())
            .limit(limit)
        )
        with self._session_factory() as session:
            records = session.scalars(statement).all()
            return [_record_to_domain(record) for record in records]

    def update(self, job: NotificationJob) -> None:
        statement = (
            update(NotificationOutboxRecord)
            .where(NotificationOutboxRecord.id == job.id)
            .values(
                status=job.status.value,
                attempt_count=job.attempt_count,
                available_at=job.available_at,
                last_error=job.last_error,
                sent_at=job.sent_at,
                updated_at=job.updated_at,
            )
        )
        with self._session_factory() as session:
            result = session.execute(statement)
            if result.rowcount != 1:
                session.rollback()
                raise LookupError(f"Notification job {job.id} was not found.")
            session.commit()


def _record_to_domain(record: NotificationOutboxRecord) -> NotificationJob:
    return NotificationJob(
        id=record.id,
        deduplication_key=record.deduplication_key,
        ticket_id=record.ticket_id,
        property_id=record.property_id,
        recipient_role=UserRole(record.recipient_role),
        recipient_user_id=record.recipient_user_id,
        kind=NotificationKind(record.kind),
        title=record.title,
        body=record.body,
        data=dict(record.data),
        status=NotificationStatus(record.status),
        attempt_count=record.attempt_count,
        available_at=_ensure_utc(record.available_at),
        last_error=record.last_error,
        sent_at=_ensure_utc(record.sent_at) if record.sent_at is not None else None,
        created_at=_ensure_utc(record.created_at),
        updated_at=_ensure_utc(record.updated_at),
    )


def _ensure_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)
