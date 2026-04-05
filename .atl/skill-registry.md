# Skill Registry — SubIA

Generated: 2026-03-13
Project: SubIA

## User-Level Skills (`~/.claude/skills/`)

| Name | Path | Trigger |
|------|------|---------|
| sdd-apply | C:\Users\santi\.claude\skills\sdd-apply\SKILL.md | Implement tasks from the change, writing actual code following the specs and design. Trigger: When the orchestrator launches you to implement one or more tasks from a change. |
| sdd-archive | C:\Users\santi\.claude\skills\sdd-archive\SKILL.md | Sync delta specs to main specs and archive a completed change. Trigger: When the orchestrator launches you to archive a change after implementation and verification. |
| sdd-design | C:\Users\santi\.claude\skills\sdd-design\SKILL.md | Create technical design document with architecture decisions and approach. Trigger: When the orchestrator launches you to write or update the technical design for a change. |
| sdd-explore | C:\Users\santi\.claude\skills\sdd-explore\SKILL.md | Explore and investigate ideas before committing to a change. Trigger: When the orchestrator launches you to think through a feature, investigate the codebase, or clarify requirements. |
| sdd-init | C:\Users\santi\.claude\skills\sdd-init\SKILL.md | Initialize Spec-Driven Development context in any project. Trigger: When user wants to initialize SDD in a project. |
| sdd-propose | C:\Users\santi\.claude\skills\sdd-propose\SKILL.md | Create a change proposal with intent, scope, and approach. Trigger: When the orchestrator launches you to create or update a proposal for a change. |
| sdd-spec | C:\Users\santi\.claude\skills\sdd-spec\SKILL.md | Write specifications with requirements and scenarios (delta specs for changes). Trigger: When the orchestrator launches you to write or update specs for a change. |
| sdd-tasks | C:\Users\santi\.claude\skills\sdd-tasks\SKILL.md | Break down a change into an implementation task checklist. Trigger: When the orchestrator launches you to create or update the task breakdown for a change. |
| sdd-verify | C:\Users\santi\.claude\skills\sdd-verify\SKILL.md | Validate that implementation matches specs, design, and tasks. Trigger: When the orchestrator launches you to verify a completed (or partially completed) change. |

## Project-Level Skills

None found.

## Project Convention Files

| File | Description |
|------|-------------|
| C:\Users\santi\OneDrive\Escritorio\SubIA\PROMPT.md | Full AI assistant briefing: product vision, stack, conventions, roadmap, security checklist, gotchas |
| C:\Users\santi\OneDrive\Escritorio\SubIA\README.md | Project overview, features, tech stack, run instructions, directory structure |
| C:\Users\santi\OneDrive\Escritorio\SubIA\CHANGELOG.md | Version history following Semantic Versioning |

## Loading Instructions

Before starting any task, load relevant skills:
1. Try: mem_search(query: "skill-registry", project: "SubIA") → mem_get_observation(id)
2. Fallback: read C:\Users\santi\OneDrive\Escritorio\SubIA\.atl\skill-registry.md
3. Match skills to your task and read their SKILL.md files
4. Also read PROMPT.md for project conventions before touching any code
