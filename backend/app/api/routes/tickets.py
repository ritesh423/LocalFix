from uuid import UUID

from fastapi import APIRouter, HTTPException, Response, status

from app.api.dependencies import (
    ResidentContextDependency,
    TicketServiceDependency,
)
from app.api.schemas import TicketCreateRequest, TicketResponse

router = APIRouter(prefix="/tickets", tags=["tickets"])


@router.post(
    "",
    response_model=TicketResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_ticket(
    payload: TicketCreateRequest,
    response: Response,
    service: TicketServiceDependency,
    resident: ResidentContextDependency,
) -> TicketResponse:
    result = service.create_ticket(payload.to_command(), resident)
    if not result.was_created:
        response.status_code = status.HTTP_200_OK
    return TicketResponse.from_domain(result.ticket)


@router.get("", response_model=list[TicketResponse])
def list_tickets(
    service: TicketServiceDependency,
    resident: ResidentContextDependency,
) -> list[TicketResponse]:
    return [
        TicketResponse.from_domain(ticket) for ticket in service.list_tickets(resident)
    ]


@router.get("/{ticket_id}", response_model=TicketResponse)
def get_ticket(
    ticket_id: UUID,
    service: TicketServiceDependency,
    resident: ResidentContextDependency,
) -> TicketResponse:
    ticket = service.get_ticket(ticket_id, resident)
    if ticket is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "code": "ticket_not_found",
                "message": "Ticket not found.",
            },
        )
    return TicketResponse.from_domain(ticket)
