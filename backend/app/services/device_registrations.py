from datetime import UTC, datetime
from uuid import UUID

from app.domain.device_registrations import DevicePlatform, DeviceRegistration
from app.domain.ticket_workflow import UserRole
from app.repositories.device_registrations import DeviceRegistrationRepository


class DeviceRegistrationService:
    def __init__(self, repository: DeviceRegistrationRepository) -> None:
        self._repository = repository

    def register(
        self,
        installation_id: UUID,
        firebase_installation_id: str,
        platform: DevicePlatform,
        role: UserRole,
        user_id: UUID,
        property_id: UUID,
    ) -> DeviceRegistration:
        now = datetime.now(UTC)
        existing = self._repository.get(installation_id)
        registration = DeviceRegistration(
            installation_id=installation_id,
            firebase_installation_id=firebase_installation_id.strip(),
            platform=platform,
            role=role,
            user_id=user_id,
            property_id=property_id,
            registered_at=existing.registered_at if existing is not None else now,
            updated_at=now,
        )
        if existing == registration:
            return existing
        return self._repository.save(registration)
