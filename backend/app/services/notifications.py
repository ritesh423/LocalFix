from uuid import UUID, uuid4

from app.domain.notifications import (
    NotificationJob,
    NotificationKind,
    NotificationStatus,
)
from app.domain.ticket_workflow import TicketAction, UserRole
from app.domain.tickets import Ticket, TicketEvent


class TicketNotificationPlanner:
    def plan(self, ticket: Ticket, event: TicketEvent) -> tuple[NotificationJob, ...]:
        notification = self._notification_for(ticket, event)
        return (notification,) if notification is not None else ()

    def _notification_for(
        self,
        ticket: Ticket,
        event: TicketEvent,
    ) -> NotificationJob | None:
        action = event.action
        if action is TicketAction.CREATE:
            return self._job(
                ticket,
                event,
                recipient_role=UserRole.MANAGER,
                recipient_user_id=None,
                kind=NotificationKind.NEW_REQUEST,
                title="New maintenance request",
                body=ticket.title,
            )
        if action is TicketAction.ASSIGN and ticket.assigned_worker_id is not None:
            return self._job(
                ticket,
                event,
                recipient_role=UserRole.WORKER,
                recipient_user_id=ticket.assigned_worker_id,
                kind=NotificationKind.JOB_ASSIGNED,
                title="New job assigned",
                body=ticket.title,
            )
        if action is TicketAction.START:
            return self._job(
                ticket,
                event,
                recipient_role=UserRole.RESIDENT,
                recipient_user_id=ticket.resident_id,
                kind=NotificationKind.WORK_STARTED,
                title="Repair started",
                body=f"Work has started on {ticket.title}.",
            )
        if action is TicketAction.SUBMIT_PROOF:
            return self._job(
                ticket,
                event,
                recipient_role=UserRole.RESIDENT,
                recipient_user_id=ticket.resident_id,
                kind=NotificationKind.READY_FOR_REVIEW,
                title="Repair ready for review",
                body=f"Check the completed work for {ticket.title}.",
            )
        if action is TicketAction.CONFIRM and ticket.assigned_worker_id is not None:
            return self._job(
                ticket,
                event,
                recipient_role=UserRole.WORKER,
                recipient_user_id=ticket.assigned_worker_id,
                kind=NotificationKind.REPAIR_CONFIRMED,
                title="Repair confirmed",
                body=f"The resident confirmed {ticket.title}.",
            )
        if (
            action is TicketAction.REQUEST_REWORK
            and ticket.assigned_worker_id is not None
        ):
            return self._job(
                ticket,
                event,
                recipient_role=UserRole.WORKER,
                recipient_user_id=ticket.assigned_worker_id,
                kind=NotificationKind.REWORK_REQUESTED,
                title="More work requested",
                body=ticket.resident_feedback or ticket.title,
            )
        return None

    @staticmethod
    def _job(
        ticket: Ticket,
        event: TicketEvent,
        recipient_role: UserRole,
        recipient_user_id: UUID | None,
        kind: NotificationKind,
        title: str,
        body: str,
    ) -> NotificationJob:
        recipient = str(recipient_user_id) if recipient_user_id is not None else "all"
        now = event.created_at
        return NotificationJob(
            id=uuid4(),
            deduplication_key=(
                f"{ticket.id}:{event.ticket_version}:{kind.value}:"
                f"{recipient_role.value}:{recipient}"
            ),
            ticket_id=ticket.id,
            property_id=ticket.property_id,
            recipient_role=recipient_role,
            recipient_user_id=recipient_user_id,
            kind=kind,
            title=title,
            body=body,
            data={
                "ticket_id": str(ticket.id),
                "ticket_version": str(event.ticket_version),
                "notification_kind": kind.value,
                "workspace": recipient_role.value,
            },
            status=NotificationStatus.PENDING,
            attempt_count=0,
            available_at=now,
            last_error=None,
            sent_at=None,
            created_at=now,
            updated_at=now,
        )
