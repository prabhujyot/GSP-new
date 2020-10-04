package `in`.allen.gsp.data.entities

data class Statement(
    var id: Int,
    var type: String,
    var description: String,
    var value: String,
    var createDate: String,
    var updateDate: String,
    var status: String
)