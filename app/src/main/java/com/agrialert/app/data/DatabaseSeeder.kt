package com.agrialert.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseSeeder(private val context: Context) {

    private val repository = AgriAlertRepository.build(context)
    private val db = AgriAlertDatabase.get(context)

    suspend fun seedIfNeeded() {
        withContext(Dispatchers.IO) {
            // Check if already seeded by looking for Admin
            if (repository.countByRole(User.ROLE_ADMIN) > 0) return@withContext
            
            seedUsers()
            seedReports()
            seedAlerts()
            seedArticles()
            Log.d("AgriAlert_Seeder", "Database seeded successfully")
        }
    }

    private suspend fun seedUsers() {
        // Admin
        repository.insertUserIfNotExists(User(
            name = EnvConfig.adminName,
            email = EnvConfig.adminEmail,
            password = EnvConfig.adminPassword,
            role = User.ROLE_ADMIN,
            district = EnvConfig.adminDistrict,
            phone = EnvConfig.adminPhone,
            status = User.STATUS_ACTIVE,
            createdDate = repository.today()
        ))

        // Sample Vets
        listOf(
            User(
                name = "Dr. Mokhele Letsie",
                email = "vet@agrialert.ls",
                password = "vet123",
                role = User.ROLE_VET,
                district = "Leribe",
                phone = "+266 5845 6789",
                status = User.STATUS_ACTIVE,
                createdDate = repository.today()
            ),
            User(
                name = "Dr. Nthabiseng Pule",
                email = "vet2@agrialert.ls",
                password = "vet123",
                role = User.ROLE_VET,
                district = "Maseru",
                phone = "+266 5856 7890",
                status = User.STATUS_ACTIVE,
                createdDate = repository.today()
            )
        ).forEach { repository.insertUserIfNotExists(it) }

        // Sample Farmers
        listOf(
            User(
                name = "Thabo Mokoena",
                email = "farmer@agrialert.ls",
                password = "farmer123",
                role = User.ROLE_FARMER,
                district = "Leribe",
                phone = "+266 5812 3456",
                status = User.STATUS_ACTIVE,
                createdDate = repository.today()
            ),
            User(
                name = "Palesa Khotle",
                email = "farmer2@agrialert.ls",
                password = "farmer123",
                role = User.ROLE_FARMER,
                district = "Maseru",
                phone = "+266 5823 4567",
                status = User.STATUS_ACTIVE,
                createdDate = repository.today()
            ),
            User(
                name = "Moseli Tau",
                email = "farmer3@agrialert.ls",
                password = "farmer123",
                role = User.ROLE_FARMER,
                district = "Berea",
                phone = "+266 5834 5678",
                status = User.STATUS_ACTIVE,
                createdDate = repository.today()
            )
        ).forEach { repository.insertUserIfNotExists(it) }
    }

    private suspend fun seedReports() {
        // Find the seeded farmers to get their real IDs
        val farmer1 = repository.login("farmer@agrialert.ls", "farmer123")
        val farmer2 = repository.login("farmer2@agrialert.ls", "farmer123")
        val farmer3 = repository.login("farmer3@agrialert.ls", "farmer123")

        if (farmer1 == null) return

        listOf(
            DiseaseReport(
                farmerId = farmer1.id,
                farmerName = farmer1.name,
                animalType = "Cattle",
                symptoms = "High fever, nasal discharge, loss of appetite, laboured breathing",
                animalsAffected = 3,
                date = repository.daysAgo(3),
                status = DiseaseReport.PENDING,
                district = "Leribe",
                latitude = -29.3167,
                longitude = 27.4833,
                submittedAt = repository.daysAgo(3)
            ),
            DiseaseReport(
                farmerId = farmer1.id,
                farmerName = farmer1.name,
                animalType = "Goat",
                symptoms = "Skin lesions, swollen lymph nodes, reduced milk production, weight loss",
                animalsAffected = 7,
                date = repository.daysAgo(7),
                status = DiseaseReport.VISIT,
                district = "Leribe",
                latitude = -29.3200,
                longitude = 27.4900,
                submittedAt = repository.daysAgo(7)
            )
        ).forEach { repository.submitReport(it) }

        farmer2?.let {
            repository.submitReport(DiseaseReport(
                farmerId = it.id,
                farmerName = it.name,
                animalType = "Sheep",
                symptoms = "Severe coughing, respiratory distress, fever above 40 degrees, rapid weight loss",
                animalsAffected = 2,
                date = repository.daysAgo(14),
                status = DiseaseReport.RESOLVED,
                district = "Maseru",
                latitude = -29.3200,
                longitude = 27.4800,
                submittedAt = repository.daysAgo(14)
            ))
        }

        farmer3?.let {
            repository.submitReport(DiseaseReport(
                farmerId = it.id,
                farmerName = it.name,
                animalType = "Poultry",
                symptoms = "Sudden death in flock, greenish diarrhea, swollen head and neck, stopped eating",
                animalsAffected = 12,
                date = repository.daysAgo(2),
                status = DiseaseReport.INVESTIGATING,
                district = "Berea",
                latitude = -29.1000,
                longitude = 27.6000,
                submittedAt = repository.daysAgo(2)
            ))
        }
    }

    private suspend fun seedAlerts() {
        val vet = repository.login("vet@agrialert.ls", "vet123") ?: return
        
        repository.insertAlertIfNoneExist(
            Alert(
                vetId = vet.id,
                vetName = vet.name,
                disease = "Foot-and-Mouth Disease (FMD)",
                region = "Leribe",
                message = "FMD outbreak confirmed in Leribe. All farmers must isolate livestock immediately. Do not move animals between farms or markets. Report new cases through this app.",
                severity = "High",
                date = repository.daysAgo(2)
            )
        )
    }

    private suspend fun seedArticles() {
        if (db.articleDao().count() > 0) return

        val articles = listOf(
            Article(
                title = "Foot-and-Mouth Disease (FMD)",
                category = "DISEASE",
                content = "FMD is a severe, highly contagious viral disease of livestock. It affects cattle, swine, sheep, goats, and other cloven-hoofed ruminants. Signs include fever and blister-like sores on the tongue and lips, in the mouth, on the teats, and between the hooves.",
                lastUpdated = repository.today()
            ),
            Article(
                title = "Anthrax Prevention",
                category = "PREVENTION",
                content = "Anthrax is caused by spore-forming bacteria. Cattle and sheep often die suddenly. Vaccination is the most effective way to prevent outbreaks. Avoid opening carcasses of animals suspected of having anthrax.",
                lastUpdated = repository.today()
            ),
            Article(
                title = "Lumpy Skin Disease (LSD)",
                category = "DISEASE",
                content = "LSD is a viral disease of cattle. It is spread by biting insects. Signs include fever, nodules on the skin, and can lead to death. Vaccination is available and highly recommended before the rainy season.",
                lastUpdated = repository.today()
            ),
            Article(
                title = "Sheep Scab Management",
                category = "TREATMENT",
                content = "Sheep scab is caused by a mite. It causes intense itching and wool loss. Treatment involve dipping or injecting with approved acaricides. Ensure all new animals are quarantined before joining the flock.",
                lastUpdated = repository.today()
            )
        )

        db.articleDao().insertAll(articles)
    }
}
