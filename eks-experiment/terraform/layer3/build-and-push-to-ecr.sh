#!/bin/bash

REPOSITORY_URL=${REPOSITORY_URL:?not set or empty}
BUILD_TAG=${BUILD_TAG:?not set or empty}

aws ecr get-login-password | docker login \
  --username AWS \
  --password-stdin ${REPOSITORY_URL}

appImage="${REPOSITORY_URL}:${BUILD_TAG}"
docker build --tag ${appImage} .
if [[ $? -ne 0 ]]; then
  echo "Failed to build Docker image"
  exit 1
fi

docker push ${appImage}
if [[ $? -ne 0 ]]; then
  echo "Failed to push Docker image"
  exit 1
fi
