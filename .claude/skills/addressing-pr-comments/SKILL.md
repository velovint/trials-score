---
name: addressing-pr-comments
description: Work through unresolved GitHub PR review comments one by one. Fetches comments, analyzes each with a planning subagent, then either fixes the issue or creates a GitHub issue based on complexity recommendation.
---

# Addressing PR Review Comments

This skill processes unresolved GitHub PR review comments systematically. For each comment it plans a solution, evaluates complexity, and either fixes the code or tracks it as a GitHub issue.

## Usage

```
/addressing-pr-comments <PR_URL>
```

Example:
```
/addressing-pr-comments https://github.com/velovint/trials-score/pull/45
```

If no URL is provided, ask the user for the PR URL before proceeding.

## Workflow

### Step 1: Fetch unresolved PR comments

Use `gh api graphql` to fetch all unresolved review threads from the PR:

```bash
gh api graphql -f query='
{
  repository(owner: "OWNER", name: "REPO") {
    pullRequest(number: PR_NUMBER) {
      reviewThreads(first: 100) {
        nodes {
          id
          isResolved
          comments(first: 1) {
            nodes {
              path
              originalLine
              body
              author { login }
            }
          }
        }
      }
    }
  }
}'
```

Filter to only unresolved threads (`isResolved: false`). Display the list to the user before proceeding.

### Step 2: For each unresolved comment — Plan

Launch a **general-purpose subagent with the sonnet model** to:
- Read the relevant file(s) at and around the flagged line
- Understand what the reviewer is asking for
- Research what changes would be required
- Assess complexity: **Low / Medium / High**
- Make a recommendation: **fix in this PR** or **create a separate issue**

**Recommendation guidance** (assume all other comments in the PR will be addressed separately):
- Fix in this PR: self-contained change, Low-Medium complexity, no external blockers, no scope creep risk
- Separate issue: requires architectural decisions, missing infrastructure/data, High complexity, or orthogonal to PR scope

Present the analysis and recommendation to the user before acting.

### Step 3: Act on the recommendation

**If recommendation is "fix in this PR":**
- Launch a **general-purpose subagent with the haiku model** to implement the fix
- The subagent should make only the minimal changes needed to address the comment
- Do not commit — user handles commits

**If recommendation is "create a separate issue":**
- Launch a **general-purpose subagent with the haiku model** to create a GitHub issue using `gh issue create`
- The issue body should include: problem description, proposed solution, specific tasks as a checklist, and a reference to the PR comment (file#line)
- Print the resulting issue URL

### Step 4: Mark comment as resolved (only for fixes applied in this PR)

After a fix is applied, resolve the corresponding GitHub review thread using:

```bash
gh api graphql -f query='
mutation {
  resolveReviewThread(input: {threadId: "THREAD_NODE_ID"}) {
    thread { isResolved }
  }
}'
```

Do NOT resolve threads where a GitHub issue was created instead — those remain open until the issue is addressed.

### Step 5: Continue to next comment

After each comment is handled, ask the user whether to continue to the next comment or stop.

## Notes

- Process comments in order (top of list first)
- Skip comments that are already resolved (`isResolved: true`)
- Do not commit any changes — the user handles commits separately
- Keep subagent prompts focused: each subagent addresses exactly one comment
- The planning subagent does research only (no code changes)
- The fixing subagent makes code changes only (no GitHub API calls)
- The issue-creation subagent makes GitHub API calls only (no code changes)