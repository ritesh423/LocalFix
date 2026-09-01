from uuid import UUID

from app.domain.tickets import (
    ManagerContext,
    ResidentContext,
    ServiceCategory,
    Worker,
    WorkerContext,
)

DEMO_RESIDENT_CONTEXT = ResidentContext(
    user_id=UUID("10000000-0000-0000-0000-000000000001"),
    property_id=UUID("20000000-0000-0000-0000-000000000001"),
    unit_id=UUID("30000000-0000-0000-0000-000000000204"),
)

DEMO_MANAGER_CONTEXT = ManagerContext(
    user_id=UUID("10000000-0000-0000-0000-000000000002"),
    property_id=UUID("20000000-0000-0000-0000-000000000001"),
)

DEMO_WORKERS = (
    Worker(
        id=UUID("40000000-0000-0000-0000-000000000001"),
        property_id=DEMO_MANAGER_CONTEXT.property_id,
        name="Arun Kumar",
        specialty=ServiceCategory.PLUMBING,
        is_active=True,
    ),
    Worker(
        id=UUID("40000000-0000-0000-0000-000000000002"),
        property_id=DEMO_MANAGER_CONTEXT.property_id,
        name="Maya Singh",
        specialty=ServiceCategory.ELECTRICAL,
        is_active=True,
    ),
    Worker(
        id=UUID("40000000-0000-0000-0000-000000000003"),
        property_id=DEMO_MANAGER_CONTEXT.property_id,
        name="Sameer Khan",
        specialty=ServiceCategory.APPLIANCE,
        is_active=True,
    ),
)

DEMO_WORKER_CONTEXT = WorkerContext(
    worker_id=DEMO_WORKERS[0].id,
    property_id=DEMO_WORKERS[0].property_id,
)
