package com.vibeup.android.di

import com.vibeup.android.data.repository.LibraryRepositoryImpl
import com.vibeup.android.data.repository.SongRepositoryImpl
import com.vibeup.android.domain.repository.LibraryRepository
import com.vibeup.android.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(
        songRepositoryImpl: SongRepositoryImpl
    ): SongRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        libraryRepositoryImpl: LibraryRepositoryImpl
    ): LibraryRepository
}