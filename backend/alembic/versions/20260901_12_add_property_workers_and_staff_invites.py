"""Add property workers and one-time staff invites.

Revision ID: 20260901_12
Revises: 20260830_11
Create Date: 2026-09-01
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260901_12"
down_revision: str | None = "20260830_11"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "property_workers",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("specialty", sa.String(length=32), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.ForeignKeyConstraint(["property_id"], ["properties.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_property_workers_property_active",
        "property_workers",
        ["property_id", "is_active"],
        unique=False,
    )
    op.create_table(
        "staff_invites",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("role", sa.String(length=32), nullable=False),
        sa.Column("code_digest", sa.String(length=64), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("claimed_by_firebase_uid", sa.String(length=128)),
        sa.Column("claimed_at", sa.DateTime(timezone=True)),
        sa.Column("revoked_at", sa.DateTime(timezone=True)),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["property_id"], ["properties.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "code_digest",
            name="uq_staff_invites_code_digest",
        ),
    )
    op.create_index(
        "ix_staff_invites_property_role",
        "staff_invites",
        ["property_id", "role"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_staff_invites_property_role", table_name="staff_invites")
    op.drop_table("staff_invites")
    op.drop_index(
        "ix_property_workers_property_active",
        table_name="property_workers",
    )
    op.drop_table("property_workers")
