import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import d3 from 'd3';
import {bindActionCreators} from 'redux';
import * as MapLocationActions from '../actions/MapLocationActions';
import styles from '../../css/app.css';
import classes from './PositionMapGoogle.css'
import shouldPureComponentUpdate from 'react-pure-render/function';
import GoogleMap from 'google-map-react';
import PositionMapTrain from './PositionMapTrains';
import PositionMapStationBoardStats from './PositionMapStationBoardStats';


class PositionMapGoogleOverlayData extends Component {
  componentDidMount() {
    var _this = this;
  }

  shouldComponentUpdate = shouldPureComponentUpdate;

  _onBoundsChange() {
    let _this = this;
  }

  render() {
    let _this = this;


    const {lat, lng, height, zoom, width, store, dispatch} = _this.props;


    return (
      <svg lat={lat}
           lng={lng}
           height={height}
           width={width}
      >
        <PositionMapStationBoardStats
          lat={lat}
          lng={lng}
          zoom={zoom}
          height={height}
          width={width}
          store={store}

        />
        <PositionMapTrain
          lat={lat}
          lng={lng}
          zoom={zoom}
          height={height}
          width={width}
          store={store}
        />
      </svg>
    );
  }
}

export default connect(state => state)(PositionMapGoogleOverlayData);
