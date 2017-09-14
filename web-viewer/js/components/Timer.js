import React, {Component} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import styles from './Timer.css';


class Timer extends Component {
  constructor(props) {
    super(props);
    let _this = this;
    _this.state = {elapsed: 0};

    _this.intervUpdate = setInterval(function () {
      let dt = (new Date().getTime() - _this.props.t0)/1000
      _this.setState({elapsed: Math.round(dt)})
    }, 500);
  }


  render() {
    let _this = this;
    const {t0} = _this.props;
    return (
      <span className={styles.timeFrom}>{_this.state.elapsed}s</span>
    );
  }
}

export default connect(state => state)(Timer)
