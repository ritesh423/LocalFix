import os

DEFAULT_DATABASE_URL = "sqlite:///./localfix.db"
DEFAULT_EVIDENCE_DIRECTORY = "./uploads"


def get_database_url() -> str:
    return os.environ.get("DATABASE_URL", DEFAULT_DATABASE_URL)


def get_evidence_directory() -> str:
    return os.environ.get("EVIDENCE_DIRECTORY", DEFAULT_EVIDENCE_DIRECTORY)


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
