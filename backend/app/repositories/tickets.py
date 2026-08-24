from collections.abc import Iterable
from threading import Lock
from typing import Protocol
from uuid import UUID

from app.domain.notifications import NotificationJob
from app.domain.tickets import Ticket, TicketEvent


class TicketRepository(Protocol):
    def create(
        self,
        ticket: Ticket,
        event: TicketEvent,
        notification_jobs: tuple[NotificationJob, ...] = (),
    ) -> tuple[Ticket, bool]: ...

    def list_for_resident(
        self,
        property_id: UUID,
        resident_id: UUID,
    ) -> list[Ticket]: ...

    def get_for_resident(
        self,
        ticket_id: UUID,
        property_id: UUID,
        resident_id: UUID,
    ) -> Ticket | None: ...

    def list_for_property(self, property_id: UUID) -> list[Ticket]: ...

    def get_for_property(
        self,
        ticket_id: UUID,
        property_id: UUID,
    ) -> Ticket | None: ...

    def list_for_worker(
        self,
        property_id: UUID,
        worker_id: UUID,
    ) -> list[Ticket]: ...

    def get_for_worker(
        self,
        ticket_id: UUID,
        property_id: UUID,
        worker_id: UUID,
    ) -> Ticket | None: ...

    def update_if_version(
        self,
        ticket: Ticket,
        expected_version: int,
        event: TicketEvent,
        notification_jobs: tuple[NotificationJob, ...] = (),
    ) -> bool: ...

    def list_events(self, ticket_id: UUID) -> list[TicketEvent]: ...


class InMemoryTicketRepository:
    def __init__(self, tickets: Iterable[Ticket] = ()) -> None:
        self._tickets = {ticket.id: ticket for ticket in tickets}
        self._ticket_ids_by_client_request = {
            ticket.client_request_id: ticket.id for ticket in tickets
        }
        self._events_by_ticket: dict[UUID, list[TicketEvent]] = {}
        self.notification_jobs: list[NotificationJob] = []
        self._lock = Lock()

    def create(
        self,
        ticket: Ticket,
        event: TicketEvent,
        notification_jobs: tuple[NotificationJob, ...] = (),
    ) -> tuple[Ticket, bool]:
        with self._lock:
            existing_id = self._ticket_ids_by_client_request.get(
                ticket.client_request_id
            )
            if existing_id is not None:
                return self._tickets[existing_id], False

            self._tickets[ticket.id] = ticket
            self._ticket_ids_by_client_request[ticket.client_request_id] = ticket.id
            self._events_by_ticket[ticket.id] = [event]
            self.notification_jobs.extend(notification_jobs)
            return ticket, True

    def list_for_resident(
        self,
        property_id: UUID,
        resident_id: UUID,
    ) -> list[Ticket]:
        visible_tickets = (
            ticket
            for ticket in self._tickets.values()
            if ticket.property_id == property_id and ticket.resident_id == resident_id
        )
        return sorted(
            visible_tickets,
            key=lambda ticket: ticket.updated_at,
            reverse=True,
        )

    def get_for_resident(
        self,
        ticket_id: UUID,
        property_id: UUID,
        resident_id: UUID,
    ) -> Ticket | None:
        ticket = self._tickets.get(ticket_id)
        if ticket is None:
            return None
        if ticket.property_id != property_id or ticket.resident_id != resident_id:
            return None
        return ticket

    def list_for_property(self, property_id: UUID) -> list[Ticket]:
        visible_tickets = (
            ticket
            for ticket in self._tickets.values()
            if ticket.property_id == property_id
        )
        return sorted(
            visible_tickets,
            key=lambda ticket: ticket.updated_at,
            reverse=True,
        )

    def get_for_property(
        self,
        ticket_id: UUID,
        property_id: UUID,
    ) -> Ticket | None:
        ticket = self._tickets.get(ticket_id)
        if ticket is None or ticket.property_id != property_id:
            return None
        return ticket

    def list_for_worker(
        self,
        property_id: UUID,
        worker_id: UUID,
    ) -> list[Ticket]:
        visible_tickets = (
            ticket
            for ticket in self._tickets.values()
            if ticket.property_id == property_id
            and ticket.assigned_worker_id == worker_id
        )
        return sorted(
            visible_tickets,
            key=lambda ticket: ticket.updated_at,
            reverse=True,
        )

    def get_for_worker(
        self,
        ticket_id: UUID,
        property_id: UUID,
        worker_id: UUID,
    ) -> Ticket | None:
        ticket = self._tickets.get(ticket_id)
        if ticket is None:
            return None
        if ticket.property_id != property_id or ticket.assigned_worker_id != worker_id:
            return None
        return ticket

    def update_if_version(
        self,
        ticket: Ticket,
        expected_version: int,
        event: TicketEvent,
        notification_jobs: tuple[NotificationJob, ...] = (),
    ) -> bool:
        with self._lock:
            current = self._tickets.get(ticket.id)
            if current is None or current.version != expected_version:
                return False
            self._tickets[ticket.id] = ticket
            self._events_by_ticket.setdefault(ticket.id, []).append(event)
            self.notification_jobs.extend(notification_jobs)
            return True

    def list_events(self, ticket_id: UUID) -> list[TicketEvent]:
        return list(self._events_by_ticket.get(ticket_id, ()))
