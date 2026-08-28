#!/usr/bin/env bash
# Guards the recorded API against what is already published: a declaration may leave the API, but
# not without the version saying so. Before 1.0.0 a removal needs a minor bump, from 1.0.0 on a
# major one. apiCheck only guards the dump against the code, which says nothing about releases.
#
# Usage: check-api-compatibility.sh <version>
set -euo pipefail

version=${1:?version to check}

# Only x.y.z tags are candidates. A prerelease promises nothing, and `sort -V` puts 0.2.0 before
# 0.2.0-rc1 - the opposite of semver - so leaving prereleases in would pick one over its own
# release and compare against an API that was never published.
version_core=${version%%-*}
head_commit=$(git rev-parse HEAD)

# The newest release this version answers for: the highest tag that is not above it, so a patch to
# an older series is compared against that series and not against a later minor release. A tag on
# the commit being checked is the release being made, not a published one, so it is skipped.
released=""
while read -r candidate; do
  [ -n "$candidate" ] || continue
  [ "$(printf '%s\n%s\n' "$candidate" "$version_core" | sort -V | head -1)" = "$candidate" ] || continue
  released=$candidate
done <<EOF
$(
  git tag --list 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | while read -r tag; do
    [ "$(git rev-parse "${tag}^{commit}")" = "$head_commit" ] && continue
    printf '%s\n' "${tag#v}"
  done | sort -V
)
EOF

if [ -z "$released" ]; then
  echo "nothing released before $version, nothing to compare against"
  exit 0
fi

tag="v$released"
released_api=$(mktemp)
if ! git show "${tag}:underlay/api/underlay.api" > "$released_api" 2>/dev/null; then
  echo "$tag carries no API dump, nothing to compare against"
  exit 0
fi

removed=$(comm -13 <(sort underlay/api/underlay.api) <(sort "$released_api"))
if [ -z "$removed" ]; then
  echo "every declaration of $tag is still here"
  exit 0
fi

series() {
  core=${1%%-*}
  if [ "$(printf '%s' "$core" | cut -d. -f1)" = 0 ]; then
    printf '%s' "$core" | cut -d. -f1,2
  else
    printf '%s' "$core" | cut -d. -f1
  fi
}

echo "gone since $tag:"
echo "$removed"
if [ "$(series "$version")" = "$(series "$released")" ]; then
  echo "::error::$version drops declarations of $tag without leaving its release series."
  echo "::error::Until 1.0.0 a removal needs a minor bump; from 1.0.0 on, a major one."
  exit 1
fi
echo "$version leaves the $released series, so the removals are declared"
