"""Add the worker queue lookup index."""

from collections.abc import Sequence

from alembic import op

revision: str = "20260816_03"
down_revision: str | None = "20260816_02"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_index(
        "ix_tickets_worker_status_updated",
        "tickets",
        ["assigned_worker_id", "status", "updated_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_tickets_worker_status_updated", table_name="tickets")
