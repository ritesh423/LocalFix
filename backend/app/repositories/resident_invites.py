from dataclasses import replace
from datetime import datetime
from typing import Protocol
from uuid import UUID

from app.domain.auth import ResidentInvite


class ResidentInviteRepository(Protocol):
    def save(self, invite: ResidentInvite) -> ResidentInvite: ...

    def find_by_digest(self, code_digest: str) -> ResidentInvite | None: ...

    def claim(
        self,
        invite_id: UUID,
        firebase_uid: str,
        claimed_at: datetime,
    ) -> ResidentInvite | None: ...


class InMemoryResidentInviteRepository:
    def __init__(self) -> None:
        self._invites: dict[UUID, ResidentInvite] = {}

    def save(self, invite: ResidentInvite) -> ResidentInvite:
        self._invites[invite.id] = invite
        return invite

    def find_by_digest(self, code_digest: str) -> ResidentInvite | None:
        return next(
            (
                invite
                for invite in self._invites.values()
                if invite.code_digest == code_digest
            ),
            None,
        )

    def claim(
        self,
        invite_id: UUID,
        firebase_uid: str,
        claimed_at: datetime,
    ) -> ResidentInvite | None:
        invite = self._invites.get(invite_id)
        if invite is None:
            return None
        if invite.claimed_by_firebase_uid not in (None, firebase_uid):
            return None
        if invite.claimed_by_firebase_uid == firebase_uid:
            return invite
        claimed = replace(
            invite,
            claimed_by_firebase_uid=firebase_uid,
            claimed_at=claimed_at,
        )
        self._invites[invite_id] = claimed
        return claimed
