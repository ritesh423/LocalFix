from datetime import UTC, datetime
from uuid import UUID

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import (
    NotificationOutboxRecord,
    TicketEventRecord,
    TicketRecord,
)
from app.domain.notifications import NotificationJob
from app.domain.ticket_workflow import TicketAction, TicketStatus, UserRole
from app.domain.tickets import (
    AccessWindow,
    ServiceCategory,
    Ticket,
    TicketEvent,
    TicketPriority,
    UrgencySuggestion,
)


class SqlAlchemyTicketRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def create(
        self,
        ticket: Ticket,
        event: TicketEvent,
        notification_jobs: tuple[NotificationJob, ...] = (),
    ) -> tuple[Ticket, bool]:
        with self._session_factory() as session:
            existing = self._find_by_client_request_id(
                session,
                ticket.client_request_id,
            )
            if existing is not None:
                return _record_to_domain(existing), False

            record = _record_from_domain(ticket)
            session.add(record)
            session.add(_event_record_from_domain(event))
            session.add_all(
                _notification_record_from_domain(job) for job in notification_jobs
            )
            try:
                session.commit()
            except IntegrityError:
                session.rollback()
                existing = self._find_by_client_request_id(
                    session,
                    ticket.client_request_id,
                )
                if existing is None:
                    raise
                return _record_to_domain(existing), False

            return _record_to_domain(record), True

    def list_for_resident(
        self,
        property_id: UUID,
        resident_id: UUID,
    ) -> list[Ticket]:
        statement = (
            select(TicketRecord)
            .where(
                TicketRecord.property_id == property_id,
                TicketRecord.resident_id == resident_id,
            )
            .order_by(TicketRecord.updated_at.desc())
        )
        with self._session_factory() as session:
            records = session.scalars(statement).all()
            return [_record_to_domain(record) for record in records]

    def get_for_resident(
        self,
        ticket_id: UUID,
        property_id: UUID,
        resident_id: UUID,
    ) -> Ticket | None:
        statement = select(TicketRecord).where(
            TicketRecord.id == ticket_id,
            TicketRecord.property_id == property_id,
            TicketRecord.resident_id == resident_id,
        )
        with self._session_factory() as session:
            record = session.scalar(statement)
            return _record_to_domain(record) if record is not None else None

    def list_for_property(self, property_id: UUID) -> list[Ticket]:
        statement = (
            select(TicketRecord)
            .where(TicketRecord.property_id == property_id)
            .order_by(TicketRecord.updated_at.desc())
        )
        with self._session_factory() as session:
            records = session.scalars(statement).all()
            return [_record_to_domain(record) for record in records]

    def get_for_property(
        self,
        ticket_id: UUID,
        property_id: UUID,
    ) -> Ticket | None:
        statement = select(TicketRecord).where(
            TicketRecord.id == ticket_id,
            TicketRecord.property_id == property_id,
        )
        with self._session_factory() as session:
            record = session.scalar(statement)
            return _record_to_domain(record) if record is not None else None

    def list_for_worker(
        self,
        property_id: UUID,
        worker_id: UUID,
    ) -> list[Ticket]:
        statement = (
            select(TicketRecord)
            .where(
                TicketRecord.property_id == property_id,
                TicketRecord.assigned_worker_id == worker_id,
            )
            .order_by(TicketRecord.updated_at.desc())
        )
        with self._session_factory() as session:
            records = session.scalars(statement).all()
            return [_record_to_domain(record) for record in records]

    def get_for_worker(
        self,
        ticket_id: UUID,
        property_id: UUID,
        worker_id: UUID,
    ) -> Ticket | None:
        statement = select(TicketRecord).where(
            TicketRecord.id == ticket_id,
            TicketRecord.property_id == property_id,
            TicketRecord.assigned_worker_id == worker_id,
        )
        with self._session_factory() as session:
            record = session.scalar(statement)
            return _record_to_domain(record) if record is not None else None

    def update_if_version(
        self,
        ticket: Ticket,
        expected_version: int,
        event: TicketEvent,
        notification_jobs: tuple[NotificationJob, ...] = (),
    ) -> bool:
        statement = (
            update(TicketRecord)
            .where(
                TicketRecord.id == ticket.id,
                TicketRecord.property_id == ticket.property_id,
                TicketRecord.version == expected_version,
            )
            .values(
                priority=ticket.priority.value if ticket.priority is not None else None,
                status=ticket.status.value,
                version=ticket.version,
                assigned_worker_id=ticket.assigned_worker_id,
                assigned_worker=ticket.assigned_worker,
                completion_note=ticket.completion_note,
                parts_used=list(ticket.parts_used),
                completion_photo_key=ticket.completion_photo_key,
                completion_submitted_at=ticket.completion_submitted_at,
                resident_rating=ticket.resident_rating,
                resident_feedback=ticket.resident_feedback,
                resident_reviewed_at=ticket.resident_reviewed_at,
                updated_at=ticket.updated_at,
            )
        )
        with self._session_factory() as session:
            result = session.execute(statement)
            if result.rowcount != 1:
                session.rollback()
                return False
            session.add(_event_record_from_domain(event))
            session.add_all(
                _notification_record_from_domain(job) for job in notification_jobs
            )
            session.commit()
            return True

    def list_events(self, ticket_id: UUID) -> list[TicketEvent]:
        statement = (
            select(TicketEventRecord)
            .where(TicketEventRecord.ticket_id == ticket_id)
            .order_by(TicketEventRecord.created_at.asc(), TicketEventRecord.id.asc())
        )
        with self._session_factory() as session:
            records = session.scalars(statement).all()
            return [_event_record_to_domain(record) for record in records]

    @staticmethod
    def _find_by_client_request_id(
        session: Session,
        client_request_id: UUID,
    ) -> TicketRecord | None:
        return session.scalar(
            select(TicketRecord).where(
                TicketRecord.client_request_id == client_request_id
            )
        )


