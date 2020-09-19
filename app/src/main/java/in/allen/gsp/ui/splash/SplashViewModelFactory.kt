package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.data.repositories.UserRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignInClient

@Suppress("UNCHECKED_CAST")
class SplashViewModelFactory(
    private val repository: UserRepository,
    private val googleSignInClient: GoogleSignInClient
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SplashViewModel(repository,googleSignInClient) as T
    }
}