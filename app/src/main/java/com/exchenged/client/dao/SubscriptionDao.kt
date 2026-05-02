package com.exchenged.client.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.exchenged.client.dto.Subscription
import kotlinx.coroutines.flow.Flow


@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscription")
    fun getALLSubscriptions(): Flow<List<Subscription>>


    @Insert
    suspend fun addSubscription(vararg subscription: Subscription): List<Long>


    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Update
    suspend fun updateSubscription(subscription: Subscription)


    @Query("SELECT * FROM subscription WHERE id = :id")
    fun selectSubscriptionById(id: Int): Flow<Subscription>
}