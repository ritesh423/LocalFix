import argparse
from datetime import timedelta
from uuid import UUID

from app.database.config import get_database_url
from app.database.session import create_database_engine, create_session_factory
from app.repositories.sqlalchemy_memberships import SqlAlchemyMembershipRepository
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.repositories.sqlalchemy_resident_invites import (
    SqlAlchemyResidentInviteRepository,
)
from app.services.resident_invites import (
    InvalidResidentInviteError,
    ResidentInviteService,
)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Create a one-time resident apartment invite."
    )
    parser.add_argument("--property-id", required=True, type=UUID)
    parser.add_argument("--unit-id", required=True, type=UUID)
    parser.add_argument("--valid-days", type=int, default=7)
    arguments = parser.parse_args()

    engine = create_database_engine(get_database_url())
    session_factory = create_session_factory(engine)
    service = ResidentInviteService(
        invites=SqlAlchemyResidentInviteRepository(session_factory),
        memberships=SqlAlchemyMembershipRepository(session_factory),
        properties=SqlAlchemyPropertyRepository(session_factory),
    )
    try:
        result = service.create(
            property_id=arguments.property_id,
            unit_id=arguments.unit_id,
            valid_for=timedelta(days=arguments.valid_days),
        )
    except InvalidResidentInviteError as error:
        parser.error(str(error))

    print(f"Resident invite: {result.code}")
    print(f"Expires at: {result.invite.expires_at.isoformat()}")
    print("Share this code only with the resident of that apartment.")


if __name__ == "__main__":
    main()
