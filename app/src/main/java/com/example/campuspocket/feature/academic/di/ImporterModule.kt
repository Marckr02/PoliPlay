package com.example.campuspocket.feature.academic.di

import com.example.campuspocket.feature.academic.importer.PdfBoxTextExtractor
import com.example.campuspocket.feature.academic.importer.PdfTextExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImporterModule {

    @Binds
    @Singleton
    abstract fun bindPdfTextExtractor(impl: PdfBoxTextExtractor): PdfTextExtractor
}
