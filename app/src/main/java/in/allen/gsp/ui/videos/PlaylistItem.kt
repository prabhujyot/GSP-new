package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.R
import `in`.allen.gsp.YTPlayerActivity
import `in`.allen.gsp.data.db.entities.Video
import `in`.allen.gsp.databinding.ItemPlaylistBinding
import `in`.allen.gsp.utils.tag
import android.content.Intent
import com.bumptech.glide.Glide
import com.xwray.groupie.databinding.BindableItem

class PlaylistItem(
    private val video: Video
): BindableItem<ItemPlaylistBinding>() {
    override fun bind(viewBinding: ItemPlaylistBinding, position: Int) {
        viewBinding.video = video
        tag("viewBinding.video.thumb ${video.thumb}")
        if(video.thumb.isNotEmpty())
            Glide.with(viewBinding.root.context)
                .load(video.thumb)
                .centerCrop()
                .into(viewBinding.thumb)

        viewBinding.ytItem.setOnClickListener {
            val act = Intent(viewBinding.root.context, YTPlayerActivity::class.java)
            act.putExtra("videoId", video.videoId)
            viewBinding.root.context.startActivity(act)
        }
    }

    override fun getLayout() = R.layout.item_playlist
}