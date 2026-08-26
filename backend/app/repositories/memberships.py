from typing import Protocol

from app.domain.auth import PropertyMembership
from app.domain.ticket_workflow import UserRole


class MembershipRepository(Protocol):
    def save(self, membership: PropertyMembership) -> PropertyMembership: ...

    def list_active(self, firebase_uid: str) -> list[PropertyMembership]: ...

    def find_active(
        self,
        firebase_uid: str,
        role: UserRole,
    ) -> PropertyMembership | None: ...


class InMemoryMembershipRepository:
    def __init__(self) -> None:
        self._memberships: dict[object, PropertyMembership] = {}

    def save(self, membership: PropertyMembership) -> PropertyMembership:
        self._memberships[membership.id] = membership
        return membership

    def list_active(self, firebase_uid: str) -> list[PropertyMembership]:
        return [
            membership
            for membership in self._memberships.values()
            if membership.firebase_uid == firebase_uid and membership.is_active
        ]

    def find_active(
        self,
        firebase_uid: str,
        role: UserRole,
    ) -> PropertyMembership | None:
        return next(
            (
                membership
                for membership in self.list_active(firebase_uid)
                if membership.role is role
            ),
            None,
        )
