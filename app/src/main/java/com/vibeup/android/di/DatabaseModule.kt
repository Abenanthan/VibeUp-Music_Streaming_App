package com.vibeup.android.di

import android.content.Context
import androidx.room.Room
import com.vibeup.android.data.local.VibeUpDatabase
import com.vibeup.android.data.local.dao.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.vibeup.android.data.local.dao.DownloadDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VibeUpDatabase {
        return Room.databaseBuilder(
            context,
            VibeUpDatabase::class.java,
            "vibeup_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDao(
        database: VibeUpDatabase
    ): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: VibeUpDatabase): DownloadDao {
        return database.downloadDao()
    }
}