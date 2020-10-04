package `in`.allen.gsp.ui.reward

import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class RewardViewModelFactory(
    private val userRepository: UserRepository,
    private val rewardRepository: RewardRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return RewardViewModel(userRepository,rewardRepository) as T
    }
}