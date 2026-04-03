package com.thingspath.di

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.remote.repository.R2ImageRepository
import com.thingspath.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context

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
    fun provideDeleteItemUseCase(
        repository: ItemRepository,
        r2ImageRepository: R2ImageRepository
    ): DeleteItemUseCase {
        return DeleteItemUseCase(repository, r2ImageRepository)
    }

    @Provides
    @Singleton
    fun provideUploadImageUseCase(
        @ApplicationContext context: Context,
        r2ImageRepository: R2ImageRepository
    ): UploadImageUseCase {
        return UploadImageUseCase(context, r2ImageRepository)
    }
}
