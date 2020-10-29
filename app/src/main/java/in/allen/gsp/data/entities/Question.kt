package `in`.allen.gsp.data.entities

data class Question (
    val qid: Int,
    val qdesc: String,
    val qdesc_hindi: String,
    val qtype: String,
    val qcat: String,
    val qattach: String,
    val qsummary: String,
    val qdifficulty_level: Int,
    val qformat: String,
    val qfile: String,
    val qPoint: Int,
    val option: List<Option>
) {
    var qno = 0
    var qTime = 30
}