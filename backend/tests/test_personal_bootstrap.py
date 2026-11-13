import unittest
from uuid import uuid4

from sqlalchemy import func, select

from app.cli.bootstrap_personal import bootstrap_personal
from app.database.models import (
    Base,
    PropertyMembershipRecord,
    PropertyRecord,
    PropertyUnitRecord,
)
from app.database.session import create_database_engine, create_session_factory


class PersonalBootstrapTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_database_engine("sqlite:///:memory:")
        Base.metadata.create_all(self.engine)
        self.session_factory = create_session_factory(self.engine)
        self.property_id = uuid4()

    def tearDown(self) -> None:
        self.engine.dispose()

    def test_restarting_bootstrap_does_not_duplicate_property_or_manager(self) -> None:
        for _ in range(2):
            bootstrap_personal(
                self.session_factory,
                property_id=self.property_id,
                property_name="Lakeview Residency",
                unit_labels=["A-101", "A-102"],
                manager_uid="firebase-manager",
            )

        with self.session_factory() as session:
            self.assertEqual(session.scalar(select(func.count(PropertyRecord.id))), 1)
            self.assertEqual(session.scalar(select(func.count(PropertyUnitRecord.id))), 2)
            self.assertEqual(
                session.scalar(select(func.count(PropertyMembershipRecord.id))), 1
            )

    def test_new_units_can_be_added_by_updating_bootstrap_labels(self) -> None:
        for labels in (["A-101"], ["A-101", "A-102"]):
            bootstrap_personal(
                self.session_factory,
                property_id=self.property_id,
                property_name="Lakeview Residency",
                unit_labels=labels,
                manager_uid="firebase-manager",
            )

        with self.session_factory() as session:
            self.assertEqual(session.scalar(select(func.count(PropertyUnitRecord.id))), 2)


if __name__ == "__main__":
    unittest.main()
