from datetime import datetime
from uuid import UUID

from sqlalchemy import JSON, DateTime, Index, Integer, String, UniqueConstraint, Uuid
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


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
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
