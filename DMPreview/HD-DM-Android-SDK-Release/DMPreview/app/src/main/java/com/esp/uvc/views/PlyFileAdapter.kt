package com.esp.uvc.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.esp.uvc.R
import kotlinx.android.synthetic.main.item_ply_file.view.*

class PlyFileAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mListener: OnClickListener? = null

    private var mFiles: List<String>? = null

    interface OnClickListener {

        fun onClick(file: String)

    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_ply_file, parent, false)
        view.setOnClickListener {
            mListener?.onClick(mFiles!![(parent as RecyclerView).getChildAdapterPosition(view)])
        }
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mFiles?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.tv_file_name.text = mFiles?.get(position)
    }

    fun setOnClickListener(listener: OnClickListener) {
        mListener = listener
    }

    fun setFiles(files: List<String>) {
        mFiles = files
        notifyDataSetChanged()
    }
}