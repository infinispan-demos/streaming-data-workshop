import React from 'react';
import {Provider} from 'react-redux';

import data  from '../data/stationboard-1';
import PositionMapStationBoardStats from '../../components/PositionMapStationBoardStats';
import SpecsOne from './SpecsOne';
import configureStore from 'redux-mock-store';

const mockStore = configureStore();

export default React.createClass({
  render() {
    let store = mockStore({
      StationBoard: {
        stats: data
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

    return (
      <Provider store={store}>
        <div>
          <SpecsOne title="zoom=11">
            <svg>
              <PositionMapStationBoardStats
                zoom={11}
                width={150}
                height={150}
              />
            </svg>
          </SpecsOne>
          <SpecsOne title="zoom=9">
            <svg>
              <PositionMapStationBoardStats
                zoom={9}
                width={150}
                height={150}
              />
            </svg>
          </SpecsOne>
          <SpecsOne title="zoom=7">
            <svg>
              <PositionMapStationBoardStats
                zoom={7}
                width={150}
                height={150}
              />
            </svg>
          </SpecsOne>
        </div>
      </Provider>
    );
  }
});
