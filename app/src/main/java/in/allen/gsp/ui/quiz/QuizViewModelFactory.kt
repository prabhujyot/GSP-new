package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.data.repositories.QuizRepository
import `in`.allen.gsp.data.repositories.UserRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class QuizViewModelFactory(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return QuizViewModel(userRepository,quizRepository) as T
    }
}