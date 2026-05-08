package com.agrialert.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DatabaseSeeder
import com.agrialert.app.data.SessionManager
import com.agrialert.app.ui.farmer.FarmerDashboardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplash")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.agrialert.app.R.layout.activity_splash)

        val repository = AgriAlertRepository.build(this)
        val session = SessionManager(this)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                DatabaseSeeder(this@SplashActivity).seedIfNeeded()
            }
            delay(1500)
            
            val intent = when {
                !session.isLoggedIn -> Intent(this@SplashActivity, LoginActivity::class.java)
                session.isFarmer -> Intent(this@SplashActivity, FarmerDashboardActivity::class.java)
                else -> {
                    session.clear()
                    Intent(this@SplashActivity, LoginActivity::class.java)
                }
            }
            startActivity(intent)
            finish()
        }
    }
}
