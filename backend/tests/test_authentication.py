import unittest
from unittest.mock import patch
from uuid import UUID, uuid4

from fastapi.testclient import TestClient

from app.database.models import Base
from app.database.session import create_database_engine, create_session_factory
from app.domain.auth import AuthenticatedIdentity, PropertyMembership
from app.domain.properties import Property, PropertyUnit
from app.domain.ticket_workflow import UserRole
from app.gateways.identity import (
    FirebaseIdentityTokenVerifier,
    InvalidIdentityTokenError,
)
from app.main import create_app
from app.repositories.memberships import InMemoryMembershipRepository
from app.repositories.properties import InMemoryPropertyRepository
from app.repositories.resident_invites import InMemoryResidentInviteRepository
from app.repositories.sqlalchemy_memberships import (
    SqlAlchemyMembershipRepository,
)
from app.repositories.tickets import InMemoryTicketRepository
from app.services.resident_invites import ResidentInviteService


class FakeIdentityTokenVerifier:
    def verify(self, token: str) -> AuthenticatedIdentity:
        if token != "valid-firebase-token":
            raise InvalidIdentityTokenError("Token is invalid.")
        return AuthenticatedIdentity(
            firebase_uid="firebase-resident-123",
            email="resident@example.com",
            display_name="Ritesh",
            email_verified=True,
        )


class AuthenticationApiTest(unittest.TestCase):
    def setUp(self) -> None:
        self.ticket_repository = InMemoryTicketRepository()
        self.memberships = InMemoryMembershipRepository()
        self.properties = InMemoryPropertyRepository()
        self.resident_membership = PropertyMembership(
            id=uuid4(),
            firebase_uid="firebase-resident-123",
            user_id=UUID("10000000-0000-0000-0000-000000000101"),
            property_id=UUID("20000000-0000-0000-0000-000000000001"),
            unit_id=UUID("30000000-0000-0000-0000-000000000204"),
            role=UserRole.RESIDENT,
        )
        self.memberships.save(self.resident_membership)
        self.properties.save_property(
            Property(
                id=self.resident_membership.property_id,
                name="Lakeview Residency",
            )
        )
        self.properties.save_unit(
            PropertyUnit(
                id=self.resident_membership.unit_id,
                property_id=self.resident_membership.property_id,
                label="Apartment A-204",
                normalized_label="apartment a-204",
            )
        )
        self.invites = InMemoryResidentInviteRepository()
        self.client = TestClient(
            create_app(
                self.ticket_repository,
                membership_repository=self.memberships,
                property_repository=self.properties,
                resident_invite_repository=self.invites,
                identity_token_verifier=FakeIdentityTokenVerifier(),
                authentication_required=True,
            )
        )

    def test_auth_session_requires_a_bearer_token(self) -> None:
        response = self.client.get("/auth/session")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.headers["www-authenticate"], "Bearer")
        self.assertEqual(
            response.json()["detail"]["code"],
            "authentication_required",
        )

    def test_invalid_firebase_token_is_rejected(self) -> None:
        response = self.client.get(
            "/auth/session",
            headers={"Authorization": "Bearer expired-token"},
        )

        self.assertEqual(response.status_code, 401)
        self.assertIn("expired", response.json()["detail"]["message"])

    def test_session_returns_only_server_stored_memberships(self) -> None:
        response = self.client.get(
            "/auth/session",
            headers=self.authorization_header(),
        )

        self.assertEqual(response.status_code, 200)
        session = response.json()
        self.assertEqual(session["user"]["firebase_uid"], "firebase-resident-123")
        self.assertEqual(session["memberships"][0]["role"], "resident")
        self.assertEqual(
            session["memberships"][0]["property_id"],
            str(self.resident_membership.property_id),
        )
        self.assertEqual(
            session["memberships"][0]["property_name"],
            "Lakeview Residency",
        )
        self.assertEqual(
            session["memberships"][0]["unit_label"],
            "Apartment A-204",
        )
        self.assertNotIn("manager", {item["role"] for item in session["memberships"]})

    def test_verified_firebase_user_can_join_with_resident_invite(self) -> None:
        second_memberships = InMemoryMembershipRepository()
        client = TestClient(
            create_app(
                InMemoryTicketRepository(),
                membership_repository=second_memberships,
                property_repository=self.properties,
                resident_invite_repository=self.invites,
                identity_token_verifier=FakeIdentityTokenVerifier(),
                authentication_required=True,
            )
        )
        invite = ResidentInviteService(
            self.invites,
            second_memberships,
            self.properties,
        ).create(
            self.resident_membership.property_id,
            self.resident_membership.unit_id,
        )

        response = client.post(
            "/auth/resident-invites/redeem",
            headers=self.authorization_header(),
            json={"invite_code": invite.code},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["membership"]["role"], "resident")
        self.assertEqual(
            response.json()["membership"]["unit_label"],
            "Apartment A-204",
        )

    def test_authenticated_resident_identity_owns_the_created_ticket(self) -> None:
        response = self.client.post(
            "/tickets",
            headers=self.authorization_header(),
            json={
                "client_request_id": str(uuid4()),
                "title": "Bedroom fan is making noise",
                "description": "The fan rattles loudly whenever it runs at full speed.",
                "category": "electrical",
                "urgency_suggestion": "soon",
                "access_window": "evening",
            },
        )

        self.assertEqual(response.status_code, 201)
        ticket = self.ticket_repository.list_for_resident(
            self.resident_membership.property_id,
            self.resident_membership.user_id,
        )[0]
        self.assertEqual(ticket.resident_id, self.resident_membership.user_id)
        self.assertEqual(ticket.unit_id, self.resident_membership.unit_id)

    def test_resident_token_cannot_open_manager_routes(self) -> None:
        response = self.client.get(
            "/manager/tickets",
            headers=self.authorization_header(),
        )

        self.assertEqual(response.status_code, 403)
        self.assertEqual(response.json()["detail"]["code"], "membership_required")

    @staticmethod
    def authorization_header() -> dict[str, str]:
        return {"Authorization": "Bearer valid-firebase-token"}


class SqlAlchemyMembershipRepositoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_database_engine("sqlite://")
        Base.metadata.create_all(self.engine)
        self.repository = SqlAlchemyMembershipRepository(
            create_session_factory(self.engine)
        )

    def tearDown(self) -> None:
        self.engine.dispose()

    def test_membership_survives_a_new_database_session(self) -> None:
        membership = PropertyMembership(
            id=uuid4(),
            firebase_uid="durable-firebase-user",
            user_id=uuid4(),
            property_id=uuid4(),
            unit_id=uuid4(),
            role=UserRole.RESIDENT,
        )

        self.repository.save(membership)

        restored = self.repository.find_active(
            membership.firebase_uid,
            UserRole.RESIDENT,
        )
        self.assertEqual(restored, membership)


class FirebaseIdentityTokenVerifierTest(unittest.TestCase):
    @patch("app.gateways.identity.auth.verify_id_token")
    @patch("app.gateways.identity.firebase_admin.get_app")
    def test_firebase_claims_become_a_small_trusted_identity(
        self,
        get_app,
        verify_id_token,
    ) -> None:
        firebase_app = object()
        get_app.return_value = firebase_app
        verify_id_token.return_value = {
            "uid": "firebase-user-456",
            "email": "manager@example.com",
            "name": "Building Manager",
        }
        verifier = FirebaseIdentityTokenVerifier(project_id="localfix-be333")

        identity = verifier.verify("signed-firebase-token")

        self.assertEqual(identity.firebase_uid, "firebase-user-456")
        self.assertEqual(identity.email, "manager@example.com")
        self.assertIs(verify_id_token.call_args.kwargs["app"], firebase_app)


if __name__ == "__main__":
    unittest.main()
