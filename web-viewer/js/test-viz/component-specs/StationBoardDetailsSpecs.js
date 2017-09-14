import React from 'react';
import {Provider} from 'react-redux';

import SpecsOne from './SpecsOne';
import data  from '../data/stationboard-8501120';
import StationBoard from '../../components/StationBoardDetails';
import configureStore from 'redux-mock-store';

const mockStore = configureStore();


export default React.createClass({
  render() {
    let store = mockStore({StationBoard: {details: {status: 'success', board: data}}});
    return (
      <Provider store={store}>
        <div>
          <SpecsOne title="200x100">
            <div style={{width:200, height:100}}>
              <StationBoard/>
            </div>
          </SpecsOne>
          <SpecsOne title="300x300">
            <div style={{width:300, height:200}}>
              <StationBoard/>
            </div>
          </SpecsOne>
          <SpecsOne title="400x?">
            <div style={{width:400}}>
              <StationBoard/>
            </div>
          </SpecsOne>
        </div>
      </Provider>
    );
  }
});
