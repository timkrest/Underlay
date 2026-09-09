#!/usr/bin/env bash
# Every file in a source set carries an SPDX header. A file copied out of the repository takes its
# provenance with it; git history stays behind.
#
# Usage: check-license-headers.sh
set -euo pipefail

marker="SPDX-License-Identifier: Apache-2.0"
missing=()

while IFS= read -r file; do
    head -20 "$file" | grep -qF "$marker" || missing+=("$file")
done < <(git ls-files | grep -E '/src/.*\.kts?$')

if ((${#missing[@]} > 0)); then
    printf 'No licence header:\n'
    printf '  %s\n' "${missing[@]}"
    exit 1
fi

printf 'Licence header present in every source file.\n'
