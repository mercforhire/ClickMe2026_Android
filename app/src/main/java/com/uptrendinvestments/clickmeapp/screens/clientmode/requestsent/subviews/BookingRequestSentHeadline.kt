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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Stands in for the expert name while formatting, so the name span can be found and styled. */
internal const val NAME_MARKER = "\u0000"

/**
 * "Request Sent!" heading and the body naming the expert. The same copy is used for free and paid bookings and
 * never mentions payment (D-14, iOS parity, UI-SPEC A2): the paid difference is the lifecycle card below the details
 * card, so CONTEXT's "paid headline" is this unchanged headline plus that card. The name is a format argument
 * (never a pattern) and rendered as a plain span.
 */
@Composable
fun BookingRequestSentHeadline(
    expertName: String,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Format with a marker so the name span can be styled even when the name itself contains text of the body.
    val template = stringResource(R.string.request_sent_body, NAME_MARKER)
    val body = requestSentBody(template, expertName, SpanStyle(color = onSurface, fontWeight = FontWeight.SemiBold))
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

/**
 * Replaces the name marker in [template] with [expertName] styled as [nameStyle]. A translation that drops the
 * `%1$s` placeholder has no marker; the template is then shown as is instead of crashing.
 */
internal fun requestSentBody(
    template: String,
    expertName: String,
    nameStyle: SpanStyle,
): AnnotatedString =
    buildAnnotatedString {
        val index = template.indexOf(NAME_MARKER)
        if (index < 0) {
            append(template)
        } else {
            append(template.substring(0, index))
            withStyle(nameStyle) { append(expertName) }
            append(template.substring(index + NAME_MARKER.length))
        }
    }
