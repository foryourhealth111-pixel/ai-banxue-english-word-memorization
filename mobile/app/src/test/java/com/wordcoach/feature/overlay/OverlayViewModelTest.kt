package com.wordcoach.feature.overlay

import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayViewModelTest {
  @Test
  fun `setLoading updates ui state`() {
    val vm = OverlayViewModel()
    vm.setLoading()
    assertTrue(vm.uiState.value is OverlayUiState.Loading)
  }

  @Test
  fun `setSuccess updates ui state`() {
    val vm = OverlayViewModel()
    vm.setSuccess("abandon", "讲解", false)
    assertTrue(vm.uiState.value is OverlayUiState.Success)
  }
}
