import React, {Component} from 'react';
import Dimensions from 'react-dimensions';
import d3 from 'd3';
import http from 'http';
import configureStore from '../store/configureStore';
import Home from '../components/Home';

import frontendConfig from '../config/FrontendConfig';
import ActionTypes from '../constants/ActionTypes';
import matStyles from 'materialize-css/bin/materialize.css';

import EventBus from 'vertx3-eventbus-client';

const store = configureStore();

/**
 * setup the pipe between receiving data via websocket and updating the stores
 * @type {WSTrainPosition}
 */


/*
 Launches the regular GET call to refresh the store.
 Train positions and station boards have different refresh rates.
 */
frontendConfig.get().then(function (config) {

  const eventBus = new EventBus(config.url.eventbus);
  eventBus.onopen = function () {
    eventBus.registerHandler('delayed-trains-positions', function (error, message) {
      if (error === null) {
        store.dispatch({
          type: ActionTypes.TRAIN_POSITION_RECEIVED,
          tsv: message.body,
          timestamp: new Date().getTime()
        });
      } else {
        console.error(error, 'trains.positions');
      }
    });
  };

  const fGetStationBoardStats = function () {
    http.get(config.url.station_board, function (res) {
        var data = '';
        res.on('data', function (chunk) {
            data += chunk;
          })
          .on('end', function () {
            store.dispatch({
              type: ActionTypes.STATION_BOARD_STATS_RECEIVED,
              data: JSON.parse(data || '{}'),
              timestamp: new Date().getTime()
            })
          });
      })
      .on('error', function (err) {
        console.error(err, config.url.station_board);
      });
  };

  fGetStationBoardStats();

  setInterval(fGetStationBoardStats, config.delay.stationBoard);
})
;


class Map extends Component {

  render() {
    const h = d3.select('main').node().getBoundingClientRect().height;
    return (
      <div className={matStyles.container} style={{height:h}}>
        <Home store={store}/>
      </div>
    );
  }
}
;

export default Dimensions()(Map);

//        <Provider store={store}>
//...
//        </Provider>
