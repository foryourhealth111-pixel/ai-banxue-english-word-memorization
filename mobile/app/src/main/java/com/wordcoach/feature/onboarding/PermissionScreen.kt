package com.wordcoach.feature.onboarding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wordcoach.R
import com.wordcoach.core.config.CoachApiSettings

@Composable
fun PermissionScreen(
  step: PermissionStep,
  onGrantOverlay: () -> Unit,
  onGrantProjection: () -> Unit,
  onGrantNotification: () -> Unit,
  onStartOverlayService: () -> Unit,
  settings: CoachApiSettings,
  onSettingsChanged: (CoachApiSettings) -> Unit,
  onSaveSettings: () -> Unit,
  onResetPromptToDefault: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFFFFF8EF), Color(0xFFF6EFE5))
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(text = stringResource(R.string.permission_header_title), style = MaterialTheme.typography.headlineSmall)
      Text(
        text = stringResource(R.string.permission_header_subtitle),
        style = MaterialTheme.typography.bodyMedium
      )

      ElevatedCard {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(stringResource(R.string.permission_section_title), style = MaterialTheme.typography.titleMedium)
          when (step) {
            PermissionStep.NEED_OVERLAY -> {
              Text(stringResource(R.string.permission_step_overlay_title))
              Button(onClick = onGrantOverlay) { Text(stringResource(R.string.permission_step_overlay_action)) }
            }
            PermissionStep.NEED_PROJECTION -> {
              Text(stringResource(R.string.permission_step_projection_title))
              Button(onClick = onGrantProjection) { Text(stringResource(R.string.permission_step_projection_action)) }
            }
            PermissionStep.NEED_NOTIFICATION -> {
              Text(stringResource(R.string.permission_step_notification_title))
              Button(onClick = onGrantNotification) { Text(stringResource(R.string.permission_step_notification_action)) }
            }
            PermissionStep.READY -> {
              Text(stringResource(R.string.permission_step_ready_title))
              Button(onClick = onStartOverlayService) { Text(stringResource(R.string.permission_step_ready_action)) }
            }
          }
        }
      }

      ElevatedCard {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(text = stringResource(R.string.settings_section_title), style = MaterialTheme.typography.titleMedium)
          Text(
            text = stringResource(R.string.settings_section_desc),
            style = MaterialTheme.typography.bodySmall
          )

          OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = settings.providerBaseUrl,
            onValueChange = { onSettingsChanged(settings.copy(providerBaseUrl = it)) },
            label = { Text(stringResource(R.string.settings_base_url_label)) },
            singleLine = true
          )

          OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = settings.providerModel,
            onValueChange = { onSettingsChanged(settings.copy(providerModel = it)) },
            label = { Text(stringResource(R.string.settings_model_label)) },
            singleLine = true
          )

          OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = settings.providerApiKey,
            onValueChange = { onSettingsChanged(settings.copy(providerApiKey = it)) },
            label = { Text(stringResource(R.string.settings_api_key_label)) },
            singleLine = true
          )

          OutlinedTextField(
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp),
            value = settings.systemPrompt,
            onValueChange = { onSettingsChanged(settings.copy(systemPrompt = it)) },
            label = { Text(stringResource(R.string.settings_prompt_label)) }
          )

          HorizontalDivider()
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onSaveSettings) { Text(stringResource(R.string.settings_save_action)) }
            Surface {
              TextButton(onClick = onResetPromptToDefault) { Text(stringResource(R.string.settings_reset_prompt_action)) }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
