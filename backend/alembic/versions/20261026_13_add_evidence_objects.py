"""Store small completion photos in the hobby database.

Revision ID: 20261026_13
Revises: 20260901_12
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20261026_13"
down_revision: str | None = "20260901_12"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "evidence_objects",
        sa.Column("key", sa.String(length=255), nullable=False),
        sa.Column("ticket_id", sa.Uuid(), nullable=False),
        sa.Column("content_type", sa.String(length=32), nullable=False),
        sa.Column("content", sa.LargeBinary(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["ticket_id"], ["tickets.id"]),
        sa.PrimaryKeyConstraint("key"),
    )
    op.create_index(
        "ix_evidence_objects_ticket_id",
        "evidence_objects",
        ["ticket_id"],
    )


def downgrade() -> None:
    op.drop_index("ix_evidence_objects_ticket_id", table_name="evidence_objects")
    op.drop_table("evidence_objects")
