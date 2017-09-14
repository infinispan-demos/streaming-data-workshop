import React, {Component, Provider} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import Dimensions from 'react-dimensions'
import matStyles from 'materialize-css/bin/materialize.css';
import ReactTooltip from 'react-tooltip';
import PositionMap from './PositionMap';
import StationBoardDetails from './StationBoardDetails';
import CFFCLock from './CFFClock';
import Timer from './Timer';

class Home extends Component {
  componentDidUpdate() {
  }

  render() {
    let _this = this;

    return (
      <div className="row" style={{height:this.props.containerHeight, marginBottom:'0px'}}>
        <div className="col s9" style={{height:this.props.containerHeight}}>
          <PositionMap
            store={_this.props.store}
          />
        </div>
        <div className="col s3">
          <div className="helpButtonContainer" style={{width:'100%', height:150}}>
            <div className="helpButton"
                 data-tip
                 data-for="tt-cff-clock">
              <i className="material-icons small">live_help</i>
            </div>
            <ReactTooltip id="tt-cff-clock" place="bottom" type="info" effect="solid">
              <div className="center-align">
                This clock has been originally designed in 1944
                <br/>by Hans Hilfiker for the Federal Swiss Railways.
                <p/>www.mondaine.com
              </div>
            </ReactTooltip>
          <CFFCLock />

        </div>
        <div style={{'height': global.window.innerHeight * 0.8 - 150 - 10}}>
          <StationBoardDetails store={_this.props.store}/>
        </div>
      </div>
  </div>
  )
    ;
  }
}

export default connect(state => state)(Dimensions()(Home));
