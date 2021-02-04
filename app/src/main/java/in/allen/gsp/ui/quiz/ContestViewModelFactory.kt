package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.data.repositories.QuizRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.AppPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class ContestViewModelFactory(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository,
    private val preferences: AppPreferences
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ContestViewModel(userRepository, quizRepository) as T
    }
}