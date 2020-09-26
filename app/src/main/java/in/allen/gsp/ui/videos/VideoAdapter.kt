package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.data.db.entities.Video
import `in`.allen.gsp.databinding.ItemPlaylistBinding
import `in`.allen.gsp.utils.loadImage
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter: PagingDataAdapter<Video, VideoAdapter.VideoViewHolder>(VideoComparator) {
    override fun onBindViewHolder(holder: VideoAdapter.VideoViewHolder, position: Int) {
        val item = getItem(position)
        item?.let { holder.bindVideo(it) }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoViewHolder {
        return VideoViewHolder(
            ItemPlaylistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    inner class VideoViewHolder(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindVideo(item: Video) = binding.thumb.loadImage(item.thumb)
    }

    object VideoComparator : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem.videoId.equals(newItem.videoId,true)
        }

        override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem == newItem
        }
    }

}