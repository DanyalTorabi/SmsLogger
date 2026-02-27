# Makefile — run any CI section locally with the exact same command used in GitHub Actions
# Usage:
#   make lint      → runs Android lint
#   make test      → runs unit tests
#   make build     → builds debug APK
#   make ci        → runs all three (lint + test + build)
#   make clean     → clean build outputs

.PHONY: lint test build ci clean

lint:
	./gradlew lint

test:
	./gradlew test

build:
	./gradlew assembleDebug

ci: lint test build

clean:
	./gradlew clean

