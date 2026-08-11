from dataclasses import dataclass
from datetime import UTC, datetime
from uuid import UUID, uuid4

from app.domain.ticket_workflow import TicketStatus
from app.domain.tickets import (
    AccessWindow,
    ResidentContext,
    ServiceCategory,
    Ticket,
    UrgencySuggestion,
)
from app.repositories.tickets import TicketRepository


@dataclass(frozen=True)
class CreateTicketCommand:
    client_request_id: UUID
    title: str
    description: str
    category: ServiceCategory
    urgency_suggestion: UrgencySuggestion
    access_window: AccessWindow


@dataclass(frozen=True)
class CreateTicketResult:
    ticket: Ticket
    was_created: bool


class TicketService:
    def __init__(self, repository: TicketRepository) -> None:
        self._repository = repository

    def create_ticket(
        self,
        command: CreateTicketCommand,
        resident: ResidentContext,
    ) -> CreateTicketResult:
        now = datetime.now(UTC)
        candidate = Ticket(
            id=uuid4(),
            client_request_id=command.client_request_id,
            property_id=resident.property_id,
            unit_id=resident.unit_id,
            resident_id=resident.user_id,
            title=command.title,
            description=command.description,
            category=command.category,
            urgency_suggestion=command.urgency_suggestion,
            access_window=command.access_window,
            status=TicketStatus.OPEN,
            version=1,
            assigned_worker=None,
            created_at=now,
            updated_at=now,
        )
        ticket, was_created = self._repository.create(candidate)
        return CreateTicketResult(ticket=ticket, was_created=was_created)

    def list_tickets(self, resident: ResidentContext) -> list[Ticket]:
        return self._repository.list_for_resident(
            property_id=resident.property_id,
            resident_id=resident.user_id,
        )

    def get_ticket(
        self,
        ticket_id: UUID,
        resident: ResidentContext,
    ) -> Ticket | None:
        return self._repository.get_for_resident(
            ticket_id=ticket_id,
            property_id=resident.property_id,
            resident_id=resident.user_id,
        )
