import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import d3 from 'd3';
import {bindActionCreators} from 'redux';
import * as TrainPositionActions from '../actions/TrainPositionActions';
import styles from '../../css/app.css';
import classes from './PositionMapCFF.css'
import { Map, Marker, Popup, TileLayer } from 'react-leaflet';
import _ from 'lodash'

let i = 0;

class PositionMapTrains extends Component {
  constructor() {
    super();
    let _this = this;
    this;
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
      svg: d3.select(el).append('svg')
        .attr({
          //viewBox: '-' + (_this._dim.width / 2) + ' -' + (_this._dim.height / 2) + ' ' + _this._dim.width + ' ' + _this._dim.height,
          //viewBox: '0 0 ' + _this._dim.width + ' ' + _this._dim.height,
          width: _this._dim.width,
          height: _this._dim.height,
          class: classes.train_overlay
        })
      //.style('overflow', 'visible')
    };
    _this._elements.svg.append('rect')
      .attr({
        width: _this._dim.width,// * 3,
        height: _this._dim.height,// * 3,
        x: 0,//-_this._dim.width,
        y: 0,//-_this._dim.height,
        class: classes.masking
      });
    _this._elements.gMainStationBoardStats = _this._elements.svg.append('g')
      .attr({
        class: 'station-board-stats-main'
      });
    _this._elements.gMainTrainPositions = _this._elements.svg.append('g')
      .attr({
        class: 'train-positions-main'
      });
    _this._renderD3(el, _this.props)
  }

  _updateData(bounds, trainPositions, stationBoardStats) {
    let _this = this;

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

    _this._stationBoardStats = _.chain(stationBoardStats)
      .map(function (p) {
        p.x = p.stop.location.lng;
        p.y = p.stop.location.lat;
        return p;
      })
      .filter(function (p) {
        return (p.x >= lngMin) && (p.x <= lngMax) && (p.y >= latMin) && (p.y <= latMax);
      })
      .sortBy(function(p){
        return -p.total;
      })
      .value();

    //
    //console.log('delayed?');
    //_.chain(stationBoardStats)
    //  .filter(function (s) {
    //    return s.delayed;
    //  })
    //  .each(function (s) {
    //    console.log(s.stop.name + ': ' + s.delayed + '/' + s.total);
    //  })
    //  .value();

    return _this;
  }

  _renderD3TrainPositions(el, zoom) {
    let _this = this;

    console.log('_renderD3TrainPositions');

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
          clazz = clazz + classes['train-cat_' + p.train_category.toLowerCase()];
          clazz = clazz + ' ' + classes.trainMarker;
          return clazz;
        }
      });
    gNew.on('mouseover', function (p) {
      console.log(p.train_name + '->' + p.train_lastStopName);
    });
    let gSymbol = gNew.append('g')
      .attr({
        class: 'train-symbol '+classes.trainSymbol
      });

    gSymbol.append('circle')
      .attr({
        cx: 0,
        cy: 0,
        r: 1
      });
    gSymbol.append('path')
      .attr({
        class:'bearing-arrow '+classes.bearingArrow,
        d:'M0.707,-0.707 L0,-2 L-0.707,-0.707 Z'
      });

    gNew.append('g')
      .attr({
        class: 'train-details ' + classes.trainDetails
      }).append('text')
      .attr({
        class: 'train-details ' + classes.positionText,
        x: 6
      })
      .text(function (p) {
        //return p.train_name.trim() + ' (' + p.train_lastStopName + ')';// +_this.props.coord2point.x(p.x);
      });

    gTrains
      .transition()
      .duration(500)
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
      .attr('transform', function(p){
        if(p.position_bearing === undefined) {
          return 'scale(' + radius + ')';
        }else{
          return 'scale(' + radius + ') rotate('+ (p.position_bearing)+')';
        }
      });
    gTrains.select('path.bearing-arrow')
      .style('display', function(p){
        return (p.position_bearing === undefined)?'none':'block';
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

  _renderD3StationBoardStats(el, zoom) {
    let _this = this;

    console.log('_renderD3StationBoardStats')

    let gStats = _this._elements.gMainStationBoardStats
      .selectAll('g.station-board-stats')
      .data(_this._stationBoardStats, function (d) {
        return d.stop.id;
      });
    let gNew = gStats.enter()
      .append('g')
      .attr('class', 'station-board-stats ' + classes['station-board-stats']);
    gNew.attr('transform', function (p) {
      return 'translate(' + _this._scales.x(p.x) + ',' + (_this._scales.y(p.y)) + ')';
    });

    gNew.append('circle');
    gNew.on('mouseover', function (s) {
      console.log(s.stop.name + ':' + s.delayed + '/' + s.total);
    });
    gNew.append('path')
      .attr({
        class: 'delayed ' + classes.stationboard_delayed
      });

    var factor;
    if (zoom <= 8) {
      factor = 0.25;
    } else if (factor >= 11) {
      factor = 1;
    } else {
      factor = ((zoom - 8) + (11 - zoom) * 0.25) / 3;
    }

    let fRadius = function (d) {
      return factor * 5 * (d.total + 1);
    };

    gStats.transition()
      .attr('transform', function (p) {
        return 'translate(' + _this._scales.x(p.x) + ',' + (_this._scales.y(p.y)) + ')';
      });
    gStats.select('circle')
      .attr({
        r: fRadius
      })
    ;
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


    let {lngMin, lngMax, latMin, latMax}  = newProps.bounds;
    _this._scales = {
      x: d3.scale.linear().range([0, _this._dim.width]).domain([lngMin, lngMax]),
      y: d3.scale.linear().range([0, _this._dim.height]).domain([latMax, latMin])
    };


    _this._updateData(newProps.bounds, newProps.positions, newProps.stationBoardStats)
      ._renderD3TrainPositions(el, newProps.zoom)
      ._renderD3StationBoardStats(el, newProps.zoom);

  }

  render() {
    const {count, positions, dispatch} = this.props;
    return (
      <div></div>
    );
  }
}

export default PositionMapTrains;
