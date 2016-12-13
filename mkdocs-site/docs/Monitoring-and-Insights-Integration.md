Dynomite-manager exports the statistics of Dynomite and Redis to Atlas for plotting and time-series analysis. We use a tiered architecture for our monitoring system. 

1. Dynomite-manager receives information about Dynomite through a REST call;
1. Dynomite-manager receives information about Redis through the [INFO](http://redis.io/commands/INFO) command.

Currently, Dynomite-manager leverages the Servo client to publish the metrics for time series processing. Nonetheless, other Insight clients can be added in order to deliver metrics to a different Insight system. 