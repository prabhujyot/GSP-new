package `in`.allen.gsp.ui.reward

import `in`.allen.gsp.R
import `in`.allen.gsp.databinding.FragmentPrizeBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class PrizeFragment : Fragment(), KodeinAware {

    private val TAG = PrizeFragment::class.java.name
    private lateinit var binding: FragmentPrizeBinding
    private lateinit var viewModel: RewardViewModel

    override val kodein by kodein()
    private val factory:RewardViewModelFactory by instance()

    private var position = 0

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_prize, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            viewModel = ViewModelProvider(it, factory).get(RewardViewModel::class.java)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(position: Int) = PrizeFragment().apply {
            arguments = Bundle().apply {
                putInt("position", position)
            }
        }
    }
}