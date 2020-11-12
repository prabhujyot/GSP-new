package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.*
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.utils.Coroutines
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


class QuizRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    private val gson = Gson()
    private val quiz = MutableLiveData<Quiz>()

    init {
        quiz.observeForever {
            setDBQuiz(it)
        }
    }

    suspend fun getQuiz(quiz_no: Int, qid: String, question_cat: String): LiveData<Quiz> {
        return withContext(Dispatchers.IO) {
            val response = apiRequest {
                api.getQuiz(quiz_no, qid, question_cat)
            }

            quiz.postValue(response?.let {
                createData(quiz_no,it)
            })

            // from database
            getDBQuiz()
        }
    }

    suspend fun getWildQuiz(user_id: Int, value: Int): String? {
        return apiRequest {
            api.getWQset(user_id, value)
        }
    }

    suspend fun getPreview(user_id: Int): LiveData<Quiz> {
        return withContext(Dispatchers.IO) {
//            val response = apiRequest {
//                api.getPreview(user_id)
//            }
//
//            quiz.postValue(response?.let {
//                createData(0,it)
//            })

            // from database
            getDBQuiz()
        }
    }

    suspend fun saveQuiz(quiz: Quiz): String? {
        return apiRequest {
            api.postQuiz(quiz)
        }
    }

    private fun getDBQuiz() = db.getQuizDao().getQuiz()

    private fun setDBQuiz(quiz: Quiz) {
        Coroutines.io {
            db.getQuizDao().upsert(quiz)
        }
    }

    private fun createData(game_no: Int, data: String): Quiz? {
        var quiz: Quiz ?= null

        val listQset = ArrayList<Question>()
        val listFset: ArrayList<Question>
        val listWset: ArrayList<Question>
        val listAttachment: ArrayList<Attachment>

        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val dataObj = response.getJSONObject("data")

            // qset
            val arr = dataObj.getJSONArray("qset")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject

                    // setting options
                    val optionsArr: JSONArray = item.getJSONArray("options")
                    val options: MutableList<Option> = ArrayList()
                    for (j in 0 until optionsArr.length()) {
                        val obj2 = optionsArr[j] as JSONObject
                        val option = Option(
                            obj2.getInt("aid"),
                            obj2.getInt("aqid"),
                            obj2.getString("adesc"),
                            obj2.getString("adesc_hindi"),
                            "",
                            obj2.getInt("acorrect")
                        )
                        options.add(option)
                    }

                    val qset = Question(
                        item.getInt("qid"),
                        item.getString("qdesc"),
                        item.getString("qdesc_hindi"),
                        item.getString("qtype"),
                        item.getString("qcat"),
                        item.getString("qattach"),
                        item.getString("qsummary"),
                        item.getInt("qdifficulty_level"),
                        item.getString("qformat"),
                        item.getString("qfile"),
                        1,
                        options
                    )

                    qset.qno = i.plus(1)
                    when(item.getInt("qdifficulty_level")) {
                        1 -> {
                            qset.qTime = 30
                        }
                        2 -> {
                            qset.qTime = 45
                        }
                        3 -> {
                            qset.qTime = 60
                        }
                    }

                    listQset.add(qset)
                }
            }

            // fset
            listFset = createFsetData(data) as ArrayList<Question>

            // wset
            listWset = createWsetData(data) as ArrayList<Question>

            // attachment
            listAttachment = createAttachmentData(data) as ArrayList<Attachment>

            quiz = Quiz(
                0,
                game_no,
                gson.toJson(listQset),
                gson.toJson(listWset),
                gson.toJson(listFset),
                gson.toJson(listAttachment),
                0,
                1)
        }
        return quiz
    }

    private fun createFsetData(data: String): List<Question> {
        val list = ArrayList<Question>()
        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val dataObj = response.getJSONObject("data")
            val arr = dataObj.getJSONArray("fset")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject

                    // setting options
                    val optionsArr: JSONArray = item.getJSONArray("options")
                    val options: MutableList<Option> = ArrayList()
                    for (j in 0 until optionsArr.length()) {
                        val obj2 = optionsArr[j] as JSONObject
                        val option = Option(
                            obj2.getInt("aid"),
                            obj2.getInt("aqid"),
                            obj2.getString("adesc"),
                            obj2.getString("adesc_hindi"),
                            "",
                            obj2.getInt("acorrect")
                        )
                        options.add(option)
                    }

                    val fset = Question(
                        item.getInt("qid"),
                        item.getString("qdesc"),
                        item.getString("qdesc_hindi"),
                        item.getString("qtype"),
                        item.getString("qcat"),
                        item.getString("qattach"),
                        item.getString("qsummary"),
                        item.getInt("qdifficulty_level"),
                        item.getString("qformat"),
                        item.getString("qfile"),
                        1,
                        options
                    )
                    list.add(fset)
                }
            }
        }
        return list
    }

    private fun createWsetData(data: String): List<Question> {
        val list = ArrayList<Question>()
        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val dataObj = response.getJSONObject("data")
            val arr = dataObj.getJSONArray("wset")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject

                    // setting options
                    val optionsArr: JSONArray = item.getJSONArray("options")
                    val options: MutableList<Option> = ArrayList()
                    for (j in 0 until optionsArr.length()) {
                        val obj2 = optionsArr[j] as JSONObject
                        val option = Option(
                            obj2.getInt("aid"),
                            obj2.getInt("aqid"),
                            obj2.getString("adesc"),
                            obj2.getString("adesc_hindi"),
                            "",
                            obj2.getInt("acorrect")
                        )
                        options.add(option)
                    }

                    val wset = Question(
                        item.getInt("qid"),
                        item.getString("qdesc"),
                        item.getString("qdesc_hindi"),
                        item.getString("qtype"),
                        item.getString("qcat"),
                        item.getString("qattach"),
                        item.getString("qsummary"),
                        item.getInt("qdifficulty_level"),
                        item.getString("qformat"),
                        item.getString("qfile"),
                        1,
                        options
                    )
                    list.add(wset)
                }
            }
        }
        return list
    }

    private fun createAttachmentData(data: String): List<Attachment> {
        val list = ArrayList<Attachment>()
        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val dataObj = response.getJSONObject("data")

            if(!dataObj.getString("attachment").equals("false",true)) {
                val arr = dataObj.getJSONArray("attachment")
                if (arr.length() > 0) {
                    for (i in 0 until arr.length()) {
                        val item = arr.get(i) as JSONObject
                        val attachment = Attachment(
                            item.getInt("qid"),
                            item.getString("type"),
                            item.getString("data"),
                            item.getString("filename")
                        )
                        list.add(attachment)
                    }
                }
            }
        }
        return list
    }


    fun getAttachmentList(quiz: Quiz): ArrayList<Attachment> {
        var list = ArrayList<Attachment>()
        if(quiz.attachment.trim().isNotEmpty()) {
            list = gson.fromJson(quiz.attachment, object: TypeToken<ArrayList<Attachment>>(){}.type)
        }
        return list
    }

    fun getQset(quiz: Quiz): ArrayList<Question> {
        var list = ArrayList<Question>()
        if(quiz.qset.trim().isNotEmpty()) {
            list = gson.fromJson(quiz.qset, object: TypeToken<ArrayList<Question>>(){}.type)
        }
        return list
    }

    fun getWset(quiz: Quiz): ArrayList<Question> {
        var list = ArrayList<Question>()
        if(quiz.wset.trim().isNotEmpty()) {
            list = gson.fromJson(quiz.wset, object: TypeToken<ArrayList<Question>>(){}.type)
        }
        return list
    }

    fun getFset(quiz: Quiz): ArrayList<Question> {
        var list = ArrayList<Question>()
        if(quiz.fset.trim().isNotEmpty()) {
            list = gson.fromJson(quiz.fset, object: TypeToken<ArrayList<Question>>(){}.type)
        }
        return list
    }

}