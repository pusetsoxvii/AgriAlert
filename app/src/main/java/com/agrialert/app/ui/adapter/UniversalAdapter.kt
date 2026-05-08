package com.agrialert.app.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agrialert.app.data.*
import com.agrialert.app.databinding.*
import java.text.SimpleDateFormat
import java.util.Locale

// ==========================================
// LIST ITEM SEALED CLASS
// ==========================================

sealed class ListItem {
    data class ReportItem(val report: DiseaseReport) : ListItem()
    data class UserItem(val user: User, val extraCount: Int = 0) : ListItem()
    data class AlertItem(val alert: Alert) : ListItem()
    data class ResponseItem(val response: VetResponse) : ListItem()
    data class NotificationItem(val notification: AppNotification) : ListItem()
    data class ArticleItem(val article: Article) : ListItem()
}

// ==========================================
// ADAPTER
// ==========================================

class UniversalAdapter(
    private var items: MutableList<ListItem> = mutableListOf(),
    private val onItemClick: (ListItem) -> Unit,
    private val onActionClick: ((ListItem, String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_REPORT = 0
        private const val TYPE_USER = 1
        private const val TYPE_ALERT = 2
        private const val TYPE_RESPONSE = 3
        private const val TYPE_NOTIFICATION = 4
        private const val TYPE_ARTICLE = 5
    }

    inner class ReportViewHolder(private val binding: ItemReportCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: DiseaseReport) {
            binding.tvAnimalType.text = report.animalType.ifEmpty { "Unknown animal" }
            binding.tvSymptoms.text = report.symptoms.take(80).ifEmpty { "No symptoms recorded" }.let { if (report.symptoms.length > 80) "$it..." else it }
            binding.tvFarmerInfo.text = buildString {
                append(report.farmerName.ifEmpty { "Unknown farmer" })
                if (report.district.isNotEmpty()) append(" · ${report.district}")
            }
            binding.tvDate.text = report.date.toDisplayDate()
            binding.tvAnimalsCount.text = "${report.animalsAffected} animals"
            binding.tvStatus.text = report.status
            binding.tvStatus.setBackgroundColor(report.status.statusColor())
            binding.tvStatus.setTextColor(Color.WHITE)
            binding.accentBar.setBackgroundColor(report.status.statusColor())
            binding.root.setOnClickListener { onItemClick(ListItem.ReportItem(report)) }
        }
    }

    inner class UserViewHolder(private val binding: ItemUserCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, extraCount: Int) {
            binding.tvInitials.text = user.initials
            binding.tvInitials.setBackgroundColor(user.role.roleColor())
            binding.tvName.text = user.name.ifEmpty { "Unknown user" }
            binding.tvRole.text = when (user.role) {
                User.ROLE_VET -> "Vet Officer"
                User.ROLE_ADMIN -> "Administrator"
                else -> "Farmer"
            }
            binding.tvDistrict.text = user.district.ifEmpty { "Unknown district" }
            binding.tvEmail.text = user.email.ifEmpty { "No email" }
            binding.tvExtraCount.text = when (user.role) {
                User.ROLE_FARMER -> "$extraCount reports submitted"
                User.ROLE_VET -> "$extraCount responses sent"
                else -> ""
            }
            binding.tvExtraCount.visibility = if (user.role == User.ROLE_ADMIN) View.GONE else View.VISIBLE
            binding.tvStatus.text = user.status
            binding.tvStatus.setBackgroundColor(if (user.status == User.STATUS_ACTIVE) Color.parseColor("#10B981") else Color.parseColor("#EF4444"))
            binding.tvStatus.setTextColor(Color.WHITE)

            if (user.role != User.ROLE_ADMIN) {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = if (user.status == User.STATUS_ACTIVE) "Deactivate" else "Activate"
                binding.btnAction.setTextColor(if (user.status == User.STATUS_ACTIVE) Color.parseColor("#EF4444") else Color.parseColor("#10B981"))
                binding.btnAction.setOnClickListener { onActionClick?.invoke(ListItem.UserItem(user), if (user.status == User.STATUS_ACTIVE) "deactivate" else "activate") }
            } else {
                binding.btnAction.visibility = View.GONE
            }
            binding.root.setOnClickListener { onItemClick(ListItem.UserItem(user)) }
        }
    }

    inner class AlertViewHolder(private val binding: ItemAlertCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alert: Alert) {
            binding.tvDisease.text = alert.disease.ifEmpty { "Unknown disease" }
            binding.tvRegion.text = alert.region.ifEmpty { "Unknown region" }
            binding.tvDate.text = alert.date.toDisplayDate()
            binding.tvVetName.text = "Issued by ${alert.vetName.ifEmpty { "Unknown vet" }}"
            binding.tvMessage.text = alert.message.take(100).ifEmpty { "No message" }.let { if (alert.message.length > 100) "$it..." else it }
            binding.tvSeverity.text = alert.severity
            binding.tvSeverity.setBackgroundColor(alert.severity.severityColor())
            binding.tvSeverity.setTextColor(Color.WHITE)
            binding.accentBar.setBackgroundColor(alert.severity.severityColor())
            binding.root.alpha = if (alert.isRead) 0.65f else 1.0f
            binding.root.setOnClickListener { onItemClick(ListItem.AlertItem(alert)) }
        }
    }

    inner class ResponseViewHolder(private val binding: ItemResponseCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(response: VetResponse) {
            binding.tvVetName.text = response.vetName.ifEmpty { "Unknown vet" }
            binding.tvDate.text = response.date.toDisplayDate()
            binding.tvAction.text = response.action.ifEmpty { "No action" }
            binding.tvAction.setBackgroundColor(Color.parseColor("#2563EB"))
            binding.tvAction.setTextColor(Color.WHITE)
            binding.tvMessage.text = response.message.ifEmpty { "No message" }
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: AppNotification) {
            binding.tvTitle.text = notification.title.ifEmpty { "Notification" }
            binding.tvMessage.text = notification.message.ifEmpty { "No message" }
            binding.tvDate.text = notification.date.toDisplayDate()
            binding.tvType.text = when (notification.type) {
                AppNotification.TYPE_RESPONSE -> "Response"
                AppNotification.TYPE_ALERT -> "Alert"
                else -> "System"
            }
            binding.tvType.setBackgroundColor(when (notification.type) {
                AppNotification.TYPE_RESPONSE -> Color.parseColor("#2563EB")
                AppNotification.TYPE_ALERT -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#6B7280")
            })
            binding.tvType.setTextColor(Color.WHITE)
            if (notification.isRead) {
                binding.root.setBackgroundColor(Color.parseColor("#F3F4F6"))
                binding.unreadIndicator.visibility = View.GONE
            } else {
                binding.root.setBackgroundColor(Color.WHITE)
                binding.unreadIndicator.visibility = View.VISIBLE
            }
            binding.root.setOnClickListener { onItemClick(ListItem.NotificationItem(notification)) }
        }
    }

    inner class ArticleViewHolder(private val binding: ItemArticleCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) {
            binding.tvTitle.text = article.title
            binding.tvCategory.text = article.category.uppercase()
            binding.tvContentPreview.text = article.content
            binding.tvLastUpdated.text = "Updated: ${article.lastUpdated.toDisplayDate()}"
            binding.root.setOnClickListener { onItemClick(ListItem.ArticleItem(article)) }
        }
    }

    override fun getItemViewType(position: Int) = when (items.getOrNull(position)) {
        is ListItem.ReportItem -> TYPE_REPORT
        is ListItem.UserItem -> TYPE_USER
        is ListItem.AlertItem -> TYPE_ALERT
        is ListItem.ResponseItem -> TYPE_RESPONSE
        is ListItem.NotificationItem -> TYPE_NOTIFICATION
        is ListItem.ArticleItem -> TYPE_ARTICLE
        null -> TYPE_REPORT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_REPORT -> ReportViewHolder(ItemReportCardBinding.inflate(inflater, parent, false))
            TYPE_USER -> UserViewHolder(ItemUserCardBinding.inflate(inflater, parent, false))
            TYPE_ALERT -> AlertViewHolder(ItemAlertCardBinding.inflate(inflater, parent, false))
            TYPE_RESPONSE -> ResponseViewHolder(ItemResponseCardBinding.inflate(inflater, parent, false))
            TYPE_ARTICLE -> ArticleViewHolder(ItemArticleCardBinding.inflate(inflater, parent, false))
            else -> NotificationViewHolder(ItemNotificationCardBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < 0 || position >= items.size) return
        when (val item = items[position]) {
            is ListItem.ReportItem -> (holder as ReportViewHolder).bind(item.report)
            is ListItem.UserItem -> (holder as UserViewHolder).bind(item.user, item.extraCount)
            is ListItem.AlertItem -> (holder as AlertViewHolder).bind(item.alert)
            is ListItem.ResponseItem -> (holder as ResponseViewHolder).bind(item.response)
            is ListItem.NotificationItem -> (holder as NotificationViewHolder).bind(item.notification)
            is ListItem.ArticleItem -> (holder as ArticleViewHolder).bind(item.article)
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<ListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateReports(reports: List<DiseaseReport>) = update(reports.map { ListItem.ReportItem(it) })
    fun updateUsers(users: List<User>, counts: Map<Int, Int> = emptyMap()) = update(users.map { ListItem.UserItem(it, counts[it.id] ?: 0) })
    fun updateAlerts(alerts: List<Alert>) = update(alerts.map { ListItem.AlertItem(it) })
    fun updateResponses(responses: List<VetResponse>) = update(responses.map { ListItem.ResponseItem(it) })
    fun updateNotifications(notifications: List<AppNotification>) = update(notifications.map { ListItem.NotificationItem(it) })
    fun updateArticles(articles: List<Article>) = update(articles.map { ListItem.ArticleItem(it) })
}

fun String.toDisplayDate(): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        output.format(input.parse(this) ?: return this)
    } catch (e: Exception) { this }
}

fun String.statusColor(): Int = when (this) {
    DiseaseReport.PENDING -> Color.parseColor("#F59E0B")
    DiseaseReport.INVESTIGATING -> Color.parseColor("#3B82F6")
    DiseaseReport.ADVICE -> Color.parseColor("#8B5CF6")
    DiseaseReport.VISIT -> Color.parseColor("#6366F1")
    DiseaseReport.RESOLVED -> Color.parseColor("#10B981")
    else -> Color.parseColor("#6B7280")
}

fun String.severityColor(): Int = when (this) {
    "High" -> Color.parseColor("#EF4444")
    "Medium" -> Color.parseColor("#F59E0B")
    "Low" -> Color.parseColor("#10B981")
    else -> Color.parseColor("#6B7280")
}

fun String.roleColor(): Int = when (this) {
    User.ROLE_FARMER -> Color.parseColor("#2563EB")
    User.ROLE_VET -> Color.parseColor("#10B981")
    User.ROLE_ADMIN -> Color.parseColor("#8B5CF6")
    else -> Color.parseColor("#6B7280")
}
