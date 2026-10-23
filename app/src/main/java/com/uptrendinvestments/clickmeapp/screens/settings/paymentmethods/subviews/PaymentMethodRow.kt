package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.CardOperationKind
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val RowMinHeight = 72.dp
private val TileWidth = 48.dp
private val TileHeight = 32.dp
private val IconSize = 24.dp
private val TrailingSize = 48.dp
private val SpinnerStroke = 2.dp
private val RowBorderWidth = 1.dp

/** Test tag on a card row's container. */
fun paymentMethodRowTag(id: String) = "payment_method_row_$id"

/** Test tag on a card row's overflow (⋮) button. */
fun paymentMethodMenuTag(id: String) = "payment_method_menu_$id"

/**
 * One saved card (PAY-01). Port of iOS `PaymentMethodRow`: brand, •••• last4, "Expires MM/YY" and a "Default" badge on
 * the default card. The row is not tappable; actions live in the ⋮ menu (D-05), whose "Remove" asks for confirmation
 * in the screen. While [busy] is set the menu button is replaced by a spinner (D-06); [actionsEnabled] is false for
 * every row while any operation runs, so a second operation cannot start.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodRow(
    card: PaymentMethodItem,
    busy: CardOperationKind?,
    actionsEnabled: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandName = stringResource(brandDisplayNameRes(card.brand))
    val last4 = card.last4.orEmpty()
    val cardName = stringResource(R.string.payment_methods_card_name, brandName, last4)
    val expiry = expiryParts(card.expMonth, card.expYear)
    val rowLabel =
        when {
            expiry == null -> cardName
            card.isDefault ->
                stringResource(R.string.payment_methods_row_a11y_default, brandName, last4, expiry.first, expiry.second)
            else -> stringResource(R.string.payment_methods_row_a11y, brandName, last4, expiry.first, expiry.second)
        }

    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = RowMinHeight).testTag(paymentMethodRowTag(card.id)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(RowBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            // Only the description half of the row is merged; the overflow button stays its own focus stop.
            Row(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = rowLabel },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandTile()
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = cardName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                        )
                        if (card.isDefault) DefaultBadge()
                    }
                    if (expiry != null) {
                        Spacer(Modifier.size(Spacing.xs))
                        Text(
                            text = stringResource(R.string.payment_methods_expires, expiry.first, expiry.second),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
            }
            Trailing(
                card = card,
                busy = busy,
                actionsEnabled = actionsEnabled,
                onSetDefault = onSetDefault,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun BrandTile() {
    Box(
        modifier =
            Modifier
                .size(width = TileWidth, height = TileHeight)
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DefaultBadge() {
    Text(
        text = stringResource(R.string.payment_methods_default_badge),
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

@Composable
private fun Trailing(
    card: PaymentMethodItem,
    busy: CardOperationKind?,
    actionsEnabled: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(TrailingSize), contentAlignment = Alignment.Center) {
        Crossfade(
            targetState = busy,
            animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
            label = "paymentMethodTrailing",
        ) { kind ->
            if (kind != null) {
                BusySpinner(kind)
            } else {
                OverflowMenu(card, actionsEnabled, onSetDefault, onRemove)
            }
        }
    }
}

@Composable
private fun BusySpinner(kind: CardOperationKind) {
    val state =
        stringResource(
            when (kind) {
                CardOperationKind.SetDefault -> R.string.payment_methods_state_updating
                CardOperationKind.Remove -> R.string.payment_methods_state_removing
            },
        )
    Box(
        modifier = Modifier.size(TrailingSize).semantics(mergeDescendants = true) { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(IconSize),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = SpinnerStroke,
        )
    }
}

@Composable
private fun OverflowMenu(
    card: PaymentMethodItem,
    actionsEnabled: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
) {
    val moreOptions =
        stringResource(
            R.string.payment_methods_more_options,
            stringResource(brandDisplayNameRes(card.brand)),
            card.last4.orEmpty(),
        )
    var expanded by rememberSaveable(card.id) { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(TrailingSize).testTag(paymentMethodMenuTag(card.id)),
            enabled = actionsEnabled,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = moreOptions,
                modifier = Modifier.size(IconSize),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            if (!card.isDefault) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.payment_methods_menu_set_default),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSetDefault()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
            // The error role, not the booking-status destructive token (UI-SPEC A5).
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.payment_methods_menu_remove),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onRemove()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize),
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
            )
        }
    }
}

@Preview(name = "PaymentMethodRow")
@Composable
private fun PaymentMethodRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PaymentMethodRow(
                    card = PaymentMethodItem("pm_1", "visa", "4242", 9, 2028, isDefault = true),
                    busy = null,
                    actionsEnabled = true,
                    onSetDefault = {},
                    onRemove = {},
                )
                PaymentMethodRow(
                    card = PaymentMethodItem("pm_2", "mastercard", "4444", 12, 2031, isDefault = false),
                    busy = CardOperationKind.Remove,
                    actionsEnabled = false,
                    onSetDefault = {},
                    onRemove = {},
                )
            }
        }
    }
}
