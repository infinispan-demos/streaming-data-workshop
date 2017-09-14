import React, {Component, Provider} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import Dimensions from 'react-dimensions'
import styles from './Header.css';
import matStyles from 'materialize-css/bin/materialize.css';
import {Link} from 'react-router';



class Home extends Component {
  render() {
    let _this = this;
    return (
      <div className="container" style={{height:'100%'}}>
        <div className="row" style={{height:'100%'}}>
          <div className="col s3"><a href="/">Swiss public transport in real time</a></div>
          <div className="col s6">&nbsp;</div>
          <div className="col s3"><a href="/#/about">About</a></div>
        </div>
      </div>
    );
  }
}

export default Dimensions()(Home);
