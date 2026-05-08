package com.agrialert.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ENTITIES
// ==========================================

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "",
    val district: String = "",
    val phone: String = "",
    val status: String = "Active",
    val createdDate: String = ""
) {
    val initials: String get() {
        val parts = name.trim().split(" ")
        return if (parts.size >= 2)
            "${parts[0].first()}${parts[1].first()}"
                .uppercase()
        else name.take(2).uppercase().ifEmpty { "??" }
    }

    val firstName: String get() =
        name.trim().split(" ").firstOrNull() ?: name

    companion object {
        const val ROLE_FARMER = "Farmer"
        const val ROLE_VET = "VetOfficer"
        const val ROLE_ADMIN = "Admin"
        const val STATUS_ACTIVE = "Active"
        const val STATUS_INACTIVE = "Inactive"
    }
}

@Entity(
    tableName = "reports",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["farmerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("farmerId")]
)
data class DiseaseReport(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val farmerId: Int = 0,
    val farmerName: String = "",
    val animalType: String = "",
    val symptoms: String = "",
    val animalsAffected: Int = 0,
    val date: String = "",
    val status: String = "Pending",
    val district: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoPath: String = "",
    val submittedAt: String = "",
    val isSynced: Boolean = true
) {
    companion object {
        const val PENDING = "Pending"
        const val INVESTIGATING = "Under Investigation"
        const val ADVICE = "Advice Provided"
        const val VISIT = "Visit Scheduled"
        const val RESOLVED = "Resolved"

        val ALL_STATUSES = listOf(
            PENDING, INVESTIGATING,
            ADVICE, VISIT, RESOLVED
        )

        val ANIMAL_TYPES = listOf(
            "Cattle", "Sheep", "Goat",
            "Poultry", "Horse", "Pig", "Other"
        )
    }
}

@Entity(
    tableName = "responses",
    foreignKeys = [
        ForeignKey(
            entity = DiseaseReport::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["vetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reportId"), Index("vetId")]
)
data class VetResponse(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val reportId: Int = 0,
    val vetId: Int = 0,
    val vetName: String = "",
    val message: String = "",
    val action: String = "",
    val date: String = ""
) {
    companion object {
        const val ACTION_ADVICE = "Advice provided"
        const val ACTION_INFO = "Request more information"
        const val ACTION_ISOLATE = "Isolate affected animals"
        const val ACTION_VISIT = "Schedule farm visit"
        const val ACTION_RESOLVED = "Case resolved"

        val ALL_ACTIONS = listOf(
            ACTION_ADVICE, ACTION_INFO,
            ACTION_ISOLATE, ACTION_VISIT, ACTION_RESOLVED
        )

        fun mapActionToStatus(action: String) = when (action) {
            ACTION_ADVICE -> DiseaseReport.ADVICE
            ACTION_INFO -> DiseaseReport.INVESTIGATING
            ACTION_ISOLATE -> DiseaseReport.INVESTIGATING
            ACTION_VISIT -> DiseaseReport.VISIT
            ACTION_RESOLVED -> DiseaseReport.RESOLVED
            else -> DiseaseReport.INVESTIGATING
        }
    }
}

@Entity(
    tableName = "alerts",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["vetId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vetId")]
)
data class Alert(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vetId: Int = 0,
    val vetName: String = "",
    val disease: String = "",
    val region: String = "",
    val message: String = "",
    val severity: String = "",
    val date: String = "",
    val isRead: Boolean = false
) {
    companion object {
        val DISEASES = listOf(
            "Foot-and-Mouth Disease (FMD)",
            "Lumpy Skin Disease",
            "Newcastle Disease",
            "Avian Influenza",
            "East Coast Fever",
            "Anthrax",
            "Brucellosis",
            "Rabies",
            "Other"
        )

        val SEVERITIES = listOf("Low", "Medium", "High")

        val DISTRICTS = listOf(
            "All districts",
            "Berea", "Butha-Buthe", "Leribe",
            "Mafeteng", "Maseru", "Mohale's Hoek",
            "Mokhotlong", "Qacha's Nek",
            "Quthing", "Thaba-Tseka"
        )
    }
}

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val referenceId: Int = 0,
    val isRead: Boolean = false,
    val date: String = ""
) {
    companion object {
        const val TYPE_RESPONSE = "response"
        const val TYPE_ALERT = "alert"
        const val TYPE_SYSTEM = "system"
    }
}

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val category: String = "",
    val content: String = "",
    val lastUpdated: String = ""
)

// ==========================================
// HELPER DATA CLASSES
// ==========================================

