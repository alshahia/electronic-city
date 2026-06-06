# AGENTS.md — Instructions for AI agents working on this codebase

> **Reading order for every session:**
> 1. **`AGENTS.md`** (this file)
> 2. **`.context/skills_guide&roadmap.md`** — local skills catalog and roadmap context, *not* committed in its current form. Treat as required reading before proposing changes.

## Single sources of truth

- [`plan.md`](./plan.md) — active refactor task list, phased (0–7). Closed items are checked off; "Future i18n Backlog" tracks deferred work.
- [`DECISIONS.md`](./DECISIONS.md) — locked architectural choices (D1–D8). Deviations need a PR discussion and an update here.

## Other reading on demand

- [`.context/skills_guide&roadmap.md`](./.context/skills_guide&roadmap.md) — local skills catalog + roadmap notes (read first, per the order above)
- [`.context/verification-validation-system-prompt.md`](./.context/verification-validation-system-prompt.md) — verification prompt for sensitive changes
- [`RUN_GUIDE.md`](./RUN_GUIDE.md) — how to run the app
- [`docs/legacy-ai-studio/`](./docs/legacy-ai-studio/) — archived AI Studio project files

## Working agreements

- Use `ServiceLocator` for repository access in ViewModels (per D7).
- All ViewModels are Activity-scoped; do not introduce God VMs.
- `LocalAppActivity` is the canonical way to reach `ComponentActivity` from composables (Phase 4 / D8.7).
- Phases 0–6 are complete. The active work is **Phase 7 (Final Verification)**.
- Manual code review stands in for `./gradlew` runs (no `gradlew` wrapper and no Java/Gradle on this machine).
- Pushes to `origin/main` are blocked (no write perms on the configured remote); verify locally with `git status` / `git log` instead.
