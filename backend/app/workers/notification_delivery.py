import argparse
import logging
import os
import time

from dotenv import load_dotenv

from app.database.config import get_database_url
from app.database.session import create_database_engine, create_session_factory
from app.gateways.push import FirebasePushGateway
from app.repositories.sqlalchemy_device_registrations import (
    SqlAlchemyDeviceRegistrationRepository,
)
from app.repositories.sqlalchemy_notification_outbox import (
    SqlAlchemyNotificationOutboxRepository,
)
from app.services.notification_delivery import NotificationDeliveryService

LOGGER = logging.getLogger("localfix.notifications")


def deliver_once() -> None:
    engine = create_database_engine(get_database_url())
    session_factory = create_session_factory(engine)
    service = NotificationDeliveryService(
        outbox=SqlAlchemyNotificationOutboxRepository(session_factory),
        device_registrations=SqlAlchemyDeviceRegistrationRepository(session_factory),
        push_gateway=FirebasePushGateway(
            project_id=os.getenv("LOCALFIX_FIREBASE_PROJECT_ID")
        ),
    )
    try:
        summary = service.deliver_ready()
        LOGGER.info(
            "Notification delivery processed=%s sent=%s rescheduled=%s failed=%s",
            summary.processed,
            summary.sent,
            summary.rescheduled,
            summary.failed,
        )
    finally:
        engine.dispose()


def main() -> None:
    load_dotenv()
    parser = argparse.ArgumentParser(description="Deliver LocalFix notifications.")
    parser.add_argument(
        "--once",
        action="store_true",
        help="Process one batch and stop instead of polling continuously.",
    )
    arguments = parser.parse_args()
    logging.basicConfig(level=logging.INFO)

    if arguments.once:
        deliver_once()
        return

    poll_seconds = max(5, int(os.getenv("LOCALFIX_NOTIFICATION_POLL_SECONDS", "15")))
    while True:
        try:
            deliver_once()
        except Exception:
            LOGGER.exception("Notification delivery batch failed.")
        time.sleep(poll_seconds)


if __name__ == "__main__":
    main()
