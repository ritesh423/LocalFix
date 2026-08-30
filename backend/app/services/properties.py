from dataclasses import dataclass
from uuid import UUID

from app.domain.properties import Property, PropertyUnit
from app.repositories.properties import PropertyRepository


class InvalidPropertyError(ValueError):
    pass


class PropertyConflictError(ValueError):
    pass


@dataclass(frozen=True)
class ProvisionPropertyResult:
    property: Property
    unit: PropertyUnit
    property_was_created: bool
    unit_was_created: bool


class PropertyProvisioningService:
    def __init__(self, repository: PropertyRepository) -> None:
        self._repository = repository

    def provision(
        self,
        property_id: UUID,
        property_name: str,
        unit_id: UUID,
        unit_label: str,
    ) -> ProvisionPropertyResult:
        clean_name = " ".join(property_name.split())
        clean_unit_label = " ".join(unit_label.split())
        if len(clean_name) < 3 or len(clean_name) > 120:
            raise InvalidPropertyError("Property name must be 3 to 120 characters.")
        if not clean_unit_label or len(clean_unit_label) > 80:
            raise InvalidPropertyError("Unit label must be 1 to 80 characters.")

        existing_property = self._repository.get_property(property_id)
        if existing_property is not None and existing_property.name != clean_name:
            raise PropertyConflictError(
                "That property ID is already registered with another name."
            )
        property_ = existing_property or Property(id=property_id, name=clean_name)
        if existing_property is None:
            self._repository.save_property(property_)

        normalized_label = normalize_unit_label(clean_unit_label)
        existing_unit = self._repository.get_unit(property_id, unit_id)
        matching_label = self._repository.find_unit_by_label(
            property_id,
            normalized_label,
        )
        if existing_unit is not None and existing_unit.label != clean_unit_label:
            raise PropertyConflictError(
                "That unit ID is already registered with another label."
            )
        if matching_label is not None and matching_label.id != unit_id:
            raise PropertyConflictError(
                "That unit label is already registered with another ID."
            )
        unit = existing_unit or PropertyUnit(
            id=unit_id,
            property_id=property_id,
            label=clean_unit_label,
            normalized_label=normalized_label,
        )
        if existing_unit is None:
            self._repository.save_unit(unit)

        return ProvisionPropertyResult(
            property=property_,
            unit=unit,
            property_was_created=existing_property is None,
            unit_was_created=existing_unit is None,
        )


def normalize_unit_label(label: str) -> str:
    return " ".join(label.casefold().split())
