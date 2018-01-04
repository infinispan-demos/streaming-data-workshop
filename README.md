# Deep Dive
## Starting the solution

1. Launch `./start-solution.sh` and verify all the components are correctly deployed
  * Connect to `https://127.0.0.1:8443/` with `developer:developer`
  * Go to `my-project` and these applications with a single pod correctly started should be present :
    - kafka 
    - zookeeper
    - datagrid-visualizer
    - delayed-listener
    - delayed-trains
    - positions-injector
    - positions transport
    - stations-injector
    - stations-transport
    - workshop-main
    - datagrid (with 3 pods)
    
2. Launch the data injectors 
  * Start the injector (collect and put in kafka) and the transport (read from kafka and put in infinispan) 
    `curl http://workshop-main-myproject.127.0.0.1.nip.io/inject`

3. Check the datagrid visualizer `
  * Go to http://datagrid-visualizer-myproject.127.0.0.1.nip.io/infinispan-visualizer/`
  * Select 'train-positions' cache. You should see data
  * Select 'stations-boards' cache. You should see data
  
4. Start the Dashboard - run the main delayed-dashboard/DelayedDashboard and see the delayed trains

5. Start the node client in the web-viewer : `node server` and see in `http://localhost:3000/` the trains moving


# Streaming Data Workshop

See
[workshop steps](workshop-steps/workshop.html) 
([HTML preview](http://htmlpreview.github.io/?https://github.com/infinispan-demos/streaming-data-workshop/blob/master/workshop-steps/workshop.html))
for all the information you need to work through the workshop. 
