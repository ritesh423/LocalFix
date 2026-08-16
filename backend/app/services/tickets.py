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
    WorkerContext,
)
from app.repositories.tickets import TicketRepository
from app.storage.evidence import EvidenceStorage


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


@dataclass(frozen=True)
class StartTicketCommand:
    expected_version: int


@dataclass(frozen=True)
class CompletionPhoto:
    content_type: str
    content: bytes


@dataclass(frozen=True)
class SubmitCompletionCommand:
    expected_version: int
    completion_note: str
    parts_used: tuple[str, ...]
    photo: CompletionPhoto


class TicketNotFoundError(LookupError):
    pass


class TicketVersionConflictError(RuntimeError):
    pass


class WorkerNotEligibleError(ValueError):
    pass


class InvalidCompletionEvidenceError(ValueError):
    pass


class TicketService:
    def __init__(
        self,
        repository: TicketRepository,
        workers: Iterable[Worker] = (),
        evidence_storage: EvidenceStorage | None = None,
    ) -> None:
        self._repository = repository
        self._workers = {worker.id: worker for worker in workers}
        self._evidence_storage = evidence_storage

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
            completion_note=None,
            parts_used=(),
            completion_photo_key=None,
            completion_submitted_at=None,
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

    def list_worker_tickets(self, worker: WorkerContext) -> list[Ticket]:
        return self._repository.list_for_worker(
            property_id=worker.property_id,
            worker_id=worker.worker_id,
        )

    def start_ticket(
        self,
        ticket_id: UUID,
        command: StartTicketCommand,
        worker: WorkerContext,
    ) -> Ticket:
        ticket = self._repository.get_for_worker(
            ticket_id=ticket_id,
            property_id=worker.property_id,
            worker_id=worker.worker_id,
        )
        if ticket is None:
            raise TicketNotFoundError
        if ticket.version != command.expected_version:
            raise TicketVersionConflictError

        started_ticket = replace(
            ticket,
            status=transition(
                current=ticket.status,
                action=TicketAction.START,
                actor=UserRole.WORKER,
            ),
            version=ticket.version + 1,
            updated_at=datetime.now(UTC),
        )
        was_updated = self._repository.update_if_version(
            started_ticket,
            expected_version=command.expected_version,
        )
        if not was_updated:
            raise TicketVersionConflictError
        return started_ticket

    def submit_completion(
        self,
        ticket_id: UUID,
        command: SubmitCompletionCommand,
        worker: WorkerContext,
    ) -> Ticket:
        ticket = self._repository.get_for_worker(
            ticket_id=ticket_id,
            property_id=worker.property_id,
            worker_id=worker.worker_id,
        )
        if ticket is None:
            raise TicketNotFoundError
        if ticket.version != command.expected_version:
            raise TicketVersionConflictError

        note = command.completion_note.strip()
        parts = tuple(part.strip() for part in command.parts_used if part.strip())
        self._validate_completion(note, parts, command.photo)
        next_status = transition(
            current=ticket.status,
            action=TicketAction.SUBMIT_PROOF,
            actor=UserRole.WORKER,
        )
        if self._evidence_storage is None:
            raise RuntimeError("Completion evidence storage is not configured.")

        photo_key = self._evidence_storage.save(
            ticket_id=ticket.id,
            content_type=command.photo.content_type,
            content=command.photo.content,
        )
        submitted_at = datetime.now(UTC)
        completed_ticket = replace(
            ticket,
            completion_note=note,
            parts_used=parts,
            completion_photo_key=photo_key,
            completion_submitted_at=submitted_at,
            status=next_status,
            version=ticket.version + 1,
            updated_at=submitted_at,
        )
        try:
            was_updated = self._repository.update_if_version(
                completed_ticket,
                expected_version=command.expected_version,
            )
        except Exception:
            self._evidence_storage.delete(photo_key)
            raise
        if not was_updated:
            self._evidence_storage.delete(photo_key)
            raise TicketVersionConflictError
        return completed_ticket

    @staticmethod
    def _validate_completion(
        note: str,
        parts: tuple[str, ...],
        photo: CompletionPhoto,
    ) -> None:
        if not 10 <= len(note) <= 500:
            raise InvalidCompletionEvidenceError(
                "Completion note must contain between 10 and 500 characters."
            )
        if len(parts) > 10 or any(len(part) > 80 for part in parts):
            raise InvalidCompletionEvidenceError(
                "Use at most 10 parts with no more than 80 characters each."
            )
        if photo.content_type not in {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif",
        }:
            raise InvalidCompletionEvidenceError(
                "Completion photo must be JPEG, PNG, WebP, HEIC, or HEIF."
            )
        if not photo.content or len(photo.content) > 5 * 1024 * 1024:
            raise InvalidCompletionEvidenceError(
                "Completion photo must be present and no larger than 5 MB."
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
