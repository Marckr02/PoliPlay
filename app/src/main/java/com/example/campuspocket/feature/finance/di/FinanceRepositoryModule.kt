package com.example.campuspocket.feature.finance.di

import com.example.campuspocket.feature.finance.data.repository.FinanceRepositoryImpl
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FinanceRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository
}
