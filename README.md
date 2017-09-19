# Requirements

* Install NVM
* Install `oc`
  * Requires OpenShift Client 3.6.
* Install `kubectl`
  * Requires Kubernetes 1.6


# Running data grid 

    ./start-openshift.sh
    cd datagrid
    ./deploy.sh
    ... make changes
    ./redeploy.sh

## Testing the data grid

After calling either of `datagrid/deploy.sh` or `datagrid/redeploy.sh`, call `datagrid/test.sh` to quickly test that the data grid responds. 


# Train position display

Run `delayed.train.positions.HttpApp` class so that train positions are served via REST API.

Then run:

    cd web-viewer
    nvm use 4.2
    npm install
    npm start

You should see these two lines:

    Listening at localhost: 3000
    webpack built ... in NNNNmss

The UI currently runs taking a snapshot of a REST endpoint with all the current train positions and displaying that.
The UI periodically requests this snapshot, default is 3 seconds.
We could consider more optimal ways of doing it? Or more streaming based? HTTP/2?
Might be a nice improvement after first workshop, need Thomas' input on this.
