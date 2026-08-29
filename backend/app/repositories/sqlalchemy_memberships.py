from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import PropertyMembershipRecord
from app.domain.auth import PropertyMembership
from app.domain.ticket_workflow import UserRole


class SqlAlchemyMembershipRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save(self, membership: PropertyMembership) -> PropertyMembership:
        with self._session_factory.begin() as session:
            session.merge(self._to_record(membership))
        return membership

    def list_active(self, firebase_uid: str) -> list[PropertyMembership]:
        with self._session_factory() as session:
            records = session.scalars(
                select(PropertyMembershipRecord).where(
                    PropertyMembershipRecord.firebase_uid == firebase_uid,
                    PropertyMembershipRecord.is_active.is_(True),
                )
            ).all()
            return [self._to_domain(record) for record in records]

    def find(
        self,
        firebase_uid: str,
        property_id: UUID,
        role: UserRole,
    ) -> PropertyMembership | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(PropertyMembershipRecord).where(
                    PropertyMembershipRecord.firebase_uid == firebase_uid,
                    PropertyMembershipRecord.property_id == property_id,
                    PropertyMembershipRecord.role == role.value,
                )
            )
            return self._to_domain(record) if record is not None else None

    def find_active(
        self,
        firebase_uid: str,
        role: UserRole,
    ) -> PropertyMembership | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(PropertyMembershipRecord).where(
                    PropertyMembershipRecord.firebase_uid == firebase_uid,
                    PropertyMembershipRecord.role == role.value,
                    PropertyMembershipRecord.is_active.is_(True),
                )
            )
            return self._to_domain(record) if record is not None else None

    @staticmethod
    def _to_record(membership: PropertyMembership) -> PropertyMembershipRecord:
        return PropertyMembershipRecord(
            id=membership.id,
            firebase_uid=membership.firebase_uid,
            user_id=membership.user_id,
            property_id=membership.property_id,
            role=membership.role.value,
            unit_id=membership.unit_id,
            is_active=membership.is_active,
        )

    @staticmethod
    def _to_domain(record: PropertyMembershipRecord) -> PropertyMembership:
        return PropertyMembership(
            id=record.id,
            firebase_uid=record.firebase_uid,
            user_id=record.user_id,
            property_id=record.property_id,
            role=UserRole(record.role),
            unit_id=record.unit_id,
            is_active=record.is_active,
        )
