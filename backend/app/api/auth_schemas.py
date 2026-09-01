from pydantic import BaseModel

from app.domain.auth import AuthenticatedIdentity, PropertyMembership
from app.domain.properties import Property, PropertyUnit


class AuthenticatedUserResponse(BaseModel):
    firebase_uid: str
    email: str | None
    display_name: str | None
    email_verified: bool

    @classmethod
    def from_domain(
        cls,
        identity: AuthenticatedIdentity,
    ) -> "AuthenticatedUserResponse":
        return cls(
            firebase_uid=identity.firebase_uid,
            email=identity.email,
            display_name=identity.display_name,
            email_verified=identity.email_verified,
        )


class MembershipResponse(BaseModel):
    property_id: str
    property_name: str | None
    user_id: str
    role: str
    unit_id: str | None
    unit_label: str | None

    @classmethod
    def from_domain(
        cls,
        membership: PropertyMembership,
        property_: Property | None,
        unit: PropertyUnit | None,
    ) -> "MembershipResponse":
        return cls(
            property_id=str(membership.property_id),
            property_name=property_.name if property_ else None,
            user_id=str(membership.user_id),
            role=membership.role.value,
            unit_id=str(membership.unit_id) if membership.unit_id else None,
            unit_label=unit.label if unit else None,
        )


class AuthSessionResponse(BaseModel):
    user: AuthenticatedUserResponse
    memberships: list[MembershipResponse]


class ResidentInviteRedemptionRequest(BaseModel):
    invite_code: str


class ResidentInviteRedemptionResponse(BaseModel):
    membership: MembershipResponse


class InviteRedemptionRequest(BaseModel):
    invite_code: str


class InviteRedemptionResponse(BaseModel):
    membership: MembershipResponse
