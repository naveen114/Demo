package ru.crew.motley.dere.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.feed_layout.view.*
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.networkrequest.feedmodels.FeedItem
import ru.crew.motley.dere.photo.activity.BucketView

class FeedAdapter(val context : Context, val feed : ArrayList<FeedItem>) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feed_layout, parent ,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return feed.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvMessage.text = feed.get(position).message
        GlideApp.with(context).asBitmap()
                .load(feed.get(position).full_picture)
                .centerCrop()
                .placeholder(R.drawable.defaultimage)
                .error(R.drawable.defaultimage)
                .into(holder.fullPicture)



        /*if (position == feed.size-1){
            val params : LinearLayout.LayoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 400
            holder.itemView.layoutParams = params
        }*/
    }


    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val fullPicture = itemView.findViewById(R.id.fullPicture) as ImageView
        val tvMessage = itemView.findViewById(R.id.tvMessage) as TextView
    }


}