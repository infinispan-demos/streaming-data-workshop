import React from 'react';
import styles from './Specs.css';

export default React.createClass({
  render() {
    let {title, children, comment} =  this.props;
    return (
      <div className="specsOne">
        <strong>{title}</strong>
        <span>{comment}</span>
          <div>
            {children}
          </div>
      </div>
  );
  }
  });
