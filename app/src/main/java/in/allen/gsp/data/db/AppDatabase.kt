package `in`.allen.gsp.data.db

import `in`.allen.gsp.data.db.dao.*
import `in`.allen.gsp.data.entities.*
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Leaderboard::class,
        Message::class,
        Video::class,
        Banner::class,
        Tile::class,
        Contest::class,
        Quiz::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {

    abstract fun getUserDao(): UserDao
    abstract fun getLeaderboardDao(): LeaderboardDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getVideoDao(): VideoDao
    abstract fun getBannerDao(): BannerDao
    abstract fun getTileDao(): TileDao
    abstract fun getQuizDao(): QuizDao
    abstract fun getContestDao(): ContestDao

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