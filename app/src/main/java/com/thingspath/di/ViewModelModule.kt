package com.thingspath.di

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    @Singleton
    fun provideGetItemsUseCase(repository: ItemRepository): GetItemsUseCase {
        return GetItemsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetItemByIdUseCase(repository: ItemRepository): GetItemByIdUseCase {
        return GetItemByIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddItemUseCase(repository: ItemRepository): AddItemUseCase {
        return AddItemUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateItemUseCase(repository: ItemRepository): UpdateItemUseCase {
        return UpdateItemUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteItemUseCase(repository: ItemRepository): DeleteItemUseCase {
        return DeleteItemUseCase(repository)
    }
}
