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

This branch uses `InfluxDB`(https://www.influxdata.com/) as persistence storage.

Also in order to not having as Primary Key only time in InfluxDB, meaning that
measurements/captures with same time but different vegetation value will be upserted from the last saved one, 
we have used as a tag, a virtual `sensor-id` which is a random UUID.


#### Requirements / Infrastructure(InfluxDB)
* You need to have `docker-compose` installed

* Then you can run `docker-compose up` in order to bring up an InfluxDB instance.

* (Optional) in order to shutdown InfluxDB instance, execute: `docker-compose down`


#### Open terminal in dockerized InfluxDB

* Execute: `docker exec -ti influxdb bash`

* Then execute: `influx` you should have an InfluxDB shell now.

* Execute: `use field_captures` in order to select the database: `field_captures`

* Sample queries to execute: 

    * `select * from field_condition_capture;`
    
    * `select min(vegetation) from field_condition_capture;`
    
    * `select max(vegetation) from field_condition_capture;`
    
    * `select mean(vegetation) from field_condition_capture;`
    
    * `select sum(vegetation) from field_condition_capture;`

    * `select count(vegetation) from field_condition_capture;`
    
    * `select sum(vegetation) / count(vegetation) from field_condition_capture where time >= now() - 30d and  time <= now();`
    

#### How to run service (you should run docker-compose up first - needs InfluxDB)
* Two options:
    * Execute: 
        * `mvn clean install -DskipUTs=true -DskipITs`
        * `java -jar -Dspring.profiles.active=dev -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector target/field-condition-statistics-service-1.0-SNAPSHOT.jar`
                
    * Execute:
        * `mvn spring-boot:run -Dspring.profiles.active=dev -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector`
        
        
#### Execute Unit Tests
* Execute: `mvn clean test`


#### Execute Integration Tests (you should run docker-compose up first - needs InfluxDB)
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
