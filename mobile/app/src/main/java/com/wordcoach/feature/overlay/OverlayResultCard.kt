package com.wordcoach.feature.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wordcoach.R

@Composable
fun OverlayResultCard(
  word: String,
  explanation: String,
  onCopy: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(text = stringResource(R.string.overlay_word_prefix, word))
      Text(text = explanation)
      Button(onClick = onCopy) {
        Text(stringResource(R.string.overlay_action_copy))
      }
    }
  }
}