def _record_from_domain(ticket: Ticket) -> TicketRecord:
    return TicketRecord(
        id=ticket.id,
        client_request_id=ticket.client_request_id,
        property_id=ticket.property_id,
        unit_id=ticket.unit_id,
        resident_id=ticket.resident_id,
        title=ticket.title,
        description=ticket.description,
        category=ticket.category.value,
        urgency_suggestion=ticket.urgency_suggestion.value,
        priority=ticket.priority.value if ticket.priority is not None else None,
        access_window=ticket.access_window.value,
        status=ticket.status.value,
        version=ticket.version,
        assigned_worker_id=ticket.assigned_worker_id,
        assigned_worker=ticket.assigned_worker,
        completion_note=ticket.completion_note,
        parts_used=list(ticket.parts_used),
        completion_photo_key=ticket.completion_photo_key,
        completion_submitted_at=ticket.completion_submitted_at,
        resident_rating=ticket.resident_rating,
        resident_feedback=ticket.resident_feedback,
        resident_reviewed_at=ticket.resident_reviewed_at,
        created_at=ticket.created_at,
        updated_at=ticket.updated_at,
    )


def _record_to_domain(record: TicketRecord) -> Ticket:
    return Ticket(
        id=record.id,
        client_request_id=record.client_request_id,
        property_id=record.property_id,
        unit_id=record.unit_id,
        resident_id=record.resident_id,
        title=record.title,
        description=record.description,
        category=ServiceCategory(record.category),
        urgency_suggestion=UrgencySuggestion(record.urgency_suggestion),
        priority=TicketPriority(record.priority)
        if record.priority is not None
        else None,
        access_window=AccessWindow(record.access_window),
        status=TicketStatus(record.status),
        version=record.version,
        assigned_worker_id=record.assigned_worker_id,
        assigned_worker=record.assigned_worker,
        completion_note=record.completion_note,
        parts_used=tuple(record.parts_used or ()),
        completion_photo_key=record.completion_photo_key,
        completion_submitted_at=_ensure_utc(record.completion_submitted_at)
        if record.completion_submitted_at is not None
        else None,
        resident_rating=record.resident_rating,
        resident_feedback=record.resident_feedback,
        resident_reviewed_at=_ensure_utc(record.resident_reviewed_at)
        if record.resident_reviewed_at is not None
        else None,
        created_at=_ensure_utc(record.created_at),
        updated_at=_ensure_utc(record.updated_at),
    )


def _event_record_from_domain(event: TicketEvent) -> TicketEventRecord:
    return TicketEventRecord(
        id=event.id,
        ticket_id=event.ticket_id,
        actor_role=event.actor_role.value,
        actor_id=event.actor_id,
        action=event.action.value,
        from_status=event.from_status.value if event.from_status is not None else None,
        to_status=event.to_status.value,
        ticket_version=event.ticket_version,
        detail=event.detail,
        created_at=event.created_at,
    )


def _event_record_to_domain(record: TicketEventRecord) -> TicketEvent:
    return TicketEvent(
        id=record.id,
        ticket_id=record.ticket_id,
        actor_role=UserRole(record.actor_role),
        actor_id=record.actor_id,
        action=TicketAction(record.action),
        from_status=TicketStatus(record.from_status)
        if record.from_status is not None
        else None,
        to_status=TicketStatus(record.to_status),
        ticket_version=record.ticket_version,
        detail=record.detail,
        created_at=_ensure_utc(record.created_at),
    )


def _notification_record_from_domain(
    job: NotificationJob,
) -> NotificationOutboxRecord:
    return NotificationOutboxRecord(
        id=job.id,
        deduplication_key=job.deduplication_key,
        ticket_id=job.ticket_id,
        property_id=job.property_id,
        recipient_role=job.recipient_role.value,
        recipient_user_id=job.recipient_user_id,
        kind=job.kind.value,
        title=job.title,
        body=job.body,
        data=job.data,
        status=job.status.value,
        attempt_count=job.attempt_count,
        available_at=job.available_at,
        last_error=job.last_error,
        sent_at=job.sent_at,
        created_at=job.created_at,
        updated_at=job.updated_at,
    )


def _ensure_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)
