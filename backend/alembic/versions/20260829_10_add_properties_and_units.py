"""Add the property and apartment-unit directory."""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "20260829_10"
down_revision: str | None = "20260826_09"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "properties",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_table(
        "property_units",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("property_id", sa.Uuid(), nullable=False),
        sa.Column("label", sa.String(length=80), nullable=False),
        sa.Column("normalized_label", sa.String(length=80), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.ForeignKeyConstraint(["property_id"], ["properties.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "property_id",
            "normalized_label",
            name="uq_property_units_property_normalized_label",
        ),
    )
    op.create_index(
        "ix_property_units_property_id",
        "property_units",
        ["property_id"],
    )


def downgrade() -> None:
    op.drop_index("ix_property_units_property_id", table_name="property_units")
    op.drop_table("property_units")
    op.drop_table("properties")
