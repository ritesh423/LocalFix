import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from tempfile import TemporaryDirectory
from uuid import uuid4

from app.database.models import Base
from app.database.session import create_database_engine, create_session_factory
from app.domain.auth import AuthenticatedIdentity
from app.domain.properties import Property
from app.domain.ticket_workflow import UserRole
from app.domain.tickets import ServiceCategory
from app.repositories.memberships import InMemoryMembershipRepository
from app.repositories.properties import InMemoryPropertyRepository
from app.repositories.sqlalchemy_memberships import SqlAlchemyMembershipRepository
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.repositories.sqlalchemy_staff_invites import SqlAlchemyStaffInviteRepository
from app.repositories.sqlalchemy_workers import SqlAlchemyWorkerRepository
from app.repositories.staff_invites import InMemoryStaffInviteRepository
from app.repositories.workers import InMemoryWorkerRepository
from app.services.staff_invites import (
    ExpiredStaffInviteError,
    StaffInviteAlreadyUsedError,
    StaffInviteService,
)


class StaffInviteServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.property_id = uuid4()
        self.properties = InMemoryPropertyRepository()
        self.properties.save_property(Property(self.property_id, "Lakeview Residency"))
        self.memberships = InMemoryMembershipRepository()
        self.invites = InMemoryStaffInviteRepository()
        self.workers = InMemoryWorkerRepository()
        self.service = StaffInviteService(
            self.invites,
            self.memberships,
            self.properties,
            self.workers,
        )
        self.now = datetime(2026, 9, 1, 10, tzinfo=UTC)

    def test_manager_creates_a_durable_worker_before_sharing_the_invite(self) -> None:
        created = self.service.create_worker_invite(
            self.property_id,
            "  Dev   Mehta ",
            ServiceCategory.ELECTRICAL,
            now=self.now,
        )

        self.assertEqual(created.worker.name, "Dev Mehta")
        self.assertTrue(created.code.startswith("LFW-"))
        self.assertEqual(
            self.workers.get(self.property_id, created.worker.id),
            created.worker,
        )

    def test_verified_worker_claims_the_account_link_once(self) -> None:
        created = self.service.create_worker_invite(
            self.property_id,
            "Dev Mehta",
            ServiceCategory.ELECTRICAL,
            now=self.now,
        )
        identity = AuthenticatedIdentity(
            firebase_uid="firebase-worker-1",
            email="dev@example.com",
            email_verified=True,
        )

        membership = self.service.redeem(created.code, identity, now=self.now)

        self.assertEqual(membership.role, UserRole.WORKER)
        self.assertEqual(membership.user_id, created.worker.id)
        self.assertEqual(membership.property_id, self.property_id)
        self.assertEqual(
            self.service.redeem(created.code, identity, now=self.now),
            membership,
        )
        with self.assertRaises(StaffInviteAlreadyUsedError):
            self.service.redeem(
                created.code,
                AuthenticatedIdentity(
                    firebase_uid="firebase-worker-2",
                    email_verified=True,
                ),
                now=self.now,
            )

    def test_expired_worker_invite_cannot_create_a_membership(self) -> None:
        created = self.service.create_worker_invite(
            self.property_id,
            "Dev Mehta",
            ServiceCategory.ELECTRICAL,
            valid_for=timedelta(hours=1),
            now=self.now,
        )

        with self.assertRaises(ExpiredStaffInviteError):
            self.service.redeem(
                created.code,
                AuthenticatedIdentity(
                    firebase_uid="firebase-worker-1",
                    email_verified=True,
                ),
                now=self.now + timedelta(hours=2),
            )

    def test_operator_manager_invite_creates_a_manager_membership(self) -> None:
        _invite, code = self.service.create_manager_invite(
            self.property_id,
            now=self.now,
        )

        membership = self.service.redeem(
            code,
            AuthenticatedIdentity(
                firebase_uid="firebase-manager-2",
                email_verified=True,
            ),
            now=self.now,
        )

        self.assertEqual(membership.role, UserRole.MANAGER)
        self.assertIsNone(membership.unit_id)


class SqlAlchemyStaffInviteRepositoryTest(unittest.TestCase):
    def test_worker_and_invite_survive_repository_recreation(self) -> None:
        with TemporaryDirectory() as directory:
            database_path = Path(directory) / "staff-invites.db"
            engine = create_database_engine(f"sqlite:///{database_path}")
            Base.metadata.create_all(engine)
            session_factory = create_session_factory(engine)
            property_id = uuid4()
            properties = SqlAlchemyPropertyRepository(session_factory)
            properties.save_property(Property(property_id, "Lakeview Residency"))

            created = StaffInviteService(
                SqlAlchemyStaffInviteRepository(session_factory),
                SqlAlchemyMembershipRepository(session_factory),
                properties,
                SqlAlchemyWorkerRepository(session_factory),
            ).create_worker_invite(
                property_id,
                "Dev Mehta",
                ServiceCategory.ELECTRICAL,
            )

            restarted_service = StaffInviteService(
                SqlAlchemyStaffInviteRepository(session_factory),
                SqlAlchemyMembershipRepository(session_factory),
                SqlAlchemyPropertyRepository(session_factory),
                SqlAlchemyWorkerRepository(session_factory),
            )
            membership = restarted_service.redeem(
                created.code,
                AuthenticatedIdentity(
                    firebase_uid="firebase-worker-1",
                    email_verified=True,
                ),
            )

            self.assertEqual(membership.user_id, created.worker.id)
            self.assertEqual(
                SqlAlchemyWorkerRepository(session_factory).get(
                    property_id,
                    created.worker.id,
                ),
                created.worker,
            )
            engine.dispose()


if __name__ == "__main__":
    unittest.main()
