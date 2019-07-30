### Field Condition Statistics Service

```
  _____.__       .__       .___                          .___.__  __  .__
_/ ____\__| ____ |  |    __| _/   ____  ____   ____    __| _/|__|/  |_|__| ____   ____
\   __\|  |/ __ \|  |   / __ |  _/ ___\/  _ \ /    \  / __ | |  \   __\  |/  _ \ /    \
 |  |  |  \  ___/|  |__/ /_/ |  \  \__(  <_> )   |  \/ /_/ | |  ||  | |  (  <_> )   |  \
 |__|  |__|\___  >____/\____ |   \___  >____/|___|  /\____ | |__||__| |__|\____/|___|  /
               \/           \/       \/           \/      \/                         \/
          __          __  .__          __  .__                                          .__
  _______/  |______ _/  |_|__| _______/  |_|__| ____   ______   ______ ______________  _|__| ____  ____
 /  ___/\   __\__  \\   __\  |/  ___/\   __\  |/ ___\ /  ___/  /  ___// __ \_  __ \  \/ /  |/ ___\/ __ \
 \___ \  |  |  / __ \|  | |  |\___ \  |  | |  \  \___ \___ \   \___ \\  ___/|  | \/\   /|  \  \__\  ___/
/____  > |__| (____  /__| |__/____  > |__| |__|\___  >____  > /____  >\___  >__|    \_/ |__|\___  >___  >
     \/            \/             \/               \/     \/       \/     \/                    \/    \/

```

##### Assignee: Nikolaos Christidis (nick.christidis@yahoo.com)

#### Technical Decisions

In order to execute the endpoint GET /field-statistics in constant time and space (`O(1)`), `pre-calculation` has been used.
We have a scheduler/worker which at fixed delay time interval, runs and calculates the field statistics(min, max, avg).

The scheduler/worker is only one (writer thread) so we need to care for the visibility of the variable it updates, so other threads (readers in our case)
could see the correct value/calculation. The keyword `volatile` has been used in order to solve this problem.

The `consistency-gap`, meaning that if the value that readers are seeing is the latest from scheduler/worker calculation, is defined by the `fixed delay` of
the scheduler/worker.

Also the repository code, where we store the captures, has been designed with having in mind weakly consistent iterators behaviour.
We sacrifice a little consistency for scalability, otherwise we will need to use locks (read/write, etc) during merge captures calculation operation 
which take place on repository code fields (`ConcurrentHashMap<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>> capturesGroupByDate`).


#### Test Data Generator
* See class in src/test with name: `SampleDataGenerator.java`


#### Merge Captures Approach
* See configuration property: `memoRepo.merged-captures.single-thread-approach=true|false`


#### InfluxDB as persistence storage
* You can find the implementation in branch: `feat_influx_as_persistence`


#### How to run service
* Two options:
    * Execute: 
        * `mvn clean install -DskipUTs=true -DskipITs`
        * `java -jar -Dspring.profiles.active=dev -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector target/field-condition-statistics-service-1.0-SNAPSHOT.jar`
                
    * Execute:
        * `mvn spring-boot:run -Dspring.profiles.active=dev -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector`
        
        
#### Execute Unit Tests
* Execute: `mvn clean test`


#### Execute Integration Tests
* Execute: `mvn clean integration-test -DskipUTs=true` or `mvn clean verify -DskipUTs=true`


#### Test Coverage (via JaCoCo)
* In order to generate reports execute: `mvn clean verify`
    * In order to see unit test coverage open with browser: `target/site/jacoco-ut/index.html`
    * In order to see integration test coverage open with browser: `target/site/jacoco-it/index.html`
    
    
#### Swagger (Documentation)
* First run the service.

* Then:
    * For UI visit: `http://localhost:8181/swagger-ui.html`

    * For plain JSON visit: `http://localhost:8181/v2/api-docs`
    
    
#### Actuator Info
* Visit: `http://localhost:8181/actuator`

* Visit health checks/stats: `http://localhost:8181/actuator/health`

* See all available metrics: `http://localhost:8181/actuator/metrics`

* See more info for a specific metric: `http://localhost:8181/actuator/metrics/{metricName}`, eg: `http://localhost:8181/actuator/metrics/jvm.memory.max`

* See loggers configuration: `http://localhost:8181/actuator/loggers`

* See info about the service: `http://localhost:8181/actuator/info`


#### Application Metrics
* Visit: `http://localhost:8181/actuator/metrics/getStatistics`

* Visit: `http://localhost:8181/actuator/metrics/store`
