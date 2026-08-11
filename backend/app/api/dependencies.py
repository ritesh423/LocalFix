from typing import Annotated
from uuid import UUID

from fastapi import Depends, Request

from app.domain.tickets import ResidentContext
from app.repositories.tickets import TicketRepository
from app.services.tickets import TicketService

DEMO_RESIDENT_CONTEXT = ResidentContext(
    user_id=UUID("10000000-0000-0000-0000-000000000001"),
    property_id=UUID("20000000-0000-0000-0000-000000000001"),
    unit_id=UUID("30000000-0000-0000-0000-000000000204"),
)


def get_ticket_repository(request: Request) -> TicketRepository:
    return request.app.state.ticket_repository


def get_ticket_service(
    repository: Annotated[TicketRepository, Depends(get_ticket_repository)],
) -> TicketService:
    return TicketService(repository)


def get_resident_context() -> ResidentContext:
    return DEMO_RESIDENT_CONTEXT


TicketServiceDependency = Annotated[TicketService, Depends(get_ticket_service)]
ResidentContextDependency = Annotated[
    ResidentContext,
    Depends(get_resident_context),
]
