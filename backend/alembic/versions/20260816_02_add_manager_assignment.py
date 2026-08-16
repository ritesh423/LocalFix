"""Add manager priority and worker assignment fields."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260816_02"
down_revision: str | None = "20260811_01"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "tickets",
        sa.Column("priority", sa.String(length=32), nullable=True),
    )
    op.add_column(
        "tickets",
        sa.Column("assigned_worker_id", sa.Uuid(), nullable=True),
    )
    op.create_index(
        "ix_tickets_manager_queue",
        "tickets",
        ["property_id", "status", "updated_at"],
    )


def downgrade() -> None:
    op.drop_index("ix_tickets_manager_queue", table_name="tickets")
    op.drop_column("tickets", "assigned_worker_id")
    op.drop_column("tickets", "priority")
