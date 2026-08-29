from dataclasses import dataclass
from uuid import UUID, uuid4, uuid5

from app.domain.auth import PropertyMembership
from app.domain.ticket_workflow import UserRole
from app.repositories.memberships import MembershipRepository

LOCALFIX_USER_NAMESPACE = UUID("5e59e1e9-433f-4f68-ae7a-ad585377b285")


class InvalidMembershipError(ValueError):
    pass


class MembershipConflictError(ValueError):
    pass


@dataclass(frozen=True)
class ProvisionMembershipResult:
    membership: PropertyMembership
    was_created: bool


class MembershipProvisioningService:
    def __init__(self, repository: MembershipRepository) -> None:
        self._repository = repository

    def provision(
        self,
        firebase_uid: str,
        property_id: UUID,
        role: UserRole,
        unit_id: UUID | None = None,
        user_id: UUID | None = None,
    ) -> ProvisionMembershipResult:
        clean_firebase_uid = firebase_uid.strip()
        if not clean_firebase_uid:
            raise InvalidMembershipError("Firebase UID is required.")
        if role is UserRole.SYSTEM:
            raise InvalidMembershipError("The system role cannot be provisioned.")
        if role is UserRole.RESIDENT and unit_id is None:
            raise InvalidMembershipError("A resident membership requires a unit ID.")
        if role is not UserRole.RESIDENT and unit_id is not None:
            raise InvalidMembershipError(
                "Only resident memberships can include a unit ID."
            )

        resolved_user_id = user_id or uuid5(
            LOCALFIX_USER_NAMESPACE,
            clean_firebase_uid,
        )
        existing = self._repository.find(
            clean_firebase_uid,
            property_id,
            role,
        )
        if existing is not None:
            if existing.user_id != resolved_user_id or existing.unit_id != unit_id:
                raise MembershipConflictError(
                    "This Firebase account already has a different membership "
                    "for that property and role."
                )
            return ProvisionMembershipResult(existing, was_created=False)

        membership = PropertyMembership(
            id=uuid4(),
            firebase_uid=clean_firebase_uid,
            user_id=resolved_user_id,
            property_id=property_id,
            role=role,
            unit_id=unit_id,
        )
        self._repository.save(membership)
        return ProvisionMembershipResult(membership, was_created=True)
