import React from 'react';

import CFFClock from '../../components/CFFClock';
import SpecsOne from './SpecsOne';


export default React.createClass({
  render() {
    return (
      <div>
        <SpecsOne title="400x400">
          <div style={{height:400, width:400}}>
            <CFFClock/>
          </div>
        </SpecsOne>
        <SpecsOne title="250x250">
          <div style={{height:250, width:250}}>
            <CFFClock/>
          </div>
        </SpecsOne>
        <SpecsOne title="100x50">
          <div style={{height:100, width:50}}>
            <CFFClock/>
          </div>
        </SpecsOne>
      </div>
    );
  }
});
