package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Quiz
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(quiz: Quiz): Long

    @Query("select * from quiz where id = 0")
    fun getQuiz(): LiveData<Quiz>

    @Query("delete from quiz")
    fun delete()

    @Query("select * from quiz where id = :id")
    fun selectQuiz(id: Int): Quiz

}