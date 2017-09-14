import {createStore, combineReducers, applyMiddleware} from 'redux';
import thunkMiddleware from 'redux-thunk';
import createLogger from 'redux-logger';
import TrainPosition from '../reducers/TrainPosition';
import StationBoard from '../reducers/StationBoard';
import MapLocation from '../reducers/MapLocation';

const loggerMiddleware = createLogger();
const rootReducer = combineReducers({TrainPosition, MapLocation, StationBoard});

export default function configureStore(initialState) {

  const appMid = applyMiddleware(thunkMiddleware)(createStore);

  return appMid(
    rootReducer,
    initialState);
}

