---
name: addressing-pr-comments
description: Work through unresolved GitHub PR review comments one by one. Fetches comments, analyzes each with a planning subagent, then either fixes the issue or creates a GitHub issue based on complexity recommendation.
---

# Addressing PR Review Comments

This skill processes unresolved GitHub PR review comments systematically. The main agent orchestrates a multi-subagent workflow: sonnet for planning, haiku for implementation, and haiku for generating responses. Each comment gets assessed, implemented (if needed), and replied to in sequence.

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
              databaseId
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

Filter to only unresolved threads. Display the list to the user before proceeding.

### Step 2: For each unresolved comment — Main agent coordinates subagent handoff

**Main agent orchestrates the following handoff:**

#### 2a. Plan (Sonnet subagent)

Launch a **general-purpose subagent with the sonnet model** to:
- Read relevant code and assess legitimacy: Is the concern valid or should it be dismissed?
- If legitimate, recommend action: "fix in this PR" (self-contained, Low-Medium complexity) or "create a separate issue" (architectural decisions, High complexity, orthogonal scope)
- Return: legitimacy assessment + recommendation + analysis

Present the analysis to the user before proceeding to implementation.

#### 2b. Implement (Haiku subagent)

**Skip this step if comment is invalid.**

Launch a **general-purpose subagent with the haiku model** to:
- **If recommendation is "fix in this PR"**: Implement minimal changes to address the comment. Do not post responses or resolve threads.
- **If recommendation is "create a separate issue"**: Create a GitHub issue with problem description, proposed solution, checklist, and PR reference. Return the issue URL.

#### 2c. Reply (Haiku subagent)

Launch a **general-purpose subagent with the haiku model** to:
- **If invalid comment**: Generate a response explaining why the concern doesn't apply
- **If fix was implemented**: Generate a response confirming the fix (brief summary)
- **If issue was created**: Generate a response with the issue URL
- Post the response using: `gh api repos/OWNER/REPO/pulls/comments/COMMENT_DATABASE_ID/replies -f body="<response>"`
- **If fix was implemented**: Also resolve the thread using the GraphQL mutation

### Step 3: Continue to next comment

After each comment is handled, ask the user whether to continue to the next comment or stop.

## Notes

- **Main agent role**: Fetch comments, coordinate subagent handoff, present analysis to user before proceeding
- **Sonnet subagent (Step 2a)**: Plans only — assesses legitimacy, analyzes code, recommends action
- **Haiku subagent (Step 2b)**: Implements only — fixes code or creates issue, makes no API calls for posting
- **Haiku subagent (Step 2c)**: Replies only — generates response and posts it (and resolves thread if applicable)
- **Step 2b is skipped for invalid comments** — proceed directly to Step 2c
- Do not commit changes — user handles commits separately
- Invalid and separate-issue threads remain unresolved for reviewer follow-up