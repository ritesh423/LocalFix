from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum
from uuid import UUID

from app.domain.ticket_workflow import UserRole


class DevicePlatform(StrEnum):
    ANDROID = "android"


@dataclass(frozen=True)
class DeviceRegistration:
    installation_id: UUID
    firebase_installation_id: str
    platform: DevicePlatform
    role: UserRole
    user_id: UUID
    property_id: UUID
    registered_at: datetime
    updated_at: datetime
