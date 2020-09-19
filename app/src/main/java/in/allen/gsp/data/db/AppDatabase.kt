package `in`.allen.gsp.data.db

import `in`.allen.gsp.data.db.dao.MessageDao
import `in`.allen.gsp.data.db.dao.UserDao
import `in`.allen.gsp.data.db.entities.Message
import `in`.allen.gsp.data.db.entities.User
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [User::class,Message::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {

    abstract fun getUserDao(): UserDao
    abstract fun getMessageDao(): MessageDao

    companion object {
        @Volatile
        private var instance: AppDatabase ?= null
        private var LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also {
                instance = it
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gspDatabase.db"
            ).build()
    }

}