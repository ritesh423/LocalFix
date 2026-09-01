from datetime import datetime
from uuid import UUID

from sqlalchemy import select, update
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import StaffInviteRecord
from app.domain.auth import StaffInvite
from app.domain.ticket_workflow import UserRole


class SqlAlchemyStaffInviteRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save(self, invite: StaffInvite) -> StaffInvite:
        with self._session_factory.begin() as session:
            session.merge(self._to_record(invite))
        return invite

    def find_by_digest(self, code_digest: str) -> StaffInvite | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(StaffInviteRecord).where(
                    StaffInviteRecord.code_digest == code_digest
                )
            )
            return self._to_domain(record) if record is not None else None

    def claim(
        self,
        invite_id: UUID,
        firebase_uid: str,
        claimed_at: datetime,
    ) -> StaffInvite | None:
        with self._session_factory.begin() as session:
            result = session.execute(
                update(StaffInviteRecord)
                .where(
                    StaffInviteRecord.id == invite_id,
                    StaffInviteRecord.claimed_by_firebase_uid.is_(None),
                )
                .values(
                    claimed_by_firebase_uid=firebase_uid,
                    claimed_at=claimed_at,
                )
            )
            if result.rowcount == 0:
                record = session.get(StaffInviteRecord, invite_id)
                if record is None or record.claimed_by_firebase_uid != firebase_uid:
                    return None
            else:
                record = session.get(StaffInviteRecord, invite_id)
            return self._to_domain(record)

    @staticmethod
    def _to_record(invite: StaffInvite) -> StaffInviteRecord:
        return StaffInviteRecord(
            id=invite.id,
            property_id=invite.property_id,
            user_id=invite.user_id,
            role=invite.role.value,
            code_digest=invite.code_digest,
            expires_at=invite.expires_at,
            claimed_by_firebase_uid=invite.claimed_by_firebase_uid,
            claimed_at=invite.claimed_at,
            revoked_at=invite.revoked_at,
            created_at=invite.created_at,
        )

    @staticmethod
    def _to_domain(record: StaffInviteRecord) -> StaffInvite:
        return StaffInvite(
            id=record.id,
            property_id=record.property_id,
            user_id=record.user_id,
            role=UserRole(record.role),
            code_digest=record.code_digest,
            expires_at=record.expires_at,
            claimed_by_firebase_uid=record.claimed_by_firebase_uid,
            claimed_at=record.claimed_at,
            revoked_at=record.revoked_at,
            created_at=record.created_at,
        )
