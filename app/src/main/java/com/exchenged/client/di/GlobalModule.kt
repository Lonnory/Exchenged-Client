package com.exchenged.client.di

import android.content.Context
import com.exchenged.client.core.TrafficDetector
import com.exchenged.client.core.XrayCoreManager
import com.exchenged.client.common.di.qualifier.Application
import com.exchenged.client.dao.SubscriptionDao
import com.exchenged.client.dao.ExchengedClientDatabase
import com.exchenged.client.parser.SubscriptionParser
import com.exchenged.client.common.di.qualifier.Background
import com.exchenged.client.common.di.qualifier.Main
import com.exchenged.client.dao.NodeDao
import com.exchenged.client.tun2socks.utils.NetPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import com.exchenged.client.tun2socks.TProxyService
import com.exchenged.client.tun2socks.Tun2SocksService
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Singleton


@Module(includes = [
    ServiceModule::class,
    ActivityModule::class,
    CoroutinesModule::class,
    NetworkModule::class
])
abstract class GlobalModule {

 companion object {

     @Provides
     @Application
     fun provideContext(context: Context): Context {
         return context.applicationContext
     }


     @Provides
     @Background
     @Singleton
     fun provideBackgroundExecutor(): Executor {
         return Executors.newSingleThreadExecutor()
     }

     @Provides
     @Main
     @Singleton
     fun provideMainExecutor(context: Context): Executor {
         return context.mainExecutor
     }


     @Provides
     @Singleton
     fun providePreferences(context: Context): NetPreferences {
         return NetPreferences(context)
     }

     @Provides
     @Singleton
     fun provideXrayDatabase(context: Context): ExchengedClientDatabase {
         return ExchengedClientDatabase.getXrayDatabase(context)
     }


     @Provides
     @Singleton
     fun provideNodeDao(exchengedClientDatabase: ExchengedClientDatabase): NodeDao {
         return exchengedClientDatabase.NodeDao()
     }

     @Provides
     @Singleton
     fun provideSubscriptionDao(exchengedClientDatabase: ExchengedClientDatabase): SubscriptionDao {
         return exchengedClientDatabase.SubscriptionDao()
     }


     @Provides
     @Singleton
     fun provideBase64Parser(): SubscriptionParser {
         return SubscriptionParser()
     }
 }

    @Binds
    abstract fun bindTun2SocksService(service: TProxyService): Tun2SocksService

    @Binds
    abstract fun bindTrafficDetector(xrayCoreManager: XrayCoreManager): TrafficDetector


}