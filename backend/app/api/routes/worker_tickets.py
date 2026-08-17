from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.api.dependencies import TicketServiceDependency, WorkerContextDependency
from app.api.schemas import TicketEventResponse, TicketResponse, TicketStartRequest
from app.domain.ticket_workflow import TransitionNotAllowed
from app.services.tickets import (
    CompletionPhoto,
    InvalidCompletionEvidenceError,
    SubmitCompletionCommand,
    TicketNotFoundError,
    TicketVersionConflictError,
)

router = APIRouter(prefix="/worker", tags=["worker"])


@router.get("/tickets", response_model=list[TicketResponse])
def list_worker_tickets(
    service: TicketServiceDependency,
    worker: WorkerContextDependency,
) -> list[TicketResponse]:
    return [
        TicketResponse.from_domain(ticket)
        for ticket in service.list_worker_tickets(worker)
    ]


@router.get(
    "/tickets/{ticket_id}/events",
    response_model=list[TicketEventResponse],
)
def list_worker_ticket_events(
    ticket_id: UUID,
    service: TicketServiceDependency,
    worker: WorkerContextDependency,
) -> list[TicketEventResponse]:
    try:
        events = service.list_worker_ticket_events(ticket_id, worker)
    except TicketNotFoundError as error:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"code": "ticket_not_found", "message": "Ticket not found."},
        ) from error
    return [TicketEventResponse.from_domain(event) for event in events]


@router.post("/tickets/{ticket_id}/start", response_model=TicketResponse)
def start_ticket(
    ticket_id: UUID,
    payload: TicketStartRequest,
    service: TicketServiceDependency,
    worker: WorkerContextDependency,
) -> TicketResponse:
    try:
        ticket = service.start_ticket(ticket_id, payload.to_command(), worker)
    except TicketNotFoundError as error:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "code": "ticket_not_found",
                "message": "Ticket not found.",
            },
        ) from error
    except TicketVersionConflictError as error:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": "ticket_version_conflict",
                "message": "This job changed. Refresh your queue and try again.",
            },
        ) from error
    except TransitionNotAllowed as error:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": error.code,
                "message": str(error),
            },
        ) from error
    return TicketResponse.from_domain(ticket)


@router.post("/tickets/{ticket_id}/completion", response_model=TicketResponse)
async def submit_completion(
    ticket_id: UUID,
    expected_version: Annotated[int, Form(ge=1)],
    completion_note: Annotated[str, Form(min_length=10, max_length=500)],
    photo: Annotated[UploadFile, File()],
    service: TicketServiceDependency,
    worker: WorkerContextDependency,
    parts_used: Annotated[list[str] | None, Form()] = None,
) -> TicketResponse:
    try:
        photo_content = await photo.read(5 * 1024 * 1024 + 1)
        command = SubmitCompletionCommand(
            expected_version=expected_version,
            completion_note=completion_note,
            parts_used=tuple(parts_used or ()),
            photo=CompletionPhoto(
                content_type=photo.content_type or "",
                content=photo_content,
            ),
        )
        ticket = service.submit_completion(ticket_id, command, worker)
    except TicketNotFoundError as error:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"code": "ticket_not_found", "message": "Ticket not found."},
        ) from error
    except TicketVersionConflictError as error:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": "ticket_version_conflict",
                "message": "This job changed. Refresh your queue and try again.",
            },
        ) from error
    except InvalidCompletionEvidenceError as error:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "code": "invalid_completion_evidence",
                "message": str(error),
            },
        ) from error
    except TransitionNotAllowed as error:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": error.code,
                "message": str(error),
            },
        ) from error
    finally:
        await photo.close()
    return TicketResponse.from_domain(ticket)
