package com.agrialert.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class AgriAlertRepository(
    private val userDao: UserDao,
    private val reportDao: ReportDao,
    private val responseDao: ResponseDao,
    private val alertDao: AlertDao,
    private val notificationDao: NotificationDao
) {

    companion object {
        fun build(context: Context): AgriAlertRepository {
            val db = AgriAlertDatabase.get(context)
            return AgriAlertRepository(
                db.userDao(),
                db.reportDao(),
                db.responseDao(),
                db.alertDao(),
                db.notificationDao()
            )
        }
    }

    // ==========================================
    // USER
    // Supports: login, register, profile,
    // admin user management
    // ==========================================

    suspend fun login(
        email: String,
        password: String
    ): User? = userDao.login(
        email.trim().lowercase(), password.trim())

    suspend fun register(user: User): Result<Long> {
        return try {
            if (userDao.isEmailTaken(user.email) > 0)
                return Result.failure(
                    Exception("Email already registered"))
            val id = userDao.insert(user)
            if (id == -1L)
                Result.failure(Exception("Insert failed"))
            else
                Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(id: Int): User? =
        userDao.getById(id)

    fun getAllUsers(): Flow<List<User>> =
        userDao.getAll()

    fun getUsersByRole(role: String): Flow<List<User>> =
        userDao.getByRole(role)

    fun searchUsers(query: String): Flow<List<User>> =
        userDao.search("%$query%")

    suspend fun updateUserStatus(id: Int, status: String) =
        userDao.updateStatus(id, status)

    suspend fun updateUser(user: User) =
        userDao.update(user)

    suspend fun countByRole(role: String): Int =
        userDao.countByRole(role)

    suspend fun getFarmerCount(district: String): Int =
        if (district == "All districts")
            userDao.totalFarmers()
        else
            userDao.farmerCountInDistrict(district)

    suspend fun insertUserIfNotExists(user: User) {
        if (userDao.isEmailTaken(user.email) == 0)
            userDao.insert(user)
    }

    // ==========================================
    // REPORTS
    // Supports: farmer submit, view, filter,
    // search, vet view/respond, admin manage
    // ==========================================

    suspend fun submitReport(
        report: DiseaseReport
    ): Result<Long> {
        return try {
            val id = reportDao.insert(report)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFarmerTotal(id: Int): Flow<Int> =
        reportDao.farmerTotal(id)

    fun getFarmerPending(id: Int): Flow<Int> =
        reportDao.farmerPending(id)

    fun getFarmerRecent(id: Int): Flow<List<DiseaseReport>> =
        reportDao.farmerRecent(id)

    fun getFarmerAll(id: Int): Flow<List<DiseaseReport>> =
        reportDao.farmerAll(id)

    fun getFarmerByStatus(
        id: Int, status: String
    ): Flow<List<DiseaseReport>> =
        reportDao.farmerByStatus(id, status)

    fun searchFarmerReports(
        id: Int, query: String
    ): Flow<List<DiseaseReport>> =
        reportDao.farmerSearch(id, "%$query%")

    suspend fun getReportById(id: Int): DiseaseReport? =
        reportDao.getById(id)

    suspend fun getReportWithResponses(
        id: Int
    ): ReportWithResponses? =
        reportDao.getWithResponses(id)

    fun getTotalInDistrict(district: String): Flow<Int> =
        reportDao.totalInDistrict(district)

    fun getPendingCountInDistrict(
        district: String
    ): Flow<Int> =
        reportDao.pendingCountInDistrict(district)

    fun getReportsByDistrict(
        district: String
    ): Flow<List<DiseaseReport>> =
        reportDao.byDistrict(district)

    fun getPendingByDistrict(
        district: String
    ): Flow<List<DiseaseReport>> =
        reportDao.pendingInDistrict(district)

    fun getLatestPendingByDistrict(
        district: String
    ): Flow<List<DiseaseReport>> =
        reportDao.latestPendingInDistrict(district)

    fun getAllReports(): Flow<List<DiseaseReport>> =
        reportDao.getAll()

    fun getReportsByStatus(
        status: String
    ): Flow<List<DiseaseReport>> =
        reportDao.allByStatus(status)

    fun getReportsByFarmer(
        farmerId: Int
    ): Flow<List<DiseaseReport>> =
        reportDao.byFarmer(farmerId)

    fun searchAllReports(
        query: String
    ): Flow<List<DiseaseReport>> =
        reportDao.searchAll("%$query%")

    suspend fun updateReportStatus(id: Int, status: String) =
        reportDao.updateStatus(id, status)

    suspend fun deleteReport(id: Int) =
        reportDao.deleteById(id)

    suspend fun getResolvedThisWeek(
        district: String
    ): Int = reportDao.resolvedFrom(district, weekStart())

    suspend fun countForFarmer(id: Int): Int =
        reportDao.countForFarmer(id)

    // ==========================================
    // RESPONSES
    // Supports: vet submit response,
    // farmer/vet/admin view responses
    // ==========================================

    suspend fun submitResponse(
        response: VetResponse,
        reportId: Int
    ): Result<Unit> {
        return try {
            responseDao.insert(response)
            val newStatus = VetResponse.mapActionToStatus(
                response.action)
            reportDao.updateStatus(reportId, newStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getResponsesForReport(
        reportId: Int
    ): Flow<List<VetResponse>> =
        responseDao.forReport(reportId)

    suspend fun getVetResponseCount(vetId: Int): Int =
        responseDao.countForVet(vetId)

    suspend fun getVetVisitCount(vetId: Int): Int =
        responseDao.visitsForVet(vetId)

    // ==========================================
    // ALERTS
    // Supports: vet send alert, farmer view alerts,
    // admin view all alerts
    // ==========================================

    suspend fun sendAlert(alert: Alert): Result<Long> {
        return try {
            val id = alertDao.insert(alert)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLatestAlert(): Flow<Alert?> =
        alertDao.latest()

    fun getAlertsForDistrict(
        district: String
    ): Flow<List<Alert>> =
        alertDao.forDistrict(district)

    fun getUnreadAlertCount(district: String): Flow<Int> =
        alertDao.unreadCount(district)

    suspend fun markAlertRead(id: Int) =
        alertDao.markRead(id)

    fun getVetAlerts(vetId: Int): Flow<List<Alert>> =
        alertDao.forVet(vetId)

    suspend fun getVetAlertCount(vetId: Int): Int =
        alertDao.vetCountFrom(vetId, monthStart())

    fun getAllAlerts(): Flow<List<Alert>> =
        alertDao.getAll()

    fun getAlertsBySeverity(
        severity: String
    ): Flow<List<Alert>> =
        alertDao.bySeverity(severity)

    suspend fun getAlertById(id: Int): Alert? =
        alertDao.getById(id)

    suspend fun insertAlertIfNoneExist(alert: Alert) {
        if (alertDao.total() == 0)
            alertDao.insert(alert)
    }

    // ==========================================
    // NOTIFICATIONS
    // Supports: farmer/vet receive notifications,
    // admin send to all users
    // ==========================================

    suspend fun insertNotification(n: AppNotification) =
        notificationDao.insert(n)

    suspend fun insertNotificationsForRole(
        role: String,
        title: String,
        message: String,
        type: String
    ) {
        val users = notificationDao.getUsersByRole(role)
        val today = today()
        val notifications = users.map { user ->
            AppNotification(
                userId = user.id,
                title = title,
                message = message,
                type = type,
                referenceId = 0,
                isRead = false,
                date = today
            )
        }
        notificationDao.insertAll(notifications)
    }

    suspend fun insertNotificationsForAll(
        title: String,
        message: String
    ) {
        listOf(
            User.ROLE_FARMER,
            User.ROLE_VET,
            User.ROLE_ADMIN
        ).forEach { role ->
            insertNotificationsForRole(
                role, title, message,
                AppNotification.TYPE_SYSTEM)
        }
    }

    fun getNotificationsForUser(
        userId: Int
    ): Flow<List<AppNotification>> =
        notificationDao.forUser(userId)

    fun getUnreadNotificationCount(
        userId: Int
    ): Flow<Int> =
        notificationDao.unreadCount(userId)

    suspend fun markNotificationRead(id: Int) =
        notificationDao.markRead(id)

    suspend fun markAllNotificationsRead(userId: Int) =
        notificationDao.markAllRead(userId)

    // ==========================================
    // ADMIN STATS
    // Supports: AdminDashboardActivity stats,
    // AdminStatsActivity charts
    // ==========================================

    suspend fun getAdminStats(): AdminStats {
        val week = weekStart()
        val month = monthStart()
        return AdminStats(
            totalFarmers = userDao.countByRole(User.ROLE_FARMER),
            totalVets = userDao.countByRole(User.ROLE_VET),
            totalAdmins = userDao.countByRole(User.ROLE_ADMIN),
            totalReports = reportDao.total(),
            pendingReports = reportDao.countByStatus(
                DiseaseReport.PENDING),
            investigatingReports = reportDao.countByStatus(
                DiseaseReport.INVESTIGATING),
            resolvedReports = reportDao.countByStatus(
                DiseaseReport.RESOLVED),
            totalAlerts = alertDao.total(),
            reportsThisWeek = reportDao.totalFrom(week),
            newUsersThisMonth = userDao.newUsersFrom(month),
            visitsScheduled = reportDao.countByStatus(
                DiseaseReport.VISIT)
        )
    }

    suspend fun getChartData(): ChartData {
        return ChartData(
            byAnimalType = reportDao.countByAnimalType(),
            byDistrict = reportDao.countByDistrict(),
            daily = reportDao.dailyCountFrom(weekStart())
        )
    }

    // ==========================================
    // DATE HELPERS
    // ==========================================

    fun today(): String = format(Date())

    fun weekStart(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return format(cal.time)
    }

    fun monthStart(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return format(cal.time)
    }

    fun daysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return format(cal.time)
    }

    private fun format(date: Date): String =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(date)
}
