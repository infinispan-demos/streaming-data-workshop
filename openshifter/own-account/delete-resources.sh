#!/bin/bash

NAME=${1}

gcloud compute disks delete $NAME-master-docker --quiet
gcloud compute disks delete $NAME-master-root --quiet
gcloud compute firewall-rules delete $NAME-internal --quiet
gcloud compute firewall-rules delete $NAME-master --quiet
gcloud compute firewall-rules delete $NAME-all --quiet
gcloud compute firewall-rules delete $NAME-infra --quiet
gcloud compute addresses delete $NAME-master --quiet
gcloud compute networks delete $NAME --quiet
