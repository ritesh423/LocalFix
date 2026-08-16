from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.domain.ticket_workflow import TicketStatus
from app.domain.tickets import (
    AccessWindow,
    ServiceCategory,
    Ticket,
    TicketPriority,
    UrgencySuggestion,
    Worker,
)
from app.services.tickets import (
    AssignTicketCommand,
    CreateTicketCommand,
    StartTicketCommand,
)


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


class TicketAssignmentRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    expected_version: int = Field(ge=1)
    priority: TicketPriority
    worker_id: UUID

    def to_command(self) -> AssignTicketCommand:
        return AssignTicketCommand(
            expected_version=self.expected_version,
            priority=self.priority,
            worker_id=self.worker_id,
        )


class TicketStartRequest(BaseModel):
    expected_version: int = Field(ge=1)

    def to_command(self) -> StartTicketCommand:
        return StartTicketCommand(expected_version=self.expected_version)


class WorkerResponse(BaseModel):
    id: UUID
    name: str
    specialty: ServiceCategory

    @classmethod
    def from_domain(cls, worker: Worker) -> "WorkerResponse":
        return cls(
            id=worker.id,
            name=worker.name,
            specialty=worker.specialty,
        )


class TicketResponse(BaseModel):
    id: UUID
    client_request_id: UUID
    property_id: UUID
    unit_id: UUID
    resident_id: UUID
    title: str
    description: str
    category: ServiceCategory
    urgency_suggestion: UrgencySuggestion
    priority: TicketPriority | None
    access_window: AccessWindow
    status: TicketStatus
    version: int
    assigned_worker_id: UUID | None
    assigned_worker: str | None
    completion_note: str | None
    parts_used: list[str]
    has_completion_photo: bool
    completion_submitted_at: datetime | None
    created_at: datetime
    updated_at: datetime

    @classmethod
    def from_domain(cls, ticket: Ticket) -> "TicketResponse":
        return cls(
            id=ticket.id,
            client_request_id=ticket.client_request_id,
            property_id=ticket.property_id,
            unit_id=ticket.unit_id,
            resident_id=ticket.resident_id,
            title=ticket.title,
            description=ticket.description,
            category=ticket.category,
            urgency_suggestion=ticket.urgency_suggestion,
            priority=ticket.priority,
            access_window=ticket.access_window,
            status=ticket.status,
            version=ticket.version,
            assigned_worker_id=ticket.assigned_worker_id,
            assigned_worker=ticket.assigned_worker,
            completion_note=ticket.completion_note,
            parts_used=list(ticket.parts_used),
            has_completion_photo=ticket.completion_photo_key is not None,
            completion_submitted_at=ticket.completion_submitted_at,
            created_at=ticket.created_at,
            updated_at=ticket.updated_at,
        )
