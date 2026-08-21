"""Add durable push-notification device registrations."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260822_07"
down_revision: str | None = "20260817_06"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "device_registrations",
        sa.Column("installation_id", sa.Uuid(), nullable=False),
        sa.Column("firebase_installation_id", sa.String(length=255), nullable=False),
        sa.Column("platform", sa.String(length=32), nullable=False),
        sa.Column("role", sa.String(length=32), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("registered_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("installation_id"),
        sa.UniqueConstraint(
            "firebase_installation_id",
            name="uq_device_registrations_firebase_installation_id",
        ),
    )
    op.create_index(
        "ix_device_registrations_recipient",
        "device_registrations",
        ["property_id", "role", "user_id"],
    )


def downgrade() -> None:
    op.drop_index(
        "ix_device_registrations_recipient",
        table_name="device_registrations",
    )
    op.drop_table("device_registrations")
