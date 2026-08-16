import os

DEFAULT_DATABASE_URL = "sqlite:///./localfix.db"
DEFAULT_EVIDENCE_DIRECTORY = "./uploads"


def get_database_url() -> str:
    return os.environ.get("DATABASE_URL", DEFAULT_DATABASE_URL)


def get_evidence_directory() -> str:
    return os.environ.get("EVIDENCE_DIRECTORY", DEFAULT_EVIDENCE_DIRECTORY)
