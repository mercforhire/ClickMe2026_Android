package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private const val NAME_MARKER = "\u0000"

/**
 * "Request Sent!" heading and the body naming the expert. The same copy is used for free bookings and never
 * mentions payment (D-14). The name is a format argument (never a pattern) and rendered as a plain span.
 */
@Composable
fun BookingRequestSentHeadline(
    expertName: String,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Format with a marker so the name span can be styled even when the name itself contains text of the body.
    val template = stringResource(R.string.request_sent_body, NAME_MARKER)
    val body =
        buildAnnotatedString {
            val index = template.indexOf(NAME_MARKER)
            append(template.substring(0, index))
            withStyle(SpanStyle(color = onSurface, fontWeight = FontWeight.SemiBold)) { append(expertName) }
            append(template.substring(index + NAME_MARKER.length))
        }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.request_sent_headline),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
