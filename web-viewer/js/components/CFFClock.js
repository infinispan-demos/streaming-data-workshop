import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import d3 from 'd3';
import  styles from './CFFClock.css'
import _ from 'lodash'
import Dimensions from 'react-dimensions'

var getNow = function () {
  var t = new Date();
  return {
    hours: t.getHours(),
    minutes: t.getMinutes(),
    seconds: t.getSeconds()
  };
};

var getNowArray = function () {
  var t = new Date();
  return [t.getHours(), t.getMinutes(), t.getSeconds()];
};

class CFFClock extends Component {
  constructor() {
    super();
    let _this = this;
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

    const height = _this.props.containerHeight;
    const width = _this.props.containerWidth;
    _this._dim = {
      height: height,
      width: width,
      radius: Math.min(height / 2, width / 2)
    };


    d3.select(el).selectAll().empty();

    var now = getNow();


    //we just print out the date as text in the target element
    const svg = d3.select(el)
      .append('svg')
      .attr({
        width: _this._dim.width,
        height: _this._dim.height
      });

    var gMain = svg.append('g').attr({
      class: 'root-container'
    });

    _this._rootContainer = gMain;

    var gTicks = gMain.append('g').attr({
      class: 'ticks'
    });

    //transform a angl between 0 and 1 in degree for svg rotation, where 0 is noon
    var rotate = function (angle01) {
      return 'rotate(' + (angle01 * 360 - 90) + ')';
    };


    var plotTicks = function (name, number, size) {
      gTicks.selectAll('g.tick.' + name)
        .data(_.range(number))
        .enter()
        .append('g')
        .attr({
          class: 'tick' +' ' + name,
          transform: function (t) {
            return 'rotate(' + (t / number * 360) + ') translate(' + 230 + ',0)';
          }
        })
        .append('path')
        .attr('d', 'm0,0 l-' + size + ',0');
    };
    plotTicks('minute', 60, 19);
    plotTicks('hour', 12, 60);


    //time for time

    var gHands = gMain
      .append('g')
      .datum(getNow())
      .attr('class', 'hands');

    var gHandHours = gHands.append('g')
      .attr('class', 'hours');
    gHandHours.append('path')
      .attr('d', `M-54,-15 L-54,15 L152,12.5 L152,-12.5 Z`);

    var gHandMinutes = gHands.append('g')
      .attr('class', 'minutes');
    gHandMinutes.append('path')
      .attr('d', `M-54,-12 L-54,12 L222,8.5 L222,-8.5 Z`);


    var gHandSeconds = gHands.append('g')
      .attr('class', 'seconds');
    gHandSeconds.append('path')
      .attr('d', `M-84,0 L146,0`);
    gHandSeconds.append('circle')
      .attr({
        cx: 150,
        cy: 0,
        r: 24.5
      });
    gHands.append('circle')
      .attr({
        class: 'middle_nail',
        r: 2
      });

    var isFirstCycle = true;
    var nextMinute = function () {
      var now = getNow();

      if (isFirstCycle) {
        isFirstCycle = false;
      } else {
        //after the first cycle, we loose a little amount of time doing the minute hand bounce
        //so we need the second hand to start smoothly from 0 and achieve the path in 58 seconds or so
        now.seconds = 0;
      }
      gHands.selectAll('g').datum(now);

      gHandMinutes.attr('transform', function (t) {
        return rotate(t.minutes / 60)
      });
      gHandHours.attr('transform', function (t) {
        return rotate(now.hours / 12 + t.minutes / (60 * 12) + t.seconds / (3600 * 12))
      });

      var minuteStopMillis = 2000;
      var remainMillis = (60 - now.seconds) * 1000 - minuteStopMillis;
      gHandSeconds.transition()
        .duration(remainMillis)
        .ease("linear")
        .attrTween('transform', function (d, i, a) {
          return function (i) {
            var x = d.seconds / 60 + (1 - d.seconds / 60) * i;
            return rotate(x);
          }
        });
      gHandMinutes.transition()
        .delay(remainMillis + minuteStopMillis)
        .ease("bounce")
        .duration(500)
        .attrTween('transform', function (d, i, a) {
          return function (i) {
            var x = d.minutes / 60 + (1 / 60) * i;
            return rotate(x);
          }
        })
        .each("end", nextMinute);
    };

    nextMinute();


    _this._elements = {
      svg: d3.select(el)
        .attr({
          width: _this._dim.width,
          height: _this._dim.height,
          class: "cff_clock"
        })
      //.style('overflow', 'visible')
    };
    _this._elements.gMainTrainPositions = _this._elements.svg.append('g')
      .attr({
        class: ''
      });
    _this._renderD3(el, _this.props)
  }


  _renderD3(el, newProps) {
    const _this = this;

    const height = newProps.containerHeight;
    const width = newProps.containerWidth;

    const m = Math.min(_this._dim.width, _this._dim.height);
    _this._rootContainer.attr(
      'transform',
      'translate(' + (_this._dim.width / 2) + ',' + (_this._dim.height / 2) + ')' +
        'scale('+(m/500)+')'
    );


  }

  render() {
    const {dispatch} = this.props;

    return <div></div>;
  }
}

export default Dimensions()(CFFClock);
