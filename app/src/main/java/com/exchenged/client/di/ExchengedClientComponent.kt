package com.exchenged.client.di

import android.app.Activity
import android.app.Service
import android.content.Context
import com.exchenged.client.XrayAppCompatFactory
import dagger.BindsInstance
import dagger.Component
import javax.inject.Provider
import javax.inject.Singleton

/**
 *
 * As the root component for Dagger dependency injection
 */

@Singleton
@Component(modules = [GlobalModule::class])
interface ExchengedClientComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun bindContext(context: Context): Builder

        fun build(): ExchengedClientComponent
    }


    fun getVpnServices(): Map<Class<*>, Provider<Service>>
    fun getActivities(): Map<Class<*>, Provider<Activity>>



    fun inject(appCompatFactory: XrayAppCompatFactory)
    
    fun inject(worker: com.exchenged.client.update.SubscriptionUpdateWorker)

}