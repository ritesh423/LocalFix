import unittest
from unittest.mock import patch
from uuid import uuid4

from app.domain.ticket_workflow import UserRole
from app.gateways.identity import FirebaseIdentityDirectory
from app.repositories.memberships import InMemoryMembershipRepository
from app.services.memberships import (
    InvalidMembershipError,
    MembershipConflictError,
    MembershipProvisioningService,
)


class MembershipProvisioningServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repository = InMemoryMembershipRepository()
        self.service = MembershipProvisioningService(self.repository)
        self.property_id = uuid4()
        self.unit_id = uuid4()

    def test_resident_membership_requires_an_apartment_unit(self) -> None:
        with self.assertRaises(InvalidMembershipError):
            self.service.provision(
                firebase_uid="firebase-resident",
                property_id=self.property_id,
                role=UserRole.RESIDENT,
            )

    def test_non_resident_membership_cannot_claim_an_apartment_unit(self) -> None:
        with self.assertRaises(InvalidMembershipError):
            self.service.provision(
                firebase_uid="firebase-manager",
                property_id=self.property_id,
                role=UserRole.MANAGER,
                unit_id=self.unit_id,
            )

    def test_repeating_the_same_command_does_not_duplicate_access(self) -> None:
        first = self.service.provision(
            firebase_uid="firebase-resident",
            property_id=self.property_id,
            role=UserRole.RESIDENT,
            unit_id=self.unit_id,
        )
        second = self.service.provision(
            firebase_uid="firebase-resident",
            property_id=self.property_id,
            role=UserRole.RESIDENT,
            unit_id=self.unit_id,
        )

        self.assertTrue(first.was_created)
        self.assertFalse(second.was_created)
        self.assertEqual(second.membership.id, first.membership.id)
        self.assertEqual(
            len(self.repository.list_active("firebase-resident")),
            1,
        )

    def test_existing_membership_is_not_silently_moved_to_another_unit(self) -> None:
        self.service.provision(
            firebase_uid="firebase-resident",
            property_id=self.property_id,
            role=UserRole.RESIDENT,
            unit_id=self.unit_id,
        )

        with self.assertRaises(MembershipConflictError):
            self.service.provision(
                firebase_uid="firebase-resident",
                property_id=self.property_id,
                role=UserRole.RESIDENT,
                unit_id=uuid4(),
            )

    def test_one_firebase_identity_keeps_the_same_internal_user_id(self) -> None:
        resident = self.service.provision(
            firebase_uid="multi-role-user",
            property_id=self.property_id,
            role=UserRole.RESIDENT,
            unit_id=self.unit_id,
        ).membership
        manager = self.service.provision(
            firebase_uid="multi-role-user",
            property_id=self.property_id,
            role=UserRole.MANAGER,
        ).membership

        self.assertEqual(manager.user_id, resident.user_id)


class FirebaseIdentityDirectoryTest(unittest.TestCase):
    @patch("app.gateways.identity.auth.get_user_by_email")
    @patch("app.gateways.identity.firebase_admin.get_app")
    def test_existing_firebase_user_is_found_by_email(
        self,
        get_app,
        get_user_by_email,
    ) -> None:
        firebase_app = object()
        get_app.return_value = firebase_app
        user = type(
            "FirebaseUser",
            (),
            {
                "uid": "firebase-user-789",
                "email": "resident@example.com",
                "display_name": "Ritesh",
            },
        )()
        get_user_by_email.return_value = user
        directory = FirebaseIdentityDirectory(project_id="localfix-be333")

        identity = directory.find_by_email(" resident@example.com ")

        self.assertEqual(identity.firebase_uid, "firebase-user-789")
        get_user_by_email.assert_called_once_with(
            "resident@example.com",
            app=firebase_app,
        )


if __name__ == "__main__":
    unittest.main()
