package com.wordcoach.feature.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PermissionStep {
  NEED_OVERLAY,
  NEED_PROJECTION,
  NEED_NOTIFICATION,
  READY
}

data class PermissionUiState(
  val step: PermissionStep = PermissionStep.NEED_OVERLAY
)

class PermissionViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(PermissionUiState())
  val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

  fun updateStatus(overlayGranted: Boolean, projectionGranted: Boolean, notificationGranted: Boolean) {
    val step = when {
      !overlayGranted -> PermissionStep.NEED_OVERLAY
      !projectionGranted -> PermissionStep.NEED_PROJECTION
      !notificationGranted -> PermissionStep.NEED_NOTIFICATION
      else -> PermissionStep.READY
    }
    _uiState.value = PermissionUiState(step)
  }
}
