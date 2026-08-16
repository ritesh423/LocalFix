from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum
from uuid import UUID

from app.domain.ticket_workflow import TicketStatus


class ServiceCategory(StrEnum):
    PLUMBING = "plumbing"
    ELECTRICAL = "electrical"
    APPLIANCE = "appliance"
    OTHER = "other"


class UrgencySuggestion(StrEnum):
    ROUTINE = "routine"
    SOON = "soon"
    URGENT = "urgent"


class TicketPriority(StrEnum):
    ROUTINE = "routine"
    SOON = "soon"
    URGENT = "urgent"


class AccessWindow(StrEnum):
    ANYTIME = "anytime"
    MORNING = "morning"
    AFTERNOON = "afternoon"
    EVENING = "evening"


@dataclass(frozen=True)
class ResidentContext:
    user_id: UUID
    property_id: UUID
    unit_id: UUID


@dataclass(frozen=True)
class ManagerContext:
    user_id: UUID
    property_id: UUID


@dataclass(frozen=True)
class Worker:
    id: UUID
    property_id: UUID
    name: str
    specialty: ServiceCategory
    is_active: bool


@dataclass(frozen=True)
class Ticket:
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
    created_at: datetime
    updated_at: datetime
