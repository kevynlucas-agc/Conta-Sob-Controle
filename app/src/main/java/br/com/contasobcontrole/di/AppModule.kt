package br.com.contasobcontrole.di

import android.content.Context
import androidx.room.Room
import br.com.contasobcontrole.data.AppDatabase
import br.com.contasobcontrole.data.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "conta-sob-controle.db").build()
    @Provides fun dao(db: AppDatabase): ExpenseDao = db.dao()
}
