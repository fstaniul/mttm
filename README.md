# Multi Tasking Teamspeak 3 Manager
A multi tasking teamspeak 3 manager containing REST endpoint and teamspeak 3 client bot administrator.

# Information for contributors
* Project is using Spring Boot 1.5.3.
* Prefered configuration files are `.xml` files. `org.apache.commons.configuration2` is default configuration provider.
  * `ConfigurationLoader` loads configuration for class based on `@UseConfig` annotation. Every component annotated with this annotation will have its configuration loaded for it and injected into fields annotated with `@WireConfig`. 
  * It is possible to retrive single value from configuration by annotating fields with `@ConfigValue`. You need to specify exact entry and possible converter if type of field is not primitive or String.
* `JTS3ServerQuery` library is used to connect to teamspeak 3 server and invoke actions. Basic component for connecting and invoking actions with teamspeak 3 server is `Query` component. There are 2 types of actions happening on teamspeak 3 server, events and commands invoked by users:
  * Events occur when clients join or leave teamspeak 3 server. Every method annotated with `Teamspeak3Event` will be called upon event call. 
  * Clients can send messages to bot to invoke commands. Methods annotated with `Teamspeak3Command` points to a certain command that can be invoked by client.
  * To narrow invocation of commands and event calls you can use `@ClientGroupAccessCheck` to check if client has access or if event should be fired for this client based on groups specified as a `value()` which points to entry in `application.properties` which should hold list of groups that are aligible to call this command or event should be call upon join. 
  * Clients can pass parameters along with commands. To ensure that parameters match certain criteria you can use on methods annotated with `@Teamspeak3Command` annotation `@ValidateParams` with class that will perform validation of paramters.
* When you want to perform certain task every now and then, you can use custom annotation that is `@Task` specifing `delay` between executions also with parameters like: 
  1. `day`- day of week (1 - 7)
  2. `hour` - hour of day (0 - 23)
  3. `minute` - minute of hour (0 - 59)
  4. `second` - second of minunte (0 - 59)
  * Execution of a task will be scheduled for the next time (if specified) after the time of application start. This is for reocuring tasks.
