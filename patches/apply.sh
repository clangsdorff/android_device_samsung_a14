#!/bin/bash
# applies the a14 patches on top of the synced tree, skips the ones already applied
set -e
top=$(cd "$(dirname "$0")/../../../.." && pwd)
dir=$(cd "$(dirname "$0")" && pwd)
for p in "$dir"/*/; do
    proj=$(basename "$p" | tr _ /)
    for f in "$p"*.patch; do
        if git -C "$top/$proj" log -100 --format=%s | grep -qxF -- "$(git mailinfo /dev/null /dev/null < "$f" | sed -n 's/^Subject: //p')"; then
            echo "skip $proj $(basename "$f")"
            continue
        fi
        echo "apply $proj $(basename "$f")"
        git -C "$top/$proj" -c user.name=Langsdorff -c user.email=131063968+clangsdorff@users.noreply.github.com am -q --3way "$f" || { git -C "$top/$proj" am --abort; exit 1; }
    done
done
