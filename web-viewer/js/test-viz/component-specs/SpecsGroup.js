import React from 'react';
import styles from './Specs.css';


export default React.createClass({
  render() {
    let {title, children, comment} =  this.props;
    return (
      <div className="specsGroup">
        <h3>{title}</h3>
        <span>{comment}</span>
        <div>
          {children}
        </div>
      </div>
    );
  }
});
