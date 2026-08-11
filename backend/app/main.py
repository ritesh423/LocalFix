from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes.tickets import router as tickets_router
from app.repositories.tickets import InMemoryTicketRepository, TicketRepository


def create_app(repository: TicketRepository | None = None) -> FastAPI:
    application = FastAPI(
        title="LocalFix API",
        version="0.1.0",
        description="Apartment maintenance workflow API.",
    )
    application.state.ticket_repository = (
        repository if repository is not None else InMemoryTicketRepository()
    )
    application.include_router(tickets_router)

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
