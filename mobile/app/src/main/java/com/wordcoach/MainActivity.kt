package com.wordcoach

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.wordcoach.core.config.CoachPromptDefaults
import com.wordcoach.core.config.CoachSettingsStore
import com.wordcoach.feature.capture.ProjectionSessionStore
import com.wordcoach.feature.onboarding.PermissionScreen
import com.wordcoach.feature.onboarding.PermissionViewModel
import com.wordcoach.feature.overlay.FloatingCoachService
import com.wordcoach.ui.theme.WordCoachTheme

class MainActivity : ComponentActivity() {
  private val permissionViewModel: PermissionViewModel by viewModels()
  private lateinit var coachSettingsStore: CoachSettingsStore

  private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    ProjectionSessionStore.save(result.resultCode, result.data)
    refreshPermissionState()
  }

  private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
    refreshPermissionState()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    coachSettingsStore = CoachSettingsStore(this)
    refreshPermissionState()
    setContent {
      val uiState by permissionViewModel.uiState.collectAsState()
      var settings by remember { mutableStateOf(coachSettingsStore.load()) }
      WordCoachTheme {
        PermissionScreen(
          step = uiState.step,
          onGrantOverlay = { requestOverlayPermission() },
          onGrantProjection = { requestProjectionPermission() },
          onGrantNotification = { requestNotificationPermission() },
          onStartOverlayService = { startOverlayService() },
          settings = settings,
          onSettingsChanged = { settings = it },
          onSaveSettings = { coachSettingsStore.save(settings) },
          onResetPromptToDefault = {
            val updated = settings.copy(systemPrompt = CoachPromptDefaults.defaultSystemPrompt())
            settings = updated
            coachSettingsStore.save(updated)
          }
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    refreshPermissionState()
  }

  private fun refreshPermissionState() {
    permissionViewModel.updateStatus(
      overlayGranted = Settings.canDrawOverlays(this),
      projectionGranted = ProjectionSessionStore.hasSession(),
      notificationGranted = hasNotificationPermission()
    )
  }

  private fun requestOverlayPermission() {
    val intent = Intent(
      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
      Uri.parse("package:$packageName")
    )
    startActivity(intent)
  }

  private fun requestProjectionPermission() {
    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    projectionLauncher.launch(manager.createScreenCaptureIntent())
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      refreshPermissionState()
    }
  }

  private fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      true
    } else {
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    }
  }

  private fun startOverlayService() {
    val intent = Intent(this, FloatingCoachService::class.java)
    ContextCompat.startForegroundService(this, intent)
  }
}
