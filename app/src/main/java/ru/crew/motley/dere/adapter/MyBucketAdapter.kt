package ru.crew.motley.dere.adapter

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.item_photo.view.*
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.photo.activity.MyBucketFullViewActivity
import ru.crew.motley.dere.photo.fragment.MyBucketFragment

class MyBucketAdapter(val context : Context,val photoWidth : Int) : RecyclerView.Adapter<MyBucketAdapter.ViewHolder>(){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view : View = LayoutInflater.from(context).inflate(R.layout.item_photo,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 10
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        GlideApp.with(context)
                .load(R.drawable.testing)
                .override(photoWidth, photoWidth)
                .centerCrop()
                .into(holder.thumbnail)
        holder.itemView.setOnClickListener {

            context.startActivity(Intent(context,MyBucketFullViewActivity::class.java));

        }
    }


    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val thumbnail = itemView.findViewById<ImageView>(R.id.thumbnail)
    }

}