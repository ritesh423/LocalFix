import unittest
from datetime import UTC, datetime, timedelta
from uuid import UUID

from app.database.models import Base
from app.database.session import create_database_engine, create_session_factory
from app.domain.auth import AuthenticatedIdentity
from app.domain.properties import Property, PropertyUnit
from app.repositories.memberships import InMemoryMembershipRepository
from app.repositories.properties import InMemoryPropertyRepository
from app.repositories.resident_invites import InMemoryResidentInviteRepository
from app.repositories.sqlalchemy_resident_invites import (
    SqlAlchemyResidentInviteRepository,
)
from app.services.resident_invites import (
    ExpiredResidentInviteError,
    ResidentInviteAlreadyUsedError,
    ResidentInviteService,
    VerifiedEmailRequiredError,
)

PROPERTY_ID = UUID("20000000-0000-0000-0000-000000000001")
UNIT_ID = UUID("30000000-0000-0000-0000-000000000204")
NOW = datetime(2026, 8, 30, 8, 0, tzinfo=UTC)


class ResidentInviteServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.memberships = InMemoryMembershipRepository()
        self.properties = InMemoryPropertyRepository()
        self.properties.save_property(Property(PROPERTY_ID, "Lakeview Residency"))
        self.properties.save_unit(
            PropertyUnit(
                UNIT_ID,
                PROPERTY_ID,
                "Apartment A-204",
                "apartment a-204",
            )
        )
        self.invites = InMemoryResidentInviteRepository()
        self.service = ResidentInviteService(
            self.invites,
            self.memberships,
            self.properties,
        )

    def test_verified_resident_can_redeem_invite_once(self) -> None:
        created = self.service.create(PROPERTY_ID, UNIT_ID, now=NOW)

        membership = self.service.redeem(
            created.code.lower().replace("-", " "),
            self.identity("firebase-resident-1"),
            now=NOW,
        )

        self.assertEqual(membership.property_id, PROPERTY_ID)
        self.assertEqual(membership.unit_id, UNIT_ID)
        self.assertNotEqual(created.code, created.invite.code_digest)
        repeated = self.service.redeem(
            created.code,
            self.identity("firebase-resident-1"),
            now=NOW,
        )
        self.assertEqual(repeated.id, membership.id)

    def test_another_account_cannot_reuse_claimed_invite(self) -> None:
        created = self.service.create(PROPERTY_ID, UNIT_ID, now=NOW)
        self.service.redeem(
            created.code,
            self.identity("firebase-resident-1"),
            now=NOW,
        )

        with self.assertRaises(ResidentInviteAlreadyUsedError):
            self.service.redeem(
                created.code,
                self.identity("firebase-resident-2"),
                now=NOW,
            )

    def test_expired_invite_is_rejected(self) -> None:
        created = self.service.create(
            PROPERTY_ID,
            UNIT_ID,
            valid_for=timedelta(hours=1),
            now=NOW,
        )

        with self.assertRaises(ExpiredResidentInviteError):
            self.service.redeem(
                created.code,
                self.identity("firebase-resident-1"),
                now=NOW + timedelta(hours=2),
            )

    def test_email_must_be_verified_before_invite_is_claimed(self) -> None:
        created = self.service.create(PROPERTY_ID, UNIT_ID, now=NOW)
        unverified = AuthenticatedIdentity(
            firebase_uid="firebase-resident-1",
            email="resident@example.com",
        )

        with self.assertRaises(VerifiedEmailRequiredError):
            self.service.redeem(created.code, unverified, now=NOW)

        self.assertIsNone(
            self.invites.find_by_digest(created.invite.code_digest)
            .claimed_by_firebase_uid
        )

    @staticmethod
    def identity(firebase_uid: str) -> AuthenticatedIdentity:
        return AuthenticatedIdentity(
            firebase_uid=firebase_uid,
            email=f"{firebase_uid}@example.com",
            email_verified=True,
        )


class SqlAlchemyResidentInviteRepositoryTest(unittest.TestCase):
    def test_invite_and_claim_survive_separate_database_sessions(self) -> None:
        engine = create_database_engine("sqlite+pysqlite:///:memory:")
        Base.metadata.create_all(engine)
        session_factory = create_session_factory(engine)
        properties = InMemoryPropertyRepository()
        properties.save_property(Property(PROPERTY_ID, "Lakeview Residency"))
        properties.save_unit(
            PropertyUnit(
                UNIT_ID,
                PROPERTY_ID,
                "Apartment A-204",
                "apartment a-204",
            )
        )
        repository = SqlAlchemyResidentInviteRepository(session_factory)
        service = ResidentInviteService(
            repository,
            InMemoryMembershipRepository(),
            properties,
        )

        created = service.create(PROPERTY_ID, UNIT_ID, now=NOW)
        claimed = repository.claim(
            created.invite.id,
            "firebase-resident-1",
            NOW,
        )

        loaded = repository.find_by_digest(created.invite.code_digest)
        self.assertIsNotNone(claimed)
        self.assertEqual(loaded.claimed_by_firebase_uid, "firebase-resident-1")


if __name__ == "__main__":
    unittest.main()
