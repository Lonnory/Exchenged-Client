package com.exchenged.client.repository

import com.exchenged.client.dao.SubscriptionDao
import com.exchenged.client.dto.Subscription
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository
@Inject constructor(
    val subscriptionDao: SubscriptionDao
){

    val allSubscriptions = subscriptionDao.getALLSubscriptions()


    suspend fun addSubscription(subscription: Subscription): Long {
        return subscriptionDao.addSubscription(subscription).first()
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription)
    }

    fun getSubscriptionById(id: Int): Flow<Subscription> {
        return subscriptionDao.selectSubscriptionById(id)
    }
}