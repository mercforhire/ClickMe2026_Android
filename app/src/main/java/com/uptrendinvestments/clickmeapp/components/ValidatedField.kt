package com.uptrendinvestments.clickmeapp.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val FieldMinHeight = 56.dp
private val FocusedBorder = 2.dp
private val RestBorder = 1.dp
private val LeadingIconSize = 20.dp
private val ToggleIconSize = 24.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * Pill text field with a leading icon, an animated error message and an optional password reveal toggle
 * (iOS ValidatedField). The border is 1 dp outline at rest, 2 dp primary when focused and 2 dp destructive on error.
 * Stateless: the caller owns [value]; only the password reveal flag is local UI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidatedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    isSecure: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    val hasError = error != null
    val extras = MaterialTheme.extras
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    var revealed by rememberSaveable { mutableStateOf(false) }

    // Keep the last message so the exit animation still has text to show.
    var lastError by remember { mutableStateOf("") }
    SideEffect { if (error != null) lastError = error }
    val shownError = error ?: lastError

    val colors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            errorTextColor = colorScheme.onSurface,
            disabledTextColor = colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
            focusedContainerColor = colorScheme.surfaceContainer,
            unfocusedContainerColor = colorScheme.surfaceContainer,
            errorContainerColor = colorScheme.surfaceContainer,
            disabledContainerColor = colorScheme.surfaceContainer,
            cursorColor = colorScheme.primary,
            errorCursorColor = extras.destructive,
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline,
            errorBorderColor = extras.destructive,
            disabledBorderColor = colorScheme.outline.copy(alpha = DISABLED_ALPHA),
            focusedPlaceholderColor = extras.onSurfaceMuted,
            unfocusedPlaceholderColor = extras.onSurfaceMuted,
            errorPlaceholderColor = extras.onSurfaceMuted,
            disabledPlaceholderColor = extras.onSurfaceMuted.copy(alpha = DISABLED_ALPHA),
        )

    val effectiveKeyboardOptions =
        if (isSecure) {
            keyboardOptions.copy(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
            )
        } else {
            keyboardOptions.copy(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false)
        }
    val visualTransformation =
        if (isSecure && !revealed) PasswordVisualTransformation() else VisualTransformation.None
    val iconTint = if (hasError) extras.destructive else extras.onSurfaceMuted
    val offsetPx = with(LocalDensity.current) { Motion.FIELD_ERROR_OFFSET_DP.dp.roundToPx() }

    Column(modifier = modifier.padding(bottom = Spacing.md)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = FieldMinHeight)
                    .semantics {
                        if (isSecure) contentType = ContentType.Password
                        if (error != null) error(error)
                    },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
            keyboardOptions = effectiveKeyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(if (hasError) extras.destructive else colorScheme.primary),
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = hasError,
                placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(LeadingIconSize),
                        tint = iconTint,
                    )
                },
                trailingIcon =
                    if (isSecure) {
                        {
                            val toggleIcon = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
                            IconButton(onClick = { revealed = !revealed }) {
                                Icon(
                                    imageVector = toggleIcon,
                                    contentDescription =
                                        stringResource(
                                            if (revealed) R.string.a11y_hide_password else R.string.a11y_show_password,
                                        ),
                                    modifier = Modifier.size(ToggleIconSize),
                                    tint = extras.onSurfaceMuted,
                                )
                            }
                        }
                    } else {
                        null
                    },
                colors = colors,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(start = Spacing.md, end = Spacing.md),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = hasError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = CircleShape,
                        focusedBorderThickness = FocusedBorder,
                        unfocusedBorderThickness = if (hasError) FocusedBorder else RestBorder,
                    )
                },
            )
        }
        AnimatedVisibility(
            visible = hasError,
            enter =
                fadeIn(tween(Motion.FIELD_ERROR_DURATION_MS)) +
                    slideInVertically(tween(Motion.FIELD_ERROR_DURATION_MS)) { -offsetPx },
            exit =
                fadeOut(tween(Motion.FIELD_ERROR_DURATION_MS)) +
                    slideOutVertically(tween(Motion.FIELD_ERROR_DURATION_MS)) { -offsetPx },
        ) {
            Text(
                text = shownError,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs),
                style = MaterialTheme.typography.labelMedium,
                color = extras.destructive,
            )
        }
    }
}

@Preview(name = "ValidatedField")
@Composable
private fun ValidatedFieldPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ValidatedField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    error = null,
                )
                ValidatedField(
                    value = "nope",
                    onValueChange = {},
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    error = "Enter a valid email address.",
                )
                ValidatedField(
                    value = "secret",
                    onValueChange = {},
                    placeholder = "Password",
                    leadingIcon = Icons.Outlined.Lock,
                    error = null,
                    isSecure = true,
                )
            }
        }
    }
}
