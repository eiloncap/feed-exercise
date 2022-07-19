package com.lightricks.feedexercise.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.lightricks.feedexercise.R
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.databinding.FeedFragmentBinding
import com.lightricks.feedexercise.network.FeedApiService

/**
 * This Fragment shows the feed grid. The feed consists of template thumbnail images.
 * Layout file: feed_fragment.xml
 */
class FeedFragment : Fragment() {
    private lateinit var dataBinding: FeedFragmentBinding
    private lateinit var viewModel: FeedViewModel
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.feed_fragment, container, false)
        setupViewModel()
        setupViews()
        return dataBinding.root
    }

    // todo: wait for Boris to answer
    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            FeedViewModelFactory(requireActivity().application)
        )[FeedViewModel::class.java]

        viewModel.getFeedItems().observe(viewLifecycleOwner, Observer { items ->
            feedAdapter.items = items
        })

        viewModel.getNetworkErrorEvent().observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { showNetworkError() }
        })
    }

    private fun setupViews() {
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewModel = viewModel
        feedAdapter = FeedAdapter()
        feedAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                dataBinding.recyclerView.scrollToPosition(0)
            }
        })
        dataBinding.recyclerView.adapter = feedAdapter
        dataBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun showNetworkError() {
        Snackbar.make(dataBinding.mainContent, R.string.network_error, LENGTH_LONG)
            .show()
    }
}