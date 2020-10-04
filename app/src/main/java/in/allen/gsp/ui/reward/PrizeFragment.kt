package `in`.allen.gsp.ui.reward

import `in`.allen.gsp.R
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PrizeFragment : Fragment() {

    private val TAG = PrizeFragment::class.java.name

    private lateinit var parentActivity: RewardActivity
    private lateinit var rootView: View

    private var position = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt("position")
        }

        parentActivity = activity as RewardActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_prize, container, false)
        return rootView
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