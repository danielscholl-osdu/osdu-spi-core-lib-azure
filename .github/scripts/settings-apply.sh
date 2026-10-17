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
# Applies .github/rulesets/*.json (create or update by name), .github/merge-settings.json
# and .github/security-on.json to the repository. Safe to run repeatedly.
#
# Arguments:
#   $1          Repository full name (owner/repo)
#   --dry-run   Print planned actions; no mutations
#
# Environment:
#   GH_TOKEN    token with Administration write on the repository

set -euo pipefail

REPO=""
DRY_RUN=false
for a in "$@"; do
  if [[ "$a" == "--dry-run" ]]; then DRY_RUN=true; else REPO="$a"; fi
done
[[ -n "$REPO" ]] || { echo "Usage: $0 <owner/repo> [--dry-run]"; exit 1; }

run() {
  if [[ "$DRY_RUN" == "true" ]]; then echo "  (dry run) $*"; else "$@"; fi
}

echo "Rulesets"
for file in .github/rulesets/*.json; do
  name="$(jq -r '.name' "$file")"
  id="$(gh api --paginate "repos/$REPO/rulesets" --jq ".[] | select(.name == \"$name\") | .id" | head -n1)"
  if [[ -n "$id" ]]; then
    run gh api --method PUT "repos/$REPO/rulesets/$id" --input "$file" --silent
    echo "  ✅ updated '$name'"
  else
    run gh api --method POST "repos/$REPO/rulesets" --input "$file" --silent
    echo "  ✅ created '$name'"
  fi
done

echo "Merge settings"
run gh api --method PATCH "repos/$REPO" --input .github/merge-settings.json --silent
echo "  ✅ applied .github/merge-settings.json"

echo "Security settings"
run gh api --method PUT "repos/$REPO/vulnerability-alerts" --silent
run gh api --method PATCH "repos/$REPO" --input .github/security-on.json --silent
echo "  ✅ applied .github/security-on.json"
