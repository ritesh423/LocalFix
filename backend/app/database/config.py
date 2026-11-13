import os
from dataclasses import dataclass

DEFAULT_DATABASE_URL = "sqlite:///./localfix.db"
DEFAULT_EVIDENCE_DIRECTORY = "./uploads"


def get_database_url() -> str:
    value = os.environ.get("DATABASE_URL", DEFAULT_DATABASE_URL).strip()
    if value.startswith("postgres://"):
        return value.replace("postgres://", "postgresql+psycopg://", 1)
    if value.startswith("postgresql://"):
        return value.replace("postgresql://", "postgresql+psycopg://", 1)
    return value


def get_evidence_directory() -> str:
    return os.environ.get("EVIDENCE_DIRECTORY", DEFAULT_EVIDENCE_DIRECTORY)


def get_evidence_storage_backend() -> str:
    return os.environ.get("EVIDENCE_STORAGE_BACKEND", "local").strip().lower()


def is_authentication_required() -> bool:
    return os.environ.get("LOCALFIX_AUTH_REQUIRED", "false").strip().lower() in {
        "1",
        "true",
        "yes",
        "on",
    }


def get_firebase_project_id() -> str | None:
    value = os.environ.get("LOCALFIX_FIREBASE_PROJECT_ID", "").strip()
    return value or None


def get_firebase_credentials_json() -> str | None:
    value = os.environ.get("LOCALFIX_FIREBASE_CREDENTIALS_JSON", "").strip()
    return value or None


def get_environment() -> str:
    return os.environ.get("LOCALFIX_ENV", "development").strip().lower()


@dataclass(frozen=True)
class S3EvidenceSettings:
    bucket: str
    region: str
    endpoint_url: str | None
    key_prefix: str


def get_s3_evidence_settings() -> S3EvidenceSettings:
    return S3EvidenceSettings(
        bucket=os.environ.get("EVIDENCE_S3_BUCKET", "").strip(),
        region=os.environ.get("EVIDENCE_S3_REGION", "auto").strip() or "auto",
        endpoint_url=(os.environ.get("EVIDENCE_S3_ENDPOINT_URL", "").strip() or None),
        key_prefix=(
            os.environ.get("EVIDENCE_S3_KEY_PREFIX", "completion-evidence")
            .strip()
            .strip("/")
        ),
    )


class InvalidProductionConfigurationError(RuntimeError):
    pass


def validate_production_configuration() -> None:
    environment = get_environment()
    if environment not in {"production", "personal"}:
        return

    errors: list[str] = []
    if not get_database_url().startswith("postgresql+psycopg://"):
        errors.append("DATABASE_URL must point to PostgreSQL when hosted")
    if not is_authentication_required():
        errors.append("LOCALFIX_AUTH_REQUIRED must be true when hosted")
    if get_firebase_project_id() is None:
        errors.append("LOCALFIX_FIREBASE_PROJECT_ID is required when hosted")
    if environment == "personal" and get_firebase_credentials_json() is None:
        errors.append("LOCALFIX_FIREBASE_CREDENTIALS_JSON is required for personal hosting")
    backend = get_evidence_storage_backend()
    if environment == "production" and backend != "s3":
        errors.append("EVIDENCE_STORAGE_BACKEND must be s3 in production")
    elif environment == "personal" and backend not in {"s3", "database"}:
        errors.append("EVIDENCE_STORAGE_BACKEND must be s3 or database when hosted")
    if backend == "s3" and not get_s3_evidence_settings().bucket:
        errors.append("EVIDENCE_S3_BUCKET is required for s3 storage")

    if errors:
        raise InvalidProductionConfigurationError("; ".join(errors) + ".")
