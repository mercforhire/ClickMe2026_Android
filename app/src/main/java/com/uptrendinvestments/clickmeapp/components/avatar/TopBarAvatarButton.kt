package com.uptrendinvestments.clickmeapp.components.avatar

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/**
 * Profile entry point on the right edge of the Explore and Dashboard top bars: a 48 dp touch
 * target around a 32 dp [Avatar].
 */
@Composable
fun TopBarAvatarButton(
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.a11y_your_profile)
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp).semantics { contentDescription = label },
    ) {
        Avatar(url = avatarUrl, contentDescription = null, size = 32.dp)
    }
}

@Preview
@Composable
private fun TopBarAvatarButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopBarAvatarButton(avatarUrl = null, onClick = {})
        }
    }
}
