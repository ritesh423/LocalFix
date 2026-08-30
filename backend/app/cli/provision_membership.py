import argparse
import sys
from uuid import UUID

from dotenv import load_dotenv

from app.database.config import get_database_url, get_firebase_project_id
from app.database.session import create_database_engine, create_session_factory
from app.domain.ticket_workflow import UserRole
from app.gateways.identity import FirebaseIdentityDirectory
from app.repositories.sqlalchemy_memberships import SqlAlchemyMembershipRepository
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.services.memberships import (
    InvalidMembershipError,
    MembershipConflictError,
    MembershipProvisioningService,
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Link an existing Firebase user to a LocalFix workspace."
    )
    parser.add_argument("--email", required=True)
    parser.add_argument(
        "--role",
        required=True,
        choices=("resident", "manager", "worker"),
    )
    parser.add_argument("--property-id", required=True, type=UUID)
    parser.add_argument("--unit-id", type=UUID)
    parser.add_argument(
        "--user-id",
        type=UUID,
        help="Optional existing LocalFix user or worker ID.",
    )
    return parser


def main(arguments: list[str] | None = None) -> int:
    load_dotenv()
    parsed = build_parser().parse_args(arguments)
    engine = create_database_engine(get_database_url())
    try:
        identity = FirebaseIdentityDirectory(
            project_id=get_firebase_project_id()
        ).find_by_email(parsed.email)
        if identity is None:
            print(
                "No Firebase Authentication user was found for that email.",
                file=sys.stderr,
            )
            return 1

        session_factory = create_session_factory(engine)
        service = MembershipProvisioningService(
            SqlAlchemyMembershipRepository(session_factory),
            SqlAlchemyPropertyRepository(session_factory),
        )
        result = service.provision(
            firebase_uid=identity.firebase_uid,
            property_id=parsed.property_id,
            role=UserRole(parsed.role),
            unit_id=parsed.unit_id,
            user_id=parsed.user_id,
        )
        action = "Created" if result.was_created else "Already present"
        print(
            f"{action}: {parsed.email} -> {parsed.role} "
            f"for property {parsed.property_id}."
        )
        return 0
    except (InvalidMembershipError, MembershipConflictError) as error:
        print(str(error), file=sys.stderr)
        return 2
    finally:
        engine.dispose()


if __name__ == "__main__":
    raise SystemExit(main())
