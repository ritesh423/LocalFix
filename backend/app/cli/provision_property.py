import argparse
import sys
from uuid import UUID

from dotenv import load_dotenv

from app.database.config import get_database_url
from app.database.session import create_database_engine, create_session_factory
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.services.properties import (
    InvalidPropertyError,
    PropertyConflictError,
    PropertyProvisioningService,
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Create a LocalFix property and its first apartment unit."
    )
    parser.add_argument("--property-id", required=True, type=UUID)
    parser.add_argument("--name", required=True)
    parser.add_argument("--unit-id", required=True, type=UUID)
    parser.add_argument("--unit-label", required=True)
    return parser


def main(arguments: list[str] | None = None) -> int:
    load_dotenv()
    parsed = build_parser().parse_args(arguments)
    engine = create_database_engine(get_database_url())
    try:
        service = PropertyProvisioningService(
            SqlAlchemyPropertyRepository(create_session_factory(engine))
        )
        result = service.provision(
            property_id=parsed.property_id,
            property_name=parsed.name,
            unit_id=parsed.unit_id,
            unit_label=parsed.unit_label,
        )
        property_action = (
            "created" if result.property_was_created else "already present"
        )
        unit_action = "created" if result.unit_was_created else "already present"
        print(
            f"Property {property_action}: {result.property.name}. "
            f"Unit {unit_action}: {result.unit.label}."
        )
        return 0
    except (InvalidPropertyError, PropertyConflictError) as error:
        print(str(error), file=sys.stderr)
        return 2
    finally:
        engine.dispose()


if __name__ == "__main__":
    raise SystemExit(main())
