package com.exchenged.client.ui.navigation

import com.exchenged.client.R
import kotlinx.serialization.Serializable

@Serializable
data object Config: NavigateDestination {
    override val route: String
        get() = "config"
    override val title: Int
        get() = R.string.config
}

