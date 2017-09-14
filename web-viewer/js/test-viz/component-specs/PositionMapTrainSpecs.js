import React from 'react';
import {Provider} from 'react-redux';

import data  from '../data/train-1';
import PositionMapTrains from '../../components/PositionMapTrains';
import SpecsOne from './SpecsOne';
import configureStore from 'redux-mock-store';

const mockStore = configureStore();
let store = mockStore({
  TrainPosition: {
    positions: data
  },
  MapLocation: {
    location: {
      bounds: {
        lngMin: 10,
        lngMax: 10.1,
        latMin: 45,
        latMax: 45.1
      }
    }
  }
});


export default React.createClass({
  render() {
    return (
      <Provider store={store}>
        <div>
          <SpecsOne title="zoom=11">
            <svg>
              <PositionMapTrains
                zoom={11}
                width={150}
                height={150}
                positions={data}
              />
            </svg>
          </SpecsOne>
          <SpecsOne title="zoom=10">
            <svg>
              <PositionMapTrains
                zoom={10}
                width={150}
                height={150}
                positions={data}
              />
            </svg>
          </SpecsOne>
          <SpecsOne title="zoom=9">
            <svg>
              <PositionMapTrains
                zoom={9}
                width={150}
                height={150}
                positions={data}
              />
            </svg>
          </SpecsOne>
        </div>
      </Provider>
    );
  }
});
