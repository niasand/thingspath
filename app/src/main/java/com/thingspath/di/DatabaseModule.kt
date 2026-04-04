package com.thingspath.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // D1ApiService and ItemRepository are @Singleton @Inject-constructable,
    // so no manual Provider needed — Hilt handles them automatically.
}
