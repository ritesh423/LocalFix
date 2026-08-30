from fastapi import APIRouter

from app.api.auth_schemas import (
    AuthenticatedUserResponse,
    AuthSessionResponse,
    MembershipResponse,
)
from app.api.dependencies import (
    AuthenticatedIdentityDependency,
    MembershipRepositoryDependency,
    PropertyRepositoryDependency,
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
