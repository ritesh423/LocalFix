from uuid import UUID

from fastapi import APIRouter, HTTPException, status

from app.api.dependencies import TicketServiceDependency, WorkerContextDependency
from app.api.schemas import TicketResponse, TicketStartRequest
from app.domain.ticket_workflow import TransitionNotAllowed
from app.services.tickets import TicketNotFoundError, TicketVersionConflictError

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
