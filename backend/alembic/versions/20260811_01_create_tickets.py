"""Create the first durable tickets table."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260811_01"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "tickets",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("client_request_id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("unit_id", sa.Uuid(), nullable=False),
        sa.Column("resident_id", sa.Uuid(), nullable=False),
        sa.Column("title", sa.String(length=80), nullable=False),
        sa.Column("description", sa.String(length=500), nullable=False),
        sa.Column("category", sa.String(length=32), nullable=False),
        sa.Column("urgency_suggestion", sa.String(length=32), nullable=False),
        sa.Column("access_window", sa.String(length=32), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False),
        sa.Column("version", sa.Integer(), nullable=False),
        sa.Column("assigned_worker", sa.String(length=120), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "client_request_id",
            name="uq_tickets_client_request_id",
        ),
    )
    op.create_index(
        "ix_tickets_resident_visibility",
        "tickets",
        ["property_id", "resident_id"],
    )
    op.create_index("ix_tickets_updated_at", "tickets", ["updated_at"])


def downgrade() -> None:
    op.drop_index("ix_tickets_updated_at", table_name="tickets")
    op.drop_index("ix_tickets_resident_visibility", table_name="tickets")
    op.drop_table("tickets")
