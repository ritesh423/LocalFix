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

    def test_manager_queue_contains_every_resident_ticket_for_the_property(
        self,
    ) -> None:
        own_ticket = self.client.post("/tickets", json=self.ticket_payload()).json()
        other_resident = ResidentContext(
            user_id=UUID("10000000-0000-0000-0000-000000000999"),
            property_id=DEMO_RESIDENT_CONTEXT.property_id,
            unit_id=UUID("30000000-0000-0000-0000-000000000999"),
        )
        other_ticket = (
            TicketService(self.repository)
            .create_ticket(
                CreateTicketCommand(
                    client_request_id=uuid4(),
                    title="Lobby light is not working",
                    description="The light near the second floor lift does not turn on.",
                    category=ServiceCategory.ELECTRICAL,
                    urgency_suggestion=UrgencySuggestion.SOON,
                    access_window=AccessWindow.ANYTIME,
                ),
                other_resident,
            )
            .ticket
        )

        manager_queue = self.client.get("/manager/tickets").json()
        resident_queue = self.client.get("/tickets").json()

        self.assertEqual(
            {ticket["id"] for ticket in manager_queue},
            {own_ticket["id"], str(other_ticket.id)},
        )
        self.assertEqual(
            [ticket["id"] for ticket in resident_queue], [own_ticket["id"]]
        )

    def test_manager_can_set_priority_and_assign_an_open_ticket(self) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()

        response = self.client.post(
            f"/manager/tickets/{created['id']}/assignment",
            json=self.assignment_payload(expected_version=created["version"]),
        )

        self.assertEqual(response.status_code, 200)
        assigned = response.json()
        self.assertEqual(assigned["status"], "assigned")
        self.assertEqual(assigned["priority"], "urgent")
        self.assertEqual(assigned["version"], 2)
        self.assertEqual(
            assigned["assigned_worker_id"],
            "40000000-0000-0000-0000-000000000001",
        )
        self.assertEqual(assigned["assigned_worker"], "Arun Kumar")
        self.assertEqual(
            self.client.get(f"/tickets/{created['id']}").json(),
            assigned,
        )

    def test_manager_can_list_active_workers_for_the_property(self) -> None:
        response = self.client.get("/manager/workers")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            [worker["name"] for worker in response.json()],
            ["Arun Kumar", "Maya Singh", "Sameer Khan"],
        )

    def test_stale_manager_version_is_rejected_without_overwriting_assignment(
        self,
    ) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()
        assignment_url = f"/manager/tickets/{created['id']}/assignment"
        first = self.client.post(assignment_url, json=self.assignment_payload(1))

        stale = self.client.post(
            assignment_url,
            json={
                **self.assignment_payload(1),
                "worker_id": "40000000-0000-0000-0000-000000000002",
            },
        )

        self.assertEqual(first.status_code, 200)
        self.assertEqual(stale.status_code, 409)
        self.assertEqual(stale.json()["detail"]["code"], "ticket_version_conflict")
        stored = self.client.get(f"/tickets/{created['id']}").json()
        self.assertEqual(stored["assigned_worker"], "Arun Kumar")
        self.assertEqual(stored["version"], 2)

    def test_unknown_worker_cannot_be_assigned(self) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()

        response = self.client.post(
            f"/manager/tickets/{created['id']}/assignment",
            json={
                **self.assignment_payload(1),
                "worker_id": "40000000-0000-0000-0000-000000000999",
            },
        )

        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.json()["detail"]["code"], "worker_not_eligible")

    def test_assignment_is_rejected_when_ticket_is_no_longer_open(self) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()
        assignment_url = f"/manager/tickets/{created['id']}/assignment"
        self.client.post(assignment_url, json=self.assignment_payload(1))

        response = self.client.post(assignment_url, json=self.assignment_payload(2))

        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.json()["detail"]["code"], "transition_not_allowed")

    def test_manager_cannot_assign_a_ticket_from_another_property(self) -> None:
        other_property_resident = ResidentContext(
            user_id=UUID("10000000-0000-0000-0000-000000000998"),
            property_id=UUID("20000000-0000-0000-0000-000000000998"),
            unit_id=UUID("30000000-0000-0000-0000-000000000998"),
        )
        hidden = (
            TicketService(self.repository)
            .create_ticket(
                CreateTicketCommand(
                    client_request_id=uuid4(),
                    title="Issue in another property",
                    description="This ticket must not appear in the demo manager queue.",
                    category=ServiceCategory.OTHER,
                    urgency_suggestion=UrgencySuggestion.ROUTINE,
                    access_window=AccessWindow.ANYTIME,
                ),
                other_property_resident,
            )
            .ticket
        )

        response = self.client.post(
            f"/manager/tickets/{hidden.id}/assignment",
            json=self.assignment_payload(1),
        )

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["detail"]["code"], "ticket_not_found")

    def test_worker_queue_contains_only_jobs_assigned_to_that_worker(self) -> None:
        arun_ticket = self.client.post("/tickets", json=self.ticket_payload()).json()
        maya_ticket = self.client.post("/tickets", json=self.ticket_payload()).json()
        self.client.post(
            f"/manager/tickets/{arun_ticket['id']}/assignment",
            json=self.assignment_payload(1),
        )
        self.client.post(
            f"/manager/tickets/{maya_ticket['id']}/assignment",
            json={
                **self.assignment_payload(1),
                "worker_id": "40000000-0000-0000-0000-000000000002",
            },
        )

        response = self.client.get("/worker/tickets")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            [ticket["id"] for ticket in response.json()],
            [arun_ticket["id"]],
        )

    def test_assigned_worker_can_start_their_job(self) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()
        assigned = self.client.post(
            f"/manager/tickets/{created['id']}/assignment",
            json=self.assignment_payload(1),
        ).json()

        response = self.client.post(
            f"/worker/tickets/{created['id']}/start",
            json={"expected_version": assigned["version"]},
        )

        self.assertEqual(response.status_code, 200)
        started = response.json()
        self.assertEqual(started["status"], "in_progress")
        self.assertEqual(started["version"], 3)
        self.assertEqual(
            self.client.get(f"/tickets/{created['id']}").json(),
            started,
        )

    def test_worker_cannot_start_another_workers_job(self) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()
        self.client.post(
            f"/manager/tickets/{created['id']}/assignment",
            json={
                **self.assignment_payload(1),
                "worker_id": "40000000-0000-0000-0000-000000000002",
            },
        )

        response = self.client.post(
            f"/worker/tickets/{created['id']}/start",
            json={"expected_version": 2},
        )

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["detail"]["code"], "ticket_not_found")

    def test_worker_start_rejects_stale_versions_and_repeated_start(self) -> None:
        created = self.client.post("/tickets", json=self.ticket_payload()).json()
        self.client.post(
            f"/manager/tickets/{created['id']}/assignment",
            json=self.assignment_payload(1),
        )
        start_url = f"/worker/tickets/{created['id']}/start"

        stale = self.client.post(start_url, json={"expected_version": 1})
        started = self.client.post(start_url, json={"expected_version": 2})
        repeated = self.client.post(start_url, json={"expected_version": 3})

        self.assertEqual(stale.status_code, 409)
        self.assertEqual(stale.json()["detail"]["code"], "ticket_version_conflict")
        self.assertEqual(started.status_code, 200)
        self.assertEqual(repeated.status_code, 409)
        self.assertEqual(repeated.json()["detail"]["code"], "transition_not_allowed")

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

    @staticmethod
    def assignment_payload(expected_version: int) -> dict[str, str | int]:
        return {
            "expected_version": expected_version,
            "priority": "urgent",
            "worker_id": "40000000-0000-0000-0000-000000000001",
        }


if __name__ == "__main__":
    unittest.main()
