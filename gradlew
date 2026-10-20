#!/bin/sh
# Convenience wrapper for local environments with Gradle installed.
# The GitHub Actions workflow installs Gradle 8.9 explicitly.
exec gradle "$@"
