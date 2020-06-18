# Golomb Ruler Solver

This is an example application for producing [Golomb Rulers](https://en.wikipedia.org/wiki/Golomb_ruler) using constraint programming (CP) via IBM CPLEX. Producing rulers is a fun way to learn CP (even if it isn't the most efficient method of producing them). IBM uses it as a training exercise [in their documentation](https://dataplatform.cloud.ibm.com/exchange/public/entry/view/f981e59a5122130858f8899a875e0b54) as well. It includes a web frontend that displays real time metrics of the solve process (see below).

From [the Wikipedia page](https://en.wikipedia.org/wiki/Golomb_ruler)
> In mathematics, a Golomb ruler is a set of marks at integer positions along an imaginary ruler such that no two pairs of marks are the same distance apart. The number of marks on the ruler is its order, and the largest distance between two of its marks is its length. Translation and reflection of a Golomb ruler are considered trivial, so the smallest mark is customarily put at 0 and the next mark at the smaller of its two possible values.

Here's an example of a Golomb Ruler of order 4 and length 6.
![Golomb Ruler Order 4](https://user-images.githubusercontent.com/3643611/81763758-d0544900-9484-11ea-9a75-0ceda490325e.png)


## Requirements
- [CPLEX 12.9](https://www.ibm.com/support/knowledgecenter/SSSA5P_12.9.0/ilog.odms.studio.help/Optimization_Studio/topics/COS_home.html)
- scala, sbt
- node

## Demo
Below is an example of running the app with an order of `7`. It displays the current best found ruler, along with real time metrics like the current objective value, `NumberOfChoicePoints`, `NumberOfBranches`, `MemoryUsage` etc.
![demo](https://user-images.githubusercontent.com/3643611/81000667-7952d200-8dfb-11ea-956b-d98cfe531de7.gif)

## Directory structure
```
root
│   README.md       <- You are here!
│
└───server          <- Web server & solver
│   │   README.md
|   |   .
|   |   .
│   
└───client          <- Web client
    │   README.md
    |   |   .
    |   |   .
```

The core of the solver logic is in `GolombRuler.scala`. That is where we model the problem and kick off the CPLEX solve process. The `Server.scala` sets up a web server to take solve requests, and sets up a web socket to send search process updates out to the client. The `client` directory contains the React front-end which subscribes to this web socket, and provides a UI to send solution requests to the server.

## Development
First, ensure that you have CPLEX 12.9 installed.

Setup the backend
```
$ cd server
$ sbt clean compile stage
$ sbt run
```

Setup the client
```
$ cd client
$ # see client/README.md
$ yarn install
$ yarn start
```

Navigate to the url provided by `yarn start`.

## Misc
To include images in the READMEs we do this little hack of uploading the images to [this github issue](https://github.com/strateos/golomb-solver/issues/3) and then copying the url of the resource hosted by github.
