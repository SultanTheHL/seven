package com.seven.seven.external

class ExternalApiException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

