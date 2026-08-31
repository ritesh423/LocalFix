import unittest
from uuid import UUID, uuid4

from fastapi.testclient import TestClient

from app.domain.auth import AuthenticatedIdentity, PropertyMembership
from app.domain.properties import Property, PropertyUnit
from app.domain.ticket_workflow import UserRole
from app.gateways.identity import InvalidIdentityTokenError
from app.main import create_app
from app.repositories.memberships import InMemoryMembershipRepository
from app.repositories.properties import InMemoryPropertyRepository
from app.repositories.resident_invites import InMemoryResidentInviteRepository
from app.repositories.tickets import InMemoryTicketRepository

PROPERTY_ID = UUID("20000000-0000-0000-0000-000000000001")
UNIT_ID = UUID("30000000-0000-0000-0000-000000000204")


class ManagerIdentityTokenVerifier:
    def verify(self, token: str) -> AuthenticatedIdentity:
        if token != "manager-token":
            raise InvalidIdentityTokenError("Token is invalid.")
        return AuthenticatedIdentity(
            firebase_uid="firebase-manager-1",
            email="manager@example.com",
            email_verified=True,
        )


class ManagerResidentInviteApiTest(unittest.TestCase):
    def setUp(self) -> None:
        self.memberships = InMemoryMembershipRepository()
        self.properties = InMemoryPropertyRepository()
        self.invites = InMemoryResidentInviteRepository()
        self.properties.save_property(Property(PROPERTY_ID, "Lakeview Residency"))
        self.properties.save_unit(
            PropertyUnit(
                UNIT_ID,
                PROPERTY_ID,
                "Apartment A-204",
                "apartment a-204",
            )
        )
        self.memberships.save(
            PropertyMembership(
                id=uuid4(),
                firebase_uid="firebase-manager-1",
                user_id=uuid4(),
                property_id=PROPERTY_ID,
                role=UserRole.MANAGER,
            )
        )
        self.client = TestClient(
            create_app(
                InMemoryTicketRepository(),
                membership_repository=self.memberships,
                property_repository=self.properties,
                resident_invite_repository=self.invites,
                identity_token_verifier=ManagerIdentityTokenVerifier(),
                authentication_required=True,
            )
        )

    def test_manager_can_list_only_their_property_units(self) -> None:
        response = self.client.get("/manager/units", headers=self.headers())

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            [{"id": str(UNIT_ID), "label": "Apartment A-204"}],
        )

    def test_manager_can_create_invite_for_their_unit(self) -> None:
        response = self.client.post(
            "/manager/resident-invites",
            headers=self.headers(),
            json={"unit_id": str(UNIT_ID), "valid_days": 7},
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["unit_label"], "Apartment A-204")
        self.assertTrue(response.json()["invite_code"].startswith("LF-"))

    def test_manager_cannot_create_invite_for_another_property_unit(self) -> None:
        other_property_id = uuid4()
        other_unit_id = uuid4()
        self.properties.save_property(Property(other_property_id, "Other Residency"))
        self.properties.save_unit(
            PropertyUnit(
                other_unit_id,
                other_property_id,
                "Apartment 1",
                "apartment 1",
            )
        )

        response = self.client.post(
            "/manager/resident-invites",
            headers=self.headers(),
            json={"unit_id": str(other_unit_id)},
        )

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["detail"]["code"], "invalid_unit")

    @staticmethod
    def headers() -> dict[str, str]:
        return {"Authorization": "Bearer manager-token"}


if __name__ == "__main__":
    unittest.main()
