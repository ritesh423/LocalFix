import tempfile
import unittest
from pathlib import Path
from uuid import UUID, uuid4

from alembic.config import Config
from sqlalchemy import inspect, text

from alembic import command
from app.api.dependencies import (
    DEMO_MANAGER_CONTEXT,
    DEMO_RESIDENT_CONTEXT,
    DEMO_WORKER_CONTEXT,
    DEMO_WORKERS,
)
from app.database.session import create_database_engine, create_session_factory
from app.domain.tickets import (
    AccessWindow,
    ResidentContext,
    ServiceCategory,
    TicketPriority,
    UrgencySuggestion,
)
from app.repositories.sqlalchemy_tickets import SqlAlchemyTicketRepository
from app.services.tickets import (
    AssignTicketCommand,
    CompletionPhoto,
    CreateTicketCommand,
    StartTicketCommand,
    SubmitCompletionCommand,
    TicketService,
)
from app.storage.evidence import InMemoryEvidenceStorage

BACKEND_ROOT = Path(__file__).resolve().parents[1]


class SqlAlchemyTicketRepositoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        database_path = Path(self.temporary_directory.name) / "localfix-test.db"
        self.database_url = f"sqlite+pysqlite:///{database_path}"
        self.upgrade_database()

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_ticket_survives_repository_and_engine_recreation(self) -> None:
        command = self.ticket_command()
        first_engine = create_database_engine(self.database_url)
        first_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(first_engine)),
            workers=DEMO_WORKERS,
        )
        created = first_service.create_ticket(
            command,
            DEMO_RESIDENT_CONTEXT,
        ).ticket
        first_engine.dispose()

        second_engine = create_database_engine(self.database_url)
        second_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(second_engine))
        )
        restored = second_service.get_ticket(created.id, DEMO_RESIDENT_CONTEXT)
        retry = second_service.create_ticket(command, DEMO_RESIDENT_CONTEXT)
        second_engine.dispose()

        self.assertIsNotNone(restored)
        self.assertEqual(restored.id, created.id)
        self.assertEqual(restored.title, "Leaking kitchen tap")
        self.assertFalse(retry.was_created)
        self.assertEqual(retry.ticket.id, created.id)

    def test_sql_repository_hides_another_residents_ticket(self) -> None:
        engine = create_database_engine(self.database_url)
        service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(engine))
        )
        other_resident = ResidentContext(
            user_id=UUID("10000000-0000-0000-0000-000000000999"),
            property_id=DEMO_RESIDENT_CONTEXT.property_id,
            unit_id=UUID("30000000-0000-0000-0000-000000000999"),
        )
        hidden = service.create_ticket(self.ticket_command(), other_resident).ticket

        visible_tickets = service.list_tickets(DEMO_RESIDENT_CONTEXT)
        visible_detail = service.get_ticket(hidden.id, DEMO_RESIDENT_CONTEXT)
        engine.dispose()

        self.assertEqual(visible_tickets, [])
        self.assertIsNone(visible_detail)

    def test_manager_assignment_survives_engine_recreation(self) -> None:
        first_engine = create_database_engine(self.database_url)
        first_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(first_engine)),
            workers=DEMO_WORKERS,
        )
        created = first_service.create_ticket(
            self.ticket_command(),
            DEMO_RESIDENT_CONTEXT,
        ).ticket
        assigned = first_service.assign_ticket(
            created.id,
            AssignTicketCommand(
                expected_version=1,
                priority=TicketPriority.URGENT,
                worker_id=UUID("40000000-0000-0000-0000-000000000001"),
            ),
            DEMO_MANAGER_CONTEXT,
        )
        first_engine.dispose()

        second_engine = create_database_engine(self.database_url)
        second_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(second_engine))
        )
        restored = second_service.get_ticket(created.id, DEMO_RESIDENT_CONTEXT)
        second_engine.dispose()

        self.assertIsNotNone(restored)
        self.assertEqual(assigned.version, 2)
        self.assertEqual(restored.priority, TicketPriority.URGENT)
        self.assertEqual(restored.assigned_worker, "Arun Kumar")
        self.assertEqual(restored.status.value, "assigned")

    def test_worker_start_survives_engine_recreation(self) -> None:
        first_engine = create_database_engine(self.database_url)
        first_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(first_engine)),
            workers=DEMO_WORKERS,
        )
        created = first_service.create_ticket(
            self.ticket_command(),
            DEMO_RESIDENT_CONTEXT,
        ).ticket
        assigned = first_service.assign_ticket(
            created.id,
            AssignTicketCommand(
                expected_version=1,
                priority=TicketPriority.SOON,
                worker_id=DEMO_WORKER_CONTEXT.worker_id,
            ),
            DEMO_MANAGER_CONTEXT,
        )
        started = first_service.start_ticket(
            assigned.id,
            StartTicketCommand(expected_version=assigned.version),
            DEMO_WORKER_CONTEXT,
        )
        first_engine.dispose()

        second_engine = create_database_engine(self.database_url)
        second_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(second_engine))
        )
        restored = second_service.get_ticket(started.id, DEMO_RESIDENT_CONTEXT)
        worker_queue = second_service.list_worker_tickets(DEMO_WORKER_CONTEXT)
        second_engine.dispose()

        self.assertIsNotNone(restored)
        self.assertEqual(restored.status.value, "in_progress")
        self.assertEqual(restored.version, 3)
        self.assertEqual([ticket.id for ticket in worker_queue], [started.id])

    def test_worker_completion_evidence_survives_engine_recreation(self) -> None:
        evidence_storage = InMemoryEvidenceStorage()
        first_engine = create_database_engine(self.database_url)
        first_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(first_engine)),
            workers=DEMO_WORKERS,
            evidence_storage=evidence_storage,
        )
        created = first_service.create_ticket(
            self.ticket_command(),
            DEMO_RESIDENT_CONTEXT,
        ).ticket
        assigned = first_service.assign_ticket(
            created.id,
            AssignTicketCommand(
                expected_version=1,
                priority=TicketPriority.SOON,
                worker_id=DEMO_WORKER_CONTEXT.worker_id,
            ),
            DEMO_MANAGER_CONTEXT,
        )
        started = first_service.start_ticket(
            assigned.id,
            StartTicketCommand(expected_version=assigned.version),
            DEMO_WORKER_CONTEXT,
        )
        completed = first_service.submit_completion(
            started.id,
            SubmitCompletionCommand(
                expected_version=started.version,
                completion_note="Replaced the washer and checked for leaks.",
                parts_used=("Rubber washer",),
                photo=CompletionPhoto("image/jpeg", b"repair evidence"),
            ),
            DEMO_WORKER_CONTEXT,
        )
        first_engine.dispose()

        second_engine = create_database_engine(self.database_url)
        second_service = TicketService(
            SqlAlchemyTicketRepository(create_session_factory(second_engine))
        )
        restored = second_service.get_ticket(completed.id, DEMO_RESIDENT_CONTEXT)
        second_engine.dispose()

        self.assertIsNotNone(restored)
        self.assertEqual(restored.status.value, "awaiting_confirmation")
        self.assertEqual(restored.completion_note, completed.completion_note)
        self.assertEqual(restored.parts_used, ("Rubber washer",))
        self.assertIsNotNone(restored.completion_photo_key)
        self.assertEqual(len(evidence_storage.content_by_key), 1)

    def test_manager_assignment_migration_preserves_existing_tickets(self) -> None:
        config = self.alembic_config()
        command.downgrade(config, "base")
        command.upgrade(config, "20260811_01")
        ticket_id = uuid4()
        engine = create_database_engine(self.database_url)
        with engine.begin() as connection:
            connection.execute(
                text(
                    """
                    INSERT INTO tickets (
                        id, client_request_id, property_id, unit_id, resident_id,
                        title, description, category, urgency_suggestion,
                        access_window, status, version, assigned_worker,
                        created_at, updated_at
                    ) VALUES (
                        :id, :client_request_id, :property_id, :unit_id, :resident_id,
                        :title, :description, :category, :urgency_suggestion,
                        :access_window, :status, :version, :assigned_worker,
                        :created_at, :updated_at
                    )
                    """
                ),
                {
                    "id": ticket_id.hex,
                    "client_request_id": uuid4().hex,
                    "property_id": DEMO_RESIDENT_CONTEXT.property_id.hex,
                    "unit_id": DEMO_RESIDENT_CONTEXT.unit_id.hex,
                    "resident_id": DEMO_RESIDENT_CONTEXT.user_id.hex,
                    "title": "Existing ticket",
                    "description": "This ticket existed before manager assignment support.",
                    "category": "other",
                    "urgency_suggestion": "routine",
                    "access_window": "anytime",
                    "status": "open",
                    "version": 1,
                    "assigned_worker": None,
                    "created_at": "2026-08-16 00:00:00.000000",
                    "updated_at": "2026-08-16 00:00:00.000000",
                },
            )
        engine.dispose()

        command.upgrade(config, "head")

        migrated_engine = create_database_engine(self.database_url)
        column_names = {
            column["name"] for column in inspect(migrated_engine).get_columns("tickets")
        }
        index_names = {
            index["name"] for index in inspect(migrated_engine).get_indexes("tickets")
        }
        with migrated_engine.connect() as connection:
            restored_title = connection.scalar(
                text("SELECT title FROM tickets WHERE id = :id"),
                {"id": ticket_id.hex},
            )
        migrated_engine.dispose()

        self.assertIn("priority", column_names)
        self.assertIn("assigned_worker_id", column_names)
        self.assertIn("completion_note", column_names)
        self.assertIn("parts_used", column_names)
        self.assertIn("completion_photo_key", column_names)
        self.assertIn("ix_tickets_worker_status_updated", index_names)
        self.assertEqual(restored_title, "Existing ticket")

    def alembic_config(self) -> Config:
        config = Config(str(BACKEND_ROOT / "alembic.ini"))
        config.set_main_option("script_location", str(BACKEND_ROOT / "alembic"))
        config.set_main_option("sqlalchemy.url", self.database_url)
        return config

    def upgrade_database(self) -> None:
        command.upgrade(self.alembic_config(), "head")

    @staticmethod
    def ticket_command() -> CreateTicketCommand:
        return CreateTicketCommand(
            client_request_id=uuid4(),
            title="Leaking kitchen tap",
            description="The tap keeps dripping even when fully closed.",
            category=ServiceCategory.PLUMBING,
            urgency_suggestion=UrgencySuggestion.SOON,
            access_window=AccessWindow.MORNING,
        )


if __name__ == "__main__":
    unittest.main()
