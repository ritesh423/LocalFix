from pathlib import Path

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes.device_registrations import router as device_registrations_router
from app.api.routes.manager_tickets import router as manager_tickets_router
from app.api.routes.tickets import router as tickets_router
from app.api.routes.worker_tickets import router as worker_tickets_router
from app.database.config import get_database_url, get_evidence_directory
from app.database.session import create_database_engine, create_session_factory
from app.repositories.device_registrations import (
    DeviceRegistrationRepository,
    InMemoryDeviceRegistrationRepository,
)
from app.repositories.sqlalchemy_device_registrations import (
    SqlAlchemyDeviceRegistrationRepository,
)
from app.repositories.sqlalchemy_tickets import SqlAlchemyTicketRepository
from app.repositories.tickets import TicketRepository
from app.storage.evidence import EvidenceStorage, LocalEvidenceStorage


def create_app(
    repository: TicketRepository | None = None,
    evidence_storage: EvidenceStorage | None = None,
    device_registration_repository: DeviceRegistrationRepository | None = None,
) -> FastAPI:
    application = FastAPI(
        title="LocalFix API",
        version="0.9.0",
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
    application.state.ticket_repository = repository
    application.state.device_registration_repository = device_registration_repository
    application.state.evidence_storage = evidence_storage or LocalEvidenceStorage(
        Path(get_evidence_directory())
    )
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

    return application


app = create_app()
