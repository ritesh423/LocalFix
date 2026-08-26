from dataclasses import dataclass
from uuid import UUID

from app.domain.ticket_workflow import UserRole
from app.domain.tickets import ManagerContext, ResidentContext, WorkerContext


@dataclass(frozen=True)
class AuthenticatedIdentity:
    firebase_uid: str
    email: str | None = None
    display_name: str | None = None


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
