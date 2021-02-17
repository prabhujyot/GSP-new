package `in`.allen.gsp.ui.leaderboard

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Leaderboard
import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityLeaderboardBinding
import `in`.allen.gsp.databinding.ItemLeaderboardBinding
import `in`.allen.gsp.utils.*
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class LeaderboardActivity : AppCompatActivity(), KodeinAware {

    private val TAG = LeaderboardActivity::class.java.name
    private lateinit var binding: ActivityLeaderboardBinding
    lateinit var viewModel: LeaderboardViewModel

    override val kodein by kodein()
    private val userRepository: UserRepository by instance()
    private val repository: LeaderboardRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard)
        viewModel = LeaderboardViewModel(userRepository,repository)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        observeLoading()
        observeError()
        observeSuccess()
    }

    private fun observeLoading() {
        viewModel.getLoading().observe(this, {
            tag("$TAG _loading: ${it.message}")
            binding.tinyProgressBar.show(false)
            if (it.data is Boolean && it.data) {
                binding.tinyProgressBar.show()
            }
        })
    }

    private fun observeError() {
        viewModel.getError().observe(this, {
            tag("$TAG _error: ${it.message}")
            if (it != null) {
                when (it.message) {
                    "alert" -> {
                        it.data?.let { it1 -> alertDialog("Error", it1) {} }
                    }
                    "tag" -> {
                        it.data?.let { it1 -> tag("$TAG $it1") }
                    }
                    "toast" -> {
                        it.data?.let { it1 -> toast(it1) }
                    }
                    "snackbar" -> {
                        it.data?.let { it1 -> binding.rootLayout.snackbar(it1) }
                    }
                }
            }
        })
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(this, {
            if(it != null) {
                when (it.message) {
                    "leaderboard" -> {
                        if(it.data is Deferred<*>) {
                            val deferredList = it.data as Deferred<LiveData<List<Leaderboard>>>
                            lifecycleScope.launch {
                                deferredList.await().observe(this@LeaderboardActivity, { list->
                                    binding.tinyProgressBar.show(false)
                                    if(list.size > 4) {
                                        val listTop = list.subList(0,3)
                                        var listOther: List<Leaderboard> = if(list.size < 100) {
                                            list.subList(3, list.size)
                                        } else {
                                            list.subList(3, 100)
                                        }

                                        // rank 1
                                        binding.root.findViewById<TextView>(R.id.rank_first_name).text = listTop[0].name
                                        binding.root.findViewById<TextView>(R.id.rank_first_score).text = "Score ${listTop[0].score}"
                                        listTop[0].avatar.let {
                                                it1 -> binding.root.findViewById<FloatingActionButton>(R.id.rank_first_avatar).loadImage(it1,true)
                                        }

                                        // rank 2
                                        binding.root.findViewById<TextView>(R.id.rank_second_name).text = listTop[1].name
                                        binding.root.findViewById<TextView>(R.id.rank_second_score).text = "Score ${listTop[1].score}"
                                        listTop[1].avatar.let {
                                                it1 -> binding.root.findViewById<FloatingActionButton>(R.id.rank_second_avatar).loadImage(it1,true)
                                        }

                                        // rank 3
                                        binding.root.findViewById<TextView>(R.id.rank_third_name).text = listTop[2].name
                                        binding.root.findViewById<TextView>(R.id.rank_third_score).text = "Score ${listTop[2].score}"
                                        listTop[2].avatar.let {
                                                it1 -> binding.root.findViewById<FloatingActionButton>(R.id.rank_third_avatar).loadImage(it1,true)
                                        }

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
                    }
                }
            }
        })
    }

    private class RecyclerViewAdapter(
        val list: List<Leaderboard>,
        val context: Context
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val binding: ItemLeaderboardBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context)
                ,R.layout.item_leaderboard,
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

        class ItemViewHolder(val binding: ItemLeaderboardBinding):
            RecyclerView.ViewHolder(binding.root) {
            fun bind(data: Leaderboard) {
                binding.leaderboard = data
                binding.rankAvatar.loadImage(data.avatar,true)
                binding.rankScore.text = "Score\n${data.score}"

                binding.executePendingBindings()
            }
        }
    }

}