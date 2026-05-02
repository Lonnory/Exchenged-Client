package com.exchenged.client.ui.navigation

import com.exchenged.client.R
import kotlinx.serialization.Serializable


@Serializable
data object Settings: NavigateDestination {
    override val route: String
        get() = "settings"
    override val title: Int
        get() = R.string.settings_title
}