from dataclasses import dataclass
from uuid import UUID


@dataclass(frozen=True)
class Property:
    id: UUID
    name: str
    is_active: bool = True


@dataclass(frozen=True)
class PropertyUnit:
    id: UUID
    property_id: UUID
    label: str
    normalized_label: str
    is_active: bool = True
