import hashlib
import secrets
from datetime import UTC, datetime

INVITE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"


def generate_invite_code(prefix: str = "LF") -> str:
    body = "".join(secrets.choice(INVITE_ALPHABET) for _ in range(12))
    return f"{prefix}-{body[:4]}-{body[4:8]}-{body[8:]}"


def digest_invite_code(code: str) -> str:
    normalized = "".join(character for character in code.upper() if character.isalnum())
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def as_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value.astimezone(UTC)
