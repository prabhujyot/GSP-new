package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest


class RewardRepository(
    private val api: Api
): SafeApiRequest() {

    suspend fun getStatement(userId: Int, type: String, page: Int): String? {
        return apiRequest {
            api.getTransactions(userId,type,page)
        }
    }

    suspend fun getDailyReward(userId: Int, type: String): String? {
        return apiRequest {
            api.getDailyReward(userId,type)
        }
    }

    suspend fun setDailyReward(userId: Int, type: String, value: Int): String? {
        return apiRequest {
            api.setDailyReward(userId,type,value)
        }
    }

    suspend fun redeem(userId: Int, coins: Int): String? {
        return apiRequest {
            api.redeem(userId,coins)
        }
    }

}