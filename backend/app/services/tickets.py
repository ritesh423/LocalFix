from collections.abc import Iterable
from dataclasses import dataclass, replace
from datetime import UTC, datetime
from uuid import UUID, uuid4

from app.domain.ticket_workflow import (
    TicketAction,
    TicketStatus,
    UserRole,
    transition,
)
from app.domain.tickets import (
    AccessWindow,
    ManagerContext,
    ResidentContext,
    ServiceCategory,
    Ticket,
    TicketPriority,
    UrgencySuggestion,
    Worker,
)
from app.repositories.tickets import TicketRepository


@dataclass(frozen=True)
class CreateTicketCommand:
    client_request_id: UUID
    title: str
    description: str
    category: ServiceCategory
    urgency_suggestion: UrgencySuggestion
    access_window: AccessWindow


@dataclass(frozen=True)
class CreateTicketResult:
    ticket: Ticket
    was_created: bool


@dataclass(frozen=True)
class AssignTicketCommand:
    expected_version: int
    priority: TicketPriority
    worker_id: UUID


class TicketNotFoundError(LookupError):
    pass


class TicketVersionConflictError(RuntimeError):
    pass


class WorkerNotEligibleError(ValueError):
    pass


class TicketService:
    def __init__(
        self,
        repository: TicketRepository,
        workers: Iterable[Worker] = (),
    ) -> None:
        self._repository = repository
        self._workers = {worker.id: worker for worker in workers}

    def create_ticket(
        self,
        command: CreateTicketCommand,
        resident: ResidentContext,
    ) -> CreateTicketResult:
        now = datetime.now(UTC)
        candidate = Ticket(
            id=uuid4(),
            client_request_id=command.client_request_id,
            property_id=resident.property_id,
            unit_id=resident.unit_id,
            resident_id=resident.user_id,
            title=command.title,
            description=command.description,
            category=command.category,
            urgency_suggestion=command.urgency_suggestion,
            priority=None,
            access_window=command.access_window,
            status=TicketStatus.OPEN,
            version=1,
            assigned_worker_id=None,
            assigned_worker=None,
            created_at=now,
            updated_at=now,
        )
        ticket, was_created = self._repository.create(candidate)
        return CreateTicketResult(ticket=ticket, was_created=was_created)

    def list_tickets(self, resident: ResidentContext) -> list[Ticket]:
        return self._repository.list_for_resident(
            property_id=resident.property_id,
            resident_id=resident.user_id,
        )

    def get_ticket(
        self,
        ticket_id: UUID,
        resident: ResidentContext,
    ) -> Ticket | None:
        return self._repository.get_for_resident(
            ticket_id=ticket_id,
            property_id=resident.property_id,
            resident_id=resident.user_id,
        )

    def list_manager_tickets(self, manager: ManagerContext) -> list[Ticket]:
        return self._repository.list_for_property(manager.property_id)

    def list_workers(self, manager: ManagerContext) -> list[Worker]:
        return sorted(
            (
                worker
                for worker in self._workers.values()
                if worker.property_id == manager.property_id and worker.is_active
            ),
            key=lambda worker: worker.name,
        )

    def assign_ticket(
        self,
        ticket_id: UUID,
        command: AssignTicketCommand,
        manager: ManagerContext,
    ) -> Ticket:
        ticket = self._repository.get_for_property(
            ticket_id=ticket_id,
            property_id=manager.property_id,
        )
        if ticket is None:
            raise TicketNotFoundError
        if ticket.version != command.expected_version:
            raise TicketVersionConflictError

        worker = self._workers.get(command.worker_id)
        if (
            worker is None
            or worker.property_id != manager.property_id
            or not worker.is_active
        ):
            raise WorkerNotEligibleError

        assigned_ticket = replace(
            ticket,
            priority=command.priority,
            status=transition(
                current=ticket.status,
                action=TicketAction.ASSIGN,
                actor=UserRole.MANAGER,
            ),
            version=ticket.version + 1,
            assigned_worker_id=worker.id,
            assigned_worker=worker.name,
            updated_at=datetime.now(UTC),
        )
        was_updated = self._repository.update_if_version(
            assigned_ticket,
            expected_version=command.expected_version,
        )
        if not was_updated:
            raise TicketVersionConflictError
        return assigned_ticket
