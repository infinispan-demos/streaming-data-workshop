# Requirements

* Install NVM

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

The UI currently runs taking a snapshot of a REST endpoint and displaying that.
We could consider more optimal ways of doing it? Or more streaming based? HTTP/2?
