from typing import Annotated
from uuid import UUID

from fastapi import Depends, Request

from app.domain.tickets import (
    ManagerContext,
    ResidentContext,
    ServiceCategory,
    Worker,
    WorkerContext,
)
from app.repositories.tickets import TicketRepository
from app.services.tickets import TicketService

DEMO_RESIDENT_CONTEXT = ResidentContext(
    user_id=UUID("10000000-0000-0000-0000-000000000001"),
    property_id=UUID("20000000-0000-0000-0000-000000000001"),
    unit_id=UUID("30000000-0000-0000-0000-000000000204"),
)

DEMO_MANAGER_CONTEXT = ManagerContext(
    user_id=UUID("10000000-0000-0000-0000-000000000002"),
    property_id=UUID("20000000-0000-0000-0000-000000000001"),
)

DEMO_WORKERS = (
    Worker(
        id=UUID("40000000-0000-0000-0000-000000000001"),
        property_id=DEMO_MANAGER_CONTEXT.property_id,
        name="Arun Kumar",
        specialty=ServiceCategory.PLUMBING,
        is_active=True,
    ),
    Worker(
        id=UUID("40000000-0000-0000-0000-000000000002"),
        property_id=DEMO_MANAGER_CONTEXT.property_id,
        name="Maya Singh",
        specialty=ServiceCategory.ELECTRICAL,
        is_active=True,
    ),
    Worker(
        id=UUID("40000000-0000-0000-0000-000000000003"),
        property_id=DEMO_MANAGER_CONTEXT.property_id,
        name="Sameer Khan",
        specialty=ServiceCategory.APPLIANCE,
        is_active=True,
    ),
)

DEMO_WORKER_CONTEXT = WorkerContext(
    worker_id=DEMO_WORKERS[0].id,
    property_id=DEMO_WORKERS[0].property_id,
)


def get_ticket_repository(request: Request) -> TicketRepository:
    return request.app.state.ticket_repository


def get_ticket_service(
    repository: Annotated[TicketRepository, Depends(get_ticket_repository)],
) -> TicketService:
    return TicketService(repository, workers=DEMO_WORKERS)


def get_resident_context() -> ResidentContext:
    return DEMO_RESIDENT_CONTEXT


def get_manager_context() -> ManagerContext:
    return DEMO_MANAGER_CONTEXT


def get_worker_context() -> WorkerContext:
    return DEMO_WORKER_CONTEXT


TicketServiceDependency = Annotated[TicketService, Depends(get_ticket_service)]
ResidentContextDependency = Annotated[
    ResidentContext,
    Depends(get_resident_context),
]
ManagerContextDependency = Annotated[
    ManagerContext,
    Depends(get_manager_context),
]
WorkerContextDependency = Annotated[
    WorkerContext,
    Depends(get_worker_context),
]
