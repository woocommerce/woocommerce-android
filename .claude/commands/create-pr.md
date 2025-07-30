Create a pull request

## Instructions to Follow:

### PR Creation Process
- ALWAYS create PRs as DRAFT initially
- NEVER mention Claude in code, commits, or PR content
- NEVER include "Co-Authored-By" in any commit messages
- NEVER mention unit tests in PR descriptions

### PR Description Format
- Use plain, simple language in PR descriptions
- REMOVE all template comments from the PR description
- Use previous PRs as a reference for the description format
- Use previous PRs as a reference for the title format

### Steps to Execute:
1. Check git status and ensure all changes are committed
2. Push the current branch to remote if not already pushed
3. Get the branch name and extract ticket number (e.g., woomob-840-... -> WOOMOB-840)
4. Look at recent commits to understand the changes made
5. Create a draft PR with:
   - Title format: [TICKET-NUMBER] Brief description of changes
   - Description following the template format seen in previous PRs:
     TICKET-NUMBER

    <!-- Remember about a good descriptive title. -->

    <!-- Id number of the Linear issue this PR addresses, e.g., WOOMOB-373. -->

    ### Description
    <!-- Take the time to write a good summary. Why is it needed? What does it do? When fixing bugs try to avoid just writing “See original issue” – clarify what the problem was and how you’ve fixed it. -->

    ### Steps to reproduce
    <!-- Step-by-step testing instructions. For new user flows, consider instead stating the goal of the workflow and see if your PR reviewer can accomplish the workflow without specific steps! -->

    ### Testing information
    <!-- This is your opportunity to break out individual scenarios that need testing (when necessary) and/or include a checklist for the reviewer to go through. Consider documenting the following from your own completed testing: devices used, alternate workflows, edge cases, affected areas, critical flows, areas not tested, and any remaining unknowns. Provide feedback on this new section of the PR template through Sept 30, 2024 to Apps Quality; additional context here: https://woomobilep2.wordpress.com/2024/05/06/woocommerce-mobile-quality-report-march-april/#comment-12036 -->

    ### The tests that have been performed
    <!-- To give the reviewer an idea of what could be missed in terms of testing -->

    ### Images/gif
    <!-- Include before and after images or gifs when appropriate. -->

    - [ ] I have considered if this change warrants release notes and have added them to `RELEASE-NOTES.txt` if necessary. Use the "[Internal]" label for non-user-facing changes.

    <!-- Pull request guidelines: https://github.com/woocommerce/woocommerce-android/blob/develop/docs/pull-request-guidelines.md -->
