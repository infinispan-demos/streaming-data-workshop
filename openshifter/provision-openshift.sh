#!/bin/bash

set -e -x

gcloud compute project-info add-metadata \
  --metadata google-compute-default-region=europe-west1,google-compute-default-zone=europe-west1-b

export PROJECT_ID=$(gcloud config get-value core/project)

gcloud iam service-accounts create openshifter \
  --display-name "OpenShifter Service Account"
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:openshifter@$PROJECT_ID.iam.gserviceaccount.com \
  --role roles/editor
gcloud iam service-accounts keys create openshifter.json \
  --iam-account openshifter@$PROJECT_ID.iam.gserviceaccount.com

ssh-keygen -f openshifter-key -q -N ""

sed -i -e 's/REPLACE_ME/'"$PROJECT_ID"'/g' streaming-data-workshop.yml

docker run -e -ti -v `pwd`:/root/data docker.io/osevg/openshifter create streaming-data-workshop
