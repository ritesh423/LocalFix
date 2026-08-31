import unittest
from uuid import uuid4

from app.database.models import Base
from app.database.session import create_database_engine, create_session_factory
from app.repositories.properties import InMemoryPropertyRepository
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.services.properties import (
    PropertyConflictError,
    PropertyProvisioningService,
)


class PropertyProvisioningServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repository = InMemoryPropertyRepository()
        self.service = PropertyProvisioningService(self.repository)
        self.property_id = uuid4()
        self.unit_id = uuid4()

    def test_property_and_first_unit_are_normalized_and_created_together(self) -> None:
        result = self.service.provision(
            property_id=self.property_id,
            property_name="  Lakeview   Residency ",
            unit_id=self.unit_id,
            unit_label=" Apartment   A-204 ",
        )

        self.assertTrue(result.property_was_created)
        self.assertTrue(result.unit_was_created)
        self.assertEqual(result.property.name, "Lakeview Residency")
        self.assertEqual(result.unit.label, "Apartment A-204")
        self.assertEqual(result.unit.normalized_label, "apartment a-204")

    def test_repeating_the_same_property_setup_is_safe(self) -> None:
        first = self.provision_pilot()
        second = self.provision_pilot()

        self.assertTrue(first.property_was_created)
        self.assertFalse(second.property_was_created)
        self.assertFalse(second.unit_was_created)

    def test_same_unit_label_cannot_receive_a_second_id(self) -> None:
        self.provision_pilot()

        with self.assertRaises(PropertyConflictError):
            self.service.provision(
                property_id=self.property_id,
                property_name="Lakeview Residency",
                unit_id=uuid4(),
                unit_label="apartment a-204",
            )

    def test_units_are_listed_in_readable_order(self) -> None:
        self.service.provision(
            property_id=self.property_id,
            property_name="Lakeview Residency",
            unit_id=uuid4(),
            unit_label="Apartment B-102",
        )
        self.service.provision(
            property_id=self.property_id,
            property_name="Lakeview Residency",
            unit_id=uuid4(),
            unit_label="Apartment A-204",
        )

        units = self.repository.list_units(self.property_id)

        self.assertEqual(
            [unit.label for unit in units],
            ["Apartment A-204", "Apartment B-102"],
        )

    def provision_pilot(self):
        return self.service.provision(
            property_id=self.property_id,
            property_name="Lakeview Residency",
            unit_id=self.unit_id,
            unit_label="Apartment A-204",
        )


class SqlAlchemyPropertyRepositoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_database_engine("sqlite://")
        Base.metadata.create_all(self.engine)
        self.repository = SqlAlchemyPropertyRepository(
            create_session_factory(self.engine)
        )

    def tearDown(self) -> None:
        self.engine.dispose()

    def test_property_and_unit_survive_new_database_sessions(self) -> None:
        property_id = uuid4()
        unit_id = uuid4()
        PropertyProvisioningService(self.repository).provision(
            property_id=property_id,
            property_name="Lakeview Residency",
            unit_id=unit_id,
            unit_label="Apartment A-204",
        )

        property_ = self.repository.get_property(property_id)
        unit = self.repository.get_unit(property_id, unit_id)
        units = self.repository.list_units(property_id)

        self.assertEqual(property_.name, "Lakeview Residency")
        self.assertEqual(unit.label, "Apartment A-204")
        self.assertEqual([stored.id for stored in units], [unit_id])


if __name__ == "__main__":
    unittest.main()
