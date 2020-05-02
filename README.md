# Golomb Ruler Solver

An application for producing [Golomb Rulers](https://en.wikipedia.org/wiki/Golomb_ruler) using constraint programming

## Requirements
- [CPLEX Community Edition 12.9](https://www.ibm.com/support/knowledgecenter/SSSA5P_12.9.0/ilog.odms.studio.help/Optimization_Studio/topics/COS_home.html)
- scala, sbt

## Roadmap
- [] Use type safety for the messages passed to the client (currently just magic strings)
- [] Address potential race condition with setting `golombActor` after future resolution in Server.scala
- [] In Server.scala we need to ignore data coming from clients of the web socket (they just get messages pushed to them)
- [] Fix cors issue