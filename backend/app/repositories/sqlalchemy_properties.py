from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, sessionmaker

from app.database.models import PropertyRecord, PropertyUnitRecord
from app.domain.properties import Property, PropertyUnit


class SqlAlchemyPropertyRepository:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def save_property(self, property_: Property) -> Property:
        with self._session_factory.begin() as session:
            session.merge(
                PropertyRecord(
                    id=property_.id,
                    name=property_.name,
                    is_active=property_.is_active,
                )
            )
        return property_

    def save_unit(self, unit: PropertyUnit) -> PropertyUnit:
        with self._session_factory.begin() as session:
            session.merge(
                PropertyUnitRecord(
                    id=unit.id,
                    property_id=unit.property_id,
                    label=unit.label,
                    normalized_label=unit.normalized_label,
                    is_active=unit.is_active,
                )
            )
        return unit

    def get_property(self, property_id: UUID) -> Property | None:
        with self._session_factory() as session:
            record = session.get(PropertyRecord, property_id)
            return self._property_to_domain(record) if record is not None else None

    def get_unit(self, property_id: UUID, unit_id: UUID) -> PropertyUnit | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(PropertyUnitRecord).where(
                    PropertyUnitRecord.id == unit_id,
                    PropertyUnitRecord.property_id == property_id,
                )
            )
            return self._unit_to_domain(record) if record is not None else None

    def find_unit_by_label(
        self,
        property_id: UUID,
        normalized_label: str,
    ) -> PropertyUnit | None:
        with self._session_factory() as session:
            record = session.scalar(
                select(PropertyUnitRecord).where(
                    PropertyUnitRecord.property_id == property_id,
                    PropertyUnitRecord.normalized_label == normalized_label,
                )
            )
            return self._unit_to_domain(record) if record is not None else None

    @staticmethod
    def _property_to_domain(record: PropertyRecord) -> Property:
        return Property(id=record.id, name=record.name, is_active=record.is_active)

    @staticmethod
    def _unit_to_domain(record: PropertyUnitRecord) -> PropertyUnit:
        return PropertyUnit(
            id=record.id,
            property_id=record.property_id,
            label=record.label,
            normalized_label=record.normalized_label,
            is_active=record.is_active,
        )
