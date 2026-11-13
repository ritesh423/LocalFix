from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, Protocol
from uuid import UUID, uuid4

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError
from sqlalchemy import text
from sqlalchemy.orm import Session, sessionmaker

from app.database.config import (
    get_evidence_directory,
    get_evidence_storage_backend,
    get_s3_evidence_settings,
)
from app.database.models import EvidenceObjectRecord


@dataclass(frozen=True)
class StoredEvidence:
    content_type: str
    content: bytes


class EvidenceStorage(Protocol):
    def check_ready(self) -> None: ...

    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str: ...

    def delete(self, key: str) -> None: ...

    def read(self, key: str) -> StoredEvidence | None: ...


class LocalEvidenceStorage:
    def __init__(self, root: Path) -> None:
        self._root = root

    def check_ready(self) -> None:
        self._root.mkdir(parents=True, exist_ok=True)

    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str:
        extension = _extension_for(content_type)
        key = f"{ticket_id}/{uuid4()}.{extension}"
        destination = self._root / key
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(content)
        return key

    def delete(self, key: str) -> None:
        path = self._root / key
        path.unlink(missing_ok=True)
        parent = path.parent
        if parent != self._root and parent.exists() and not any(parent.iterdir()):
            parent.rmdir()

    def read(self, key: str) -> StoredEvidence | None:
        root = self._root.resolve()
        path = (self._root / key).resolve()
        if root not in path.parents or not path.is_file():
            return None
        content_type = _content_type_for(path.suffix.lower())
        if content_type is None:
            return None
        return StoredEvidence(content_type=content_type, content=path.read_bytes())


class DatabaseEvidenceStorage:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def check_ready(self) -> None:
        with self._session_factory() as session:
            session.execute(text("SELECT 1 FROM evidence_objects LIMIT 1"))

    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str:
        extension = _extension_for(content_type)
        key = f"database/{ticket_id}/{uuid4()}.{extension}"
        with self._session_factory.begin() as session:
            session.add(
                EvidenceObjectRecord(
                    key=key,
                    ticket_id=ticket_id,
                    content_type=content_type,
                    content=content,
                    created_at=datetime.now(UTC),
                )
            )
        return key

    def delete(self, key: str) -> None:
        with self._session_factory.begin() as session:
            record = session.get(EvidenceObjectRecord, key)
            if record is not None:
                session.delete(record)

    def read(self, key: str) -> StoredEvidence | None:
        with self._session_factory() as session:
            record = session.get(EvidenceObjectRecord, key)
            if record is None:
                return None
            return StoredEvidence(
                content_type=record.content_type,
                content=record.content,
            )


class S3EvidenceStorage:
    def __init__(
        self,
        bucket: str,
        region: str,
        endpoint_url: str | None = None,
        key_prefix: str = "completion-evidence",
        client: Any | None = None,
    ) -> None:
        clean_bucket = bucket.strip()
        if not clean_bucket:
            raise ValueError("An S3 evidence bucket is required.")
        self._bucket = clean_bucket
        self._key_prefix = key_prefix.strip().strip("/")
        self._client = client or boto3.client(
            "s3",
            region_name=region,
            endpoint_url=endpoint_url,
            config=Config(
                signature_version="s3v4",
                connect_timeout=5,
                read_timeout=20,
                retries={"max_attempts": 3, "mode": "standard"},
            ),
        )

    def check_ready(self) -> None:
        self._client.head_bucket(Bucket=self._bucket)

    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str:
        extension = _extension_for(content_type)
        object_name = f"{ticket_id}/{uuid4()}.{extension}"
        key = f"{self._key_prefix}/{object_name}" if self._key_prefix else object_name
        self._client.put_object(
            Bucket=self._bucket,
            Key=key,
            Body=content,
            ContentType=content_type,
        )
        return key

    def delete(self, key: str) -> None:
        self._client.delete_object(Bucket=self._bucket, Key=key)

    def read(self, key: str) -> StoredEvidence | None:
        try:
            response = self._client.get_object(Bucket=self._bucket, Key=key)
        except ClientError as error:
            error_code = str(error.response.get("Error", {}).get("Code", ""))
            if error_code in {"NoSuchKey", "NotFound", "404"}:
                return None
            raise

        body = response["Body"]
        try:
            content = body.read()
        finally:
            close = getattr(body, "close", None)
            if close is not None:
                close()
        content_type = str(response.get("ContentType", "")).lower()
        if content_type not in SUPPORTED_CONTENT_TYPES:
            content_type = _content_type_for(Path(key).suffix.lower()) or ""
        if content_type not in SUPPORTED_CONTENT_TYPES:
            return None
        return StoredEvidence(content_type=content_type, content=content)


class InMemoryEvidenceStorage:
    def __init__(self) -> None:
        self.content_by_key: dict[str, bytes] = {}
        self.content_type_by_key: dict[str, str] = {}

    def check_ready(self) -> None:
        return None

    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str:
        key = f"{ticket_id}/{uuid4()}.{_extension_for(content_type)}"
        self.content_by_key[key] = content
        self.content_type_by_key[key] = content_type
        return key

    def delete(self, key: str) -> None:
        self.content_by_key.pop(key, None)
        self.content_type_by_key.pop(key, None)

    def read(self, key: str) -> StoredEvidence | None:
        content = self.content_by_key.get(key)
        content_type = self.content_type_by_key.get(key)
        if content is None or content_type is None:
            return None
        return StoredEvidence(content_type=content_type, content=content)


def create_configured_evidence_storage(
    session_factory: sessionmaker[Session] | None = None,
) -> EvidenceStorage:
    backend = get_evidence_storage_backend()
    if backend == "local":
        return LocalEvidenceStorage(Path(get_evidence_directory()))
    if backend == "database":
        if session_factory is None:
            raise ValueError("Database evidence storage requires a database session.")
        return DatabaseEvidenceStorage(session_factory)
    if backend == "s3":
        settings = get_s3_evidence_settings()
        return S3EvidenceStorage(
            bucket=settings.bucket,
            region=settings.region,
            endpoint_url=settings.endpoint_url,
            key_prefix=settings.key_prefix,
        )
    raise ValueError(
        "EVIDENCE_STORAGE_BACKEND must be 'local', 'database', or 's3'."
    )


SUPPORTED_CONTENT_TYPES = {
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "image/heif",
}


def _extension_for(content_type: str) -> str:
    return {
        "image/jpeg": "jpg",
        "image/png": "png",
        "image/webp": "webp",
        "image/heic": "heic",
        "image/heif": "heif",
    }[content_type]


def _content_type_for(extension: str) -> str | None:
    return {
        ".jpg": "image/jpeg",
        ".png": "image/png",
        ".webp": "image/webp",
        ".heic": "image/heic",
        ".heif": "image/heif",
    }.get(extension)
