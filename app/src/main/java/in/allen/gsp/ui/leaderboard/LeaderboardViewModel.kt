package `in`.allen.gsp.ui.leaderboard

import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.utils.lazyDeferred
import androidx.lifecycle.ViewModel


class LeaderboardViewModel(
    private val repository: LeaderboardRepository
): ViewModel() {

    val leaderboard by lazyDeferred {
        repository.getList()
    }

}