"""Create the first property and manager without a Render shell."""

import os
from uuid import UUID, uuid5

from dotenv import load_dotenv
from sqlalchemy.orm import Session, sessionmaker

from app.database.config import get_database_url, get_environment
from app.database.session import create_database_engine, create_session_factory
from app.domain.ticket_workflow import UserRole
from app.repositories.sqlalchemy_memberships import SqlAlchemyMembershipRepository
from app.repositories.sqlalchemy_properties import SqlAlchemyPropertyRepository
from app.services.memberships import MembershipProvisioningService
from app.services.properties import PropertyProvisioningService, normalize_unit_label


def bootstrap_personal(
    session_factory: sessionmaker[Session],
    *,
    property_id: UUID,
    property_name: str,
    unit_labels: list[str],
    manager_uid: str,
) -> None:
    properties = SqlAlchemyPropertyRepository(session_factory)
    provisioning = PropertyProvisioningService(properties)
    for label in unit_labels:
        provisioning.provision(
            property_id=property_id,
            property_name=property_name,
            unit_id=uuid5(property_id, normalize_unit_label(label)),
            unit_label=label,
        )
    MembershipProvisioningService(
        SqlAlchemyMembershipRepository(session_factory),
        properties,
    ).provision(
        firebase_uid=manager_uid,
        property_id=property_id,
        role=UserRole.MANAGER,
    )


def main() -> None:
    load_dotenv()
    if get_environment() != "personal":
        return

    manager_uid = os.environ.get("LOCALFIX_BOOTSTRAP_MANAGER_UID", "").strip()
    property_name = os.environ.get("LOCALFIX_BOOTSTRAP_PROPERTY_NAME", "").strip()
    unit_labels = [
        label.strip()
        for label in os.environ.get("LOCALFIX_BOOTSTRAP_UNIT_LABELS", "").split(",")
        if label.strip()
    ]
    property_id_text = os.environ.get("LOCALFIX_BOOTSTRAP_PROPERTY_ID", "").strip()
    if not all((manager_uid, property_name, unit_labels, property_id_text)):
        raise ValueError(
            "Personal hosting needs a manager Firebase UID, property name, "
            "at least one unit label, and property ID."
        )

    engine = create_database_engine(get_database_url())
    try:
        bootstrap_personal(
            create_session_factory(engine),
            property_id=UUID(property_id_text),
            property_name=property_name,
            unit_labels=unit_labels,
            manager_uid=manager_uid,
        )
    finally:
        engine.dispose()


if __name__ == "__main__":
    main()
