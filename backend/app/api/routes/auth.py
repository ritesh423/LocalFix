from fastapi import APIRouter, HTTPException, status

from app.api.auth_schemas import (
    AuthenticatedUserResponse,
    AuthSessionResponse,
    MembershipResponse,
    ResidentInviteRedemptionRequest,
    ResidentInviteRedemptionResponse,
)
from app.api.dependencies import (
    AuthenticatedIdentityDependency,
    MembershipRepositoryDependency,
    PropertyRepositoryDependency,
    ResidentInviteRepositoryDependency,
)
from app.services.memberships import MembershipConflictError
from app.services.resident_invites import (
    ExpiredResidentInviteError,
    InvalidResidentInviteError,
    ResidentInviteAlreadyUsedError,
    ResidentInviteService,
    VerifiedEmailRequiredError,
)

router = APIRouter(prefix="/auth", tags=["authentication"])


@router.get("/session", response_model=AuthSessionResponse)
def get_auth_session(
    identity: AuthenticatedIdentityDependency,
    memberships: MembershipRepositoryDependency,
    properties: PropertyRepositoryDependency,
) -> AuthSessionResponse:
    active_memberships = memberships.list_active(identity.firebase_uid)
    return AuthSessionResponse(
        user=AuthenticatedUserResponse.from_domain(identity),
        memberships=[
            MembershipResponse.from_domain(
                membership,
                property_=properties.get_property(membership.property_id),
                unit=(
                    properties.get_unit(membership.property_id, membership.unit_id)
                    if membership.unit_id is not None
                    else None
                ),
            )
            for membership in active_memberships
        ],
    )


@router.post(
    "/resident-invites/redeem",
    response_model=ResidentInviteRedemptionResponse,
)
def redeem_resident_invite(
    request: ResidentInviteRedemptionRequest,
    identity: AuthenticatedIdentityDependency,
    memberships: MembershipRepositoryDependency,
    properties: PropertyRepositoryDependency,
    invites: ResidentInviteRepositoryDependency,
) -> ResidentInviteRedemptionResponse:
    service = ResidentInviteService(invites, memberships, properties)
    try:
        membership = service.redeem(request.invite_code, identity)
    except VerifiedEmailRequiredError as error:
        _raise_invite_error(status.HTTP_403_FORBIDDEN, "email_not_verified", error)
    except ExpiredResidentInviteError as error:
        _raise_invite_error(status.HTTP_410_GONE, "invite_expired", error)
    except ResidentInviteAlreadyUsedError as error:
        _raise_invite_error(status.HTTP_409_CONFLICT, "invite_already_used", error)
    except MembershipConflictError as error:
        _raise_invite_error(status.HTTP_409_CONFLICT, "membership_conflict", error)
    except InvalidResidentInviteError as error:
        _raise_invite_error(status.HTTP_400_BAD_REQUEST, "invalid_invite", error)

    return ResidentInviteRedemptionResponse(
        membership=MembershipResponse.from_domain(
            membership,
            property_=properties.get_property(membership.property_id),
            unit=properties.get_unit(membership.property_id, membership.unit_id),
        )
    )


def _raise_invite_error(
    status_code: int,
    code: str,
    error: Exception,
) -> None:
    raise HTTPException(
        status_code=status_code,
        detail={"code": code, "message": str(error)},
    ) from error
