# invite-server
[![Build Status](https://github.com/SURFnet/invite-server/actions/workflows/maven.yml/badge.svg)](https://github.com/SURFnet/invite-server/actions/workflows/maven.yml/badge.svg)

Guest application API

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 17
- Maven 3

First install Java 17 with a package manager and then export the correct the `JAVA_HOME`. For example on Mac OS:
```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home/
```
Then create the MySQL database:
```
DROP DATABASE IF EXISTS guests;
CREATE DATABASE guests CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### [Building and running](#building-and-running)

This project uses Spring Boot and Maven. To run locally, type:
```
mvn spring-boot:run
```
To build and deploy (the latter requires credentials in your maven settings):
```
mvn clean deploy
```
