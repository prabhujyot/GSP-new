package `in`.allen.gsp.ui.leaderboard

import `in`.allen.gsp.data.repositories.LeaderboardRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class LeaderboardViewModelFactory(
    private val repository: LeaderboardRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return LeaderboardViewModel(repository) as T
    }
}