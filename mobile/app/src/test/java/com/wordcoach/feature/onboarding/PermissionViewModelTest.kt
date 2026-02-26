package com.wordcoach.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionViewModelTest {
  @Test
  fun `state moves to ready when all permissions granted`() {
    val vm = PermissionViewModel()
    vm.updateStatus(overlayGranted = true, projectionGranted = true, notificationGranted = true)
    assertEquals(PermissionStep.READY, vm.uiState.value.step)
  }

  @Test
  fun `state prioritizes overlay permission first`() {
    val vm = PermissionViewModel()
    vm.updateStatus(overlayGranted = false, projectionGranted = true, notificationGranted = true)
    assertEquals(PermissionStep.NEED_OVERLAY, vm.uiState.value.step)
  }
}
