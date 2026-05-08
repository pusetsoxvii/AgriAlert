package com.agrialert.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.User
import com.agrialert.app.databinding.ActivityUserManagementBinding
import com.agrialert.app.ui.RegisterActivity
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager
    private lateinit var adapter: UniversalAdapter

    private var allUsers: List<User> = emptyList()
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isAdmin) {
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeUsers()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(
            onItemClick = { item ->
                if (item is ListItem.UserItem) {
                    val intent = Intent(this, UserDetailActivity::class.java)
                    intent.putExtra("user_id", item.user.id)
                    startActivity(intent)
                }
            },
            onActionClick = { item, action ->
                if (item is ListItem.UserItem) {
                    handleUserAction(item.user, action)
                }
            }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { filterAndDisplay() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query ?: ""
                filterAndDisplay()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                filterAndDisplay()
                return true
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            observeUsers()
        }

        binding.fabAddUser.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Add User")
                .setMessage("Add a new vet officer account?")
                .setPositiveButton("Add") { _, _ ->
                    startActivity(Intent(this, RegisterActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeUsers() {
        lifecycleScope.launch {
            repository.getAllUsers().collectLatest { users ->
                allUsers = users
                updateTabBadges()
                filterAndDisplay()
            }
        }
    }

    private fun updateTabBadges() {
        val farmerCount = allUsers.count { it.role == User.ROLE_FARMER }
        val vetCount = allUsers.count { it.role == User.ROLE_VET }

        binding.tabLayout.getTabAt(0)?.text = "All (${allUsers.size})"
        binding.tabLayout.getTabAt(1)?.text = "Farmers ($farmerCount)"
        binding.tabLayout.getTabAt(2)?.text = "Vet Officers ($vetCount)"
    }

    private fun filterAndDisplay() {
        val tabIndex = binding.tabLayout.selectedTabPosition
        var filtered = when (tabIndex) {
            1 -> allUsers.filter { it.role == User.ROLE_FARMER }
            2 -> allUsers.filter { it.role == User.ROLE_VET }
            else -> allUsers
        }

        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(currentQuery, ignoreCase = true) ||
                it.email.contains(currentQuery, ignoreCase = true) ||
                it.district.contains(currentQuery, ignoreCase = true)
            }
        }

        lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                filtered.associate { user ->
                    val count = if (user.role == User.ROLE_FARMER) {
                        repository.countForFarmer(user.id)
                    } else {
                        repository.getVetResponseCount(user.id)
                    }
                    user.id to count
                }
            }
            adapter.updateUsers(filtered, counts)
            binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun handleUserAction(user: User, action: String) {
        val isActivating = action == "activate"
        AlertDialog.Builder(this)
            .setTitle(if (isActivating) "Activate User" else "Deactivate User")
            .setMessage(if (isActivating) "Activate ${user.name}? They will be able to log in."
                        else "Deactivate ${user.name}? They will not be able to log in.")
            .setPositiveButton(if (isActivating) "Activate" else "Deactivate") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.updateUserStatus(user.id, if (isActivating) "Active" else "Inactive")
                    }
                    Toast.makeText(this@UserManagementActivity, "${user.name} has been ${if (isActivating) "activated" else "deactivated"}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
