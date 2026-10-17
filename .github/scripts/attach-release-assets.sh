#!/usr/bin/env bash
# Copyright © Microsoft Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Attaches a released version's jar and pom, read from an existing Maven repository, to the
# GitHub release for its tag, creating the release when the tag has none. Publish restores a
# version from these assets. Used to bring versions released before the move.
#
# Arguments:
#   $1  Repository full name (owner/repo)
#   $2  Tag, for example v3.0.4
#   $3  Maven repository URL that holds the version
#
# Environment:
#   GH_TOKEN    token with Contents write on the repository

set -euo pipefail

REPO="${1:?owner/repo}"
TAG="${2:?tag}"
SOURCE="${3:?maven repository url}"
[[ "$TAG" =~ ^v([0-9]+\.[0-9]+\.[0-9]+)$ ]] || { echo "Tag $TAG is not of the form vX.Y.Z"; exit 1; }
VERSION="${BASH_REMATCH[1]}"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
for ext in jar pom; do
  file="core-lib-azure-$VERSION.$ext"
  url="$SOURCE/org/opengroup/osdu/core-lib-azure/$VERSION/$file"
  curl -fsSL -o "$work/$file" "$url"
  [[ "$(sha1sum "$work/$file" | cut -d' ' -f1)" == "$(curl -fsSL "$url.sha1" | cut -c1-40)" ]] ||
    { echo "Checksum mismatch for $url"; exit 1; }
done

if ! gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  gh release create "$TAG" --repo "$REPO" --verify-tag --latest=false --title "$TAG" \
    --notes "Released before the move to GitHub. The jar and pom are the ones published to $SOURCE."
fi
gh release upload "$TAG" --repo "$REPO" --clobber "$work/core-lib-azure-$VERSION.jar" "$work/core-lib-azure-$VERSION.pom"
echo "Attached core-lib-azure $VERSION to $TAG"
