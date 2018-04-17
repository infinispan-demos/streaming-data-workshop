# OpenShift Provisioning

This file explains how to provision OpenShift clusters in cloud environments.
For the time being it focuses on Google Cloud.


## Own Account

Use these instructions if provisioning on a Google account that you own.

TODO: Explain values


### Create Google Cloud Project

TODO


### Start OpenShift

```bash
> gcloud auth login ${EMAIL}
> gcloud config set project ${PROJECT_NAME}
> gcloud config set compute/region ${PROJECT_REGION}
> gcloud config set compute/zone ${PROJECT_ZONE}
> docker run -e -ti -v \
    `pwd`:/root/data docker.io/osevg/openshifter \
    create ${OPENSHIFTER_CLUSTER_NAME}
```

Upon completion, head to the Google Cloud console and find out the OpenShift master IP address.
The OpenShift console should be available at:

[`https://console.${OPENSHIFTER_CLUSTER_NAME}.${OPENSHIFT_MASTER_IP}.nip.io:8443/console`](https://console.${OPENSHIFTER_CLUSTER_NAME}.${OPENSHIFT_MASTER_IP}.nip.io:8443/console)


### Stop OpenShift

```bash
> docker run -e -ti -v \
    `pwd`:/root/data docker.io/osevg/openshifter \
    destroy ${OPENSHIFTER_CLUSTER_NAME}
```

If getting timeouts when destroying the OpenShift cluster, call `delete-resources.sh` to complete cleanup:

```bash
> ./delete-resources.sh ${OPENSHIFTER_CLUSTER_NAME}
```


## Test Account

Use these instructions if provisioning on a test Google account handed out to you during a workshop.


### Start OpenShift

TODO


# Common Errors


## `NoValidConnectionsError: [Errno None] Unable to connect to port 22`

You might see this error when trying to provision OpenShift.
If that happens, simply repeat the call to create the OpenShift cluster.
OpenShifter tool has been designed to be idempotent. 
This means that you can call create on OpenShifter multiple times without problems.
In fact, calling create is common if encountering any problems. 
