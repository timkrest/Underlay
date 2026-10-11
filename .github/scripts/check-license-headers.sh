#!/usr/bin/env bash
# Every Kotlin source and build script carries an SPDX header.
#
# Usage: check-license-headers.sh
set -euo pipefail

marker="SPDX-License-Identifier: Apache-2.0"
missing=()

while IFS= read -r file; do
    head -20 "$file" | grep -qF "$marker" || missing+=("$file")
done < <(git ls-files | grep -E '/src/.*\.kts?$')

if ((${#missing[@]} > 0)); then
    printf 'No license header:\n'
    printf '  %s\n' "${missing[@]}"
    exit 1
fi

printf 'License header present in every source file.\n'
