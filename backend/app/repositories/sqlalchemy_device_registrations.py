from datetime import UTC, datetime
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import DeviceRegistrationRecord
from app.domain.device_registrations import DevicePlatform, DeviceRegistration
from app.domain.ticket_workflow import UserRole


class SqlAlchemyDeviceRegistrationRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save(self, registration: DeviceRegistration) -> DeviceRegistration:
        with self._session_factory() as session:
            duplicate = session.scalar(
                select(DeviceRegistrationRecord).where(
                    DeviceRegistrationRecord.firebase_installation_id
                    == registration.firebase_installation_id,
                    DeviceRegistrationRecord.installation_id
                    != registration.installation_id,
                )
            )
            if duplicate is not None:
                session.delete(duplicate)

            record = session.get(
                DeviceRegistrationRecord,
                registration.installation_id,
            )
            if record is None:
                record = DeviceRegistrationRecord(
                    installation_id=registration.installation_id,
                    registered_at=registration.registered_at,
                )
                session.add(record)
            record.firebase_installation_id = registration.firebase_installation_id
            record.platform = registration.platform.value
            record.role = registration.role.value
            record.user_id = registration.user_id
            record.property_id = registration.property_id
            record.updated_at = registration.updated_at
            session.commit()
            return _record_to_domain(record)

    def get(self, installation_id: UUID) -> DeviceRegistration | None:
        with self._session_factory() as session:
            record = session.get(DeviceRegistrationRecord, installation_id)
            return _record_to_domain(record) if record is not None else None


def _record_to_domain(record: DeviceRegistrationRecord) -> DeviceRegistration:
    return DeviceRegistration(
        installation_id=record.installation_id,
        firebase_installation_id=record.firebase_installation_id,
        platform=DevicePlatform(record.platform),
        role=UserRole(record.role),
        user_id=record.user_id,
        property_id=record.property_id,
        registered_at=_ensure_utc(record.registered_at),
        updated_at=_ensure_utc(record.updated_at),
    )


def _ensure_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)
