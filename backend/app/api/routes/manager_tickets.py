from datetime import timedelta
from uuid import UUID

from fastapi import APIRouter, HTTPException, status

from app.api.dependencies import (
    ManagerContextDependency,
    MembershipRepositoryDependency,
    PropertyRepositoryDependency,
    ResidentInviteRepositoryDependency,
    TicketServiceDependency,
)
from app.api.schemas import (
    ManagerPropertyUnitResponse,
    ManagerResidentInviteCreateRequest,
    ManagerResidentInviteResponse,
    ManagerTicketSummaryResponse,
    TicketAssignmentRequest,
    TicketEventResponse,
    TicketResponse,
    WorkerResponse,
)
from app.domain.ticket_workflow import TransitionNotAllowed
from app.services.resident_invites import (
    InvalidResidentInviteError,
    ResidentInviteService,
)
from app.services.tickets import (
    TicketNotFoundError,
    TicketVersionConflictError,
    WorkerNotEligibleError,
)

router = APIRouter(prefix="/manager", tags=["manager"])


@router.get("/units", response_model=list[ManagerPropertyUnitResponse])
def list_property_units(
    manager: ManagerContextDependency,
    properties: PropertyRepositoryDependency,
) -> list[ManagerPropertyUnitResponse]:
    return [
        ManagerPropertyUnitResponse.from_domain(unit)
        for unit in properties.list_units(manager.property_id)
        if unit.is_active
    ]


@router.post(
    "/resident-invites",
    response_model=ManagerResidentInviteResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_resident_invite(
    payload: ManagerResidentInviteCreateRequest,
    manager: ManagerContextDependency,
    memberships: MembershipRepositoryDependency,
    properties: PropertyRepositoryDependency,
    invites: ResidentInviteRepositoryDependency,
) -> ManagerResidentInviteResponse:
    service = ResidentInviteService(invites, memberships, properties)
    try:
        created = service.create(
            property_id=manager.property_id,
            unit_id=payload.unit_id,
            valid_for=timedelta(days=payload.valid_days),
        )
    except InvalidResidentInviteError as error:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"code": "invalid_unit", "message": str(error)},
        ) from error
    unit = properties.get_unit(manager.property_id, created.invite.unit_id)
    if unit is None:
        raise RuntimeError("Created invite unit could not be loaded.")
    return ManagerResidentInviteResponse(
        invite_code=created.code,
        unit_id=unit.id,
        unit_label=unit.label,
        expires_at=created.invite.expires_at,
    )


@router.get("/summary", response_model=ManagerTicketSummaryResponse)
def get_manager_summary(
    service: TicketServiceDependency,
    manager: ManagerContextDependency,
) -> ManagerTicketSummaryResponse:
    return ManagerTicketSummaryResponse.from_domain(
        service.get_manager_summary(manager)
    )


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


@router.get(
    "/tickets/{ticket_id}/events",
    response_model=list[TicketEventResponse],
)
def list_manager_ticket_events(
    ticket_id: UUID,
    service: TicketServiceDependency,
    manager: ManagerContextDependency,
) -> list[TicketEventResponse]:
    try:
        events = service.list_manager_ticket_events(ticket_id, manager)
    except TicketNotFoundError as error:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"code": "ticket_not_found", "message": "Ticket not found."},
        ) from error
    return [TicketEventResponse.from_domain(event) for event in events]


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
