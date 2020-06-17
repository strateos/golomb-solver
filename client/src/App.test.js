import React from 'react';
import { shallow } from 'enzyme';
import App, { Ruler, TimeSeriesChart } from './App';

describe('<App />', () => {
  test('App smoke test', () => {
    shallow(<App />);
  });
});

describe('<Ruler />', () => {
  test('Ruler smoke test', () => {
    shallow(<Ruler solution={[1,2,3]} />);
  });
});

describe('<TimeSeriesChart />', () => {
  test('TimeSeries smoke test', () => {
    shallow(
      <TimeSeriesChart
        data={[
          { time: Date.now(), value: 1 },
          { time: Date.now(), value: 2 },
          { time: Date.now(), value: 3 }
        ]}
        title="foo title"
      />
    )
  });
});
