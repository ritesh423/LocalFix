from typing import Annotated
from uuid import UUID

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.demo import (
    DEMO_MANAGER_CONTEXT,
    DEMO_RESIDENT_CONTEXT,
    DEMO_WORKER_CONTEXT,
    DEMO_WORKERS,
)
from app.domain.auth import AuthenticatedIdentity
from app.domain.ticket_workflow import UserRole
from app.domain.tickets import ManagerContext, ResidentContext, WorkerContext
from app.gateways.identity import IdentityTokenVerifier, InvalidIdentityTokenError
from app.repositories.device_registrations import DeviceRegistrationRepository
from app.repositories.memberships import MembershipRepository
from app.repositories.properties import PropertyRepository
from app.repositories.resident_invites import ResidentInviteRepository
from app.repositories.staff_invites import StaffInviteRepository
from app.repositories.tickets import TicketRepository
from app.repositories.workers import WorkerRepository
from app.services.device_registrations import DeviceRegistrationService
from app.services.tickets import TicketService
from app.storage.evidence import EvidenceStorage

bearer_token = HTTPBearer(auto_error=False)


def get_ticket_repository(request: Request) -> TicketRepository:
    return request.app.state.ticket_repository


def get_evidence_storage(request: Request) -> EvidenceStorage:
    return request.app.state.evidence_storage


def get_worker_repository(request: Request) -> WorkerRepository:
    return request.app.state.worker_repository


def get_ticket_service(
    repository: Annotated[TicketRepository, Depends(get_ticket_repository)],
    evidence_storage: Annotated[EvidenceStorage, Depends(get_evidence_storage)],
    workers: Annotated[WorkerRepository, Depends(get_worker_repository)],
) -> TicketService:
    return TicketService(
        repository,
        workers=DEMO_WORKERS,
        evidence_storage=evidence_storage,
        worker_repository=workers,
    )


def get_device_registration_repository(
    request: Request,
) -> DeviceRegistrationRepository:
    return request.app.state.device_registration_repository


def get_device_registration_service(
    repository: Annotated[
        DeviceRegistrationRepository,
        Depends(get_device_registration_repository),
    ],
) -> DeviceRegistrationService:
    return DeviceRegistrationService(repository)


def get_identity_token_verifier(request: Request) -> IdentityTokenVerifier:
    return request.app.state.identity_token_verifier


def get_membership_repository(request: Request) -> MembershipRepository:
    return request.app.state.membership_repository


def get_property_repository(request: Request) -> PropertyRepository:
    return request.app.state.property_repository


def get_resident_invite_repository(request: Request) -> ResidentInviteRepository:
    return request.app.state.resident_invite_repository


def get_staff_invite_repository(request: Request) -> StaffInviteRepository:
    return request.app.state.staff_invite_repository


def get_authenticated_identity(
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer_token),
    ],
    verifier: Annotated[
        IdentityTokenVerifier,
        Depends(get_identity_token_verifier),
    ],
) -> AuthenticatedIdentity:
    if credentials is None or credentials.scheme.lower() != "bearer":
        _raise_unauthenticated("Sign in to continue.")
    try:
        return verifier.verify(credentials.credentials)
    except InvalidIdentityTokenError:
        _raise_unauthenticated("Your sign-in has expired. Sign in again.")


def get_request_identity(
    request: Request,
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer_token),
    ],
    verifier: Annotated[
        IdentityTokenVerifier,
        Depends(get_identity_token_verifier),
    ],
) -> AuthenticatedIdentity | None:
    if not request.app.state.authentication_required:
        return None
    if credentials is None or credentials.scheme.lower() != "bearer":
        _raise_unauthenticated("Sign in to continue.")
    try:
        return verifier.verify(credentials.credentials)
    except InvalidIdentityTokenError:
        _raise_unauthenticated("Your sign-in has expired. Sign in again.")


