package `in`.allen.gsp.ui.message

import `in`.allen.gsp.data.repositories.MessageRepository
import `in`.allen.gsp.data.repositories.UserRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class NotificationViewModelFactory(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificationViewModel(userRepository,messageRepository) as T
    }
}