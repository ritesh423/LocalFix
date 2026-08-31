"""Add one-time resident apartment invites.

Revision ID: 20260830_11
Revises: 20260829_10
Create Date: 2026-08-30
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260830_11"
down_revision: str | None = "20260829_10"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "resident_invites",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("unit_id", sa.Uuid(), nullable=False),
        sa.Column("code_digest", sa.String(length=64), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column(
            "claimed_by_firebase_uid",
            sa.String(length=128),
            nullable=True,
        ),
        sa.Column("claimed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["property_id"], ["properties.id"]),
        sa.ForeignKeyConstraint(["unit_id"], ["property_units.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "code_digest",
            name="uq_resident_invites_code_digest",
        ),
    )
    op.create_index(
        "ix_resident_invites_property_unit",
        "resident_invites",
        ["property_id", "unit_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(
        "ix_resident_invites_property_unit",
        table_name="resident_invites",
    )
    op.drop_table("resident_invites")
