import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import d3 from 'd3';
import {bindActionCreators} from 'redux';
import * as TrainPositionActions from '../actions/TrainPositionActions';
import * as MapLocationActions from '../actions/MapLocationActions';
import frontendConfig from '../config/FrontendConfig';

import '../../css/app.css';
import  styles from './PositionMapCFF.css'
import {Map, Marker, Popup, TileLayer} from 'react-leaflet';
import _ from 'lodash'

let i = 0;

var trainPositionUpdateDelay = 500;
frontendConfig.get().then(function (config) {
  trainPositionUpdateDelay = config.delay.trainPosition;
});

class PositionMapTrains extends Component {
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
    //look_minx=5850000&look_maxx=10540000&look_miny=45850000&look_maxy=47800000


    d3.select(el).selectAll().empty();
    _this._elements = {
      svg: d3.select(el)
        .attr({
          //viewBox: '-' + (_this._dim.width / 2) + ' -' + (_this._dim.height / 2) + ' ' + _this._dim.width + ' ' + _this._dim.height,
          //viewBox: '0 0 ' + _this._dim.width + ' ' + _this._dim.height,
          width: _this._dim.width,
          height: _this._dim.height,
          class: 'train_overlay'
        })
      //.style('overflow', 'visible')
    };
    _this._elements.gMainTrainPositions = _this._elements.svg.append('g')
      .attr({
        class: 'train-positions-main'
      });
    _this._renderD3(el, _this.props)
  }

  _updateData(bounds, trainPositions, timestamp) {
    let _this = this;

    if ((timestamp === _this.lastUpdated.timestamp) && _.isEqual(_this.lastUpdated.bounds, bounds)) {
      return false;
    }
    _this.lastUpdated.timestamp = timestamp;

    let {lngMin, lngMax, latMin, latMax}  = bounds;

    _this._trainPositions = _.chain(trainPositions)
      .map(function (p) {
        p.x = p.position_lng;
        p.y = p.position_lat;
        return p;
      })
      .filter(function (p) {
        return (p.x >= lngMin) && (p.x <= lngMax) && (p.y >= latMin) && (p.y <= latMax);
      }).value();
    return _this;
  }

  _renderD3TrainPositions(el, bounds, zoom) {
    let _this = this;

    let hasBoundMoved = !_.isEqual(bounds, _this.lastUpdated.bounds);
    _this.lastUpdated.bounds = bounds;

    let gTrains = _this._elements.gMainTrainPositions
      .selectAll('g.train-position')
      .data(_this._trainPositions, function (d) {
        return d.train_id;
      });

    let gNew = gTrains.enter()
      .append('g')
      .attr({
        transform: function (p) {
          return 'translate(' + _this._scales.x(p.x) + ',' + _this._scales.y(p.y) + ')';
        },
        class: function (p) {
          let s = p.train_name.trim();
          let i = s.indexOf(' ');
          let clazz = 'train-position ';
          clazz = clazz + 'train-cat_' + p.train_category.toLowerCase();
          clazz = clazz + ' trainMarker';
          return clazz;
        }
      });
    gNew.on('mouseover', function (p) {
      console.log(p);
    });
    let gSymbol = gNew.append('g')
      .attr({
        class: 'train-symbol trainSymbol ' + styles.trainSymbol
      });

    gSymbol.append('circle')
      .attr({
        cx: 0,
        cy: 0,
        r: 1
      });
    // gSymbol.append('path')
    //   .attr({
    //     class: 'bearing-arrow bearingArrow',
    //     d: 'M0.707,-0.707 L0,-2 L-0.707,-0.707 Z'
    //   });

    gNew.append('g')
      .attr({
        class: 'train-details trainDetails'
      }).append('text')
      .attr({
        class: 'train-details positionText',
        x: 6
      })
      .text(function (p) {
        //return p.train_name.trim() + ' (' + p.train_lastStopName + ')';// +_this.props.coord2point.x(p.x);
      });

    var transitionDuration = (zoom<7 || hasBoundMoved) ? 0 : trainPositionUpdateDelay;

    gTrains.transition().duration(transitionDuration).ease("linear")
      .attr('transform', function (p) {
        return 'translate(' + _this._scales.x(p.x) + ',' + (_this._scales.y(p.y)) + ')';
      });

    var radius;
    if (zoom <= 7) {
      radius = 1;
    } else if (zoom >= 11) {
      radius = 4;
    } else {
      radius = zoom - 7;
    }
    gTrains.select('g.train-symbol')
      .attr('transform', function (p) {
//        if (p.position_bearing === undefined) {
        return 'scale(' + radius + ')';
        // } else {
        //   return 'scale(' + radius + ') rotate(' + (p.position_bearing) + ')';
        // }
      });
    gTrains.select('path.bearing-arrow')
      .style('display', function (p) {
        return (p.position_bearing === undefined) ? 'none' : 'block';
      });


    let fText_0 = function () {
      return '';
    };
    let fText_1 = function (p) {
      return p.train_category;
    };
    let fText_2 = function (p) {
      return p.train_name;
    };
    let fText_3 = function (p) {
      return p.train_name.trim() + ' (' + p.train_lastStopName + ')';
    };
    var fText;
    if (zoom < 10) {
      fText = fText_0;
    }
    if (zoom == 10) {
      fText = fText_1;
    }
    if (zoom == 11) {
      fText = fText_2;
    }
    if (zoom >= 12) {
      fText = fText_3;
    }
    gTrains.selectAll('text.train-details')
      .text(fText);

    gTrains.exit().transition()
      .duration(300)
      .style('opacity', 1e-6)
      .remove();
    return _this;
  };


  _renderD3(el, newProps) {
    let _this = this;

    let {lngMin, lngMax, latMin, latMax}  = newProps.MapLocation.location.bounds;
    _this._scales = {
      x: d3.scale.linear().range([0, _this._dim.width]).domain([lngMin, lngMax]),
      y: d3.scale.linear().range([0, _this._dim.height]).domain([latMax, latMin])
    };

    if (_this._updateData(
        newProps.MapLocation.location.bounds,
        newProps.TrainPosition.positions,
        newProps.TrainPosition.timestamp
      )) {
      _this._renderD3TrainPositions(el, newProps.MapLocation.location.bounds, newProps.zoom);
    }

  }

  render() {
    const {dispatch} = this.props;
    const actions = {
      ...bindActionCreators(TrainPositionActions, dispatch),
      ...bindActionCreators(MapLocationActions, dispatch)
    };

    return (
      <g></g>
    );
  }
}

export default connect(state => state)(PositionMapTrains);
