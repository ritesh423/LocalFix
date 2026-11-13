import io
import unittest
from uuid import uuid4

from botocore.exceptions import ClientError

from app.database.models import Base
from app.database.session import create_database_engine, create_session_factory
from app.storage.evidence import (
    DatabaseEvidenceStorage,
    S3EvidenceStorage,
    StoredEvidence,
)


class FakeS3Client:
    def __init__(self) -> None:
        self.objects: dict[tuple[str, str], tuple[str, bytes]] = {}
        self.checked_buckets: list[str] = []

    def head_bucket(self, Bucket: str) -> None:
        self.checked_buckets.append(Bucket)

    def put_object(
        self,
        Bucket: str,
        Key: str,
        Body: bytes,
        ContentType: str,
    ) -> None:
        self.objects[(Bucket, Key)] = (ContentType, Body)

    def get_object(self, Bucket: str, Key: str) -> dict[str, object]:
        stored = self.objects.get((Bucket, Key))
        if stored is None:
            raise ClientError(
                {"Error": {"Code": "NoSuchKey", "Message": "Not found"}},
                "GetObject",
            )
        content_type, content = stored
        return {"ContentType": content_type, "Body": io.BytesIO(content)}

    def delete_object(self, Bucket: str, Key: str) -> None:
        self.objects.pop((Bucket, Key), None)


class S3EvidenceStorageTest(unittest.TestCase):
    def setUp(self) -> None:
        self.client = FakeS3Client()
        self.storage = S3EvidenceStorage(
            bucket="localfix-evidence",
            region="auto",
            endpoint_url="https://account.r2.cloudflarestorage.com",
            key_prefix="completion-evidence",
            client=self.client,
        )

    def test_photo_is_written_and_read_from_the_private_bucket(self) -> None:
        ticket_id = uuid4()
        key = self.storage.save(ticket_id, "image/jpeg", b"photo-bytes")

        self.assertTrue(key.startswith(f"completion-evidence/{ticket_id}/"))
        self.assertTrue(key.endswith(".jpg"))
        self.assertEqual(
            self.storage.read(key),
            self.stored_evidence("image/jpeg", b"photo-bytes"),
        )

    def test_readiness_checks_that_the_private_bucket_is_reachable(self) -> None:
        self.storage.check_ready()

        self.assertEqual(self.client.checked_buckets, ["localfix-evidence"])

    def test_deleted_or_missing_photo_returns_none(self) -> None:
        key = self.storage.save(uuid4(), "image/png", b"photo-bytes")

        self.storage.delete(key)

        self.assertIsNone(self.storage.read(key))

    @staticmethod
    def stored_evidence(content_type: str, content: bytes) -> StoredEvidence:
        return StoredEvidence(content_type=content_type, content=content)


class DatabaseEvidenceStorageTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_database_engine("sqlite:///:memory:")
        Base.metadata.create_all(self.engine)
        self.session_factory = create_session_factory(self.engine)
        self.storage = DatabaseEvidenceStorage(self.session_factory)

    def tearDown(self) -> None:
        self.engine.dispose()

    def test_photo_survives_a_new_storage_instance(self) -> None:
        ticket_id = uuid4()
        self.storage.check_ready()
        key = self.storage.save(ticket_id, "image/webp", b"photo-bytes")

        self.assertTrue(key.startswith(f"database/{ticket_id}/"))
        self.assertEqual(
            DatabaseEvidenceStorage(self.session_factory).read(key),
            StoredEvidence("image/webp", b"photo-bytes"),
        )

    def test_delete_removes_photo(self) -> None:
        key = self.storage.save(uuid4(), "image/png", b"photo-bytes")

        self.storage.delete(key)

        self.assertIsNone(self.storage.read(key))


if __name__ == "__main__":
    unittest.main()
