package com.proxerme.app.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.library.connection.user.entitiy.ToptenEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenAdapter(savedInstanceState: Bundle? = null,
                    @CategoryParameter.Category private val category: String) :
        PagingAdapter<ToptenEntry>(savedInstanceState) {

    private companion object {
        private const val ITEMS_STATE = "adapter_topten_state_items"
    }

    override val stateKey = "${ITEMS_STATE}_$category"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_topten_entry, parent, false))

    class ViewHolder(itemView: View) : PagingViewHolder<ToptenEntry>(itemView) {

        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)

        init {
            itemView.setOnClickListener {
                //TODO
            }
        }

        override fun bind(item: ToptenEntry) {
            title.text = item.name

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

    }

}