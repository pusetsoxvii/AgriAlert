package com.agrialert.app.ui.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.DiseaseReport
import com.agrialert.app.data.SessionManager
import com.agrialert.app.data.SyncWorker
import com.agrialert.app.databinding.ActivityReportFormBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportFormBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var session: SessionManager

    private var editingReportId: Int = -1
    private var selectedDate = ""
    private var photoPath = ""
    private var capturedLat = 0.0
    private var capturedLng = 0.0
    private var capturedAddress = ""

    private val calendar = Calendar.getInstance()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            showPhotoPreview(photoPath)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val internalPath = copyUriToInternalStorage(it)
            if (internalPath != null) {
                photoPath = internalPath
                showPhotoPreview(photoPath)
            } else {
                Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val mapPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            capturedLat = data?.getDoubleExtra("lat", 0.0) ?: 0.0
            capturedLng = data?.getDoubleExtra("lng", 0.0) ?: 0.0
            capturedAddress = data?.getStringExtra("address") ?: ""
            
            if (capturedLat != 0.0) {
                binding.tvCoordinates.text = capturedAddress.ifEmpty { "%.4f, %.4f".format(capturedLat, capturedLng) }
                binding.tvLocationStatus.text = "Location pinned ✓"
                binding.tvLocationStatus.setTextColor(ContextCompat.getColor(this, com.agrialert.app.R.color.accentGreen))
                binding.btnCaptureLocation.text = "Re-pin Location"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)
        session = SessionManager(this)

        if (!session.isFarmer) {
            finish()
            return
        }

        setupForm()

        editingReportId = intent.getIntExtra("report_id", -1)
        if (editingReportId != -1) {
            loadReportForEditing(editingReportId)
        }

        binding.btnSubmit.setOnClickListener { attemptSubmit() }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupForm() {
        val types = listOf("Select animal type") + DiseaseReport.ANIMAL_TYPES
        binding.spinnerAnimalType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        selectedDate = repository.today()
        binding.tvDate.text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        binding.tvDate.setOnClickListener { showDatePicker() }

        binding.etSymptoms.addTextChangedListener {
            val count = it?.length ?: 0
            binding.tilSymptoms.helperText = "$count / 500"
        }

        binding.btnTakePhoto.setOnClickListener { checkCameraPermission() }
        binding.btnChooseGallery.setOnClickListener { checkGalleryPermission() }
        binding.btnRemovePhoto.setOnClickListener { removePhoto() }
        binding.btnCaptureLocation.setOnClickListener { openMapPicker() }
        
        binding.tvLocationStatus.text = "Tap button to pin location on map"
    }

    private fun loadReportForEditing(id: Int) {
        lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) { repository.getReportById(id) }
            report?.let { r ->
                binding.toolbar.title = "Edit Report"
                binding.btnSubmit.text = "Update Report"
                
                val typeIndex = (listOf("Select animal type") + DiseaseReport.ANIMAL_TYPES).indexOf(r.animalType)
                if (typeIndex != -1) binding.spinnerAnimalType.setSelection(typeIndex)
                
                binding.etAnimalsAffected.setText(r.animalsAffected.toString())
                binding.etSymptoms.setText(r.symptoms)
                
                selectedDate = r.date
                binding.tvDate.text = r.date
                
                capturedLat = r.latitude
                capturedLng = r.longitude
                binding.tvCoordinates.text = "%.4f, %.4f".format(r.latitude, r.longitude)
                binding.tvLocationStatus.text = "Location pinned ✓"
                
                if (r.photoPath.isNotEmpty()) {
                    photoPath = r.photoPath
                    showPhotoPreview(photoPath)
                }
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.tvDate.text = displayFormat.format(calendar.time)
            selectedDate = dbFormat.format(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val file = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        photoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        cameraLauncher.launch(intent)
    }

    private fun checkGalleryPermission() {
        galleryLauncher.launch("image/*")
    }

    private fun showPhotoPreview(path: String) {
        binding.cardPreview.visibility = View.VISIBLE
        Glide.with(this).load(File(path)).centerCrop().into(binding.ivPreview)
    }

    private fun removePhoto() {
        photoPath = ""
        binding.cardPreview.visibility = View.GONE
        binding.ivPreview.setImageDrawable(null)
    }

    private fun openMapPicker() {
        val intent = Intent(this, MapPickerActivity::class.java)
        mapPickerLauncher.launch(intent)
    }

    private fun attemptSubmit() {
        val animalType = binding.spinnerAnimalType.selectedItem.toString()
        val affectedStr = binding.etAnimalsAffected.text.toString()
        val symptoms = binding.etSymptoms.text.toString().trim()

        var isValid = true

        if (animalType == "Select animal type") {
            Toast.makeText(this, "Select animal type", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (affectedStr.isEmpty() || affectedStr.toIntOrNull() ?: 0 <= 0) {
            binding.tilAnimalsAffected.error = "Enter a valid number"
            isValid = false
        } else binding.tilAnimalsAffected.error = null

        if (symptoms.length < 20) {
            binding.tilSymptoms.error = "Minimum 20 characters required"
            isValid = false
        } else binding.tilSymptoms.error = null

        if (capturedLat == 0.0) {
            Toast.makeText(this, "Please pin the location on the map", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) return

        setSubmitting(true)

        lifecycleScope.launch {
            val report = DiseaseReport(
                id = if (editingReportId != -1) editingReportId else 0,
                farmerId = session.id,
                farmerName = session.name,
                animalType = animalType,
                symptoms = symptoms,
                animalsAffected = affectedStr.toInt(),
                date = selectedDate,
                district = session.district,
                latitude = capturedLat,
                longitude = capturedLng,
                photoPath = photoPath,
                submittedAt = if (editingReportId != -1) "" else repository.today(),
                isSynced = false // Mark as unsynced initially
            )

            val result = withContext(Dispatchers.IO) {
                repository.submitReport(report)
            }

            setSubmitting(false)

            result.onSuccess {
                scheduleSync()
                Toast.makeText(this@ReportFormActivity, if (editingReportId != -1) "Report updated successfully" else "Report submitted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@ReportFormActivity, "Failed to submit report", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "report_sync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }

    private fun setSubmitting(loading: Boolean) {
        binding.btnSubmit.isEnabled = !loading
        binding.pbSubmitting.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.text = if (loading) "" else if (editingReportId != -1) "Update Report" else "Submit Report"
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile("GALLERY_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            val outputStream = FileOutputStream(file)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(index)
            }
        }
        return null
    }
}
