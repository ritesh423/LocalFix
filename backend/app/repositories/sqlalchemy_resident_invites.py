from datetime import datetime
from uuid import UUID

from sqlalchemy import select, update
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import ResidentInviteRecord
from app.domain.auth import ResidentInvite


class SqlAlchemyResidentInviteRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save(self, invite: ResidentInvite) -> ResidentInvite:
        with self._session_factory.begin() as session:
            session.merge(self._to_record(invite))
        return invite

    def find_by_digest(self, code_digest: str) -> ResidentInvite | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(ResidentInviteRecord).where(
                    ResidentInviteRecord.code_digest == code_digest
                )
            )
            return self._to_domain(record) if record is not None else None

    def claim(
        self,
        invite_id: UUID,
        firebase_uid: str,
        claimed_at: datetime,
    ) -> ResidentInvite | None:
        with self._session_factory.begin() as session:
            result = session.execute(
                update(ResidentInviteRecord)
                .where(
                    ResidentInviteRecord.id == invite_id,
                    ResidentInviteRecord.claimed_by_firebase_uid.is_(None),
                )
                .values(
                    claimed_by_firebase_uid=firebase_uid,
                    claimed_at=claimed_at,
                )
            )
            if result.rowcount == 0:
                record = session.get(ResidentInviteRecord, invite_id)
                if (
                    record is None
                    or record.claimed_by_firebase_uid != firebase_uid
                ):
                    return None
            else:
                record = session.get(ResidentInviteRecord, invite_id)
            return self._to_domain(record)

    @staticmethod
    def _to_record(invite: ResidentInvite) -> ResidentInviteRecord:
        return ResidentInviteRecord(
            id=invite.id,
            property_id=invite.property_id,
            unit_id=invite.unit_id,
            code_digest=invite.code_digest,
            expires_at=invite.expires_at,
            claimed_by_firebase_uid=invite.claimed_by_firebase_uid,
            claimed_at=invite.claimed_at,
            revoked_at=invite.revoked_at,
            created_at=invite.created_at,
        )

    @staticmethod
    def _to_domain(record: ResidentInviteRecord) -> ResidentInvite:
        return ResidentInvite(
            id=record.id,
            property_id=record.property_id,
            unit_id=record.unit_id,
            code_digest=record.code_digest,
            expires_at=record.expires_at,
            claimed_by_firebase_uid=record.claimed_by_firebase_uid,
            claimed_at=record.claimed_at,
            revoked_at=record.revoked_at,
            created_at=record.created_at,
        )