def get_resident_context(
    identity: Annotated[AuthenticatedIdentity | None, Depends(get_request_identity)],
    memberships: Annotated[
        MembershipRepository,
        Depends(get_membership_repository),
    ],
    properties: Annotated[
        PropertyRepository,
        Depends(get_property_repository),
    ],
) -> ResidentContext:
    if identity is None:
        return DEMO_RESIDENT_CONTEXT
    membership = memberships.find_active(identity.firebase_uid, UserRole.RESIDENT)
    if membership is None:
        _raise_forbidden("You do not have an active resident membership.")
    _require_active_property(properties, membership.property_id)
    unit = (
        properties.get_unit(membership.property_id, membership.unit_id)
        if membership.unit_id is not None
        else None
    )
    if unit is None or not unit.is_active:
        _raise_forbidden("Your apartment unit is not active for this property.")
    try:
        return membership.resident_context()
    except ValueError:
        _raise_forbidden("Your resident membership is missing an apartment unit.")


def get_manager_context(
    identity: Annotated[AuthenticatedIdentity | None, Depends(get_request_identity)],
    memberships: Annotated[
        MembershipRepository,
        Depends(get_membership_repository),
    ],
    properties: Annotated[
        PropertyRepository,
        Depends(get_property_repository),
    ],
) -> ManagerContext:
    if identity is None:
        return DEMO_MANAGER_CONTEXT
    membership = memberships.find_active(identity.firebase_uid, UserRole.MANAGER)
    if membership is None:
        _raise_forbidden("You do not have an active manager membership.")
    _require_active_property(properties, membership.property_id)
    return membership.manager_context()


def get_worker_context(
    identity: Annotated[AuthenticatedIdentity | None, Depends(get_request_identity)],
    memberships: Annotated[
        MembershipRepository,
        Depends(get_membership_repository),
    ],
    properties: Annotated[
        PropertyRepository,
        Depends(get_property_repository),
    ],
    workers: Annotated[
        WorkerRepository,
        Depends(get_worker_repository),
    ],
) -> WorkerContext:
    if identity is None:
        return DEMO_WORKER_CONTEXT
    membership = memberships.find_active(identity.firebase_uid, UserRole.WORKER)
    if membership is None:
        _raise_forbidden("You do not have an active worker membership.")
    _require_active_property(properties, membership.property_id)
    worker = workers.get(membership.property_id, membership.user_id)
    if worker is None or not worker.is_active:
        _raise_forbidden("Your worker profile is not active for this property.")
    return membership.worker_context()


def _require_active_property(
    properties: PropertyRepository,
    property_id: UUID,
) -> None:
    property_ = properties.get_property(property_id)
    if property_ is None or not property_.is_active:
        _raise_forbidden("Your LocalFix property is not active.")


def _raise_unauthenticated(message: str) -> None:
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail={"code": "authentication_required", "message": message},
        headers={"WWW-Authenticate": "Bearer"},
    )


def _raise_forbidden(message: str) -> None:
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail={"code": "membership_required", "message": message},
    )


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
DeviceRegistrationServiceDependency = Annotated[
    DeviceRegistrationService,
    Depends(get_device_registration_service),
]
AuthenticatedIdentityDependency = Annotated[
    AuthenticatedIdentity,
    Depends(get_authenticated_identity),
]
MembershipRepositoryDependency = Annotated[
    MembershipRepository,
    Depends(get_membership_repository),
]
PropertyRepositoryDependency = Annotated[
    PropertyRepository,
    Depends(get_property_repository),
]
ResidentInviteRepositoryDependency = Annotated[
    ResidentInviteRepository,
    Depends(get_resident_invite_repository),
]
StaffInviteRepositoryDependency = Annotated[
    StaffInviteRepository,
    Depends(get_staff_invite_repository),
]
WorkerRepositoryDependency = Annotated[
    WorkerRepository,
    Depends(get_worker_repository),
]
