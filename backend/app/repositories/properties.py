from typing import Protocol
from uuid import UUID

from app.domain.properties import Property, PropertyUnit


class PropertyRepository(Protocol):
    def save_property(self, property_: Property) -> Property: ...

    def save_unit(self, unit: PropertyUnit) -> PropertyUnit: ...

    def get_property(self, property_id: UUID) -> Property | None: ...

    def get_unit(self, property_id: UUID, unit_id: UUID) -> PropertyUnit | None: ...

    def find_unit_by_label(
        self,
        property_id: UUID,
        normalized_label: str,
    ) -> PropertyUnit | None: ...


class InMemoryPropertyRepository:
    def __init__(self) -> None:
        self._properties: dict[UUID, Property] = {}
        self._units: dict[UUID, PropertyUnit] = {}

    def save_property(self, property_: Property) -> Property:
        self._properties[property_.id] = property_
        return property_

    def save_unit(self, unit: PropertyUnit) -> PropertyUnit:
        self._units[unit.id] = unit
        return unit

    def get_property(self, property_id: UUID) -> Property | None:
        return self._properties.get(property_id)

    def get_unit(self, property_id: UUID, unit_id: UUID) -> PropertyUnit | None:
        unit = self._units.get(unit_id)
        return unit if unit is not None and unit.property_id == property_id else None

    def find_unit_by_label(
        self,
        property_id: UUID,
        normalized_label: str,
    ) -> PropertyUnit | None:
        return next(
            (
                unit
                for unit in self._units.values()
                if unit.property_id == property_id
                and unit.normalized_label == normalized_label
            ),
            None,
        )
