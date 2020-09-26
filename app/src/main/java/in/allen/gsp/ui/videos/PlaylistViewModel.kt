package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.data.repositories.VideosRepository
import `in`.allen.gsp.utils.lazyDeferred
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig

class PlaylistViewModel(repository: VideosRepository) : ViewModel() {

    val hashMap = HashMap<String,String>()

    val videos by lazyDeferred {
        repository.getVideos(hashMap)
    }

}