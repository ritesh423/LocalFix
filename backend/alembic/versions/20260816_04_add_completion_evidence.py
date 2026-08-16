"""Add worker completion evidence fields."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260816_04"
down_revision: str | None = "20260816_03"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "tickets",
        sa.Column("completion_note", sa.String(length=500), nullable=True),
    )
    op.add_column(
        "tickets",
        sa.Column("parts_used", sa.JSON(), nullable=True),
    )
    op.add_column(
        "tickets",
        sa.Column("completion_photo_key", sa.String(length=255), nullable=True),
    )
    op.add_column(
        "tickets",
        sa.Column("completion_submitted_at", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("tickets", "completion_submitted_at")
    op.drop_column("tickets", "completion_photo_key")
    op.drop_column("tickets", "parts_used")
    op.drop_column("tickets", "completion_note")
