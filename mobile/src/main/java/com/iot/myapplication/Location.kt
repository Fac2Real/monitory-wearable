package com.iot.myapplication

import com.google.gson.annotations.SerializedName

data class Location(
    @SerializedName("zoneId")
    val zoneId: String,
    @SerializedName("zoneName")
    val zoneName: String,
)
