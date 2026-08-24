"""Add a durable outbox for maintenance notifications."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260822_08"
down_revision: str | None = "20260822_07"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "notification_outbox",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("deduplication_key", sa.String(length=255), nullable=False),
        sa.Column("ticket_id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("recipient_role", sa.String(length=32), nullable=False),
        sa.Column("recipient_user_id", sa.Uuid(), nullable=True),
        sa.Column("kind", sa.String(length=64), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("body", sa.String(length=500), nullable=False),
        sa.Column("data", sa.JSON(), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False),
        sa.Column("attempt_count", sa.Integer(), nullable=False),
        sa.Column("available_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("last_error", sa.String(length=500), nullable=True),
        sa.Column("sent_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["ticket_id"], ["tickets.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "deduplication_key",
            name="uq_notification_outbox_deduplication_key",
        ),
    )
    op.create_index(
        "ix_notification_outbox_delivery",
        "notification_outbox",
        ["status", "available_at", "created_at"],
    )
    op.create_index(
        "ix_notification_outbox_recipient",
        "notification_outbox",
        ["property_id", "recipient_role", "recipient_user_id"],
    )


def downgrade() -> None:
    op.drop_index(
        "ix_notification_outbox_recipient",
        table_name="notification_outbox",
    )
    op.drop_index(
        "ix_notification_outbox_delivery",
        table_name="notification_outbox",
    )
    op.drop_table("notification_outbox")
