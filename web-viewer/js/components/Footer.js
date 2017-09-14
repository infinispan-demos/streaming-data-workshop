import React, {Component, Provider} from 'react';
import ReactDOM  from 'react-dom';
import {connect} from 'react-redux';
import Dimensions from 'react-dimensions'
import styles from './Footer.css';
import matStyles from 'materialize-css/bin/materialize.css';

import {
  ShareButtons,
  ShareCounts,
  generateShareIcon
} from 'react-share';

const {
  FacebookShareButton,
  GooglePlusShareButton,
  LinkedinShareButton,
  TwitterShareButton
} = ShareButtons;


const TwitterIcon = generateShareIcon('twitter');
const GooglePlusIcon = generateShareIcon('google');
const LinkedInIcon = generateShareIcon('linkedin');
const FacebookIcon = generateShareIcon('facebook');
const url = location.origin;
const title = 'Realtime Swiss transport by @OCTOSuisse';

class Home extends Component {
  render() {
    let _this = this;
    return (
      <div className="container" style={{height:'100%'}}>
        <div className="row noBottomMargin valign-wrapper" style={{height:'100%'}}>
          <div className="col s2">&nbsp;</div>
          <div className="col s8 valign">made with fun by OCTO Technology Suisse - <a href="http://www.octo.ch">www.octo.ch</a>
            &nbsp;- <a href="mailto:amasselot@octo.com">email us <i className="material-icons tiny">email</i></a></div>
          <div className="col s2 left">
            <div className="left">
              <TwitterShareButton
                url={url}
                title={title}>
                <TwitterIcon
                  size={24}
                  round/>
              </TwitterShareButton>
            </div>
            <div className="left">
              <GooglePlusShareButton
                url={url}
                title={title}>
                <GooglePlusIcon
                  size={24}
                  round/>
              </GooglePlusShareButton>
            </div>
            <div className="left">
              <LinkedinShareButton
                url={url}
                title={title}>
                <LinkedInIcon
                  size={24}
                  round/>
              </LinkedinShareButton>
            </div>
            <div className="left">
              <FacebookShareButton
                url={url}
                title={title}>
                <FacebookIcon
                  size={24}
                  round/>
              </FacebookShareButton>
            </div>
          </div>
        </div>
      </div>
    );
  }
}


export default Dimensions()(Home);
