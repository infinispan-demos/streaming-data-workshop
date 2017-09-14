import actionTypes from '../constants/ActionTypes';

export function updateLocation(loc) {
  return {
    type: actionTypes.MAP_LOCATION_CHANGED,
    center:loc.center,
    zoom:loc.zoom,
    bounds: {
      latMin: loc.bounds.se.lat,
      latMax: loc.bounds.nw.lat,
      lngMin: loc.bounds.nw.lng,
      lngMax: loc.bounds.se.lng
    }
  }
}
