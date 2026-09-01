from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from uuid import UUID, uuid4

from app.domain.auth import AuthenticatedIdentity, PropertyMembership, StaffInvite
from app.domain.ticket_workflow import UserRole
from app.domain.tickets import ServiceCategory, Worker
from app.repositories.memberships import MembershipRepository
from app.repositories.properties import PropertyRepository
from app.repositories.staff_invites import StaffInviteRepository
from app.repositories.workers import WorkerRepository
from app.services.invite_codes import as_utc, digest_invite_code, generate_invite_code
from app.services.memberships import (
    MembershipConflictError,
    MembershipProvisioningService,
)


class InvalidStaffInviteError(ValueError):
    pass


class ExpiredStaffInviteError(ValueError):
    pass


class StaffInviteAlreadyUsedError(ValueError):
    pass


class StaffVerifiedEmailRequiredError(ValueError):
    pass


@dataclass(frozen=True)
class CreatedWorkerInvite:
    worker: Worker
    invite: StaffInvite
    code: str


class StaffInviteService:
    def __init__(
        self,
        invites: StaffInviteRepository,
        memberships: MembershipRepository,
        properties: PropertyRepository,
        workers: WorkerRepository,
    ) -> None:
        self._invites = invites
        self._memberships = memberships
        self._properties = properties
        self._workers = workers

    def create_worker_invite(
        self,
        property_id: UUID,
        name: str,
        specialty: ServiceCategory,
        valid_for: timedelta = timedelta(days=7),
        now: datetime | None = None,
    ) -> CreatedWorkerInvite:
        property_ = self._properties.get_property(property_id)
        clean_name = " ".join(name.split())
        if property_ is None or not property_.is_active:
            raise InvalidStaffInviteError("Property is not registered or active.")
        if not 2 <= len(clean_name) <= 120:
            raise InvalidStaffInviteError(
                "Worker name must contain between 2 and 120 characters."
            )
        if valid_for <= timedelta(0):
            raise InvalidStaffInviteError("Invite lifetime must be positive.")

        created_at = as_utc(now or datetime.now(UTC))
        worker = Worker(
            id=uuid4(),
            property_id=property_id,
            name=clean_name,
            specialty=specialty,
            is_active=True,
        )
        code = generate_invite_code("LFW")
        invite = StaffInvite(
            id=uuid4(),
            property_id=property_id,
            user_id=worker.id,
            role=UserRole.WORKER,
            code_digest=digest_invite_code(code),
            expires_at=created_at + valid_for,
            created_at=created_at,
        )
        self._workers.save(worker)
        self._invites.save(invite)
        return CreatedWorkerInvite(worker=worker, invite=invite, code=code)

    def create_manager_invite(
        self,
        property_id: UUID,
        valid_for: timedelta = timedelta(days=7),
        now: datetime | None = None,
    ) -> tuple[StaffInvite, str]:
        property_ = self._properties.get_property(property_id)
        if property_ is None or not property_.is_active:
            raise InvalidStaffInviteError("Property is not registered or active.")
        if valid_for <= timedelta(0):
            raise InvalidStaffInviteError("Invite lifetime must be positive.")
        created_at = as_utc(now or datetime.now(UTC))
        code = generate_invite_code("LFM")
        invite = StaffInvite(
            id=uuid4(),
            property_id=property_id,
            user_id=uuid4(),
            role=UserRole.MANAGER,
            code_digest=digest_invite_code(code),
            expires_at=created_at + valid_for,
            created_at=created_at,
        )
        self._invites.save(invite)
        return invite, code

    def redeem(
        self,
        code: str,
        identity: AuthenticatedIdentity,
        now: datetime | None = None,
    ) -> PropertyMembership:
        if not identity.email_verified:
            raise StaffVerifiedEmailRequiredError(
                "Verify your email address before joining a workspace."
            )
        invite = self._invites.find_by_digest(digest_invite_code(code))
        if invite is None or invite.revoked_at is not None:
            raise InvalidStaffInviteError("Invite code is invalid.")
        if invite.role not in {UserRole.MANAGER, UserRole.WORKER}:
            raise InvalidStaffInviteError("Invite code has an unsupported role.")

        redeemed_at = as_utc(now or datetime.now(UTC))
        if as_utc(invite.expires_at) <= redeemed_at:
            raise ExpiredStaffInviteError("Invite code has expired.")
        if invite.claimed_by_firebase_uid not in (None, identity.firebase_uid):
            raise StaffInviteAlreadyUsedError("Invite code has already been used.")
        if invite.role is UserRole.WORKER:
            worker = self._workers.get(invite.property_id, invite.user_id)
            if worker is None or not worker.is_active:
                raise InvalidStaffInviteError("Worker is no longer active.")

        existing = self._memberships.find(
            identity.firebase_uid,
            invite.property_id,
            invite.role,
        )
        if existing is not None and existing.user_id != invite.user_id:
            raise MembershipConflictError(
                "This account already has a different staff membership "
                "for this property."
            )

        claimed = self._invites.claim(invite.id, identity.firebase_uid, redeemed_at)
        if claimed is None:
            raise StaffInviteAlreadyUsedError("Invite code has already been used.")
        return (
            MembershipProvisioningService(
                self._memberships,
                self._properties,
            )
            .provision(
                firebase_uid=identity.firebase_uid,
                property_id=invite.property_id,
                role=invite.role,
                user_id=invite.user_id,
            )
            .membership
        )
