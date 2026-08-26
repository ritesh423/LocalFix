from fastapi import APIRouter

from app.api.auth_schemas import (
    AuthenticatedUserResponse,
    AuthSessionResponse,
    MembershipResponse,
)
from app.api.dependencies import (
    AuthenticatedIdentityDependency,
    MembershipRepositoryDependency,
)

router = APIRouter(prefix="/auth", tags=["authentication"])


@router.get("/session", response_model=AuthSessionResponse)
def get_auth_session(
    identity: AuthenticatedIdentityDependency,
    memberships: MembershipRepositoryDependency,
) -> AuthSessionResponse:
    return AuthSessionResponse(
        user=AuthenticatedUserResponse.from_domain(identity),
        memberships=[
            MembershipResponse.from_domain(membership)
            for membership in memberships.list_active(identity.firebase_uid)
        ],
    )
