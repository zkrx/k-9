package com.fsck.k9.ui.crypto.keyimport

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.secret_key_import_list_item.*

class ImportListItem(override var identifier: Long, private val userId: String) : AbstractItem<ImportViewHolder>() {
    override val type = R.id.secret_key_import_list_item
    override val layoutRes = R.layout.secret_key_import_list_item

    override fun getViewHolder(v: View): ImportViewHolder {
        return ImportViewHolder(v)
    }

    override fun bindView(holder: ImportViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)

        holder.title.text = userId
    }
}

class ImportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
    override val containerView = itemView
}
