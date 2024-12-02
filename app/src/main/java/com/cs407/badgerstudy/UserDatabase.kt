package com.cs407.badgerstudy

import UserDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao // little link the DAO to the database

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        // Singleton to prevent multiple instances of the database :)
        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database" // Database file name!!!!
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
