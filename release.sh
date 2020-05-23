#! /usr/bin/sh -e
VERSION=$1

git checkout -b $1
mvn versions:set -DnewVersion=$1
git add .
git commit -m "bump version to $1"
git tag $1
git push origin tag $1