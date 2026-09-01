from dataclasses import dataclass
from datetime import datetime
from uuid import UUID

from app.domain.ticket_workflow import UserRole
from app.domain.tickets import ManagerContext, ResidentContext, WorkerContext


@dataclass(frozen=True)
class AuthenticatedIdentity:
    firebase_uid: str
    email: str | None = None
    display_name: str | None = None
    email_verified: bool = False


@dataclass(frozen=True)
class ResidentInvite:
    id: UUID
    property_id: UUID
    unit_id: UUID
    code_digest: str
    expires_at: datetime
    created_at: datetime
    claimed_by_firebase_uid: str | None = None
    claimed_at: datetime | None = None
    revoked_at: datetime | None = None


@dataclass(frozen=True)
class StaffInvite:
    id: UUID
    property_id: UUID
    user_id: UUID
    role: UserRole
    code_digest: str
    expires_at: datetime
    created_at: datetime
    claimed_by_firebase_uid: str | None = None
    claimed_at: datetime | None = None
    revoked_at: datetime | None = None


@dataclass(frozen=True)
class PropertyMembership:
    id: UUID
    firebase_uid: str
    user_id: UUID
    property_id: UUID
    role: UserRole
    unit_id: UUID | None = None
    is_active: bool = True

    def resident_context(self) -> ResidentContext:
        if self.role is not UserRole.RESIDENT or self.unit_id is None:
            raise ValueError("A resident membership must include a unit.")
        return ResidentContext(
            user_id=self.user_id,
            property_id=self.property_id,
            unit_id=self.unit_id,
        )

    def manager_context(self) -> ManagerContext:
        if self.role is not UserRole.MANAGER:
            raise ValueError("A manager membership is required.")
        return ManagerContext(
            user_id=self.user_id,
            property_id=self.property_id,
        )

    def worker_context(self) -> WorkerContext:
        if self.role is not UserRole.WORKER:
            raise ValueError("A worker membership is required.")
        return WorkerContext(
            worker_id=self.user_id,
            property_id=self.property_id,
        )
