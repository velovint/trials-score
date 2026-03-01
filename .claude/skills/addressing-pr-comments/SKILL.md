---
name: addressing-pr-comments
description: Work through unresolved GitHub PR review comments one by one. Fetches comments, analyzes each with a planning subagent, then either fixes the issue or creates a GitHub issue based on complexity recommendation.
---

# Addressing PR Review Comments

This skill processes unresolved GitHub PR review comments systematically. 
The main agent orchestrates a multi-subagent workflow consisting of: 
- comment assessment
- addressing comment
- responding to comment

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
- Assess validity of the comment: Is the concern valid or should it be dismissed?
- If the comment is invalid report back to main agent
- If the comment is valid then plan the fix and report back to main agen

#### 2b. Analyze complexity

Analyze complexity of the change and make recommendation to either
- "fix in this PR" for simple, self-contain, low-medium complexity changes 
- "create a separate issue" for architectural or high complexity changes 

Present the analysis to the user before proceeding to implementation.

#### 2c. Implement (Haiku subagent)

If comment is invalid - skip this step

If recommendation is "fix in this PR":
- Launch a general purpose subagent with haiku model and pass instructions to fix the code.

If recommendation is "create a separate issue":
- Launch a general purpose subagent with haiku model to Create a GitHub issue with problem description, proposed solution, checklist, and PR reference. Return the issue URL.

#### 2d. Reply (Haiku subagent)

Launch a **general-purpose subagent with the haiku model** to:
- **If invalid comment**: Generate a response explaining why the concern doesn't apply
- **If fix was implemented**: Generate a response confirming the fix (brief summary) and mark the thread as resolved
- **If issue was created**: Generate a response with the issue URL
 
Post the response using: `gh api repos/OWNER/REPO/pulls/comments/COMMENT_DATABASE_ID/replies -f body="<response>"`

### Step 3: Continue to next comment

After each comment is handled, ask the user whether to continue to the next comment or stop.