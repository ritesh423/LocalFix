from datetime import datetime
from uuid import UUID

from sqlalchemy import (
    JSON,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    String,
    UniqueConstraint,
    Uuid,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class DeviceRegistrationRecord(Base):
    __tablename__ = "device_registrations"
    __table_args__ = (
        Index(
            "ix_device_registrations_recipient",
            "property_id",
            "role",
            "user_id",
        ),
        UniqueConstraint(
            "firebase_installation_id",
            name="uq_device_registrations_firebase_installation_id",
        ),
    )

    installation_id: Mapped[UUID] = mapped_column(Uuid(), primary_key=True)
    firebase_installation_id: Mapped[str] = mapped_column(String(255))
    platform: Mapped[str] = mapped_column(String(32))
    role: Mapped[str] = mapped_column(String(32))
    user_id: Mapped[UUID] = mapped_column(Uuid())
    property_id: Mapped[UUID] = mapped_column(Uuid())
    registered_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))


class TicketRecord(Base):
    __tablename__ = "tickets"
    __table_args__ = (
        Index(
            "ix_tickets_resident_visibility",
            "property_id",
            "resident_id",
        ),
        Index(
            "ix_tickets_manager_queue",
            "property_id",
            "status",
            "updated_at",
        ),
        Index(
            "ix_tickets_worker_status_updated",
            "assigned_worker_id",
            "status",
            "updated_at",
        ),
        UniqueConstraint(
            "client_request_id",
            name="uq_tickets_client_request_id",
        ),
    )

    id: Mapped[UUID] = mapped_column(Uuid(), primary_key=True)
    client_request_id: Mapped[UUID] = mapped_column(Uuid())
    property_id: Mapped[UUID] = mapped_column(Uuid())
    unit_id: Mapped[UUID] = mapped_column(Uuid())
    resident_id: Mapped[UUID] = mapped_column(Uuid())
    title: Mapped[str] = mapped_column(String(80))
    description: Mapped[str] = mapped_column(String(500))
    category: Mapped[str] = mapped_column(String(32))
    urgency_suggestion: Mapped[str] = mapped_column(String(32))
    priority: Mapped[str | None] = mapped_column(String(32), nullable=True)
    access_window: Mapped[str] = mapped_column(String(32))
    status: Mapped[str] = mapped_column(String(32))
    version: Mapped[int] = mapped_column(Integer)
    assigned_worker_id: Mapped[UUID | None] = mapped_column(Uuid(), nullable=True)
    assigned_worker: Mapped[str | None] = mapped_column(String(120), nullable=True)
    completion_note: Mapped[str | None] = mapped_column(String(500), nullable=True)
    parts_used: Mapped[list[str] | None] = mapped_column(JSON, nullable=True)
    completion_photo_key: Mapped[str | None] = mapped_column(String(255), nullable=True)
    completion_submitted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )
    resident_rating: Mapped[int | None] = mapped_column(Integer, nullable=True)
    resident_feedback: Mapped[str | None] = mapped_column(String(500), nullable=True)
    resident_reviewed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)


class TicketEventRecord(Base):
    __tablename__ = "ticket_events"
    __table_args__ = (
        Index(
            "ix_ticket_events_ticket_created",
            "ticket_id",
            "created_at",
        ),
    )

    id: Mapped[UUID] = mapped_column(Uuid(), primary_key=True)
    ticket_id: Mapped[UUID] = mapped_column(
        Uuid(),
        ForeignKey("tickets.id"),
        nullable=False,
    )
    actor_role: Mapped[str] = mapped_column(String(32))
    actor_id: Mapped[UUID | None] = mapped_column(Uuid(), nullable=True)
    action: Mapped[str] = mapped_column(String(32))
    from_status: Mapped[str | None] = mapped_column(String(32), nullable=True)
    to_status: Mapped[str] = mapped_column(String(32))
    ticket_version: Mapped[int] = mapped_column(Integer)
    detail: Mapped[str | None] = mapped_column(String(500), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
