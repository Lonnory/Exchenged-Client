package com.exchenged.client.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mark: String,
    val url: String,
    val isAutoUpdate: Boolean = false,
    val updateInterval: Long = 0, // hours
    val userInfo: String? = null,
    val supportUrl: String? = null,
    val webPageUrl: String? = null,
    val isLocked: Boolean = false,
    val announce: String? = null
)
