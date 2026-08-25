import unittest
from datetime import timedelta
from unittest.mock import patch
from uuid import uuid4

from app.api.dependencies import DEMO_MANAGER_CONTEXT, DEMO_RESIDENT_CONTEXT
from app.domain.device_registrations import DevicePlatform
from app.domain.notifications import NotificationStatus
from app.domain.ticket_workflow import UserRole
from app.domain.tickets import (
    AccessWindow,
    ServiceCategory,
    UrgencySuggestion,
)
from app.gateways.push import FirebasePushGateway, PushDeliveryError
from app.repositories.device_registrations import (
    InMemoryDeviceRegistrationRepository,
)
from app.repositories.notification_outbox import (
    InMemoryNotificationOutboxRepository,
)
from app.repositories.tickets import InMemoryTicketRepository
from app.services.device_registrations import DeviceRegistrationService
from app.services.notification_delivery import NotificationDeliveryService
from app.services.tickets import CreateTicketCommand, TicketService


class FakePushGateway:
    def __init__(self, error: Exception | None = None) -> None:
        self.error = error
        self.messages: list[dict[str, object]] = []

    def send(
        self,
        firebase_installation_id: str,
        title: str,
        body: str,
        data: dict[str, str],
    ) -> str:
        if self.error is not None:
            raise self.error
        self.messages.append(
            {
                "fid": firebase_installation_id,
                "title": title,
                "body": body,
                "data": data,
            }
        )
        return "projects/localfix/messages/test-message"


class NotificationDeliveryServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.ticket_repository = InMemoryTicketRepository()
        self.ticket = TicketService(self.ticket_repository).create_ticket(
            CreateTicketCommand(
                client_request_id=uuid4(),
                title="Leaking kitchen tap",
                description="The tap keeps dripping even when fully closed.",
                category=ServiceCategory.PLUMBING,
                urgency_suggestion=UrgencySuggestion.SOON,
                access_window=AccessWindow.MORNING,
            ),
            DEMO_RESIDENT_CONTEXT,
        ).ticket
        self.outbox = InMemoryNotificationOutboxRepository(
            self.ticket_repository.notification_jobs
        )
        self.device_repository = InMemoryDeviceRegistrationRepository()

    def test_ready_notification_is_sent_to_the_matching_firebase_installation(
        self,
    ) -> None:
        self.register_manager_device("manager-firebase-installation")
        self.register_resident_device("resident-firebase-installation")
        gateway = FakePushGateway()
        service = NotificationDeliveryService(
            self.outbox,
            self.device_repository,
            gateway,
        )

        summary = service.deliver_ready(
            now=self.ticket_repository.notification_jobs[0].available_at
        )

        delivered = self.ticket_repository.notification_jobs[0]
        self.assertEqual(summary.sent, 1)
        self.assertEqual(summary.rescheduled, 0)
        self.assertEqual(delivered.status, NotificationStatus.SENT)
        self.assertEqual(delivered.attempt_count, 1)
        self.assertEqual(len(gateway.messages), 1)
        self.assertEqual(
            gateway.messages[0]["fid"],
            "manager-firebase-installation",
        )
        self.assertEqual(
            gateway.messages[0]["data"]["ticket_id"],
            str(self.ticket.id),
        )

    def test_missing_device_is_retried_with_exponential_backoff(self) -> None:
        job = self.ticket_repository.notification_jobs[0]
        service = NotificationDeliveryService(
            self.outbox,
            self.device_repository,
            FakePushGateway(),
        )

        summary = service.deliver_ready(now=job.available_at)

        rescheduled = self.ticket_repository.notification_jobs[0]
        self.assertEqual(summary.rescheduled, 1)
        self.assertEqual(rescheduled.status, NotificationStatus.PENDING)
        self.assertEqual(rescheduled.attempt_count, 1)
        self.assertEqual(
            rescheduled.available_at,
            job.available_at + timedelta(seconds=30),
        )
        self.assertIn("No registered device", rescheduled.last_error)

    def test_notification_stops_retrying_after_five_failures(self) -> None:
        self.register_manager_device("manager-firebase-installation")
        service = NotificationDeliveryService(
            self.outbox,
            self.device_repository,
            FakePushGateway(
                PushDeliveryError("Firebase is temporarily unavailable.")
            ),
        )

        for _ in range(5):
            current = self.ticket_repository.notification_jobs[0]
            service.deliver_ready(now=current.available_at)

        failed = self.ticket_repository.notification_jobs[0]
        self.assertEqual(failed.status, NotificationStatus.FAILED)
        self.assertEqual(failed.attempt_count, 5)
        self.assertEqual(failed.last_error, "Firebase is temporarily unavailable.")

    def register_manager_device(self, firebase_installation_id: str) -> None:
        DeviceRegistrationService(self.device_repository).register(
            installation_id=uuid4(),
            firebase_installation_id=firebase_installation_id,
            platform=DevicePlatform.ANDROID,
            role=UserRole.MANAGER,
            user_id=DEMO_MANAGER_CONTEXT.user_id,
            property_id=DEMO_MANAGER_CONTEXT.property_id,
        )

    def register_resident_device(self, firebase_installation_id: str) -> None:
        DeviceRegistrationService(self.device_repository).register(
            installation_id=uuid4(),
            firebase_installation_id=firebase_installation_id,
            platform=DevicePlatform.ANDROID,
            role=UserRole.RESIDENT,
            user_id=DEMO_RESIDENT_CONTEXT.user_id,
            property_id=DEMO_RESIDENT_CONTEXT.property_id,
        )


class FirebasePushGatewayTest(unittest.TestCase):
    @patch("app.gateways.push.messaging.send")
    @patch("app.gateways.push.firebase_admin.get_app")
    def test_gateway_targets_the_firebase_installation_id(
        self,
        get_app,
        send,
    ) -> None:
        firebase_app = object()
        get_app.return_value = firebase_app
        send.return_value = "projects/localfix/messages/test-message"
        gateway = FirebasePushGateway(project_id="localfix-be333")

        response = gateway.send(
            firebase_installation_id="registered-firebase-installation",
            title="New job assigned",
            body="Leaking kitchen tap",
            data={"ticket_id": str(self.ticket_id())},
        )

        message = send.call_args.args[0]
        self.assertEqual(response, "projects/localfix/messages/test-message")
        self.assertEqual(message.fid, "registered-firebase-installation")
        self.assertEqual(message.notification.title, "New job assigned")
        self.assertEqual(message.android.notification.channel_id, "localfix_updates")
        self.assertIs(send.call_args.kwargs["app"], firebase_app)

    @staticmethod
    def ticket_id():
        return uuid4()


if __name__ == "__main__":
    unittest.main()
