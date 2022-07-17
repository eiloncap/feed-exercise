package com.lightricks.feedexercise.ui.feed

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lightricks.feedexercise.R
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.databinding.FeedItemBinding

/**
 * This class populates the [RecyclerView] with [FeedItem]s.
 * Item layout file: feed_item.xml
 */
class FeedAdapter : RecyclerView.Adapter<FeedItemHolder>() {
    var items: List<FeedItem> = emptyList()
        set(value) {
            val diff = DiffUtil.calculateDiff(FeedDiffer(field, value))
            field = value
            diff.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedItemHolder {
        return FeedItemHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.feed_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: FeedItemHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.binding.feedItemImage)
            .load(Uri.parse(item.thumbnailUrl))
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.binding.feedItemImage)
    }
}

/**
 * This class holds the views of a grid element.
 */
class FeedItemHolder(val binding: FeedItemBinding) : RecyclerView.ViewHolder(binding.root)

/**
 * This class helps to calculate the difference between two lists.
 */
class FeedDiffer(
    private val oldList: List<FeedItem>,
    private val newList: List<FeedItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}