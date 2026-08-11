from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.domain.ticket_workflow import TicketStatus
from app.domain.tickets import (
    AccessWindow,
    ServiceCategory,
    Ticket,
    UrgencySuggestion,
)
from app.services.tickets import CreateTicketCommand


class TicketCreateRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    client_request_id: UUID
    title: str = Field(min_length=5, max_length=80)
    description: str = Field(min_length=10, max_length=500)
    category: ServiceCategory
    urgency_suggestion: UrgencySuggestion
    access_window: AccessWindow

    def to_command(self) -> CreateTicketCommand:
        return CreateTicketCommand(
            client_request_id=self.client_request_id,
            title=self.title.strip(),
            description=self.description.strip(),
            category=self.category,
            urgency_suggestion=self.urgency_suggestion,
            access_window=self.access_window,
        )


class TicketResponse(BaseModel):
    id: UUID
    client_request_id: UUID
    unit_id: UUID
    title: str
    description: str
    category: ServiceCategory
    urgency_suggestion: UrgencySuggestion
    access_window: AccessWindow
    status: TicketStatus
    version: int
    assigned_worker: str | None
    created_at: datetime
    updated_at: datetime

    @classmethod
    def from_domain(cls, ticket: Ticket) -> "TicketResponse":
        return cls(
            id=ticket.id,
            client_request_id=ticket.client_request_id,
            unit_id=ticket.unit_id,
            title=ticket.title,
            description=ticket.description,
            category=ticket.category,
            urgency_suggestion=ticket.urgency_suggestion,
            access_window=ticket.access_window,
            status=ticket.status,
            version=ticket.version,
            assigned_worker=ticket.assigned_worker,
            created_at=ticket.created_at,
            updated_at=ticket.updated_at,
        )
