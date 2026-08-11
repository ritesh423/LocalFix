# LocalFix

LocalFix is an offline-first maintenance system for apartments, hostels, and
small properties. It turns maintenance complaints that normally disappear in
calls and chat threads into assigned, traceable jobs.

## Project status

**Current:** Stage 2 - Resident ticket vertical slice

**Latest session:** Stage 2, Session 4 - Resident Request Details

Development sessions use this format:

> **Stage N · Session N - Feature or outcome**

Each session has two parts:

1. Implement one coherent outcome.
2. Explain the implementation, then record which concepts are familiar,
   partial, or new.

## MVP workflow

1. A resident reports a maintenance issue with its category, urgency, photo,
   and preferred access time.
2. A manager reviews and assigns the ticket.
3. A worker receives the job, including when connectivity is intermittent.
4. The worker records progress, parts, and before/after proof.
5. The resident confirms completion and can leave a rating.
6. Chargeable work can use a sandbox payment flow after the core workflow is
   reliable.

The first release is an internal property-maintenance workflow, not a public
services marketplace.

## Planned technology

- Android: Kotlin, Jetpack Compose, Material 3, Room, WorkManager
- Backend: FastAPI and PostgreSQL
- Notifications: Firebase Cloud Messaging
- Payments: Razorpay Test Mode
- Automation: GitHub Actions

Technology choices remain provisional until the relevant feature is designed.
See [the roadmap](docs/ROADMAP.md) and [product brief](docs/PRODUCT.md) for the
current boundaries and open decisions.

## Repository guide

- `docs/PRODUCT.md` - problem, users, MVP, research, and success measures
- `docs/ROADMAP.md` - current stage, later stages, and exit criteria
- `docs/DESIGN_SYSTEM.md` - initial visual direction and UI principles
- `docs/LEARNING_LOG.md` - personalized explanation record
- `docs/sessions/` - outcome and decisions from every build session
- `android/` - Kotlin and Jetpack Compose application