data class ReportWithResponses(
    @Embedded val report: DiseaseReport,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val responses: List<VetResponse>
)

data class AnimalTypeCount(
    val animalType: String,
    val count: Int
)

data class DistrictCount(
    val district: String,
    val count: Int
)

data class DailyCount(
    val date: String,
    val count: Int
)

data class AdminStats(
    val totalFarmers: Int = 0,
    val totalVets: Int = 0,
    val totalAdmins: Int = 0,
    val totalReports: Int = 0,
    val pendingReports: Int = 0,
    val investigatingReports: Int = 0,
    val resolvedReports: Int = 0,
    val totalAlerts: Int = 0,
    val reportsThisWeek: Int = 0,
    val newUsersThisMonth: Int = 0,
    val visitsScheduled: Int = 0
)

data class ChartData(
    val byAnimalType: List<AnimalTypeCount> = emptyList(),
    val byDistrict: List<DistrictCount> = emptyList(),
    val daily: List<DailyCount> = emptyList()
)

// ==========================================
// DAOs
// ==========================================

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(users: List<User>)
    @Update
    suspend fun update(user: User)
    @Query("SELECT * FROM users WHERE email = :email AND password = :password AND status = 'Active' LIMIT 1")
    suspend fun login(email: String, password: String): User?
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun isEmailTaken(email: String): Int
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): User?
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAll(): Flow<List<User>>
    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getByRole(role: String): Flow<List<User>>
    @Query("SELECT * FROM users WHERE name LIKE :q OR email LIKE :q OR district LIKE :q ORDER BY name ASC")
    fun search(q: String): Flow<List<User>>
    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    suspend fun countByRole(role: String): Int
    @Query("UPDATE users SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)
    @Query("SELECT COUNT(*) FROM users WHERE role = 'Farmer' AND district = :district")
    suspend fun farmerCountInDistrict(district: String): Int
    @Query("SELECT COUNT(*) FROM users WHERE role = 'Farmer'")
    suspend fun totalFarmers(): Int
    @Query("SELECT COUNT(*) FROM users WHERE createdDate >= :from")
    suspend fun newUsersFrom(from: String): Int
}

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: DiseaseReport): Long
    @Query("SELECT COUNT(*) FROM reports WHERE farmerId = :id")
    fun farmerTotal(id: Int): Flow<Int>
    @Query("SELECT COUNT(*) FROM reports WHERE farmerId = :id AND status = 'Pending'")
    fun farmerPending(id: Int): Flow<Int>
    @Query("SELECT * FROM reports WHERE farmerId = :id ORDER BY date DESC LIMIT 3")
    fun farmerRecent(id: Int): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE farmerId = :id ORDER BY date DESC")
    fun farmerAll(id: Int): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE farmerId = :id AND status = :status ORDER BY date DESC")
    fun farmerByStatus(id: Int, status: String): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE farmerId = :id AND (animalType LIKE :q OR symptoms LIKE :q) ORDER BY date DESC")
    fun farmerSearch(id: Int, q: String): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): DiseaseReport?
    @Transaction
    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getWithResponses(id: Int): ReportWithResponses?
    @Query("SELECT COUNT(*) FROM reports WHERE district = :district")
    fun totalInDistrict(district: String): Flow<Int>
    @Query("SELECT COUNT(*) FROM reports WHERE district = :district AND status IN ('Pending','Under Investigation')")
    fun pendingCountInDistrict(district: String): Flow<Int>
    @Query("SELECT * FROM reports WHERE district = :district ORDER BY date DESC")
    fun byDistrict(district: String): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE district = :district AND status IN ('Pending','Under Investigation') ORDER BY date DESC")
    fun pendingInDistrict(district: String): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE district = :district AND status IN ('Pending','Under Investigation') ORDER BY date DESC LIMIT 3")
    fun latestPendingInDistrict(district: String): Flow<List<DiseaseReport>>
    @Query("UPDATE reports SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)
    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: Int)
    @Query("SELECT * FROM reports ORDER BY date DESC")
    fun getAll(): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE status = :status ORDER BY date DESC")
    fun allByStatus(status: String): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE farmerId = :farmerId ORDER BY date DESC")
    fun byFarmer(farmerId: Int): Flow<List<DiseaseReport>>
    @Query("SELECT * FROM reports WHERE farmerName LIKE :q OR animalType LIKE :q OR district LIKE :q OR symptoms LIKE :q ORDER BY date DESC")
    fun searchAll(q: String): Flow<List<DiseaseReport>>
    @Query("SELECT COUNT(*) FROM reports WHERE status = :status")
    suspend fun countByStatus(status: String): Int
    @Query("SELECT COUNT(*) FROM reports")
    suspend fun total(): Int
    @Query("SELECT COUNT(*) FROM reports WHERE date >= :from")
    suspend fun totalFrom(from: String): Int
    @Query("SELECT animalType, COUNT(*) as count FROM reports GROUP BY animalType")
    suspend fun countByAnimalType(): List<AnimalTypeCount>
    @Query("SELECT district, COUNT(*) as count FROM reports GROUP BY district ORDER BY count DESC")
    suspend fun countByDistrict(): List<DistrictCount>
    @Query("SELECT date, COUNT(*) as count FROM reports WHERE date >= :from GROUP BY date ORDER BY date ASC")
    suspend fun dailyCountFrom(from: String): List<DailyCount>
    @Query("SELECT COUNT(*) FROM reports WHERE farmerId = :id")
    suspend fun countForFarmer(id: Int): Int
    @Query("SELECT COUNT(*) FROM reports WHERE district = :district AND status = 'Resolved' AND date >= :from")
    suspend fun resolvedFrom(district: String, from: String): Int
    @Query("SELECT COUNT(*) FROM reports WHERE date BETWEEN :from AND :to")
    suspend fun countBetween(from: String, to: String): Int
    @Query("SELECT * FROM reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<DiseaseReport>
    @Query("UPDATE reports SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)
}

@Dao
interface ResponseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(response: VetResponse): Long
    @Query("SELECT * FROM responses WHERE reportId = :id ORDER BY date ASC")
    fun forReport(id: Int): Flow<List<VetResponse>>
    @Query("SELECT COUNT(*) FROM responses WHERE vetId = :id")
    suspend fun countForVet(id: Int): Int
    @Query("SELECT COUNT(*) FROM responses WHERE vetId = :id AND action = 'Schedule farm visit'")
    suspend fun visitsForVet(id: Int): Int
    @Query("SELECT COUNT(*) FROM responses WHERE reportId = :id")
    suspend fun countForReport(id: Int): Int
}

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: Alert): Long
    @Query("SELECT * FROM alerts ORDER BY date DESC LIMIT 1")
    fun latest(): Flow<Alert?>
    @Query("SELECT * FROM alerts WHERE region = :district OR region = 'All districts' ORDER BY date DESC")
    fun forDistrict(district: String): Flow<List<Alert>>
    @Query("SELECT COUNT(*) FROM alerts WHERE (region = :district OR region = 'All districts') AND isRead = 0")
    fun unreadCount(district: String): Flow<Int>
    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)
    @Query("SELECT * FROM alerts WHERE vetId = :id ORDER BY date DESC")
    fun forVet(id: Int): Flow<List<Alert>>
    @Query("SELECT COUNT(*) FROM alerts WHERE vetId = :id AND date >= :from")
    suspend fun vetCountFrom(id: Int, from: String): Int
    @Query("SELECT * FROM alerts ORDER BY date DESC")
    fun getAll(): Flow<List<Alert>>
    @Query("SELECT * FROM alerts WHERE severity = :severity ORDER BY date DESC")
    fun bySeverity(severity: String): Flow<List<Alert>>
    @Query("SELECT COUNT(*) FROM alerts")
    suspend fun total(): Int
    @Query("SELECT * FROM alerts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Alert?
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(n: AppNotification)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC")
    fun forUser(userId: Int): Flow<List<AppNotification>>
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun unreadCount(userId: Int): Flow<Int>
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: Int)
    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: String): List<User>
}

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: Article)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<Article>)
    @Query("SELECT * FROM articles ORDER BY title ASC")
    fun getAll(): Flow<List<Article>>
    @Query("SELECT * FROM articles WHERE title LIKE :q OR content LIKE :q OR category LIKE :q ORDER BY title ASC")
    fun search(q: String): Flow<List<Article>>
    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Article?
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int
}

// ==========================================
// DATABASE
// ==========================================

@Database(
    entities = [
        User::class,
        DiseaseReport::class,
        VetResponse::class,
        Alert::class,
        AppNotification::class,
        Article::class
    ],
    version = 3, // Incremented version for Article entity
    exportSchema = false
)
abstract class AgriAlertDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun reportDao(): ReportDao
    abstract fun responseDao(): ResponseDao
    abstract fun alertDao(): AlertDao
    abstract fun notificationDao(): NotificationDao
    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AgriAlertDatabase? = null

        fun get(context: Context): AgriAlertDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AgriAlertDatabase::class.java,
                    "agrialert.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
