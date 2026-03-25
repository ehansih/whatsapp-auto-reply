package com.ehansih.whatsapprules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ehansih.whatsapprules.databinding.ItemRuleBinding

class RulesAdapter(
    private val onEdit: (Rule) -> Unit,
    private val onDelete: (Rule) -> Unit,
    private val onToggle: (Rule, Boolean) -> Unit
) : ListAdapter<Rule, RulesAdapter.RuleViewHolder>(DiffCallback) {

    inner class RuleViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: Rule) {
            val contact = if (rule.contactName == "*") "Everyone" else rule.contactName
            val keyword = if (rule.keyword == "*") "Any message" else "\"${rule.keyword}\""

            binding.tvContact.text = contact
            binding.tvKeyword.text = keyword
            binding.tvReply.text = rule.replyMessage
            binding.switchEnabled.isChecked = rule.isEnabled

            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(rule, isChecked)
            }
            binding.btnEdit.setOnClickListener { onEdit(rule) }
            binding.btnDelete.setOnClickListener { onDelete(rule) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Rule>() {
        override fun areItemsTheSame(oldItem: Rule, newItem: Rule) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Rule, newItem: Rule) = oldItem == newItem
    }
}
