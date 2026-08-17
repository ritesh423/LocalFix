"""Add resident completion review fields."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260816_05"
down_revision: str | None = "20260816_04"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("tickets", sa.Column("resident_rating", sa.Integer(), nullable=True))
    op.add_column(
        "tickets",
        sa.Column("resident_feedback", sa.String(length=500), nullable=True),
    )
    op.add_column(
        "tickets",
        sa.Column("resident_reviewed_at", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("tickets", "resident_reviewed_at")
    op.drop_column("tickets", "resident_feedback")
    op.drop_column("tickets", "resident_rating")
