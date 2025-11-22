package com.seven.seven.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external")
data class ExternalApiProperties(
    val google: Google = Google(),
    val weather: Weather = Weather()
) {
    data class Google(
        val apiKey: String = "",
        val directionsUrl: String = "https://maps.googleapis.com/maps/api/directions/json",
        val elevationUrl: String = "https://maps.googleapis.com/maps/api/elevation/json",
        val roadsUrl: String = "https://roads.googleapis.com/v1/snapToRoads"
    )

    data class Weather(
        val apiKey: String = "",
        val forecastUrl: String = "https://api.openweathermap.org/data/2.5/forecast"
    )
}

