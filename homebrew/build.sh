#!/usr/bin/env bash
# Test and audit the Homebrew formula for flyprint.
#
# Usage: ./build.sh [--update-resources]
#   --update-resources   Regenerate PyPI resource stanzas before testing
set -euo pipefail

cd "$(dirname "$0")"

if [[ "${1:-}" == "--update-resources" ]]; then
    echo "==> Regenerating PyPI resource stanzas..."
    ./generate_resources.sh > /tmp/flyprint_resources.rb
    echo "    Output written to /tmp/flyprint_resources.rb"
    echo "    Paste into flyprint.rb replacing existing resource blocks."
    echo
fi

echo "==> Auditing formula..."
brew audit --formula flyprint.rb || true

echo "==> Testing install from local formula..."
brew install --formula --build-from-source flyprint.rb

echo "==> Running formula tests..."
brew test flyprint.rb

echo "==> Done. To start the service:"
echo "    brew services start flyprint"
