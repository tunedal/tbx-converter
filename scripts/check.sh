#!/usr/bin/bash

set -eu

BASEDIR="$(dirname "$0")/.."

check() {
    flake8 "$BASEDIR"/{tbxconverter,tests}
    python3 -m unittest discover -s "$BASEDIR"
    mypy "$BASEDIR"/{tbxconverter,tests}
}

if [ "${1:-}" = "-c" ]; then
    while true; do
        check || true
        echo
        inotifywait -e MODIFY "$BASEDIR"/{tbxconverter,tests}/*.py
        sleep 0.1
    done
else
    check
fi
