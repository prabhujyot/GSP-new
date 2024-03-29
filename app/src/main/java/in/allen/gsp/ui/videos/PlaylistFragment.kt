package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Video
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.repositories.VideosRepository
import `in`.allen.gsp.databinding.FragmentPlaylistBinding
import `in`.allen.gsp.databinding.ItemPlaylistBinding
import `in`.allen.gsp.utils.loadImage
import `in`.allen.gsp.utils.show
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class PlaylistFragment : Fragment(), DIAware {

    private val TAG = PlaylistFragment::class.java.name
    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var viewModel: VideosViewModel

    override val di: DI by lazy { (requireContext() as DIAware).di }
    private val userRepository: UserRepository by instance()
    private val videosRepository: VideosRepository by instance()

    private lateinit var channelId: String

    private lateinit var parentActivity: VideosActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelId = it.getString("channelId").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        parentActivity = activity as VideosActivity
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_playlist,container,false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = VideosViewModel(userRepository,videosRepository)

        observeSuccess()
        viewModel.videoList(channelId)
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(viewLifecycleOwner) {
            if (it != null) {
                when (it.message) {
                    "videoList" -> {
                        if (it.data is Deferred<*>) {
                            val deferredList = it.data as Deferred<LiveData<List<Video>>>
                            lifecycleScope.launch {
                                deferredList.await().observe(viewLifecycleOwner) { list ->
                                    binding.tinyProgressBar.show(false)
                                    val recyclerAdapter =
                                        context?.let { it1 -> RecyclerViewAdapter(list, it1) }
                                    binding.recyclerView.apply {
                                        layoutManager = LinearLayoutManager(context)
                                        setHasFixedSize(true)
                                        adapter = recyclerAdapter
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(channelId: String) =
            PlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString("channelId", channelId)
                }
            }
    }


    private class RecyclerViewAdapter(
        val list: List<Video>,
        val context: Context
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val binding: ItemPlaylistBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.item_playlist,
                parent,
                false)
            return ItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int {
            return list.size
        }

        class ItemViewHolder(val binding: ItemPlaylistBinding):
            RecyclerView.ViewHolder(binding.root) {
            fun bind(data: Video) {
                binding.video = data

                if (data.thumb.isNotBlank())
                    binding.thumb.loadImage(data.thumb)

                binding.ytItem.setOnClickListener {
                    val act = Intent(binding.root.context, YTPlayerActivity::class.java)
                    act.putExtra("videoId", data.videoId)
                    binding.root.context.startActivity(act)
                }

                binding.executePendingBindings()
            }
        }
    }

}