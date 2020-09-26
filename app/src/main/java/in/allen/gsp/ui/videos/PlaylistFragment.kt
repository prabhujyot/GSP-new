package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.R
import `in`.allen.gsp.data.db.entities.Video
import `in`.allen.gsp.utils.Coroutines
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.playlist_fragment.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class PlaylistFragment : Fragment(), KodeinAware {

    override val kodein by kodein()
    private lateinit var viewModel: PlaylistViewModel
    private val factory:VideosViewModelFactory by instance()

    private lateinit var playlistId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString("playlistId").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.playlist_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this, factory).get(PlaylistViewModel::class.java)
        bindUI()
    }

    private fun bindUI() = Coroutines.main {
        viewModel.hashMap["part"] = "snippet"
        viewModel.hashMap["maxResults"] = "50"
        viewModel.hashMap["playlistId"] = playlistId
        viewModel.hashMap["key"] = "AIzaSyDbpMioiHvdMHmA41UETZy6sCO1txortVc"
        viewModel.videos.await().observe(viewLifecycleOwner, Observer {
            tinyProgressBar.visibility = View.GONE
            initRecyclerView(it.toPlaylisItem())
        })
    }

    private fun initRecyclerView(playlistItem: List<PlaylistItem>) {
        var pageToken = ""
        var nomore = false
        var loading = true
        var pastVisiblesItems = 0
        var visibleItemCount = 0
        var totalItemCount = 0

        val groupAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(playlistItem)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = groupAdapter

//            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                    if (dy > 0) {
//                        val linearLayoutManager = layoutManager as LinearLayoutManager
//                        visibleItemCount = linearLayoutManager.childCount
//                        totalItemCount = linearLayoutManager.itemCount
//                        pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition()
//                        if (loading) {
//                            if (visibleItemCount + pastVisiblesItems >= totalItemCount - 2) {
//                                loading = false
//                                if (!nomore) {
//                                    getPlaylist(playlistId)
//                                }
//                            }
//                        }
//                    }
//                }
//            })
        }
    }

    private fun List<Video>.toPlaylisItem(): List<PlaylistItem> {
        return map {
            PlaylistItem(it)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(playlistId: String) =
            PlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString("playlistId", playlistId)
                }
            }
    }

}