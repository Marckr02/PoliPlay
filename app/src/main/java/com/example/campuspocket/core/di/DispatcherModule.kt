package com.example.campuspocket.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    @Named("io")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @Named("default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @Named("main")
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    fun provideNamedDispatchers(
        @Named("io") io: CoroutineDispatcher,
        @Named("default") default: CoroutineDispatcher,
        @Named("main") main: CoroutineDispatcher
    ): NamedDispatchers = NamedDispatchers(io, default, main)
}

class NamedDispatchers @Inject constructor(
    @Named("io") val io: CoroutineDispatcher,
    @Named("default") val default: CoroutineDispatcher,
    @Named("main") val main: CoroutineDispatcher
)