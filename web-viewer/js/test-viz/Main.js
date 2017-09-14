import React from 'react';
import SpecsGroup from './component-specs/SpecsGroup';
import StationBoardDetailsSpecs from './component-specs/StationBoardDetailsSpecs';
import PositionMapTrainSpecs from './component-specs/PositionMapTrainSpecs';
import PositionMapStationBoardSpecs from './component-specs/PositionMapStationBoardStatsSpecs';
import CFFCLockSpecs from './component-specs/CFFCLockSpecs';



export default React.createClass({
  render() {
    return (
      <div>
        <SpecsGroup title="Station boards">
          <StationBoardDetailsSpecs />
        </SpecsGroup>
        <SpecsGroup title="Train positions"
                    comment="3 different trains, 2 categories, 'S x' has no bearing information">
          <PositionMapTrainSpecs/>
        </SpecsGroup>
        <SpecsGroup title="station boards stats" comment="3 different station board statistics">
          <PositionMapStationBoardSpecs/>
        </SpecsGroup>
        <SpecsGroup title="cff-clock" comment="the cff clock with different sizes">
          <CFFCLockSpecs/>
        </SpecsGroup>
      </div>
    );
  }


})
;
