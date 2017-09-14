'use strict';
import ActionTypes from '../constants/ActionTypes';
import fetch from 'isomorphic-fetch';
import frontendConfig from '../config/FrontendConfig';


export function updateStationBoardStats(data) {
  return {
    type: ActionTypes.STATION_BOARD_STATS_RECEIVED,
    timestamp: new Date().getTime(),
    data
  }
}

export function updateStationBoardDetails(data) {
  return {
    type: ActionTypes.STATION_BOARD_DETAILS_RECEIVED,
    board: data
  }
}

export function emptyStationBoardDetails() {
  return {
    type: ActionTypes.STATION_BOARD_DETAILS_EMPTY
  }
}


export function getStationBoardDetails(stopId, stopName) {
  return function (dispatch) {
    return frontendConfig.get().then(function (config) {
      let url = config.url.station_board + '/' + stopId;
      return fetch(url)
        .then(response => response.json())
        .then(json => dispatch(updateStationBoardDetails(json)))
        .catch(function (err) {
          console.error(err, url);
        });
    });
  }
}
