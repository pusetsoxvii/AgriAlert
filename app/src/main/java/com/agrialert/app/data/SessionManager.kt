package com.agrialert.app.data

import android.content.Context
import com.agrialert.app.data.User
import java.util.Calendar

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "AgriAlertSession", Context.MODE_PRIVATE)

    fun save(user: User) {
        prefs.edit()
            .putInt("id", user.id)
            .putString("name", user.name)
            .putString("role", user.role)
            .putString("district", user.district)
            .putString("email", user.email)
            .putString("phone", user.phone)
            .apply()
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("lang", lang).apply()
    }

    val language: String get() = prefs.getString("lang", "en") ?: "en"

    fun clear() = prefs.edit().clear().apply()

    val id: Int get() = prefs.getInt("id", -1)
    val name: String
        get() = prefs.getString("name", "") ?: ""
    val role: String
        get() = prefs.getString("role", "") ?: ""
    val district: String
        get() = prefs.getString("district", "") ?: ""
    val email: String
        get() = prefs.getString("email", "") ?: ""
    val phone: String
        get() = prefs.getString("phone", "") ?: ""

    val isLoggedIn: Boolean get() = id != -1
    val isFarmer: Boolean get() = role == User.ROLE_FARMER
    val isVet: Boolean get() = role == User.ROLE_VET
    val isAdmin: Boolean get() = role == User.ROLE_ADMIN

    val initials: String get() {
        val parts = name.trim().split(" ")
        return if (parts.size >= 2)
            "${parts[0].first()}${parts[1].first()}"
                .uppercase()
        else name.take(2).uppercase().ifEmpty { "??" }
    }

    val firstName: String get() =
        name.trim().split(" ").firstOrNull() ?: name

    val greeting: String get() {
        val hour = Calendar.getInstance()
            .get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
