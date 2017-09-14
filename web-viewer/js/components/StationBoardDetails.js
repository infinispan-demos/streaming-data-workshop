import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import Dimensions from 'react-dimensions'
import dateFormat from 'dateformat';
import StatusTypes from '../constants/StatusTypes';


import './StationBoardDetails.css';
import 'materialize-css/bin/materialize.css';

const fBoardTR = function (evt) {
  let dep = dateFormat(new Date(evt.departureTimestamp), "HH:MM");
  var elDep;
  if (evt.delayMinute) {
    elDep = <span>{dep} <span className="delayed">+{evt.delayMinute}</span></span>;
  } else {
    elDep = <span>{dep}</span>;
  }
  return (<tr key={evt.train.id} className="trainEvent">
    <td>{evt.train.name}</td>
    <td>{elDep}</td>
    <td>{evt.train.lastStopName}</td>
    <td>{evt.platform}</td>
  </tr>)
};

const fActualStationBoard = function (board, width, height) {

  return <div className="station_board" style={{
  width:width,
  height:(height||null),
  fontSize:Math.round(100*width/400)+'%'
  }}>
    <div className="stopName">Depuis {board.stop.name}</div>
    <table className="trainEvents">
      <tbody>
      {_.chain(board.events)
        .values()
        .sortBy(function (e) {
          return e.departureTimestamp;
        })
        .map(function (e) {
          return fBoardTR(e);
        })
        .value()
      }
      </tbody>
    </table>
  </div>

}

class StationBoard extends Component {
  constructor(props) {
    super(props);
  }


  render(newProps) {
    let _this = this;
    let {containerWidth, containerHeight} = _this.props;

    let details = _this.props.StationBoard.details;
    switch (details.status) {
      case StatusTypes.UNAVAILABLE:
        return <span>-</span>
      case StatusTypes.ERROR :
        return <span>error</span>
      case StatusTypes.SUCCESS:
        let {stop} = details.board;
        return fActualStationBoard(details.board, containerWidth, containerHeight);
      default:
      {
        return <span>WTF?</span>
      }
    }
  }
}

export default connect(state => state)(Dimensions()(StationBoard));
