package `in`.allen.gsp.fragments

import `in`.allen.gsp.R
import `in`.allen.gsp.RewardActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class Prize : Fragment() {

    private val TAG = Prize::class.java.name

    private lateinit var parentActivity: RewardActivity
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        fun newInstance() = Prize()
    }
}