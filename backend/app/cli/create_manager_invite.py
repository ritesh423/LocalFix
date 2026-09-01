import argparse
from datetime import timedelta
from uuid import UUID

from dotenv import load_dotenv

from app.database.config import get_database_url
from app.database.session import create_database_engine, create_session_factory
from app.repositories.sqlalchemy_memberships import SqlAlchemyMembershipRepository
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.repositories.sqlalchemy_staff_invites import SqlAlchemyStaffInviteRepository
from app.repositories.sqlalchemy_workers import SqlAlchemyWorkerRepository
from app.services.staff_invites import InvalidStaffInviteError, StaffInviteService


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Create the first one-time manager invite for a property."
    )
    parser.add_argument("--property-id", required=True, type=UUID)
    parser.add_argument("--valid-days", type=int, default=7)
    arguments = parser.parse_args()

    load_dotenv()
    engine = create_database_engine(get_database_url())
    session_factory = create_session_factory(engine)
    service = StaffInviteService(
        invites=SqlAlchemyStaffInviteRepository(session_factory),
        memberships=SqlAlchemyMembershipRepository(session_factory),
        properties=SqlAlchemyPropertyRepository(session_factory),
        workers=SqlAlchemyWorkerRepository(session_factory),
    )
    try:
        invite, code = service.create_manager_invite(
            property_id=arguments.property_id,
            valid_for=timedelta(days=arguments.valid_days),
        )
    except InvalidStaffInviteError as error:
        parser.error(str(error))
    finally:
        engine.dispose()

    print(f"Manager invite: {code}")
    print(f"Expires at: {invite.expires_at.isoformat()}")
    print("Share this code only with the manager of that property.")


if __name__ == "__main__":
    main()
