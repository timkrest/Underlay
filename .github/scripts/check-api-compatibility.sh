#!/usr/bin/env bash
# Fails when a released declaration is gone without a minor bump (0.x) or a major one (1.0+).
#
# Usage: check-api-compatibility.sh <version>
set -euo pipefail

version=${1:?version to check}

# Releases only: sort -V puts 0.2.0 before 0.2.0-rc1.
version_core=${version%%-*}
head_commit=$(git rev-parse HEAD)

# The newest release not above this version. A tag on HEAD is the one being made.
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
trap 'rm -f "$released_api"' EXIT
if ! git show "${tag}:underlay/api/underlay.api" > "$released_api" 2>/dev/null; then
  echo "$tag carries no API dump, nothing to compare against"
  exit 0
fi

# Two classes can declare the same member line.
declarations() {
  awk '/^\t/ { print owner " ::" $0; next } /^[^}]/ { owner = $0; print }' "$1" | sort
}

removed=$(comm -13 <(declarations underlay/api/underlay.api) <(declarations "$released_api"))
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
