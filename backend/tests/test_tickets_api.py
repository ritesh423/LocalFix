import unittest
from uuid import UUID, uuid4

from fastapi.testclient import TestClient

from app.api.dependencies import DEMO_RESIDENT_CONTEXT
from app.domain.tickets import (
    AccessWindow,
    ResidentContext,
    ServiceCategory,
    UrgencySuggestion,
)
from app.main import create_app
from app.repositories.tickets import InMemoryTicketRepository
from app.services.tickets import CreateTicketCommand, TicketService


class TicketsApiTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repository = InMemoryTicketRepository()
        self.client = TestClient(create_app(self.repository))

    def test_health_reports_that_the_api_is_running(self) -> None:
        response = self.client.get("/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_resident_can_create_and_read_a_ticket(self) -> None:
        response = self.client.post("/tickets", json=self.ticket_payload())

        self.assertEqual(response.status_code, 201)
        created = response.json()
        self.assertEqual(created["status"], "open")
        self.assertEqual(created["version"], 1)
        self.assertIsNone(created["assigned_worker"])

        detail_response = self.client.get(f"/tickets/{created['id']}")

        self.assertEqual(detail_response.status_code, 200)
        self.assertEqual(detail_response.json(), created)

    def test_retrying_the_same_client_request_does_not_duplicate_ticket(self) -> None:
        payload = self.ticket_payload()

        first_response = self.client.post("/tickets", json=payload)
        retry_response = self.client.post("/tickets", json=payload)
        list_response = self.client.get("/tickets")

        self.assertEqual(first_response.status_code, 201)
        self.assertEqual(retry_response.status_code, 200)
        self.assertEqual(retry_response.json()["id"], first_response.json()["id"])
        self.assertEqual(len(list_response.json()), 1)

    def test_invalid_ticket_is_rejected_with_the_api_error_contract(self) -> None:
        payload = self.ticket_payload()
        payload["title"] = "     "

        response = self.client.post("/tickets", json=payload)

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["error"]["code"], "validation_error")

    def test_unknown_or_hidden_ticket_uses_the_same_not_found_response(self) -> None:
        other_resident = ResidentContext(
            user_id=UUID("10000000-0000-0000-0000-000000000999"),
            property_id=DEMO_RESIDENT_CONTEXT.property_id,
            unit_id=UUID("30000000-0000-0000-0000-000000000999"),
        )
        hidden_ticket = (
            TicketService(self.repository)
            .create_ticket(
                CreateTicketCommand(
                    client_request_id=uuid4(),
                    title="Private apartment issue",
                    description="This ticket belongs to a different resident.",
                    category=ServiceCategory.OTHER,
                    urgency_suggestion=UrgencySuggestion.ROUTINE,
                    access_window=AccessWindow.ANYTIME,
                ),
                other_resident,
            )
            .ticket
        )

        hidden_response = self.client.get(f"/tickets/{hidden_ticket.id}")
        unknown_response = self.client.get(f"/tickets/{uuid4()}")

        self.assertEqual(hidden_response.status_code, 404)
        self.assertEqual(unknown_response.status_code, 404)
        self.assertEqual(hidden_response.json(), unknown_response.json())
        self.assertEqual(self.client.get("/tickets").json(), [])

    @staticmethod
    def ticket_payload() -> dict[str, str]:
        return {
            "client_request_id": str(uuid4()),
            "title": "Leaking kitchen tap",
            "description": "The tap keeps dripping even when fully closed.",
            "category": "plumbing",
            "urgency_suggestion": "soon",
            "access_window": "morning",
        }


if __name__ == "__main__":
    unittest.main()
