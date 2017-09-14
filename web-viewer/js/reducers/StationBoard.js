'use strict';
import ActionTypes from '../constants/ActionTypes';
import StatusTypes from '../constants/StatusTypes';
import frontendConfig from '../config/FrontendConfig';
import http from 'http';

let defaultState = {
  stats: {},
  details: {status: StatusTypes.UNAVAILABLE},
  timestamp: new Date().getTime()
};

export default function (state = defaultState, action) {
  let _this = this;
  switch (action.type) {
    case ActionTypes.STATION_BOARD_STATS_RECEIVED:
      return {...state, stats: action.data.stats, timestamp: action.timestamp};
    case ActionTypes.STATION_BOARD_DETAILS_RECEIVED:
      return {...state, details: {status: StatusTypes.SUCCESS, board: action.board}};
    case ActionTypes.STATION_BOARD_DETAILS_FETCHING:
      return {...state, details: {status: StatusTypes.FETCHING, stopName: action.stopName}};
    case ActionTypes.STATION_BOARD_DETAILS_EMPTY:
      return {...state, details: {status: StatusTypes.UNAVAILABLE}};
    default:
      return state;
  }

}
