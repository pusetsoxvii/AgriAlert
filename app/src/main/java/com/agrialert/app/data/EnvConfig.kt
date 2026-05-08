package com.agrialert.app.data

import android.util.Patterns
import com.agrialert.app.BuildConfig

object EnvConfig {

    val adminEmail: String
        get() = BuildConfig.ADMIN_EMAIL

    val adminPassword: String
        get() = BuildConfig.ADMIN_PASSWORD

    val adminName: String
        get() = BuildConfig.ADMIN_NAME

    val adminDistrict: String
        get() = BuildConfig.ADMIN_DISTRICT

    val adminPhone: String
        get() = BuildConfig.ADMIN_PHONE

    val isValid: Boolean
        get() = adminEmail.isNotBlank() &&
                adminPassword.length >= 6 &&
                adminName.isNotBlank() &&
                Patterns.EMAIL_ADDRESS
                    .matcher(adminEmail).matches()
}
