import React from 'react';
import { shallow } from 'enzyme';
import App, { Ruler, TimeSeriesChart } from './App';

describe('<App />', () => {
  test('App smoke test', () => {
    shallow(<App />).unmount();
  });
});

describe('<Ruler />', () => {
  test('Ruler smoke test', () => {
    shallow(<Ruler solution={[1,2,3]} />).unmount();
  });

  test("Ruler rendres a ruler with borderBottom", () => {
    const wrapper = shallow(<Ruler solution={[1,2,3]} />);
    expect(wrapper.find({ className: "ruler" }).length).toBe(1);
    wrapper.unmount();
  });

  test('Ruler renders all marks as divs', () => {
    const marks = [1,2,3];
    const wrapper = shallow(<Ruler solution={marks} />);
    expect(wrapper.find({ className: "mark" }).length).toBe(3);
    marks.forEach((mark) => {
      expect(
        wrapper.findWhere(
          (node) => {
            return node.key() === `${mark}` && node.hasClass("mark");
          }
        ).length
      ).toBe(1);
    })
    wrapper.unmount();
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
    ).unmount()
  });

  test('TimeSeries renders a VictoryChart', () => {
    const wrapper = shallow(
      <TimeSeriesChart
        data={[
          { time: Date.now(), value: 1 },
          { time: Date.now(), value: 2 },
          { time: Date.now(), value: 3 }
        ]}
        title="foo title"
      />
    );
    expect(wrapper.find('VictoryChart').length).toBe(1);
    wrapper.unmount();
  });

  test('TimeSeries renders a title', () => {
    const title = 'foo-bar-title';
    const wrapper = shallow(
      <TimeSeriesChart
        data={[
          { time: Date.now(), value: 1 },
          { time: Date.now(), value: 2 },
          { time: Date.now(), value: 3 }
        ]}
        title={title}
      />
    );
    expect(wrapper.find('h5').text()).toBe(title);
    wrapper.unmount();
  });

  test('TimeSeries renders a VictoryChart with undefined domain', () => {
    const wrapper = shallow(
      <TimeSeriesChart
        data={[
          { time: Date.now(), value: 1 },
          { time: Date.now(), value: 2 },
          { time: Date.now(), value: 3 }
        ]}
        title="foo title"
      />
    );
    const chartWrapper = wrapper.find('VictoryChart');
    expect(chartWrapper.props().domain).toBe(undefined);
    wrapper.unmount();
  });

  test('TimeSeries renders a VictoryChart with provided domain', () => {
    const domain = { x: [0,1], y: [0,1]};

    const wrapper = shallow(
      <TimeSeriesChart
        data={[
          { time: Date.now(), value: 1 },
          { time: Date.now(), value: 2 },
          { time: Date.now(), value: 3 }
        ]}
        domain={domain}
        title="foo title"
      />
    );
    const chartWrapper = wrapper.find('VictoryChart');
    expect(chartWrapper.props().domain).toStrictEqual(domain);
    wrapper.unmount();
  });

});
