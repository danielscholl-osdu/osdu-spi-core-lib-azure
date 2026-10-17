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

Merging the release PR tags `vX.Y.Z`, creates the GitHub release, attaches the jar and pom to it, and publishes them to the package host. Only the release App can create, move or delete a `v*` tag, and the `publish` environment accepts runs from `main` only.

## Forcing a version

To release a version the commits would not produce, such as a major for a change that is not breaking in code, run **Release Override** from the Actions tab. Choose `major`, `minor` or `patch`, or type an exact version. It opens a PR whose commit carries a `Release-As:` footer; once that PR merges, the release PR moves to the requested version.

## Trying a change in a service before release

Run **Pre-release** from the Actions tab on your branch. It publishes `<version>-<branch>-SNAPSHOT` to `<host>/maven-snapshots`, and each run replaces the last one. Point the service fork at it by adding that URL as a repository with snapshots enabled and pinning the snapshot version. Snapshots expire 30 days after their last upload.

Without the workflow, `mvn install` puts the library in your local repository, where a local service build finds it.

## Restoring the package host

The GitHub releases hold every published jar and pom. If the host loses a version, run **Publish** with that tag: it uploads the attached files without rebuilding. Publish refuses a version whose pom is already on the host, and replaces the rest of a version that has no pom, which is how an interrupted upload is finished.

## Dependencies

Dependabot opens one grouped PR a week for Maven minor and patch updates (`fix(deps)`, so merging it cuts a patch release) and one for workflow actions (`ci(deps)`, no release). Major dependency upgrades are made by hand.

A grouped PR can raise Spring Boot or the Azure SDK BOM by a minor version, and that still ships as a library patch. Services pin exact versions and take a release through a bump PR that runs their own tests, so they see the change before they deploy it.

## Building locally

```bash
CI_JOB_TOKEN= mvn --settings=.mvn/community-maven.settings.xml verify
```

`os-core-common` comes from the OSDU community GitLab registry. The empty `CI_JOB_TOKEN` makes that read anonymous.
