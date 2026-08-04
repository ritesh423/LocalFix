from dataclasses import dataclass
from enum import StrEnum


class TicketStatus(StrEnum):
    OPEN = "open"
    ASSIGNED = "assigned"
    IN_PROGRESS = "in_progress"
    BLOCKED = "blocked"
    AWAITING_CONFIRMATION = "awaiting_confirmation"
    COMPLETED = "completed"
    CANCELLED = "cancelled"


class UserRole(StrEnum):
    RESIDENT = "resident"
    MANAGER = "manager"
    WORKER = "worker"


class TicketAction(StrEnum):
    ASSIGN = "assign"
    REASSIGN = "reassign"
    START = "start"
    REPORT_BLOCKER = "report_blocker"
    RESOLVE_BLOCKER = "resolve_blocker"
    SUBMIT_PROOF = "submit_proof"
    CONFIRM = "confirm"
    REQUEST_REWORK = "request_rework"
    CANCEL = "cancel"


class WorkflowError(ValueError):
    code = "workflow_error"


class PermissionDenied(WorkflowError):
    code = "permission_denied"


class TransitionNotAllowed(WorkflowError):
    code = "transition_not_allowed"


@dataclass(frozen=True)
class TransitionRule:
    current: TicketStatus
    action: TicketAction
    actor: UserRole
    result: TicketStatus


TRANSITION_RULES = (
    TransitionRule(
        TicketStatus.OPEN,
        TicketAction.ASSIGN,
        UserRole.MANAGER,
        TicketStatus.ASSIGNED,
    ),
    TransitionRule(
        TicketStatus.ASSIGNED,
        TicketAction.REASSIGN,
        UserRole.MANAGER,
        TicketStatus.ASSIGNED,
    ),
    TransitionRule(
        TicketStatus.IN_PROGRESS,
        TicketAction.REASSIGN,
        UserRole.MANAGER,
        TicketStatus.ASSIGNED,
    ),
    TransitionRule(
        TicketStatus.BLOCKED,
        TicketAction.REASSIGN,
        UserRole.MANAGER,
        TicketStatus.ASSIGNED,
    ),
    TransitionRule(
        TicketStatus.ASSIGNED,
        TicketAction.START,
        UserRole.WORKER,
        TicketStatus.IN_PROGRESS,
    ),
    TransitionRule(
        TicketStatus.ASSIGNED,
        TicketAction.REPORT_BLOCKER,
        UserRole.WORKER,
        TicketStatus.BLOCKED,
    ),
    TransitionRule(
        TicketStatus.IN_PROGRESS,
        TicketAction.REPORT_BLOCKER,
        UserRole.WORKER,
        TicketStatus.BLOCKED,
    ),
    TransitionRule(
        TicketStatus.BLOCKED,
        TicketAction.RESOLVE_BLOCKER,
        UserRole.MANAGER,
        TicketStatus.ASSIGNED,
    ),
    TransitionRule(
        TicketStatus.IN_PROGRESS,
        TicketAction.SUBMIT_PROOF,
        UserRole.WORKER,
        TicketStatus.AWAITING_CONFIRMATION,
    ),
    TransitionRule(
        TicketStatus.AWAITING_CONFIRMATION,
        TicketAction.CONFIRM,
        UserRole.RESIDENT,
        TicketStatus.COMPLETED,
    ),
    TransitionRule(
        TicketStatus.AWAITING_CONFIRMATION,
        TicketAction.REQUEST_REWORK,
        UserRole.RESIDENT,
        TicketStatus.ASSIGNED,
    ),
    TransitionRule(
        TicketStatus.OPEN,
        TicketAction.CANCEL,
        UserRole.RESIDENT,
        TicketStatus.CANCELLED,
    ),
    TransitionRule(
        TicketStatus.OPEN,
        TicketAction.CANCEL,
        UserRole.MANAGER,
        TicketStatus.CANCELLED,
    ),
    TransitionRule(
        TicketStatus.ASSIGNED,
        TicketAction.CANCEL,
        UserRole.MANAGER,
        TicketStatus.CANCELLED,
    ),
    TransitionRule(
        TicketStatus.IN_PROGRESS,
        TicketAction.CANCEL,
        UserRole.MANAGER,
        TicketStatus.CANCELLED,
    ),
    TransitionRule(
        TicketStatus.BLOCKED,
        TicketAction.CANCEL,
        UserRole.MANAGER,
        TicketStatus.CANCELLED,
    ),
)


TERMINAL_STATUSES = frozenset(
    {
        TicketStatus.COMPLETED,
        TicketStatus.CANCELLED,
    }
)


# Finds the next valid ticket status.
def transition(
    current: TicketStatus,
    action: TicketAction,
    actor: UserRole,
) -> TicketStatus:
    rules_for_action = tuple(
        rule
        for rule in TRANSITION_RULES
        if rule.current is current and rule.action is action
    )

    if not rules_for_action:
        raise TransitionNotAllowed(
            f"Action '{action}' is not allowed while ticket is '{current}'."
        )

    for rule in rules_for_action:
        if rule.actor is actor:
            return rule.result

    raise PermissionDenied(
        f"Role '{actor}' cannot perform '{action}' while ticket is '{current}'."
    )


# Finds what this user can do with the ticket.
def available_actions(
    current: TicketStatus,
    actor: UserRole,
) -> tuple[TicketAction, ...]:
    return tuple(
        rule.action
        for rule in TRANSITION_RULES
        if rule.current is current and rule.actor is actor
    )
