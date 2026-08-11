import tempfile
import unittest
from pathlib import Path
from uuid import UUID, uuid4

from alembic.config import Config

from alembic import command
from app.api.dependencies import DEMO_RESIDENT_CONTEXT
from app.database.session import create_database_engine, create_session_factory
from app.domain.tickets import (
    AccessWindow,
    ResidentContext,
    ServiceCategory,
    UrgencySuggestion,
)
from app.repositories.sqlalchemy_tickets import SqlAlchemyTicketRepository
from app.services.tickets import CreateTicketCommand, TicketService

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
            SqlAlchemyTicketRepository(create_session_factory(first_engine))
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

    def upgrade_database(self) -> None:
        config = Config(str(BACKEND_ROOT / "alembic.ini"))
        config.set_main_option("script_location", str(BACKEND_ROOT / "alembic"))
        config.set_main_option("sqlalchemy.url", self.database_url)
        command.upgrade(config, "head")

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
