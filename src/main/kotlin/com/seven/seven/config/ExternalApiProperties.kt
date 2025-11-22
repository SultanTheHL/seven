package com.seven.seven.config

import io.github.cdimascio.dotenv.Dotenv
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external")
class ExternalApiProperties {
    var google: Google = Google()
    var weather: Weather = Weather()
    
    private val logger = LoggerFactory.getLogger(ExternalApiProperties::class.java)

    @PostConstruct
    fun init() {
        logger.info("=== External API Properties Initialization ===")
        
        // Load .env file from project root
        val dotenv = try {
            Dotenv.configure()
                .directory(".") // Look in project root
                .ignoreIfMissing() // Don't fail if .env doesn't exist
                .load()
        } catch (e: Exception) {
            logger.warn("Could not load .env file: ${e.message}")
            null
        }
        
        // Try to get API keys in order of priority:
        // 1. From application.properties (already bound)
        // 2. From .env file
        // 3. From environment variables
        // 4. From system properties
        
        val googleApiKey = when {
            google.apiKey.isNotBlank() -> {
                logger.info("Google API Key already set from application.properties")
                null // Already set, don't override
            }
            dotenv?.get("GOOGLE_API_KEY") != null -> {
                logger.info("Loading GOOGLE_API_KEY from .env file")
                dotenv.get("GOOGLE_API_KEY")
            }
            System.getenv("GOOGLE_API_KEY") != null -> {
                logger.info("Loading GOOGLE_API_KEY from environment variable")
                System.getenv("GOOGLE_API_KEY")
            }
            System.getProperty("GOOGLE_API_KEY") != null -> {
                logger.info("Loading GOOGLE_API_KEY from system property")
                System.getProperty("GOOGLE_API_KEY")
            }
            else -> {
                logger.warn("GOOGLE_API_KEY not found in .env, environment variables, or system properties")
                null
            }
        }
        
        val weatherApiKey = when {
            weather.apiKey.isNotBlank() -> {
                logger.info("Weather API Key already set from application.properties")
                null // Already set, don't override
            }
            dotenv?.get("WEATHER_API_KEY") != null -> {
                logger.info("Loading WEATHER_API_KEY from .env file")
                dotenv.get("WEATHER_API_KEY")
            }
            System.getenv("WEATHER_API_KEY") != null -> {
                logger.info("Loading WEATHER_API_KEY from environment variable")
                System.getenv("WEATHER_API_KEY")
            }
            System.getProperty("WEATHER_API_KEY") != null -> {
                logger.info("Loading WEATHER_API_KEY from system property")
                System.getProperty("WEATHER_API_KEY")
            }
            else -> {
                logger.warn("WEATHER_API_KEY not found in .env, environment variables, or system properties")
                null
            }
        }
        
        if (googleApiKey != null) {
            google = google.copy(apiKey = googleApiKey)
        }
        
        if (weatherApiKey != null) {
            weather = weather.copy(apiKey = weatherApiKey)
        }
        
        logger.info("Final Google API Key: ${if (google.apiKey.isNotBlank()) "SET (length: ${google.apiKey.length}, last 4: ${google.apiKey.takeLast(4)})" else "NOT SET - THIS WILL CAUSE ERRORS!"}")
        logger.info("Final Weather API Key: ${if (weather.apiKey.isNotBlank()) "SET (length: ${weather.apiKey.length}, last 4: ${weather.apiKey.takeLast(4)})" else "NOT SET - THIS WILL CAUSE ERRORS!"}")
        logger.info("=== End External API Properties Initialization ===")
    }

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

