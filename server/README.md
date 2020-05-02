# Golomb Solver Backend

This is a Scala project that uses IBM's CPLEX to find solutions for [Golomb Rulers](https://en.wikipedia.org/wiki/Golomb_ruler). It exposes an HTTP API to `GET` a solution, and provides a web socket for subscribing to real time updates of the solver.
