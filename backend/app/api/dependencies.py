from typing import Annotated
from uuid import UUID

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.domain.auth import AuthenticatedIdentity
from app.domain.ticket_workflow import UserRole
from app.domain.tickets import (
    ManagerContext,
    ResidentContext,
    ServiceCategory,
    Worker,
    WorkerContext,
)
from app.gateways.identity import IdentityTokenVerifier, InvalidIdentityTokenError
from app.repositories.device_registrations import DeviceRegistrationRepository
from app.repositories.memberships import MembershipRepository
from app.repositories.properties import PropertyRepository
from app.repositories.tickets import TicketRepository
from app.services.device_registrations import DeviceRegistrationService
from app.services.tickets import TicketService
from app.storage.evidence import EvidenceStorage

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

bearer_token = HTTPBearer(auto_error=False)


def get_ticket_repository(request: Request) -> TicketRepository:
    return request.app.state.ticket_repository


def get_evidence_storage(request: Request) -> EvidenceStorage:
    return request.app.state.evidence_storage


def get_ticket_service(
    repository: Annotated[TicketRepository, Depends(get_ticket_repository)],
    evidence_storage: Annotated[EvidenceStorage, Depends(get_evidence_storage)],
) -> TicketService:
    return TicketService(
        repository,
        workers=DEMO_WORKERS,
        evidence_storage=evidence_storage,
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
) -> WorkerContext:
    if identity is None:
        return DEMO_WORKER_CONTEXT
    membership = memberships.find_active(identity.firebase_uid, UserRole.WORKER)
    if membership is None:
        _raise_forbidden("You do not have an active worker membership.")
    _require_active_property(properties, membership.property_id)
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
