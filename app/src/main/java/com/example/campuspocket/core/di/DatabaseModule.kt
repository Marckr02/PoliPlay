package com.example.campuspocket.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.core.database.ImportHintDao
import com.example.campuspocket.core.database.Migrations
import com.example.campuspocket.core.database.Seeder
import com.example.campuspocket.feature.academic.data.ClassSessionDao
import com.example.campuspocket.feature.academic.data.CourseDao
import com.example.campuspocket.feature.academic.data.SemesterDao
import com.example.campuspocket.feature.academic.data.TaskDao
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import com.example.campuspocket.feature.finance.data.AccountDao
import com.example.campuspocket.feature.finance.data.BudgetDao
import com.example.campuspocket.feature.finance.data.CategoryDao
import com.example.campuspocket.feature.finance.data.ScheduledPaymentDao
import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.notes.data.FolderDao
import com.example.campuspocket.feature.notes.data.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "campus_pocket.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(*Migrations.ALL)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    Seeder.seed(db)
                }
            })
            .build()

    @Provides fun provideSemesterDao(db: AppDatabase): SemesterDao = db.semesterDao()
    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun provideClassSessionDao(db: AppDatabase): ClassSessionDao = db.classSessionDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideTaskReminderDao(db: AppDatabase): TaskReminderDao = db.taskReminderDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideScheduledPaymentDao(db: AppDatabase): ScheduledPaymentDao = db.scheduledPaymentDao()
    @Provides fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideImportHintDao(db: AppDatabase): ImportHintDao = db.importHintDao()
}
