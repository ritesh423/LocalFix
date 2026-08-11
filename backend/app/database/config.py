import os

DEFAULT_DATABASE_URL = "sqlite:///./localfix.db"


def get_database_url() -> str:
    return os.environ.get("DATABASE_URL", DEFAULT_DATABASE_URL)
