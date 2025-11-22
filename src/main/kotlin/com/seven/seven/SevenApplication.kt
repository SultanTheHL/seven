package com.seven.seven

import com.seven.seven.config.ExternalApiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ExternalApiProperties::class)
class SevenApplication

fun main(args: Array<String>) {
	runApplication<SevenApplication>(*args)
}
