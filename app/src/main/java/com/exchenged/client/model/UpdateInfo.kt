package com.exchenged.client.model

import com.google.gson.annotations.SerializedName

data class UpdateInfo(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("changelog") val changelog: String
)
