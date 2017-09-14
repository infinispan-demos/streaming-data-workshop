import React from 'react';
import ReactDOM from 'react-dom';
import { Router, Route, hashHistory } from 'react-router'
import matStyles from 'materialize-css/bin/materialize.css';

import Map from './containers/Map';
import About from './about/index';
import Header from './components/Header';
import Footer from './components/Footer';


ReactDOM.render(
  <Header/>,
  document.getElementById('header')
);

ReactDOM.render(
  <Footer/>,
  document.getElementById('footer')
);
ReactDOM.render(
  <Router location="history" history={hashHistory}>
    <Route path="/" component={Map}/>
    <Route path="/about" component={About}/>
  </Router>, document.getElementById("main"));
