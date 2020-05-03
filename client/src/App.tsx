import React, { useEffect, useState } from 'react';
import { VictoryLine, VictoryChart, VictoryTheme } from 'victory';
import './App.css';

const SERVER_HOST = 'localhost';
const SERVER_PORT = '8080';
const SERVER_URL = `http://${SERVER_HOST}:${SERVER_PORT}`;

enum SolverStates {
  Searching = 'Searching',
  Idle      = 'Idle'
}

type TimeSeries = { time: number, value: number }[];

type JsonPayload = {
  name: string;
  data: any;
}

type Solution = number[];

// _TODO_ Detect current state of solver on load.
const App: React.SFC = () => {
  /* Solver Inputs */
  const [timeoutText, setTimeoutText] = useState('15'); // TODO number
  const [orderText, setOrderText]     = useState('5');  // TODO number

  /* Solver state */
  const [startTime, setStartTime]           = useState<Date | undefined>(undefined);
  const [endTime, setEndTime]               = useState<Date | undefined>(undefined);
  const [solverState, setSolverState]       = useState(SolverStates.Idle);
  const [solution, setSolution]             = useState<Solution | undefined>(undefined);
  const [intermediate, setIntermediate]     = useState<Solution | undefined>(undefined);
  const [currentVars, setCurrentVars]       = useState<String>(""); // TODO are we using this?
  const [currentOrder, setCurrentOrder]     = useState<number | undefined>(undefined);
  const [orderHistory, setOrderHistory]     = useState<TimeSeries>([]);
  const [boundHistory, setBoundHistory]     = useState<TimeSeries>([]);
  const [gapHistory, setGapHistory]         = useState<TimeSeries>([]);

  /* UI state showing animated epsilon for search text */
  const [timeElapsed, setTimeElapsed] = useState(0);
  setTimeout(
    () => {
      setTimeElapsed(timeElapsed + 1);
    },
    100
  )


  const handleJsonMessages = (message: JsonPayload) => {
    const { name, data } = message;
    switch (name) {
      case 'StartSearch':
        setStartTime(new Date());               // Initialize search state
        setSolverState(SolverStates.Searching); //

        setIntermediate(undefined);             // Reset to initial values
        setSolution(undefined);                 //
        setCurrentOrder(undefined);             //
        setOrderHistory([]);                    //
        setBoundHistory([]);                    //
        setGapHistory([])                       //
        setEndTime(undefined);                  //
        break;
      case 'NewOrder':
        setCurrentOrder(data);
        setOrderHistory((existingHistory) => {
          const newHistory = [...existingHistory, { time: Date.now(), value: data }];
          return newHistory;
        });
        break;
      case 'ObjBound':
        setBoundHistory((existingHistory) => {
          const newHistory = [...existingHistory, { time: Date.now(), value: data }];
          return newHistory;
        })
        break;
      case 'Periodic':
        setSolverState(SolverStates.Searching);
        setCurrentVars(data);
        break;
      case 'Gap':
        setGapHistory((existingHistory) => {
          const newHistory = [...existingHistory, { time: Date.now(), value: data }];
          return newHistory;
        })
        break;
      case 'NewSolution':
        setIntermediate(data);
        break;
      case 'EndSearch':
        setSolverState(SolverStates.Idle);
        setEndTime(new Date());
        break;
      case 'Final':
        setSolution(data);
        break;
      default:
    }
  }

  // Setup the websocket
  useEffect(
    () => {
      const socket = new WebSocket(`ws://${SERVER_HOST}:${SERVER_PORT}/ws`);
      socket.addEventListener('message', (evt) => {
        const payload: JsonPayload = JSON.parse(evt.data);
        handleJsonMessages(payload);
      });
      return () => socket.close();
    },
    []
  );

  // _TODO_ these should be validators on the input components
  const timeout = parseInt(timeoutText, 10);
  const validatedTimeout = isNaN(timeout) ? 30 : timeout;
  const order = parseInt(orderText, 10);
  const validatedOrder = isNaN(order) ? 5 : order;

  let stateText;
  if (solverState === SolverStates.Searching) {
    const numDots = timeElapsed % 5;
    stateText = "Searching" + (".".repeat(numDots));
  } else if (solverState === SolverStates.Idle) {
    stateText = "Idle"
  }

  const solutionForRuler = solution ? solution : intermediate;

  // We render the charts regardless of if twe have data or not, so we need
  // to come up with some default time range to display
  const graphTime0 = startTime ? startTime.getTime() : (Date.now() - 100000) // default to some time in past
  const graphTime1 = solverState === SolverStates.Searching ? Date.now() : (endTime || Date.now()); // default to now if we aren't searching (or done)
  const timeDomain = [graphTime0, graphTime1];

  return (
    <div>
      <div style={{ margin: '20px 0 0 20px' }}>
        <div>
          <label style={{ marginRight: 15 }}>Timeout (s)</label>
          <input type="number" value={timeoutText} onChange={(e) => setTimeoutText(e.target.value)} />
        </div>
        <div>
          <label style={{ marginRight: 15 }}>Ruler Order</label>
          <input type="number" value={orderText} onChange={(e) => setOrderText(e.target.value)} />
        </div>
        <div>
          <button
            onClick={() => {
              fetch(`${SERVER_URL}/solve?timeout=${validatedTimeout}&order=${validatedOrder}`)
            }}
          >
            Solve
          </button>
        </div>
        <div style={{ marginTop: 20, marginBottom: 20 }}>State: {stateText}</div>
      </div>
      <div style={{ margin: '20px 0 0 20px' }}>
        {solutionForRuler && (
          <div>
            <h5>Current Best Ruler</h5>
            <Ruler solution={solutionForRuler} />
          </div>
        )}
        <div style={{ marginTop: 20, display: 'flex', flexDirection: 'row' }}>
          <TimeSeriesChart data={orderHistory} title="Objective" domain={{ x: timeDomain }} />
          <TimeSeriesChart data={boundHistory} title="Objective Lower Bound" domain={{ x: timeDomain  }} />
          <TimeSeriesChart data={gapHistory} title="Gap" domain={{ x: timeDomain, y: [0,1] }} />
        </div>
        <table className="results-table">
          <tbody>
            <tr>
              <td>Current:</td>
              <td>{currentVars}</td>
            </tr>
            <tr>
              <td>Order:</td>
              <td>{currentOrder}</td>
            </tr>
            <tr>
              <td>Intermediate:</td>
              <td>{intermediate || "n/a"}</td>
            </tr>
            <tr>
              <td>Result:</td>
              <td>{solution || "n/a"}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}

const Ruler: React.SFC<{solution: number[]}> = ({ solution }) => {
  const width = 800;
  const height = 100;
  const markWidth = 3;

  let markMax = 0;
  solution.forEach(mark => {
    if (mark > markMax) markMax = mark;
  });
  const scale = width / markMax;
  function markLeftPos(mark: number): number {
    // scale then pull back to center
    return mark * scale - (markWidth / 2);
  }

  return (
    <div
      className="ruler"
      style={{
        width,
        height,
        position: 'relative', // required for children abs pos
        borderBottom: '2px solid blue'
      }}
    >
      {solution.map((mark: number) => {
        return (
          <div
            style={{
              position: 'absolute',
              width: markWidth,
              height,
              backgroundColor: 'blue',
              left: markLeftPos(mark)
            }}
            key={mark}
            className="mark"
          >
            <div
              style={{
                position: 'absolute',
                top: height + 5,
                fontSize: 11,
                fontWeight: 'bold'
              }}
            >
              {mark}
            </div>
          </div>
        );
      })}
    </div>
  );
};

const TimeSeriesChart: React.SFC<{data: TimeSeries, title: string; domain?: any}> = (props) => {
  return (
    <div
      style={{
        width: 400,
        height: 400
      }}
    >
      <h5>{props.title}</h5>
      <VictoryChart
        theme={VictoryTheme.material}
        padding={{ top: 10, left: 40, right: 40, bottom: 40 }}
        domain={props.domain ? props.domain : undefined}
      >
        <VictoryLine
          scale={{ x: "time", y: "linear" }}
          data={props.data.map((point) => {
            return { x: point.time, y: point.value };
          })}
          style={{
            data: { stroke: "#c43a31" },
            parent: { border: "1px solid #ccc"}
          }}
        />
      </VictoryChart>
    </div>
  )
}



export default App;
