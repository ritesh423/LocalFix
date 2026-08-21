from fastapi import APIRouter

from app.api.dependencies import (
    DeviceRegistrationServiceDependency,
    ManagerContextDependency,
    ResidentContextDependency,
    WorkerContextDependency,
)
from app.api.schemas import DeviceRegistrationRequest, DeviceRegistrationResponse
from app.domain.ticket_workflow import UserRole

router = APIRouter(prefix="/devices", tags=["notifications"])


@router.post("/resident", response_model=DeviceRegistrationResponse)
def register_resident_device(
    payload: DeviceRegistrationRequest,
    service: DeviceRegistrationServiceDependency,
    resident: ResidentContextDependency,
) -> DeviceRegistrationResponse:
    return DeviceRegistrationResponse.from_domain(
        service.register(
            **payload.registration_fields(),
            role=UserRole.RESIDENT,
            user_id=resident.user_id,
            property_id=resident.property_id,
        )
    )


@router.post("/manager", response_model=DeviceRegistrationResponse)
def register_manager_device(
    payload: DeviceRegistrationRequest,
    service: DeviceRegistrationServiceDependency,
    manager: ManagerContextDependency,
) -> DeviceRegistrationResponse:
    return DeviceRegistrationResponse.from_domain(
        service.register(
            **payload.registration_fields(),
            role=UserRole.MANAGER,
            user_id=manager.user_id,
            property_id=manager.property_id,
        )
    )


@router.post("/worker", response_model=DeviceRegistrationResponse)
def register_worker_device(
    payload: DeviceRegistrationRequest,
    service: DeviceRegistrationServiceDependency,
    worker: WorkerContextDependency,
) -> DeviceRegistrationResponse:
    return DeviceRegistrationResponse.from_domain(
        service.register(
            **payload.registration_fields(),
            role=UserRole.WORKER,
            user_id=worker.worker_id,
            property_id=worker.property_id,
        )
    )
