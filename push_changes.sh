#!/bin/bash
set -e
set -x

# Navigate to the project directory
cd /home/danto/AndroidStudioProjects/SmsLogger

# Check git status
echo "Current git status:"
git status

# Stage changes
echo "Staging changes..."
git add README.md
git add .github/workflows/android-ci.yml

# Commit changes
echo "Committing changes..."
git commit -m "Add code coverage badge and CI workflow - Fixes #25"

# Push changes
echo "Pushing changes to the repository..."
git push

echo "Done!"
