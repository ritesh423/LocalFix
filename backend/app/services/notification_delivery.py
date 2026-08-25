from dataclasses import dataclass, replace
from datetime import UTC, datetime, timedelta

from app.domain.notifications import NotificationJob, NotificationStatus
from app.gateways.push import PushDeliveryError, PushGateway
from app.repositories.device_registrations import DeviceRegistrationRepository
from app.repositories.notification_outbox import NotificationOutboxRepository

MAX_DELIVERY_ATTEMPTS = 5
MAX_RETRY_SECONDS = 60 * 60


@dataclass(frozen=True)
class NotificationDeliverySummary:
    processed: int
    sent: int
    rescheduled: int
    failed: int


class NotificationDeliveryService:
    def __init__(
        self,
        outbox: NotificationOutboxRepository,
        device_registrations: DeviceRegistrationRepository,
        push_gateway: PushGateway,
    ) -> None:
        self._outbox = outbox
        self._device_registrations = device_registrations
        self._push_gateway = push_gateway

    def deliver_ready(
        self,
        now: datetime | None = None,
        limit: int = 100,
    ) -> NotificationDeliverySummary:
        delivery_time = now or datetime.now(UTC)
        jobs = self._outbox.list_ready(delivery_time, limit)
        sent = 0
        rescheduled = 0
        failed = 0

        for job in jobs:
            registrations = self._device_registrations.list_for_recipient(
                property_id=job.property_id,
                role=job.recipient_role,
                user_id=job.recipient_user_id,
            )
            if not registrations:
                updated = self._record_failure(
                    job,
                    delivery_time,
                    "No registered device is available for this recipient.",
                )
            else:
                try:
                    for registration in registrations:
                        self._push_gateway.send(
                            firebase_installation_id=(
                                registration.firebase_installation_id
                            ),
                            title=job.title,
                            body=job.body,
                            data=job.data,
                        )
                except PushDeliveryError as error:
                    updated = self._record_failure(
                        job,
                        delivery_time,
                        self._safe_error(error),
                    )
                else:
                    updated = replace(
                        job,
                        status=NotificationStatus.SENT,
                        attempt_count=job.attempt_count + 1,
                        last_error=None,
                        sent_at=delivery_time,
                        updated_at=delivery_time,
                    )

            self._outbox.update(updated)
            if updated.status is NotificationStatus.SENT:
                sent += 1
            elif updated.status is NotificationStatus.FAILED:
                failed += 1
            else:
                rescheduled += 1

        return NotificationDeliverySummary(
            processed=len(jobs),
            sent=sent,
            rescheduled=rescheduled,
            failed=failed,
        )

    @staticmethod
    def _record_failure(
        job: NotificationJob,
        failed_at: datetime,
        error: str,
    ) -> NotificationJob:
        attempt_count = job.attempt_count + 1
        terminal = attempt_count >= MAX_DELIVERY_ATTEMPTS
        retry_seconds = min(30 * (2 ** (attempt_count - 1)), MAX_RETRY_SECONDS)
        return replace(
            job,
            status=(
                NotificationStatus.FAILED
                if terminal
                else NotificationStatus.PENDING
            ),
            attempt_count=attempt_count,
            available_at=(
                failed_at if terminal else failed_at + timedelta(seconds=retry_seconds)
            ),
            last_error=error,
            updated_at=failed_at,
        )

    @staticmethod
    def _safe_error(error: Exception) -> str:
        message = str(error).strip() or error.__class__.__name__
        return message[:500]
