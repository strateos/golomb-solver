# Golomb Ruler Solver

An application for producing [Golomb Rulers](https://en.wikipedia.org/wiki/Golomb_ruler) using constraint programming (CP). While CP may not be the best tool for the job in terms of efficiency, it is a nice problem to use while learning CP. IBM uses it as a training exercise [in their documentation](https://dataplatform.cloud.ibm.com/exchange/public/entry/view/f981e59a5122130858f8899a875e0b54) as well.

## Requirements
- [CPLEX Community Edition 12.9](https://www.ibm.com/support/knowledgecenter/SSSA5P_12.9.0/ilog.odms.studio.help/Optimization_Studio/topics/COS_home.html)
- scala, sbt

## Demo
Below is an example of running the app with an order of `15`.
![demo](https://user-images.githubusercontent.com/3643611/80891962-5f3eb580-8c7c-11ea-9122-8781a94feddc.gif)

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

## Roadmap
- [] Use better type safety for the messages passed to the client (still a few stringly typed things left)
- [] Address potential race condition with setting `golombActor` after future resolution in Server.scala
- [] In Server.scala we need to ignore data coming from clients of the web socket (they just get messages pushed to them)
