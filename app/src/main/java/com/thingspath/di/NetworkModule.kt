package com.thingspath.di

import com.thingspath.data.remote.SiliconFlowClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSiliconFlowClient(): SiliconFlowClient {
        return SiliconFlowClient()
    }
}
