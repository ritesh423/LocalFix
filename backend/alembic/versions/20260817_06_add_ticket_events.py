"""Add immutable ticket event history."""

from collections.abc import Sequence
from uuid import uuid4

import sqlalchemy as sa

from alembic import op

revision: str = "20260817_06"
down_revision: str | None = "20260816_05"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "ticket_events",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("ticket_id", sa.Uuid(), nullable=False),
        sa.Column("actor_role", sa.String(length=32), nullable=False),
        sa.Column("actor_id", sa.Uuid(), nullable=True),
        sa.Column("action", sa.String(length=32), nullable=False),
        sa.Column("from_status", sa.String(length=32), nullable=True),
        sa.Column("to_status", sa.String(length=32), nullable=False),
        sa.Column("ticket_version", sa.Integer(), nullable=False),
        sa.Column("detail", sa.String(length=500), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["ticket_id"], ["tickets.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_ticket_events_ticket_created",
        "ticket_events",
        ["ticket_id", "created_at"],
    )

    connection = op.get_bind()
    tickets = connection.execute(
        sa.text("SELECT id, status, version, updated_at FROM tickets")
    ).mappings()
    for ticket in tickets:
        connection.execute(
            sa.text(
                """
                INSERT INTO ticket_events (
                    id, ticket_id, actor_role, actor_id, action, from_status,
                    to_status, ticket_version, detail, created_at
                ) VALUES (
                    :id, :ticket_id, 'system', NULL, 'history_started', NULL,
                    :to_status, :ticket_version, :detail, :created_at
                )
                """
            ),
            {
                "id": uuid4().hex,
                "ticket_id": ticket["id"],
                "to_status": ticket["status"],
                "ticket_version": ticket["version"],
                "detail": "History tracking started for this existing ticket.",
                "created_at": ticket["updated_at"],
            },
        )


def downgrade() -> None:
    op.drop_index("ix_ticket_events_ticket_created", table_name="ticket_events")
    op.drop_table("ticket_events")
