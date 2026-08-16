from datetime import UTC, datetime
from uuid import UUID

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import TicketRecord
from app.domain.ticket_workflow import TicketStatus
from app.domain.tickets import (
    AccessWindow,
    ServiceCategory,
    Ticket,
    TicketPriority,
    UrgencySuggestion,
)


class SqlAlchemyTicketRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def create(self, ticket: Ticket) -> tuple[Ticket, bool]:
        with self._session_factory() as session:
            existing = self._find_by_client_request_id(
                session,
                ticket.client_request_id,
            )
            if existing is not None:
                return _record_to_domain(existing), False

            record = _record_from_domain(ticket)
            session.add(record)
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

    def update_if_version(
        self,
        ticket: Ticket,
        expected_version: int,
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
                updated_at=ticket.updated_at,
            )
        )
        with self._session_factory() as session:
            result = session.execute(statement)
            if result.rowcount != 1:
                session.rollback()
                return False
            session.commit()
            return True

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
        created_at=_ensure_utc(record.created_at),
        updated_at=_ensure_utc(record.updated_at),
    )


def _ensure_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)
