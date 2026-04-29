package com.exchenged.client.ui.navigation

import com.exchenged.client.R
import kotlinx.serialization.Serializable

@Serializable
data object Apps: NavigateDestination {
    override val route: String
        get() = "apps"
    override val title: Int
        get() = R.string.all_app_settings
}