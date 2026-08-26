"""Link Firebase identities to LocalFix properties and roles."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260826_09"
down_revision: str | None = "20260822_08"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "property_memberships",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("firebase_uid", sa.String(length=128), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("role", sa.String(length=32), nullable=False),
        sa.Column("unit_id", sa.Uuid(), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "firebase_uid",
            "property_id",
            "role",
            name="uq_property_memberships_identity_property_role",
        ),
    )
    op.create_index(
        "ix_property_memberships_firebase_uid",
        "property_memberships",
        ["firebase_uid", "is_active"],
    )


def downgrade() -> None:
    op.drop_index(
        "ix_property_memberships_firebase_uid",
        table_name="property_memberships",
    )
    op.drop_table("property_memberships")
