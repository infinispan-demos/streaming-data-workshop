# Requirements

* Install `docker`
  * Recommended Docker version is 1.13.1.
* Install `oc`
  * Requires OpenShift Client 3.6.
* Install `kubectl`
  * Requires Kubernetes 1.6
* Install [`kubetail` 1.2.1](https://github.com/johanhaleby/kubetail/tree/1.2.1)
  * Helps with keeping track of pods
* Install `NVM` (Node.js Version Manager)
  * Makes it wasy to work with multiple Node.js versions 


# Running Workshop Step-by-Step

You can find detailed information on how to work through workshop in the [step-by-step instructions file](/workshop-steps/workshop.html).


# Running Workshop Solution

These set of instructions are designed for those that want to run the final solution of the workshop directly.

Start by calling:

    ./start-solution.sh

This script starts OpenShift with the service catalog and installs templates.
Then, it creates a data grid using those templates and deploys all components.

Once all components have been deployed, you can verify that the data grid is accessible by calling (the output might vary slightly):

    $ curl -H "Content-Type: application/json" -X POST -d '{"id":1,"name":"Oihana"}' http://simple-web-application-myproject.127.0.0.1.nip.io/api/duchess
    Duchess Added
    $ curl -X GET -i -H "Accept: application/json" http://simple-web-application-myproject.127.0.0.1.nip.io/api/duchess/1
    ...
    {"Duchess":"Oihana"}

The simple web applications is an Vert.x example that stores and retrieves key/value pairs into the data grid.
The above example first sends a `POST` request that's handled by Vert.x and converts into a store operation.
Then, it sends a `GET` call that's handled by Vert.x and results in retrieving value associated with the given key from the data grid.

The data grid can also be visualized by opening the browser to the address:
[http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer](http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer)

If you select the `default` cache, which should be selected by default, you should see that 2 of the 3 nodes have a dot.
This represents the nodes that contain the key/value pair inserted by the previous example.
As you progress with the workshop, you can see the impact of the changes or data injections in the data grid using this visualization tool.  

Next, start the delayed train dashboard class (`dashboard.DelayedDashboard`) in module `delayed-dashboard` from your IDE. 
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

    kubetail -l group=workshop
