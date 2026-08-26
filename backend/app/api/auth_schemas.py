from pydantic import BaseModel

from app.domain.auth import AuthenticatedIdentity, PropertyMembership


class AuthenticatedUserResponse(BaseModel):
    firebase_uid: str
    email: str | None
    display_name: str | None

    @classmethod
    def from_domain(
        cls,
        identity: AuthenticatedIdentity,
    ) -> "AuthenticatedUserResponse":
        return cls(
            firebase_uid=identity.firebase_uid,
            email=identity.email,
            display_name=identity.display_name,
        )


class MembershipResponse(BaseModel):
    property_id: str
    user_id: str
    role: str
    unit_id: str | None

    @classmethod
    def from_domain(cls, membership: PropertyMembership) -> "MembershipResponse":
        return cls(
            property_id=str(membership.property_id),
            user_id=str(membership.user_id),
            role=membership.role.value,
            unit_id=str(membership.unit_id) if membership.unit_id else None,
        )


class AuthSessionResponse(BaseModel):
    user: AuthenticatedUserResponse
    memberships: list[MembershipResponse]
