import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import d3 from 'd3';
import {bindActionCreators} from 'redux';
import * as TrainPositionActions from '../actions/TrainPositionActions';
import  '../../css/app.css';
import styles from './PositionMapCFF.css'
import { Map, Marker, Popup, TileLayer } from 'react-leaflet';
import _ from 'lodash'
import * as StationBoardActions from '../actions/StationBoardActions';
import * as MapLocationActions from '../actions/MapLocationActions';

let i = 0;

class PositionMapStationBoardStats extends Component {
  constructor() {
    super();
    let _this = this;

    _this.lastUpdated = {};
  }

  componentDidMount() {
    var _this = this;
    _this._setupD3(ReactDOM.findDOMNode(this));
  }

  shouldComponentUpdate(props) {
    this._renderD3(ReactDOM.findDOMNode(this), props);
    // always skip React's render step
    return false;
  }

  _setupD3(el) {
    let _this = this;

    _this._dim = {
      height: parseInt(_this.props.height),
      width: parseInt(_this.props.width)
    };

    d3.select(el).selectAll().empty();
    _this._elements = {
      svg: d3.select(el)
        .attr({
          width: _this._dim.width,
          height: _this._dim.height,
          class: "train_overlay"
        })
    };

    _this._elements.gMainStationBoardStats = _this._elements.svg.append('g')
      .attr({
        class: 'station-board-stats-main'
      });
    _this._renderD3(el, _this.props)
  }

  /*
   return true if data is to be rendered
   */
  _updateData(bounds, stationBoardStats, timestamp) {
    let _this = this;

    if ((timestamp === _this.lastUpdated.timestamp) && _.isEqual(_this.lastUpdated.bounds, bounds)) {
      return false;
    }
    _this.lastUpdated.timestamp = timestamp;

    let {lngMin, lngMax, latMin, latMax}  = bounds;

    _this._stationBoardStats = _.chain(stationBoardStats)
      .map(function (p) {
        p.x = p.stop.location.lng;
        p.y = p.stop.location.lat;
        return p;
      })
      .filter(function (p) {
        return (p.x >= lngMin) && (p.x <= lngMax) && (p.y >= latMin) && (p.y <= latMax);
      })
      .sortBy(function (p) {
        return -p.total;
      })
      .value();


    return true;
  }

  _renderD3StationBoardStats(el, bounds, zoom, callbacks) {
    let _this = this;


    let hasBoundMoved = !_.isEqual(bounds, _this.lastUpdated.bounds);
    _this.lastUpdated.bounds = bounds;

    let gStats = _this._elements.gMainStationBoardStats
      .selectAll('g.station-board-stats')
      .data(_this._stationBoardStats, function (d) {
        return d.stop.id;
      });
    let gNew = gStats.enter()
      .append('g')
      .attr('class', 'station-board-stats');
    gNew.attr('transform', function (p) {
      return 'translate(' + _this._scales.x(p.x) + ',' + (_this._scales.y(p.y)) + ')';
    });

    if (callbacks && callbacks.mouseover) {
      gNew.on('mouseover', callbacks.mouseover)
    }

    gNew.append('circle');
    gNew.append('path')
      .attr({
        class: 'delayed stationboard_delayed'
      });

    var factor;
    if (zoom <= 8) {
      factor = 0.1;
    } else if (factor >= 11) {
      factor = 1;
    } else {
      factor = ((zoom - 8) + (11 - zoom) * 0.25) / 3;
    }

    let fRadius = function (d) {
      return 1 + factor * 2 * (d.total + 1);
    };

    gStats.attr('transform', function (p) {
      return 'translate(' + _this._scales.x(p.x) + ',' + (_this._scales.y(p.y)) + ')';
    });

    gStats.select('circle')
      .attr({
        r: fRadius
      });

    gStats.selectAll('path.delayed')
      .attr('d', d3.svg.arc()
        .innerRadius(0)
        .outerRadius(fRadius)
        .startAngle(0)
        .endAngle(function (d) {
          return d.total ? (2 * Math.PI * d.delayed / d.total) : 0;
        })
      );

    gStats.exit().remove()
  };

  _renderD3(el, newProps) {
    let _this = this;

    const {dispatch} = newProps;
    const actions = {
      ...bindActionCreators(StationBoardActions, dispatch),
      ...bindActionCreators(MapLocationActions, dispatch)
    };

    let {lngMin, lngMax, latMin, latMax}  = newProps.MapLocation.location.bounds;
    _this._scales = {
      x: d3.scale.linear().range([0, _this._dim.width]).domain([lngMin, lngMax]),
      y: d3.scale.linear().range([0, _this._dim.height]).domain([latMax, latMin])
    };


    if (_this._updateData(
        newProps.MapLocation.location.bounds,
        newProps.StationBoard.stats,
        newProps.StationBoard.timestamp)) {
      _this._renderD3StationBoardStats(el,
        newProps.MapLocation.location.bounds,
        newProps.zoom,
        {
          mouseover: function (p) {
            actions.getStationBoardDetails(p.stop.id);
          }
        }
      );
    }
  }

  render() {


    return (
      <g/>
    );
  }
}

export default connect(state => state)(PositionMapStationBoardStats);
