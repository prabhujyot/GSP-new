package `in`.allen.gsp.ui.leaderboard

import `in`.allen.gsp.R
import `in`.allen.gsp.data.db.entities.Leaderboard
import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.databinding.ActivityLeaderboardBinding
import `in`.allen.gsp.databinding.ItemLeaderboardBinding
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.loadImage
import `in`.allen.gsp.utils.show
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class LeaderboardActivity : AppCompatActivity(), KodeinAware {

    private val TAG = LeaderboardActivity::class.java.name
    private lateinit var binding: ActivityLeaderboardBinding
    lateinit var viewModel: LeaderboardViewModel

    override val kodein by kodein()
    private val repository: LeaderboardRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard)
        viewModel = LeaderboardViewModel(repository)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        getLeaderboard()
    }

    private fun getLeaderboard() {
        lifecycleScope.launch {
            viewModel.leaderboard.await().observe(this@LeaderboardActivity, {
                binding.tinyProgressBar.show(false)

                if(it.size > 4) {
                    val listTop = it.subList(0,3)
                    val listOther = it.subList(3,100)

                    // rank 1
                    binding.rankFirstName.text = listTop[0].name
                    binding.rankFirstScore.text = "Score ${listTop[0].score}"
                    listTop[0].avatar.let { it1 -> binding.rankFirstAvatar.loadImage(it1,true) }

                    // rank 2
                    binding.rankSecondName.text = listTop[1].name
                    binding.rankSecondScore.text = "Score ${listTop[1].score}"
                    listTop[1].avatar.let { it1 -> binding.rankSecondAvatar.loadImage(it1,true) }

                    // rank 3
                    binding.rankThirdName.text = listTop[2].name
                    binding.rankThirdScore.text = "Score ${listTop[2].score}"
                    listTop[2].avatar.let { it1 -> binding.rankThirdAvatar.loadImage(it1,true) }


                    val recyclerAdapter = RecyclerViewAdapter(listOther, this@LeaderboardActivity)
                    binding.recyclerView.apply {
                        layoutManager =  LinearLayoutManager(context)
                        setHasFixedSize(true)
                        adapter = recyclerAdapter
                    }
                }
            })
        }
    }

    private class RecyclerViewAdapter(
        val list: List<Leaderboard>,
        val context: Context
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val binding: ItemLeaderboardBinding = DataBindingUtil.inflate(LayoutInflater.from(context),R.layout.item_leaderboard,parent,false)
            return ItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int {
            return list.size
        }

        class ItemViewHolder(val binding: ItemLeaderboardBinding):
            RecyclerView.ViewHolder(binding.root) {
            fun bind(data: Leaderboard) {
                binding.leaderboard = data
                binding.executePendingBindings()

                binding.rankAvatar.loadImage(data.avatar,true)
                binding.rankScore.text = "Score\n${data.score}"
            }
        }
    }

}