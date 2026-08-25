from threading import Lock
from typing import Protocol
from uuid import UUID

from app.domain.device_registrations import DeviceRegistration
from app.domain.ticket_workflow import UserRole


class DeviceRegistrationRepository(Protocol):
    def save(self, registration: DeviceRegistration) -> DeviceRegistration: ...

    def get(self, installation_id: UUID) -> DeviceRegistration | None: ...

    def list_for_recipient(
        self,
        property_id: UUID,
        role: UserRole,
        user_id: UUID | None,
    ) -> list[DeviceRegistration]: ...


class InMemoryDeviceRegistrationRepository:
    def __init__(self) -> None:
        self._registrations: dict[UUID, DeviceRegistration] = {}
        self._lock = Lock()

    def save(self, registration: DeviceRegistration) -> DeviceRegistration:
        with self._lock:
            duplicate_ids = [
                installation_id
                for installation_id, existing in self._registrations.items()
                if existing.firebase_installation_id
                == registration.firebase_installation_id
                and installation_id != registration.installation_id
            ]
            for installation_id in duplicate_ids:
                del self._registrations[installation_id]
            self._registrations[registration.installation_id] = registration
            return registration

    def get(self, installation_id: UUID) -> DeviceRegistration | None:
        return self._registrations.get(installation_id)

    def list_for_recipient(
        self,
        property_id: UUID,
        role: UserRole,
        user_id: UUID | None,
    ) -> list[DeviceRegistration]:
        return sorted(
            (
                registration
                for registration in self._registrations.values()
                if registration.property_id == property_id
                and registration.role is role
                and (user_id is None or registration.user_id == user_id)
            ),
            key=lambda registration: registration.updated_at,
            reverse=True,
        )
