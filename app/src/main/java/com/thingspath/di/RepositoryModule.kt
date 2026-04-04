package com.thingspath.di

import coil.ImageLoader
import com.thingspath.ThingsPathApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideImageLoader(app: ThingsPathApp): ImageLoader {
        return app.newImageLoader()
    }
}
