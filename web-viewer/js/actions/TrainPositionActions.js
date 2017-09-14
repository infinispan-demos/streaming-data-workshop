import {TRAIN_POSITION_RECEIVED} from '../constants/ActionTypes';

export function updatePositions(data) {
  return {
    type: TRAIN_POSITION_RECEIVED,
    timestamp:new Date().getTime(),
    data
  }
}
