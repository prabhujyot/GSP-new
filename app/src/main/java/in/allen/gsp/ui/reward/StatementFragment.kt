package `in`.allen.gsp.ui.reward

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Statement
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.FragmentStatementBinding
import `in`.allen.gsp.databinding.ItemStatementBinding
import `in`.allen.gsp.utils.show
import `in`.allen.gsp.utils.tag
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class StatementFragment : Fragment(), KodeinAware {

    private val TAG = StatementFragment::class.java.name
    private lateinit var binding: FragmentStatementBinding
    private lateinit var viewModel: RewardViewModel

    override val kodein by kodein()
    private val factory:RewardViewModelFactory by instance()

    private var position = 0

    private var type = ""
    private var page = 1
    private var nomore = false
    private var loading = true
    private var pastVisibleItems: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

    private val list = ArrayList<Statement>()
    private lateinit var recyclerAdapter: RecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt("position")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_statement, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this, factory).get(RewardViewModel::class.java)

        if(position == 1) {
            type = "earn"
        } else if(position == 2) {
            type = "redeem"
        }

        initRecyclerView()
        observeSuccess()
        viewModel.userData()
    }

    companion object {
        @JvmStatic
        fun newInstance(position: Int) =
            StatementFragment().apply {
                arguments = Bundle().apply {
                    putInt("position", position)
                }
            }
    }

    private fun observeSuccess() {
        viewModel.stateSuccess().observe(viewLifecycleOwner, {
            tag("TAG _success: $it")
            if(it != null) {
                when (it.message) {
                    "user" -> {
                        viewModel.getStatement(type,page)
                    }

                    "statement" -> {
                        loading = true
                        binding.progressBar.show(false)
                        if(it.data is JSONObject) {
                            val data = it.data
                            if (!data.getString("list").equals("false",true)) {
                                val arr: JSONArray = data.getJSONArray("list")
                                if (arr.length() > 0) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr[i] as JSONObject
                                        if (obj.getString("value")
                                                .equals("0", ignoreCase = true) && obj.getString("status")
                                                .equals("1", ignoreCase = true)
                                        ) {
                                            continue
                                        }
                                        val statement = Statement(
                                            obj.getInt("id"),
                                            obj.getString("type"),
                                            obj.getString("description"),
                                            obj.getString("value"),
                                            obj.getString("create_date"),
                                            obj.getString("update_date"),
                                            obj.getString("status")
                                        )
                                        list.add(statement)
                                    }
                                }

                                page = data.getInt("page")
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                            } else {
                                nomore = true
                            }
                        }
                    }
                }
            }
        })
    }

    private fun initRecyclerView() {
        recyclerAdapter = context?.let { RecyclerViewAdapter(list, it) }!!
        binding.recyclerView.apply {
            layoutManager =  LinearLayoutManager(context)
            setHasFixedSize(true)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        visibleItemCount = (layoutManager as LinearLayoutManager).childCount
                        totalItemCount = (layoutManager as LinearLayoutManager).itemCount
                        pastVisibleItems =
                            (recyclerView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
                        if (loading) {
                            if (visibleItemCount + pastVisibleItems >= totalItemCount - 2) {
                                loading = false
                                if (!nomore) {
                                    viewModel.getStatement(type,page)
                                }
                            }
                        }
                    }
                }
            })

            adapter = recyclerAdapter
        }
    }

    private class RecyclerViewAdapter(
        val items: List<Statement>,
        val context: Context
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val binding: ItemStatementBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.item_statement,
                parent,
                false
            )
            return ItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(items[position])
            if(position == 0) {
                holder.separator.show(false)
            } else {
                holder.separator.show()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        class ItemViewHolder(
            val binding: ItemStatementBinding
        ): RecyclerView.ViewHolder(binding.root) {
            fun bind(data: Statement) {
                binding.statement = data
                binding.executePendingBindings()
            }
            val separator: View = binding.separator
        }
    }
}