import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
//import * as TrainPositionActions from '../actions/TrainPositionActions';
//import * as StationBoardActions from '../actions/StationBoardActions';
//import * as MapLocationActions from '../actions/MapLocationActions';
import styles from '../../css/app.css';

import PositionMap from './PositionMap';
import StationBoard from './StationBoardDetails';
import Timer from './Timer';

class Home extends Component {
  render() {
    const {MapLocation, TrainPosition, StationBoard, dispatch} = this.props;
    //const actions = {
    //  ...bindActionCreators(TrainPositionActions, dispatch)
    //  , ...bindActionCreators(MapLocationActions, dispatch)
    //  , ...bindActionCreators(StationBoardActions, dispatch)
    //};
    let tTrain = _.chain(TrainPosition.positions)
      .map('timeStamp')
      .max()
      .value();

    return (
      <main>
        <h4 className={styles.text}>Positions updated on map <Timer t0={TrainPosition.timestamp}/> ago. Last train
          updated from CFF <Timer t0={tTrain}/> ago</h4>

        <div style={{height:global.window.innerHeight*0.8, width:'80%'}}>
          <PositionMap
                       //positions={TrainPosition.positions}
                       //location={MapLocation.location}
                       //stationBoardStats={StationBoard.stats}
                       //onLocationChanged={actions.updateLocation}
          />
        </div>


      </main>
    );
  }
}

export default connect(state => state)(Home)
