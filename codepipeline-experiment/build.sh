#!/usr/bin/env bash

if [[ "${GREETING}" == "" ]]; then
  echo "GREETING is not set"
  exit 1
fi

if [[ "${ENV_TAG}" == "" ]]; then
  echo "ENV_TAG is not set"
  exit 1
fi

echo "${GREETING} `date`!!! (built for envTag=${ENV_TAG})" > index.html
