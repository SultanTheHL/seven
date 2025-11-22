package com.seven.seven.config

import tools.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun restClient(): RestClient = RestClient.builder().build()

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
}

