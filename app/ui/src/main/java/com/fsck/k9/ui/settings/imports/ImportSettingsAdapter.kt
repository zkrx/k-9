package com.fsck.k9.ui.settings.imports

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.fsck.k9.preferences.SettingsImporter
import com.fsck.k9.ui.R

class ImportSettingsAdapter(
        private val layoutInflater: LayoutInflater
) : RecyclerView.Adapter<ImportAccountViewHolder>() {
    val accounts = mutableListOf<SettingsImporter.AccountDescription>()
    val checked = mutableListOf<Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ImportAccountViewHolder(layoutInflater.inflate(R.layout.item_import_account, parent, false))

    override fun getItemCount() = accounts.size

    override fun onBindViewHolder(holder: ImportAccountViewHolder, position: Int) {
        val accountDescription = accounts[position]
        holder.description.text = accountDescription.name
    }

    fun replaceData(accounts: List<SettingsImporter.AccountDescription>) {
        this.accounts.clear()
        this.accounts.addAll(accounts)
        this.checked.clear()
        for (i in 0..accounts.size) {
            this.checked.add(false)
        }
        notifyDataSetChanged()
    }

    fun getCheckedUuids() = accounts.asSequence().filterIndexed { i, _ -> checked[i] }.map { it.uuid }.toList()
}

class ImportAccountViewHolder(
        itemView: View,
        val description: CheckBox = itemView.findViewById(R.id.checkbox_import_account)
) : RecyclerView.ViewHolder(itemView)
