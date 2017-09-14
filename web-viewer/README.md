


# web-realtime-viewer (web-realtime-viewer)

> Swiss train real time viewer

## Running your project

##Prerequisite

    nvm use 4.2

#Develop

The generated project includes a development server on port `3000`, which will rebuild the app whenever you change application code. To start the server (with the dev-tools enabled), run:

    npm start

To run the server with the dev-tools disabled, run:

    DEBUG=false npm start


##Developing viz widgets

In can be a pain to develop widget and have to browse into the application to see them through various configuration.
For this purpose, we set up a page where components are packed. Head to http://localhost:3003 after having launched:
 
     npm run-script test-viz
     

To add more test widget, open `test-viz/Main.js`and follow the StationBoard example, with both various size and mock data.

#Build
To build for production, this command will output optimized production code:

```bash
$ npm run build
```

