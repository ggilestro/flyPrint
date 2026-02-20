#!/usr/bin/env bash
# Generate Homebrew resource stanzas for flyprint dependencies.
# Queries PyPI JSON API for sdist URLs and SHA256 hashes.
#
# Usage: ./generate_resources.sh
#
# Paste the output into homebrew/flyprint.rb replacing the existing
# resource blocks.

set -euo pipefail

PACKAGES=(
    # Core
    click requests certifi charset-normalizer idna urllib3
    # CUPS
    pycups
    # GUI
    pystray Pillow six
    # pystray macOS backend
    pyobjc-core pyobjc-framework-Cocoa pyobjc-framework-Quartz
)

for pkg in "${PACKAGES[@]}"; do
    json=$(curl -sf "https://pypi.org/pypi/${pkg}/json")
    version=$(echo "$json" | python3 -c "import sys,json; print(json.load(sys.stdin)['info']['version'])")

    # Try sdist first, fall back to wheel
    sdist_url=$(echo "$json" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for f in data['releases'].get('${version}', []):
    if f['packagetype'] == 'sdist':
        print(f['url']); break
else:
    for f in data['releases'].get('${version}', []):
        if f['packagetype'] == 'bdist_wheel':
            print(f['url']); break
")
    sha256=$(echo "$json" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for f in data['releases'].get('${version}', []):
    if f['packagetype'] == 'sdist':
        print(f['digests']['sha256']); break
else:
    for f in data['releases'].get('${version}', []):
        if f['packagetype'] == 'bdist_wheel':
            print(f['digests']['sha256']); break
")

    if [ -z "$sdist_url" ]; then
        echo "  # WARNING: No sdist or wheel found for ${pkg} ${version}"
    else
        echo "  resource \"${pkg}\" do"
        echo "    url \"${sdist_url}\""
        echo "    sha256 \"${sha256}\""
        echo "  end"
    fi
    echo
done
