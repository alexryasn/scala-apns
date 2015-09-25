package ru.ryasale.apns

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} %level %logger{5}.%M - %msg%n"
        //pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} %caller{2} - %msg%n"
    }
}

logger("com.javacodegeeks.examples.logbackexample.beans", INFO)
root(DEBUG, ["STDOUT"])