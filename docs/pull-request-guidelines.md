# Pull Request Guidelines

## Branch Naming

Feature or fix branch names should use the pattern `issue/ISSUEID-description` where the `ISSUEID` is the Github issue number. For example, if the issue number is 1000 and the issue is related to an error with the order list, an appropriate branch name would be `issue/1000-order-list-error`.

If there is no Github issue, you can use prefixes like `feature/` or `fix/`.

> NOTE: Features are typically large PRs (pull requests) that contain smaller PRs that build a feature and are labeled as an `epic`.

## Commits

As you commit code to these branches, don’t tag the issue number in the individual commit messages as it pollutes the pull request and makes it messier, just attach the issue number to the final pull request. Before you submit your final pull request, make sure all your branches are up to date with `trunk`.

## Anatomy of a Good Pull Request

When you are ready, please, spend time crafting a good Pull Request, since it will have a huge impact on the work of reviewers, release managers and testers.

**Title**: A good descriptive title.

**Issue**: Link to the GitHub issue this PR addresses.

**Description**: Take the time to write a good summary. Why is it needed? What does it do? When fixing bugs try to avoid just writing “See original issue” – clarify what the problem was and how you’ve fixed it.

**Testing instructions**: Step by step testing instructions. When necessary break out individual scenarios that need testing, consider including a checklist for the reviewer to go through.

**Images and Gif**: Include before and after images or gifs when appropriate.

### Labels

We have labels for different types of PRs as well as different application areas, use them accordingly. A minimum of one label should be assigned to the PR, but strongly encourage finding labels from two or more label categories.

Some examples of labels can be found below:

* **Type**: Crash, Bug, Question, Task, Enhancement, Tech Debt, Tooling, etc.
* **Status**: Do not merge, on-hold, Blocked, feature-flagged etc.
* **Priority**: Low, High, Critical etc.
* **Feature**: Login, notifications, stats, order list etc.
* **Action**: needs design, docs, feedback, api-support, etc.
* **Category**: accessibility, aanalytics, performance, design, dark mode, etc.

*This list is by no means exhaustive so please choose labels that is relevant to the feature you are working on.*

_Thank you very much for contributing to WooCommerce for Android!_
