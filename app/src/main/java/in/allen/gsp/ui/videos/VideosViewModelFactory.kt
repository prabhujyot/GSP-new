package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.data.repositories.VideosRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class VideosViewModelFactory(
    private val repository: VideosRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistViewModel(repository) as T
    }
}