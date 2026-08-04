import unittest

from app.domain.ticket_workflow import (
    PermissionDenied,
    TicketAction,
    TicketStatus,
    TransitionNotAllowed,
    UserRole,
    available_actions,
    transition,
)


class TicketWorkflowTest(unittest.TestCase):
    def test_primary_apartment_repair_journey(self) -> None:
        status = TicketStatus.OPEN

        status = transition(status, TicketAction.ASSIGN, UserRole.MANAGER)
        self.assertEqual(status, TicketStatus.ASSIGNED)

        status = transition(status, TicketAction.START, UserRole.WORKER)
        self.assertEqual(status, TicketStatus.IN_PROGRESS)

        status = transition(status, TicketAction.SUBMIT_PROOF, UserRole.WORKER)
        self.assertEqual(status, TicketStatus.AWAITING_CONFIRMATION)

        status = transition(status, TicketAction.CONFIRM, UserRole.RESIDENT)
        self.assertEqual(status, TicketStatus.COMPLETED)

    def test_worker_cannot_assign_a_ticket(self) -> None:
        with self.assertRaises(PermissionDenied):
            transition(
                TicketStatus.OPEN,
                TicketAction.ASSIGN,
                UserRole.WORKER,
            )

    def test_worker_cannot_skip_starting_the_job(self) -> None:
        with self.assertRaises(TransitionNotAllowed):
            transition(
                TicketStatus.ASSIGNED,
                TicketAction.SUBMIT_PROOF,
                UserRole.WORKER,
            )

    def test_worker_can_report_a_blocker_after_starting(self) -> None:
        result = transition(
            TicketStatus.IN_PROGRESS,
            TicketAction.REPORT_BLOCKER,
            UserRole.WORKER,
        )

        self.assertEqual(result, TicketStatus.BLOCKED)

    def test_resident_can_request_rework(self) -> None:
        result = transition(
            TicketStatus.AWAITING_CONFIRMATION,
            TicketAction.REQUEST_REWORK,
            UserRole.RESIDENT,
        )

        self.assertEqual(result, TicketStatus.ASSIGNED)

    def test_completed_ticket_is_terminal(self) -> None:
        with self.assertRaises(TransitionNotAllowed):
            transition(
                TicketStatus.COMPLETED,
                TicketAction.START,
                UserRole.WORKER,
            )

    def test_available_actions_are_filtered_by_role(self) -> None:
        resident_actions = available_actions(
            TicketStatus.AWAITING_CONFIRMATION,
            UserRole.RESIDENT,
        )
        worker_actions = available_actions(
            TicketStatus.AWAITING_CONFIRMATION,
            UserRole.WORKER,
        )

        self.assertEqual(
            resident_actions,
            (TicketAction.CONFIRM, TicketAction.REQUEST_REWORK),
        )
        self.assertEqual(worker_actions, ())


if __name__ == "__main__":
    unittest.main()
