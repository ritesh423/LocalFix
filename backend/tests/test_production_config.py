import os
import unittest
from unittest.mock import patch

from app.database.config import (
    InvalidProductionConfigurationError,
    get_database_url,
    get_s3_evidence_settings,
    validate_production_configuration,
)


class ProductionConfigurationTest(unittest.TestCase):
    def test_render_postgres_url_is_normalized_for_psycopg(self) -> None:
        with patch.dict(
            os.environ,
            {"DATABASE_URL": "postgresql://user:secret@postgres/localfix"},
        ):
            self.assertEqual(
                get_database_url(),
                "postgresql+psycopg://user:secret@postgres/localfix",
            )

    def test_r2_settings_are_read_without_exposing_credentials(self) -> None:
        with patch.dict(
            os.environ,
            {
                "EVIDENCE_S3_BUCKET": "localfix-evidence",
                "EVIDENCE_S3_ENDPOINT_URL": (
                    "https://account.r2.cloudflarestorage.com"
                ),
                "EVIDENCE_S3_REGION": "auto",
                "EVIDENCE_S3_KEY_PREFIX": "/pilot/photos/",
            },
        ):
            settings = get_s3_evidence_settings()

        self.assertEqual(settings.bucket, "localfix-evidence")
        self.assertEqual(settings.region, "auto")
        self.assertEqual(settings.key_prefix, "pilot/photos")

    def test_production_refuses_sqlite_or_local_photo_storage(self) -> None:
        with (
            patch.dict(
                os.environ,
                {
                    "LOCALFIX_ENV": "production",
                    "DATABASE_URL": "sqlite:///./localfix.db",
                    "LOCALFIX_AUTH_REQUIRED": "false",
                    "LOCALFIX_FIREBASE_PROJECT_ID": "",
                    "EVIDENCE_STORAGE_BACKEND": "local",
                    "EVIDENCE_S3_BUCKET": "",
                },
                clear=True,
            ),
            self.assertRaises(InvalidProductionConfigurationError) as raised,
        ):
            validate_production_configuration()

        message = str(raised.exception)
        self.assertIn("PostgreSQL", message)
        self.assertIn("LOCALFIX_AUTH_REQUIRED", message)
        self.assertIn("EVIDENCE_STORAGE_BACKEND", message)

    def test_complete_production_environment_passes_startup_validation(self) -> None:
        with patch.dict(
            os.environ,
            {
                "LOCALFIX_ENV": "production",
                "DATABASE_URL": "postgresql://user:secret@postgres/localfix",
                "LOCALFIX_AUTH_REQUIRED": "true",
                "LOCALFIX_FIREBASE_PROJECT_ID": "localfix-be333",
                "EVIDENCE_STORAGE_BACKEND": "s3",
                "EVIDENCE_S3_BUCKET": "localfix-evidence",
            },
            clear=True,
        ):
            validate_production_configuration()

    def test_personal_hosting_accepts_database_photo_storage(self) -> None:
        with patch.dict(
            os.environ,
            {
                "LOCALFIX_ENV": "personal",
                "DATABASE_URL": "postgresql://user:secret@postgres/localfix",
                "LOCALFIX_AUTH_REQUIRED": "true",
                "LOCALFIX_FIREBASE_PROJECT_ID": "localfix-be333",
                "LOCALFIX_FIREBASE_CREDENTIALS_JSON": '{"type":"service_account"}',
                "EVIDENCE_STORAGE_BACKEND": "database",
            },
            clear=True,
        ):
            validate_production_configuration()


if __name__ == "__main__":
    unittest.main()
