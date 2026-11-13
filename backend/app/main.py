from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy import text

from app.api.routes.auth import router as auth_router
from app.api.routes.device_registrations import router as device_registrations_router
from app.api.routes.manager_tickets import router as manager_tickets_router
from app.api.routes.tickets import router as tickets_router
from app.api.routes.worker_tickets import router as worker_tickets_router
from app.database.config import (
    get_database_url,
    get_firebase_credentials_json,
    get_firebase_project_id,
    is_authentication_required,
    validate_production_configuration,
)
from app.database.session import create_database_engine, create_session_factory
from app.demo import DEMO_WORKERS
from app.gateways.identity import FirebaseIdentityTokenVerifier, IdentityTokenVerifier
from app.repositories.device_registrations import (
    DeviceRegistrationRepository,
    InMemoryDeviceRegistrationRepository,
)
from app.repositories.memberships import (
    InMemoryMembershipRepository,
    MembershipRepository,
)
from app.repositories.notification_outbox import (
    InMemoryNotificationOutboxRepository,
    NotificationOutboxRepository,
)
from app.repositories.properties import InMemoryPropertyRepository, PropertyRepository
from app.repositories.resident_invites import (
    InMemoryResidentInviteRepository,
    ResidentInviteRepository,
)
from app.repositories.sqlalchemy_device_registrations import (
    SqlAlchemyDeviceRegistrationRepository,
)
from app.repositories.sqlalchemy_memberships import SqlAlchemyMembershipRepository
from app.repositories.sqlalchemy_notification_outbox import (
    SqlAlchemyNotificationOutboxRepository,
)
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.repositories.sqlalchemy_resident_invites import (
    SqlAlchemyResidentInviteRepository,
)
from app.repositories.sqlalchemy_staff_invites import SqlAlchemyStaffInviteRepository
from app.repositories.sqlalchemy_tickets import SqlAlchemyTicketRepository
from app.repositories.sqlalchemy_workers import SqlAlchemyWorkerRepository
from app.repositories.staff_invites import (
    InMemoryStaffInviteRepository,
    StaffInviteRepository,
)
from app.repositories.tickets import InMemoryTicketRepository, TicketRepository
from app.repositories.workers import InMemoryWorkerRepository, WorkerRepository
from app.storage.evidence import EvidenceStorage, create_configured_evidence_storage


def create_app(
    repository: TicketRepository | None = None,
    evidence_storage: EvidenceStorage | None = None,
    device_registration_repository: DeviceRegistrationRepository | None = None,
    notification_outbox_repository: NotificationOutboxRepository | None = None,
    membership_repository: MembershipRepository | None = None,
    property_repository: PropertyRepository | None = None,
    resident_invite_repository: ResidentInviteRepository | None = None,
    staff_invite_repository: StaffInviteRepository | None = None,
    worker_repository: WorkerRepository | None = None,
    identity_token_verifier: IdentityTokenVerifier | None = None,
    authentication_required: bool = False,
) -> FastAPI:
    validate_production_configuration()
    application = FastAPI(
        title="LocalFix API",
        version="0.15.0",
        description="Apartment maintenance workflow API.",
    )
    session_factory = None
    if repository is None:
        engine = create_database_engine(get_database_url())
        application.state.database_engine = engine
        session_factory = create_session_factory(engine)
        repository = SqlAlchemyTicketRepository(session_factory)
    if device_registration_repository is None:
        device_registration_repository = (
            SqlAlchemyDeviceRegistrationRepository(session_factory)
            if session_factory is not None
            else InMemoryDeviceRegistrationRepository()
        )
    if notification_outbox_repository is None:
        if session_factory is not None:
            notification_outbox_repository = SqlAlchemyNotificationOutboxRepository(
                session_factory
            )
        else:
            in_memory_jobs = (
                repository.notification_jobs
                if isinstance(repository, InMemoryTicketRepository)
                else []
            )
            notification_outbox_repository = InMemoryNotificationOutboxRepository(
                in_memory_jobs
            )
    if membership_repository is None:
        membership_repository = (
            SqlAlchemyMembershipRepository(session_factory)
            if session_factory is not None
            else InMemoryMembershipRepository()
        )
    if property_repository is None:
        property_repository = (
            SqlAlchemyPropertyRepository(session_factory)
            if session_factory is not None
            else InMemoryPropertyRepository()
        )
    if resident_invite_repository is None:
        resident_invite_repository = (
            SqlAlchemyResidentInviteRepository(session_factory)
            if session_factory is not None
            else InMemoryResidentInviteRepository()
        )
    if staff_invite_repository is None:
        staff_invite_repository = (
            SqlAlchemyStaffInviteRepository(session_factory)
            if session_factory is not None
            else InMemoryStaffInviteRepository()
        )
    if worker_repository is None:
        worker_repository = (
            SqlAlchemyWorkerRepository(session_factory)
            if session_factory is not None
            else InMemoryWorkerRepository(DEMO_WORKERS)
        )
    application.state.ticket_repository = repository
    application.state.device_registration_repository = device_registration_repository
    application.state.notification_outbox_repository = notification_outbox_repository
    application.state.membership_repository = membership_repository
    application.state.property_repository = property_repository
    application.state.resident_invite_repository = resident_invite_repository
    application.state.staff_invite_repository = staff_invite_repository
    application.state.worker_repository = worker_repository
    application.state.identity_token_verifier = (
        identity_token_verifier
        or FirebaseIdentityTokenVerifier(
            project_id=get_firebase_project_id(),
            credentials_json=get_firebase_credentials_json(),
        )
    )
    application.state.authentication_required = authentication_required
    application.state.evidence_storage = (
        evidence_storage or create_configured_evidence_storage(session_factory)
    )
    application.include_router(auth_router)
    application.include_router(tickets_router)
    application.include_router(device_registrations_router)
    application.include_router(manager_tickets_router)
    application.include_router(worker_tickets_router)

    @application.exception_handler(RequestValidationError)
    async def request_validation_error(
        _request: Request,
        error: RequestValidationError,
    ) -> JSONResponse:
        fields = [
            {
                "location": ".".join(str(part) for part in issue["loc"]),
                "message": issue["msg"],
            }
            for issue in error.errors()
        ]
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "error": {
                    "code": "validation_error",
                    "message": "Request validation failed.",
                    "fields": fields,
                }
            },
        )

    @application.get("/health", tags=["system"])
    def health() -> dict[str, str]:
        return {"status": "ok"}

    @application.get("/health/ready", tags=["system"])
    def readiness() -> dict[str, str]:
        engine = getattr(application.state, "database_engine", None)
        if engine is not None:
            try:
                with engine.connect() as connection:
                    connection.execute(text("SELECT 1"))
            except Exception as error:
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail={
                        "code": "database_unavailable",
                        "message": "The database is not ready.",
                    },
                ) from error
        try:
            application.state.evidence_storage.check_ready()
        except Exception as error:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail={
                    "code": "evidence_storage_unavailable",
                    "message": "Completion photo storage is not ready.",
                },
            ) from error
        return {"status": "ready"}

    return application


load_dotenv()
app = create_app(authentication_required=is_authentication_required())
