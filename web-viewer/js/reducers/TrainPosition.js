'use strict';
import ActionTypes from '../constants/ActionTypes';
import Papa from 'papaparse';

let defaultState = {
  positions: [],
  count: 0,
  timestamp: new Date().getTime()
};

export default function (state = defaultState, action) {
  switch (action.type) {
    case ActionTypes.TRAIN_POSITION_RECEIVED:
      var pos;
      if(action.tsv.trim() === ''){
        pos =[];
      }else{
        let parsed = Papa.parse(action.tsv, {header: true, delimiter: "\t"});
        if(parsed.errors.length >0){
          console.error('error parsing train position TSV', parsed.errors);
          pos=[];
        }else{
          pos = _.map(parsed.data, function(d){
            if(d.position_bearing === ""){
              delete d.position_bearing;
            }
            return d;
          });
        }
      }
      return {...state, positions: pos, timestamp: action.timestamp};
    default:
      return state;
  }
}

