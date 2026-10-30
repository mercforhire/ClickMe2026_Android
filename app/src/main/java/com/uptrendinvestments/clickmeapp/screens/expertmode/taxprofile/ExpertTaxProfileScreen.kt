package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockPerson
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.PickerField
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Test tag on the Save tax profile button. */
const val TAX_SAVE_TAG = "tax_save"

/** Test tag on the TIN field. */
const val TAX_TIN_TAG = "tax_tin"

private val FieldBlockHeight = 56.dp
private val IconSize = 24.dp
private val BorderWidth = 1.dp
private const val MIN_DOB_YEAR = 1900
private const val SKELETON_SECTIONS = 3
private const val SKELETON_FIELDS = 4
private const val SKELETON_TEXT_LINES = 3
private const val SKELETON_LAST_LINE_FRACTION = 0.6f

/**
 * No autofill service knows this hint, so a password manager is not offered the TIN to save as a password even though
 * the field is masked (UI-SPEC: the TIN uses no autofill hint).
 */
private val TinContentType = ContentType("taxIdentificationNumber")

/** ViewModel callbacks of the Tax & Payout Info screen, bundled so the stateless content is easy to drive in tests. */
data class TaxProfileActions(
    val onFieldChange: (TaxProfileField, String) -> Unit,
    val onDobPicked: (Long) -> Unit,
    val save: () -> Unit,
    val retry: () -> Unit,
    val dismissDialog: () -> Unit,
    val consumeSaved: () -> Unit,
    val consumeFocusTin: () -> Unit,
)

/** Hilt-aware Tax & Payout Info route (PAYOUT-04). */
@Composable
fun ExpertTaxProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpertTaxProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExpertTaxProfileContent(
        state = state,
        actions =
            TaxProfileActions(
                onFieldChange = viewModel::onFieldChange,
                onDobPicked = viewModel::onDobPicked,
                save = viewModel::save,
                retry = viewModel::retry,
                dismissDialog = viewModel::dismissDialog,
                consumeSaved = viewModel::consumeSaved,
                consumeFocusTin = viewModel::consumeFocusTin,
            ),
        onBack = onBack,
        modifier = modifier,
    )
}

/** Stateless Tax & Payout Info screen: a skeleton, an error with Retry, or the form with its pinned Save bar. */
@Composable
fun ExpertTaxProfileContent(
    state: TaxProfileUiState,
    actions: TaxProfileActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.tax_profile_saved)
    LaunchedEffect(state.savedEvent) {
        if (state.savedEvent) {
            // Launched on the composition scope so consuming the one-shot does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(savedMessage, duration = SnackbarDuration.Short) }
            actions.consumeSaved()
        }
    }

    // While a save is in flight Back (arrow and system) is ignored: the request is already out.
    BackHandler(enabled = state.saving) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.tax_profile_title),
                onBack = onBack,
                backEnabled = !state.saving,
            )
        },
        snackbarHost = {
            // The Scaffold has zero insets (the body pads itself), so the host clears the bars and the keyboard itself.
            SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding().imePadding())
        },
    ) { innerPadding ->
        when (val load = state.load) {
            TaxProfileLoad.Loading -> TaxProfileSkeleton(Modifier.padding(innerPadding))
            is TaxProfileLoad.Failed ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.tax_profile_load_error),
                        body = load.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.expert_retry), actions.retry),
                    )
                }
            TaxProfileLoad.Ready -> TaxProfileBody(state, actions, Modifier.padding(innerPadding))
        }
    }

    state.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.asString(),
            onDismiss = actions.dismissDialog,
            title = stringResource(R.string.tax_profile_save_error),
            confirmLabel = stringResource(R.string.tax_profile_got_it),
        )
    }
}

