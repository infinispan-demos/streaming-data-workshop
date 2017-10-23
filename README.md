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

To get injectors running, start delayed dashboard from IDE, see below.

Once data grid is running, changes to datagrid deployment can be made calling `datagrid/redeploy.sh`.
Injectors can also be redeployed using `station-boards-injector/redeploy.sh`.
Delayed train positions snapshot generator can be redeployed using `delayed-train-positions/redeploy.sh` 

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


# Delayed train position snapshot

To obtain a snapshot of the train positions of delayed trains, do:

    curl http://delayed-train-positions-myproject.127.0.0.1.nip.io/position
    
You should see something like:

    train_id        train_category  train_name      train_lastStopName      position_lat    position_lng    position_bearing
    84/26411/18/24/95       R       R 7377  Sonceboz-Sombeval       47.194977       7.166263        200.0
    84/26701/18/20/95       R       R 11065 Coppet  46.316335       6.187086        50.0
    ...

There's no direct mapping between the station boards data streaming and train positions stream
(this should be stressed in workshop since often the data streams don't match up).

So, the way the code currently gets around the issue is the following:
If there's a delayed train amongst the train positions of a particular train route name, it picks that train to track it.
If none of those trains are delayed, it just picks one of the trains.


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
