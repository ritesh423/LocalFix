from pathlib import Path
from typing import Protocol
from uuid import UUID, uuid4


class EvidenceStorage(Protocol):
    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str: ...

    def delete(self, key: str) -> None: ...


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


class InMemoryEvidenceStorage:
    def __init__(self) -> None:
        self.content_by_key: dict[str, bytes] = {}

    def save(self, ticket_id: UUID, content_type: str, content: bytes) -> str:
        key = f"{ticket_id}/{uuid4()}.{_extension_for(content_type)}"
        self.content_by_key[key] = content
        return key

    def delete(self, key: str) -> None:
        self.content_by_key.pop(key, None)


def _extension_for(content_type: str) -> str:
    return {
        "image/jpeg": "jpg",
        "image/png": "png",
        "image/webp": "webp",
        "image/heic": "heic",
        "image/heif": "heif",
    }[content_type]
