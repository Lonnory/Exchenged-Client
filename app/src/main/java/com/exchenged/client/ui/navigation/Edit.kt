package com.exchenged.client.ui.navigation

import com.exchenged.client.R
import kotlinx.serialization.Serializable

@Serializable
data object Edit: NavigateDestination {
    override val route: String
        get() = "edit"
    override val title: Int
        get() = R.string.edit
}