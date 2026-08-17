from dataclasses import dataclass
from pathlib import Path
from typing import Protocol
from uuid import UUID, uuid4


@dataclass(frozen=True)
class StoredEvidence:
    content_type: str
    content: bytes


class EvidenceStorage(Protocol):
    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str: ...

    def delete(self, key: str) -> None: ...

    def read(self, key: str) -> StoredEvidence | None: ...


class LocalEvidenceStorage:
    def __init__(self, root: Path) -> None:
        self._root = root

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


class InMemoryEvidenceStorage:
    def __init__(self) -> None:
        self.content_by_key: dict[str, bytes] = {}
        self.content_type_by_key: dict[str, str] = {}

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
