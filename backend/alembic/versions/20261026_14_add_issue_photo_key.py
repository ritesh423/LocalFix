"""Keep the resident's issue photo with the submitted ticket.

Revision ID: 20261026_14
Revises: 20261026_13
"""

import sqlalchemy as sa

from alembic import op

revision = "20261026_14"
down_revision = "20261026_13"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("tickets", sa.Column("issue_photo_key", sa.String(255)))


def downgrade() -> None:
    op.drop_column("tickets", "issue_photo_key")
