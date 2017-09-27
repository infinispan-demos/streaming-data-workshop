# Requirements

* Install NVM
* Install `oc`
  * Requires OpenShift Client 3.6.
* Install `kubectl`
  * Requires Kubernetes 1.6
* Install [`kubetail` 1.2.1](https://github.com/johanhaleby/kubetail/tree/1.2.1)
  * Helps with keeping track of pods


# Running data grid and injectors 

    ./start-openshift.sh
    cd station-boards-injector
    ./deploy.sh

Once data grid is running, changes to datagrid deployment can be made calling `datagrid/redeploy.sh`.
Injectors can also be redeployed using `station-boards-injector/redeploy.sh`. 

You can track what the station board injector is doing by calling:

    kubetail -l app=station-boards-injector

You can also track output of datagrid nodes calling:

    kubetail -l application=datagrid


## Testing the data grid

After calling either of `datagrid/deploy.sh` or `datagrid/redeploy.sh`, call `datagrid/test.sh` to quickly test that the data grid responds. 


# Delayed train dashboard

As a stepping stone for workshop attendees, a Java FX dashboard of delayed trains has been developed.

The dashboard, which can be run from the IDE, connects to a URL exposed by a verticle and it triggers the injection of station boards.
Behind the scene, a verticle that runs a continuous query looking out for delayed trains publishes those delays to the event bus.
The event bus messages are then shipped via websocket to the Java FX dashboard.


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
