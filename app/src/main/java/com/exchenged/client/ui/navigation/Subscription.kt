package com.exchenged.client.ui.navigation

import com.exchenged.client.R
import kotlinx.serialization.Serializable

@Serializable
data object Subscription: NavigateDestination {
    override val route: String
        get() = "subscription"
    override val title: Int
        get() = R.string.subscription_title
}