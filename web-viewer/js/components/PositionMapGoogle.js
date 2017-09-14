import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import d3 from 'd3';
import {bindActionCreators} from 'redux';
import * as MapLocationActions from '../actions/MapLocationActions';
import '../../css/app.css';
import './PositionMapGoogle.css'
import shouldPureComponentUpdate from 'react-pure-render/function';
import GoogleMap from 'google-map-react';
import PositionMapGoogleOverlayData from './PositionMapGoogleOverlayData';


class PositionMapGoogle extends Component {
  componentDidMount() {
    var _this = this;
  }

  shouldComponentUpdate = shouldPureComponentUpdate;

  _onBoundsChange() {
    let _this = this;
  }

  render() {
    let _this = this;


    const {height, width, dispatch, store} = _this.props;

    const actions = {
      ...bindActionCreators(MapLocationActions, dispatch)
    };

    let {center, zoom, bounds} = _this.props.MapLocation.location;

    return (
      <div style={{height:height, width:width}}>
        <GoogleMap
          bootstrapURLKeys={{
                            key: 'AIzaSyC8NUflwgX3no42ur0wyxDZ2hG68tvmpDA'
                           }}
          center={center}
          zoom={zoom}
          onChange={actions.updateLocation}
        >
          <div className="masking"
               lat={2*bounds.latMax-bounds.latMin}
               lng={2*bounds.lngMin-bounds.lngMax}
               style={{height:height*3, width:width*3}}>
          </div>
          <PositionMapGoogleOverlayData lat={bounds.latMax}
                                        lng={bounds.lngMin}
                                        height={height}
                                        width={width}
                                        zoom={zoom}
                                        store={store}

          />

        </GoogleMap></div>
    );
  }
}

export default connect(state => state)(PositionMapGoogle);
