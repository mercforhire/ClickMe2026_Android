package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.subviews.ExpertiseEditorSheet
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TopicDescriptionField
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TopicDurationStepper
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TopicIconPicker
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TopicPriceField
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews.TopicTagPicker
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.util.Money
import java.util.UUID

/** Test tag of the app bar Save action. */
const val TOPIC_EDITOR_SAVE_TAG = "topic_editor_save"

/** Test tag of the title text field. */
const val TOPIC_EDITOR_TITLE_TAG = "topic_editor_title"

private val ActionBoxSize = 48.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp

/** The ViewModel callbacks the stateless [TopicEditorContent] drives. */
data class TopicEditorActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val decreaseDuration: () -> Unit = {},
    val increaseDuration: () -> Unit = {},
    val onPriceChange: (String) -> Unit = {},
    val toggleIcon: (String) -> Unit = {},
    val onTagFilterChange: (String) -> Unit = {},
    val toggleTag: (ExpertiseTagItem) -> Unit = {},
    val retryTags: () -> Unit = {},
    val onExpertiseSaved: (List<ExpertiseTagItem>) -> Unit = {},
    val save: () -> Unit = {},
    val dismissDialog: () -> Unit = {},
    val onFocusHandled: () -> Unit = {},
)

/**
 * Topic Editor route (D-07): full screen with Save in the app bar. A successful save pops back to the list through
 * [onBack]. "Add" / "Add Expertise" opens the Edit Expertise sheet (D-08); the sheet gets a fresh ViewModel for each
 * opening so every open re-seeds from the server, while rotation keeps the same one.
 */
@Composable
fun TopicEditorScreen(
    viewModel: TopicEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.finished) {
        if (state.finished) currentOnBack()
    }
    val actions =
        remember(viewModel) {
            TopicEditorActions(
                onTitleChange = viewModel::onTitleChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                decreaseDuration = viewModel::decreaseDuration,
                increaseDuration = viewModel::increaseDuration,
                onPriceChange = viewModel::onPriceChange,
                toggleIcon = viewModel::toggleIcon,
                onTagFilterChange = viewModel::onTagFilterChange,
                toggleTag = viewModel::toggleTag,
                retryTags = viewModel::retryTags,
                onExpertiseSaved = viewModel::onExpertiseSaved,
                save = viewModel::save,
                dismissDialog = viewModel::dismissDialog,
                onFocusHandled = viewModel::onFocusHandled,
            )
        }
    TopicEditorContent(
        state = state,
        actions = actions,
        onBack = onBack,
        expertiseSheet = { onDismiss, onSaved ->
            // The slot is only composed while the sheet is open, so this id is new for every opening and survives
            // rotation (rememberSaveable), giving each opening its own ViewModel.
            val session = rememberSaveable { UUID.randomUUID().toString() }
            ExpertiseEditorSheet(
                onDismiss = onDismiss,
                onSaved = onSaved,
                viewModel = hiltViewModel(key = "expertise_editor_$session"),
            )
        },
    )
}

/**
 * Stateless Topic Editor body. While [TopicEditorUiState.isSaving] the back arrow, system Back and every field are
 * locked. Leaving with unsaved edits discards them silently (D-16). [expertiseSheet] renders the Edit Expertise
 * sheet while it is open.
 */
@Composable
fun TopicEditorContent(
    state: TopicEditorUiState,
    actions: TopicEditorActions,
    onBack: () -> Unit,
    expertiseSheet: @Composable (onDismiss: () -> Unit, onSaved: (List<ExpertiseTagItem>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.isSaving) {}
    var showExpertiseSheet by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    val currentOnFocusHandled by rememberUpdatedState(actions.onFocusHandled)
    LaunchedEffect(state.focusTarget) {
        when (state.focusTarget) {
            EditorField.Title -> titleFocus.requestFocus()
            EditorField.Price -> priceFocus.requestFocus()
            null -> return@LaunchedEffect
        }
        currentOnFocusHandled()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuthTopAppBar(
                title =
                    stringResource(
                        if (state.isEdit) R.string.topic_editor_title_edit else R.string.topic_editor_title_add,
                    ),
                onBack = onBack,
                backEnabled = !state.isSaving,
                actions = { SaveAction(isSaving = state.isSaving, onSave = actions.save) },
            )
        },
    ) { innerPadding ->
        EditorBody(
            state = state,
            actions = actions,
            titleFocus = titleFocus,
            priceFocus = priceFocus,
            onAddExpertise = { showExpertiseSheet = true },
            modifier = Modifier.padding(innerPadding),
        )
    }

    state.dialog?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = actions.dismissDialog,
            title = stringResource(R.string.topic_editor_save_error_title),
        )
    }

    if (showExpertiseSheet) {
        expertiseSheet(
            { showExpertiseSheet = false },
            { tags ->
                actions.onExpertiseSaved(tags)
                showExpertiseSheet = false
            },
        )
    }
}

