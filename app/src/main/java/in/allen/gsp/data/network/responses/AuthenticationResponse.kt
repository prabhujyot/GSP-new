package `in`.allen.gsp.data.network.responses

import `in`.allen.gsp.data.db.entities.User

data class AuthenticationResponse(
    val status: Int,
    val message: String,
    val data: User
)