# Requirements

* Install `oc`
  * Requires OpenShift Client 3.6.
* Install `kubectl`
  * Requires Kubernetes 1.6
* Install [`kubetail` 1.2.1](https://github.com/johanhaleby/kubetail/tree/1.2.1)
  * Helps with keeping track of pods
* Install `NVM` (Node.js Version Manager)
  * Makes it wasy to work with multiple Node.js versions 


# Running Workshop

Start by calling:

    ./start-all.sh

This script starts OpenShift with the service catalog and installs templates.
Then, it creates a data grid using those templates and deploys all components.

Once all components have been deployed, you can verify that the data grid is accessible by calling (the output might vary slightly):

    $ curl http://workshop-main-myproject.127.0.0.1.nip.io/test
    {
      "get(hello)" : "world",
      "topology" : "[172.17.0.10:11222, 172.17.0.11:11222, 172.17.0.12:11222]"
    }

The `/test` URI stores a key/value pair `hello`/`world` and then retrieves the value associated with key `hello`.
It also returns the topology of the data grid which should include 3 nodes (the IP addresses might vary).

The data grid can also be visualized by opening the browser to the address:
[http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer](http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer)

If you select the `default` cache, which should be selected by default, you should see that 2 of the 3 nodes have a dot.
This represents the nodes that contain the key/value pair inserted when calling `/test` URI.
As you progress with the workshop, you can see the impact of the changes or data injections in the data grid using this visualization tool.  

Next, start the delayed train dashboard class (`dashboard.DelayedDashboard`) from your IDE. 
This will load a JavaFX dashboard which will slowly fill up with delayed trains.

Finally, the workshop visualizes the location of those trains that are delayed in a map.
To access the map, load up [http://localhost:3000](http://localhost:3000) in your browser.
You see a map and some moving dots which represent the positions of the delayed trains.

If running the workshop offline, the map might not be available.
In that case, you can still verify that the delayed train positions are being correctly published by opening:
[http://delayed-trains-myproject.127.0.0.1.nip.io/positions](http://delayed-trains-myproject.127.0.0.1.nip.io/positions) 

The output of should resemble:

    train_id        train_category  train_name      train_lastStopName      position_lat    position_lng    position_bearing
    84/26411/18/24/95       R       R 7377  Sonceboz-Sombeval       47.194977       7.166263        200.0
    84/26701/18/20/95       R       R 11065 Coppet  46.316335       6.187086        50.0
    ...


 
# Debugging

To get more information about the deployed modules, you can head to the 
[OpenShift console](https://127.0.0.1:8443/console/project/myproject)
and click on each pod to see its logs.

Alternatively, using `kubetail` tool you can track all logs for modules related to this project:

    kubetail -l project=workshop


# Workshop Steps

For workshop attendees, the steps to run the workshop are slightly different.
The aim of these instructions is to discover the capabilities that the workshop tries to convey and slowly get confidence on the system.

Start OpenShift by calling:

    ./start-openshift.sh

Load the [OpenShift console](https://127.0.0.1:8443/console) and select the Infinsipan Ephemeral service.
Enter the following details leaving the rest of parameters as they are:

* `APPLICATION_NAME`: datagrid
* `MANAGEMENT_PASSWORD`: developer
* `MANAGEMENT_USER`: developer
* `NUMBER_OF_INSTANCES`: 3 

Once all pods of the data grid service are up and running, load up the data grid visualizer and verify that you can see 3 nodes:
[http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer](http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer)

Next, deploy all modules by calling:

    ./deploy-all.sh 

More instructions to come...


# Deploying Code Changes

When making code changes in a given module, you can deploy these changes by doing:

    cd <module-name>
    ./redeploy.sh

In some cases, you might also want to deploy changes to a module as well as any dependencies.
To do that, call:

    cd <module-name
    ./redeploy.sh -am

Finally, you might at times make wide ranging changes to multiple modules.
To deploy all these changes, you can simply call the following from the root of this repo:

    ./redeploy-all.sh 
