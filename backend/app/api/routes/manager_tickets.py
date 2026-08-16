from uuid import UUID

from fastapi import APIRouter, HTTPException, status

from app.api.dependencies import ManagerContextDependency, TicketServiceDependency
from app.api.schemas import TicketAssignmentRequest, TicketResponse, WorkerResponse
from app.domain.ticket_workflow import TransitionNotAllowed
from app.services.tickets import (
    TicketNotFoundError,
    TicketVersionConflictError,
    WorkerNotEligibleError,
)

router = APIRouter(prefix="/manager", tags=["manager"])


@router.get("/tickets", response_model=list[TicketResponse])
def list_manager_tickets(
    service: TicketServiceDependency,
    manager: ManagerContextDependency,
) -> list[TicketResponse]:
    return [
        TicketResponse.from_domain(ticket)
        for ticket in service.list_manager_tickets(manager)
    ]


@router.get("/workers", response_model=list[WorkerResponse])
def list_workers(
    service: TicketServiceDependency,
    manager: ManagerContextDependency,
) -> list[WorkerResponse]:
    return [
        WorkerResponse.from_domain(worker) for worker in service.list_workers(manager)
    ]


@router.post("/tickets/{ticket_id}/assignment", response_model=TicketResponse)
def assign_ticket(
    ticket_id: UUID,
    payload: TicketAssignmentRequest,
    service: TicketServiceDependency,
    manager: ManagerContextDependency,
) -> TicketResponse:
    try:
        ticket = service.assign_ticket(ticket_id, payload.to_command(), manager)
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
                "message": "This ticket changed. Refresh the queue and try again.",
            },
        ) from error
    except WorkerNotEligibleError as error:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": "worker_not_eligible",
                "message": "That worker is not available for this property.",
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
