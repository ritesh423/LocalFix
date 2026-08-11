from datetime import datetime
from uuid import UUID

from sqlalchemy import DateTime, Index, Integer, String, UniqueConstraint, Uuid
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
    access_window: Mapped[str] = mapped_column(String(32))
    status: Mapped[str] = mapped_column(String(32))
    version: Mapped[int] = mapped_column(Integer)
    assigned_worker: Mapped[str | None] = mapped_column(String(120), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
