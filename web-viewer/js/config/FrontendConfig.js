'use strict'
import httpClient from '../core/HttpClient';
import _ from 'lodash';

class FrontendConfig {

  constructor() {
    let _this = this;
    _this._config = undefined;

    _this._init();
    return _this;
  }

  get() {
    let _this = this;
    if (_this._config !== undefined) {
      return new Promise(function (accept) {
        accept(_this._config);
      })
    }
    return _this._promise;
  }

  _init() {
    var _this = this;

    _this._promise = new Promise(function (accept, reject) {
      return httpClient.get("config.json")
        .then(function (cfg) {
          if (cfg.url) {
            cfg.url = _.mapValues(cfg.url, function (u, k) {
              return u
                .replace(/http:\/\/\-:/, 'http://' + global.location.hostname + ':')
                .replace(/http:\/\/\-\//, 'http://' + global.location.hostname + '/');
            });
          }
          _this._config = cfg;
          accept(cfg);
        })
        .catch(function (error) {
          console.error("ERROR when initializing frontend config", error)
        });
    });
    return _this;
  }
}

export default new FrontendConfig();
