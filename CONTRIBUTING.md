# Contributing

Changes reach `main` through pull requests. Each PR needs a code owner's approval and two passing checks: **Validation Summary** (lint, build and unit tests) and **CodeQL**. PRs are squash merged, so the PR title becomes the commit on `main`.

## PR titles pick the next version

PR titles follow [Conventional Commits](https://www.conventionalcommits.org/). release-please reads them on `main` and keeps a release PR open with the next version and changelog.

| PR title | Next release |
|---|---|
| `fix: ...`, `fix(deps): ...` | patch, 3.1.0 to 3.1.1 |
| `feat: ...` | minor, 3.1.0 to 3.2.0 |
| `feat!: ...`, or a `BREAKING CHANGE:` footer in a commit | major, 3.1.0 to 4.0.0 |
| `docs:`, `ci:`, `chore:`, `test:`, `refactor:`, `build:`, `style:` | no release on its own |

Merging the release PR tags `vX.Y.Z`, creates the GitHub release, and publishes the jar and pom to the package host.

## Forcing a version

To release a version the commits would not produce, such as a major for a change that is not breaking in code, run **Release Override** from the Actions tab. Choose `major`, `minor` or `patch`, or type an exact version. It opens a PR whose commit carries a `Release-As:` footer; once that PR merges, the release PR moves to the requested version.

## Dependencies

Dependabot opens one grouped PR a week for Maven minor and patch updates (`fix(deps)`, so merging it cuts a patch release) and one for workflow actions (`ci(deps)`, no release). Major dependency upgrades are made by hand.

## Building locally

```bash
CI_JOB_TOKEN= mvn --settings=.mvn/community-maven.settings.xml verify
```

`os-core-common` comes from the OSDU community GitLab registry. The empty `CI_JOB_TOKEN` makes that read anonymous.
