package com.retrofit

import java.time.LocalDateTime

data class WorkerLocationRequest (
    val zoneId: String,
    val workerId: String,
    val timestamp: String
)