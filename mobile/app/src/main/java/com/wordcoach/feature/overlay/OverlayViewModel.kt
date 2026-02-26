package com.wordcoach.feature.overlay

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface OverlayUiState {
  data object Idle : OverlayUiState
  data object Loading : OverlayUiState
  data class Success(val word: String, val explanation: String, val fromCache: Boolean) : OverlayUiState
  data class Ambiguous(val candidates: List<String>) : OverlayUiState
  data class Error(val message: String) : OverlayUiState
}

class OverlayViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<OverlayUiState>(OverlayUiState.Idle)
  val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

  fun setLoading() {
    _uiState.value = OverlayUiState.Loading
  }

  fun setSuccess(word: String, explanation: String, fromCache: Boolean) {
    _uiState.value = OverlayUiState.Success(word, explanation, fromCache)
  }

  fun setAmbiguous(candidates: List<String>) {
    _uiState.value = OverlayUiState.Ambiguous(candidates)
  }

  fun setError(message: String) {
    _uiState.value = OverlayUiState.Error(message)
  }
}