@Composable
private fun TaxProfileSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md).padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(SKELETON_TEXT_LINES) { line ->
                    val last = line == SKELETON_TEXT_LINES - 1
                    SkeletonBlock(
                        Modifier.fillMaxWidth(if (last) SKELETON_LAST_LINE_FRACTION else 1f).height(Spacing.md),
                    )
                }
            }
            repeat(SKELETON_SECTIONS) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    repeat(SKELETON_FIELDS) {
                        SkeletonBlock(
                            Modifier.fillMaxWidth().height(FieldBlockHeight),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaxProfileBody(
    state: TaxProfileUiState,
    actions: TaxProfileActions,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val enabled = !state.saving
    val errors = state.fieldErrors
    val tinFocus = remember { FocusRequester() }
    var showDobPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.focusTinEvent) {
        if (state.focusTinEvent) {
            tinFocus.requestFocus()
            actions.consumeFocusTin()
        }
    }

    @Composable
    fun row(
        target: TaxProfileField,
        label: Int,
        value: String,
        icon: ImageVector,
        imeAction: ImeAction = ImeAction.Next,
    ) {
        TaxField(
            label = stringResource(label),
            value = value,
            onValueChange = { actions.onFieldChange(target, it) },
            icon = icon,
            error = errors[target],
            enabled = enabled,
            imeAction = imeAction,
        )
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .contentMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.tax_profile_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TaxSection(stringResource(R.string.tax_profile_section_identity)) {
                row(TaxProfileField.LegalName, R.string.tax_profile_legal_name, form.legalName, Icons.Outlined.Person)
                DobField(
                    iso = form.dob,
                    error = errors[TaxProfileField.Dob],
                    enabled = enabled,
                    onClick = { showDobPicker = true },
                )
            }
            TaxSection(stringResource(R.string.tax_profile_section_address)) {
                row(TaxProfileField.Line1, R.string.tax_profile_line1, form.line1, Icons.Outlined.Home)
                row(TaxProfileField.Line2, R.string.tax_profile_line2, form.line2, Icons.Outlined.Apartment)
                row(TaxProfileField.City, R.string.tax_profile_city, form.city, Icons.Outlined.LocationCity)
                row(TaxProfileField.Region, R.string.tax_profile_region, form.region, Icons.Outlined.Map)
                row(
                    TaxProfileField.PostalCode,
                    R.string.tax_profile_postal_code,
                    form.postalCode,
                    Icons.Outlined.MailOutline,
                )
                row(TaxProfileField.Country, R.string.tax_profile_country, form.country, Icons.Outlined.Public)
            }
            TaxSection(stringResource(R.string.tax_profile_section_tax)) {
                row(
                    TaxProfileField.FinancialAccount,
                    R.string.tax_profile_financial_account,
                    form.financialAccount,
                    Icons.Outlined.AccountBalance,
                )
                if (state.tinOnFile) TinOnFileRow()
                TaxField(
                    label =
                        stringResource(
                            if (state.tinOnFile) R.string.tax_profile_tin_replace else R.string.tax_profile_tin,
                        ),
                    value = form.tin,
                    onValueChange = { actions.onFieldChange(TaxProfileField.Tin, it) },
                    icon = Icons.Outlined.Lock,
                    error = errors[TaxProfileField.Tin],
                    enabled = enabled,
                    imeAction = ImeAction.Done,
                    isSecure = true,
                    modifier = Modifier.focusRequester(tinFocus).testTag(TAX_TIN_TAG),
                )
                if (!state.tinOnFile) {
                    Text(
                        text = stringResource(R.string.tax_profile_tin_helper),
                        modifier = Modifier.padding(horizontal = Spacing.xs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .contentMaxWidth()
                    .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.banner?.let { TinBanner(it.asString()) }
            AsyncSaveButton(
                title = stringResource(R.string.tax_profile_save),
                state = state.saveState,
                onClick = actions.save,
                modifier = Modifier.testTag(TAX_SAVE_TAG),
                enabled = state.canSave,
            )
        }
    }

    if (showDobPicker) {
        DobPickerDialog(
            iso = form.dob,
            onPicked = actions.onDobPicked,
            onDismiss = { showDobPicker = false },
        )
    }
}

@Composable
private fun TaxSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = Spacing.sm).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** A [ValidatedField] with its label above, since the placeholder disappears once the field holds a value. */
@Composable
private fun TaxField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    error: String?,
    enabled: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    isSecure: Boolean = false,
) {
    Column {
        FieldLabel(label)
        ValidatedField(
            value = value,
            onValueChange = onValueChange,
            placeholder = label,
            leadingIcon = icon,
            error = error,
            modifier = modifier,
            isSecure = isSecure,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            enabled = enabled,
            contentType = if (isSecure) TinContentType else null,
        )
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DobField(
    iso: String,
    error: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.tax_profile_dob)
    Column {
        FieldLabel(label)
        PickerField(
            value = formatTaxDob(iso) ?: iso.takeIf { it.isNotBlank() },
            placeholder = label,
            leadingIcon = Icons.Outlined.CalendarToday,
            onClick = onClick,
            error = error,
            enabled = enabled,
        )
    }
}

/** The server deferred the save because it still needs the TIN. Information, not an error. */
@Composable
private fun TinBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Material date picker for the date of birth (UI-SPEC A6): 1900 through the current year, no future days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobPickerDialog(
    iso: String,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = utcMillisOf(iso),
            yearRange = taxDobYearRange(),
            selectableDates = remember { NotInTheFuture(todayUtcMillis()) },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let(onPicked)
                    onDismiss()
                },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.tax_profile_dob_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tax_profile_dob_dismiss)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class NotInTheFuture(
    private val todayUtcMillis: Long,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis

    override fun isSelectableYear(year: Int): Boolean = year <= Year.now().value
}

private fun utcMillis(day: LocalDate): Long {
    val start = day.atStartOfDay(ZoneOffset.UTC)
    return start.toInstant().toEpochMilli()
}

private fun todayUtcMillis(): Long = utcMillis(LocalDate.now())

/** The picker throws on an initial date outside its year range, so an out-of-range stored date starts empty. */
private fun utcMillisOf(iso: String): Long? {
    val day = runCatching { LocalDate.parse(iso.trim()) }.getOrNull() ?: return null
    if (day.year !in taxDobYearRange()) return null
    return utcMillis(day)
}

@Composable
private fun TinOnFileRow() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LockPerson,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.tax_profile_tin_on_file),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Years the date of birth picker offers: 1900 through [currentYear]. */
internal fun taxDobYearRange(currentYear: Int = Year.now().value): IntRange = MIN_DOB_YEAR..currentYear

/** `yyyy-MM-dd` as `MMM d, yyyy`, or null when [iso] is not a calendar date. */
internal fun formatTaxDob(iso: String): String? =
    runCatching {
        LocalDate.parse(iso.trim()).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }.getOrNull()
