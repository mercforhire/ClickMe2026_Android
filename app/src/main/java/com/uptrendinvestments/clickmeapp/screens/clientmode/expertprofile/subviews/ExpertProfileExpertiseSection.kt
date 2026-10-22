package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ExpertTagChip
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Expertise tags from the preview key. The caller hides the section when [tags] is empty (D-12). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertProfileExpertiseSection(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.expert_profile_expertise),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            tags.forEach { ExpertTagChip(label = it) }
        }
    }
}

@Preview(name = "ExpertProfileExpertiseSection")
@Composable
private fun ExpertProfileExpertiseSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileExpertiseSection(tags = listOf("AI", "Math", "Machine Learning"))
        }
    }
}
