package com.dc2cell.brankaspdf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dc2cell.brankaspdf.databinding.ItemPdfBinding

class PdfAdapter(private val onClick: (CloudFile) -> Unit) : ListAdapter<CloudFile, PdfAdapter.VH>(DIFF) {
    inner class VH(private val b: ItemPdfBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: CloudFile) { b.txtName.text = file.name; b.txtSize.text = "${formatBytes(file.size)} • ${file.mimeType}"; b.root.setOnClickListener { onClick(file) } }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    private fun formatBytes(value: Long): String = if (value >= 1024 * 1024) "%.1f MB".format(value / 1024f / 1024f) else "${(value / 1024).coerceAtLeast(1)} KB"
    companion object { private val DIFF = object : DiffUtil.ItemCallback<CloudFile>() { override fun areItemsTheSame(a: CloudFile, b: CloudFile) = a.id == b.id; override fun areContentsTheSame(a: CloudFile, b: CloudFile) = a == b } }
}
