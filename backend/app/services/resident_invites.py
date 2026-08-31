import hashlib
import secrets
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from uuid import UUID, uuid4

from app.domain.auth import AuthenticatedIdentity, PropertyMembership, ResidentInvite
from app.domain.ticket_workflow import UserRole
from app.repositories.memberships import MembershipRepository
from app.repositories.properties import PropertyRepository
from app.repositories.resident_invites import ResidentInviteRepository
from app.services.memberships import (
    MembershipConflictError,
    MembershipProvisioningService,
)

INVITE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"


class InvalidResidentInviteError(ValueError):
    pass


class ExpiredResidentInviteError(ValueError):
    pass


class ResidentInviteAlreadyUsedError(ValueError):
    pass


class VerifiedEmailRequiredError(ValueError):
    pass


@dataclass(frozen=True)
class CreatedResidentInvite:
    invite: ResidentInvite
    code: str


class ResidentInviteService:
    def __init__(
        self,
        invites: ResidentInviteRepository,
        memberships: MembershipRepository,
        properties: PropertyRepository,
    ) -> None:
        self._invites = invites
        self._memberships = memberships
        self._properties = properties

    def create(
        self,
        property_id: UUID,
        unit_id: UUID,
        valid_for: timedelta = timedelta(days=7),
        now: datetime | None = None,
    ) -> CreatedResidentInvite:
        property_ = self._properties.get_property(property_id)
        unit = self._properties.get_unit(property_id, unit_id)
        if property_ is None or not property_.is_active:
            raise InvalidResidentInviteError("Property is not registered or active.")
        if unit is None or not unit.is_active:
            raise InvalidResidentInviteError(
                "Apartment unit is not registered or active for this property."
            )
        if valid_for <= timedelta(0):
            raise InvalidResidentInviteError("Invite lifetime must be positive.")

        created_at = _as_utc(now or datetime.now(UTC))
        code = _generate_code()
        invite = ResidentInvite(
            id=uuid4(),
            property_id=property_id,
            unit_id=unit_id,
            code_digest=_digest(code),
            expires_at=created_at + valid_for,
            created_at=created_at,
        )
        self._invites.save(invite)
        return CreatedResidentInvite(invite=invite, code=code)

    def redeem(
        self,
        code: str,
        identity: AuthenticatedIdentity,
        now: datetime | None = None,
    ) -> PropertyMembership:
        if not identity.email_verified:
            raise VerifiedEmailRequiredError(
                "Verify your email address before joining an apartment."
            )
        invite = self._invites.find_by_digest(_digest(code))
        if invite is None or invite.revoked_at is not None:
            raise InvalidResidentInviteError("Invite code is invalid.")

        redeemed_at = _as_utc(now or datetime.now(UTC))
        if _as_utc(invite.expires_at) <= redeemed_at:
            raise ExpiredResidentInviteError("Invite code has expired.")
        if invite.claimed_by_firebase_uid not in (None, identity.firebase_uid):
            raise ResidentInviteAlreadyUsedError("Invite code has already been used.")

        existing = self._memberships.find(
            identity.firebase_uid,
            invite.property_id,
            UserRole.RESIDENT,
        )
        if existing is not None and existing.unit_id != invite.unit_id:
            raise MembershipConflictError(
                "This account already belongs to another unit in this property."
            )

        claimed = self._invites.claim(
            invite.id,
            identity.firebase_uid,
            redeemed_at,
        )
        if claimed is None:
            raise ResidentInviteAlreadyUsedError("Invite code has already been used.")

        return MembershipProvisioningService(
            self._memberships,
            self._properties,
        ).provision(
            firebase_uid=identity.firebase_uid,
            property_id=invite.property_id,
            role=UserRole.RESIDENT,
            unit_id=invite.unit_id,
        ).membership


def _generate_code() -> str:
    body = "".join(secrets.choice(INVITE_ALPHABET) for _ in range(12))
    return f"LF-{body[:4]}-{body[4:8]}-{body[8:]}"


def _normalize(code: str) -> str:
    return "".join(character for character in code.upper() if character.isalnum())


def _digest(code: str) -> str:
    return hashlib.sha256(_normalize(code).encode("utf-8")).hexdigest()


def _as_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)
