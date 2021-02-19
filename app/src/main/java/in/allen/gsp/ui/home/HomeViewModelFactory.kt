package `in`.allen.gsp.ui.home

import `in`.allen.gsp.data.repositories.BannerRepository
import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.data.repositories.MessageRepository
import `in`.allen.gsp.data.repositories.UserRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class HomeViewModelFactory(
    private val userRepository: UserRepository,
    private val bannerRepository: BannerRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val messageRepository: MessageRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HomeViewModel(
            userRepository,
            bannerRepository,
            leaderboardRepository,
            messageRepository
        ) as T
    }
}