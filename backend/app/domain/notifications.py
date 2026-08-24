from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum
from uuid import UUID

from app.domain.ticket_workflow import UserRole


class NotificationKind(StrEnum):
    NEW_REQUEST = "new_request"
    JOB_ASSIGNED = "job_assigned"
    WORK_STARTED = "work_started"
    READY_FOR_REVIEW = "ready_for_review"
    REPAIR_CONFIRMED = "repair_confirmed"
    REWORK_REQUESTED = "rework_requested"


class NotificationStatus(StrEnum):
    PENDING = "pending"
    SENT = "sent"
    FAILED = "failed"


@dataclass(frozen=True)
class NotificationJob:
    id: UUID
    deduplication_key: str
    ticket_id: UUID
    property_id: UUID
    recipient_role: UserRole
    recipient_user_id: UUID | None
    kind: NotificationKind
    title: str
    body: str
    data: dict[str, str]
    status: NotificationStatus
    attempt_count: int
    available_at: datetime
    last_error: str | None
    sent_at: datetime | None
    created_at: datetime
    updated_at: datetime