@Composable
private fun EditorBody(
    state: TopicEditorUiState,
    actions: TopicEditorActions,
    titleFocus: FocusRequester,
    priceFocus: FocusRequester,
    onAddExpertise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isSaving
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .contentMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        TopicIconPicker(
            options = state.iconOptions,
            selected = state.iconSlug,
            onToggle = actions.toggleIcon,
            enabled = enabled,
        )
        TitleSection(state = state, onChange = actions.onTitleChange, focus = titleFocus, enabled = enabled)
        TopicDescriptionField(value = state.description, onValueChange = actions.onDescriptionChange, enabled = enabled)
        TopicDurationStepper(
            minutes = state.durationMins,
            onDecrease = actions.decreaseDuration,
            onIncrease = actions.increaseDuration,
            enabled = enabled,
        )
        PriceSection(state = state, onChange = actions.onPriceChange, focus = priceFocus, enabled = enabled)
        TopicTagPicker(
            isLoading = state.tags is TopicTagsLoad.Loading,
            loadFailed = state.tags is TopicTagsLoad.Failed,
            hasProfileTags = state.profileTags.isNotEmpty(),
            visibleTags = state.visibleTags,
            selectedIds = state.selectedIds,
            atCap = state.atCap,
            filter = state.tagFilter,
            onFilterChange = actions.onTagFilterChange,
            onToggle = actions.toggleTag,
            onAddExpertise = onAddExpertise,
            onRetry = actions.retryTags,
            enabled = enabled,
        )
        Spacer(Modifier.height(Spacing.xl - Spacing.lg))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun TitleSection(
    state: TopicEditorUiState,
    onChange: (String) -> Unit,
    focus: FocusRequester,
    enabled: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.topic_editor_title_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(Spacing.sm))
        ValidatedField(
            value = state.title,
            onValueChange = onChange,
            placeholder = stringResource(R.string.topic_editor_title_placeholder),
            leadingIcon = Icons.Outlined.Title,
            error = state.titleError?.asString(),
            modifier = Modifier.focusRequester(focus).testTag(TOPIC_EDITOR_TITLE_TAG),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            enabled = enabled,
        )
    }
}

@Composable
private fun PriceSection(
    state: TopicEditorUiState,
    onChange: (String) -> Unit,
    focus: FocusRequester,
    enabled: Boolean,
) {
    val currency = state.currency
    val floorText = state.floorMinor?.let { Money.format(it.toLong(), currency) }
    val helper = floorText?.let { stringResource(R.string.topic_editor_price_helper, it) }
    val error =
        when (val priceError = state.priceError) {
            null -> null
            is PriceFieldError.BelowFloor ->
                stringResource(
                    R.string.topic_editor_price_below_floor,
                    Money.format(priceError.floorMinor.toLong(), priceError.currency),
                )
            PriceFieldError.Invalid -> stringResource(R.string.topic_editor_required_error)
            is PriceFieldError.Server ->
                priceError.message ?: stringResource(R.string.expert_error_generic)
        }
    TopicPriceField(
        text = state.priceText,
        onTextChange = onChange,
        currencyCode = currency,
        helperText = helper,
        error = error,
        decimalKeyboard = TopicPrice.fractionDigits(currency) > 0,
        enabled = enabled,
        modifier = Modifier.focusRequester(focus),
    )
}

/** App bar Save: a text button when idle, a 24 dp spinner (announced as "Saving topic") while saving. */
@Composable
private fun RowScope.SaveAction(
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    if (isSaving) {
        val savingText = stringResource(R.string.topic_editor_saving)
        Box(
            modifier = Modifier.size(ActionBoxSize).semantics { stateDescription = savingText },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = SpinnerStroke,
            )
        }
    } else {
        val saveLabel = stringResource(R.string.topic_editor_save_a11y)
        TextButton(
            onClick = onSave,
            modifier =
                Modifier
                    .heightIn(min = ActionBoxSize)
                    .testTag(TOPIC_EDITOR_SAVE_TAG)
                    .semantics {
                        contentDescription = saveLabel
                        onClick(label = saveLabel) {
                            onSave()
                            true
                        }
                    },
        ) {
            Text(
                text = stringResource(R.string.topic_editor_save),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(name = "TopicEditorContent")
@Composable
private fun TopicEditorContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicEditorContent(
                state =
                    TopicEditorUiState(
                        isEdit = false,
                        title = "Pricing deep dive",
                        priceText = "2",
                        floorMinor = 500,
                        priceError = PriceFieldError.BelowFloor(500, "CAD"),
                        currency = "CAD",
                    ),
                actions = TopicEditorActions(),
                onBack = {},
                expertiseSheet = { _, _ -> },
            )
        }
    }
}
