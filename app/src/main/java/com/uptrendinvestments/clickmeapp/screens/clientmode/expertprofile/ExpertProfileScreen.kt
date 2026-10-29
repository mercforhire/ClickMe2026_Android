package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.SectionAppear
import com.uptrendinvestments.clickmeapp.components.chat.ChatEntryErrorDialog
import com.uptrendinvestments.clickmeapp.components.safety.BlockUserDialog
import com.uptrendinvestments.clickmeapp.components.safety.ReportUserSheet
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatPeer
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileAboutCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileAboutCardSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileActionButtons
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileActionButtonsSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileCardSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileExpertiseSection
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileHero
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileReviewsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileReviewsCardSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileStatsBar
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileStatsBarSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews.ExpertProfileTopicsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.launch

private const val SKELETON_FADE_MS = 150
private val MenuTarget = 48.dp

private enum class DetailsKind { Loading, Loaded, Error }

private enum class ReviewsKind { Loading, Shown, Absent }

/**
 * Expert Profile route body. The ViewModel (created by the Nav3 entry) starts both requests in its `init`; Book
 * Session hands the [BookingKey] to [onBook].
 *
 * Book Session is hidden when [showBookSession] is false, when [isSelfView] is true (the expert opened their own
 * profile from the expert hub; booking yourself ends in `422 CANNOT_BOOK_SELF`) or when the profile is the signed-in
 * user's own. The expert shell passes `showBookSession = false` (an expert cannot book from Expert mode and
 * `BookingKey` is not registered there) and the hub passes [isSelfView] from the key. `isSelf` already hides
 * Message and the safety menu.
 */
@Composable
fun ExpertProfileScreen(
    viewModel: ExpertProfileViewModel,
    onBack: () -> Unit,
    onBook: (BookingKey) -> Unit,
    modifier: Modifier = Modifier,
    isSelfView: Boolean = false,
    showBookSession: Boolean = true,
    chatEntry: ChatEntryViewModel = hiltViewModel(),
    safety: ProfileSafetyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entryState by chatEntry.state.collectAsStateWithLifecycle()
    val safetyState by safety.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val thanks = stringResource(R.string.report_thanks)
    val blocked =
        (safetyState.snackbar as? ProfileSafetySnackbar.Blocked)?.name?.let {
            stringResource(R.string.profile_blocked_snackbar, it)
        } ?: stringResource(R.string.profile_blocked_snackbar_fallback)
    LaunchedEffect(safetyState.snackbar) {
        val message =
            when (safetyState.snackbar) {
                ProfileSafetySnackbar.ReportThanks -> thanks
                is ProfileSafetySnackbar.Blocked -> blocked
                null -> null
            }
        if (message != null) {
            // Launched on the composition scope so consuming the flag does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(message) }
            safety.consumeSnackbar()
        }
    }
    ChatEntryErrorDialog(error = entryState.error, onDismiss = chatEntry::dismissError)
    if (safetyState.sheetOpen) {
        ReportUserSheet(
            name = state.name,
            reason = safetyState.reason,
            details = safetyState.details,
            submitting = safetyState.reportSubmitting,
            onSelectReason = safety::selectReason,
            onDetailsChange = safety::onDetailsChange,
            onSubmit = { safety.submitReport(state.expertId) },
            onDismiss = safety::dismissReport,
        )
    }
    if (safetyState.blockConfirmOpen) {
        BlockUserDialog(
            name = state.name,
            inFlight = safetyState.blockInFlight,
            onConfirm = { safety.confirmBlock(state.expertId, state.name) },
            onDismiss = safety::dismissBlock,
        )
    }
    safetyState.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.message.asString(),
            title = stringResource(dialog.titleRes),
            onDismiss = safety::dismissDialog,
        )
    }
    ExpertProfileContent(
        state = state,
        onBack = onBack,
        onBookSession = { onBook(viewModel.bookingKey()) },
        onRetry = viewModel::retry,
        modifier = modifier,
        showBookSession = showBookSession && !isSelfView && !chatEntry.isSelf(state.expertId),
        showMessage = !chatEntry.isSelf(state.expertId),
        messageBusy = entryState.busyPeerId != null,
        onMessage = { chatEntry.open(ChatPeer(state.expertId, state.name, state.imageUrl, isExpert = true)) },
        showSafetyMenu = !chatEntry.isSelf(state.expertId) && state.details is DetailsState.Loaded,
        onReport = safety::openReport,
        onBlock = safety::openBlock,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Stateless profile content (DISC-03). The hero renders from [state] at once; the sections below it follow the
 * details and reviews load states (D-12, D-13, D-15).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertProfileContent(
    state: ExpertProfileUiState,
    onBack: () -> Unit,
    onBookSession: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    showBookSession: Boolean = true,
    showMessage: Boolean = false,
    messageBusy: Boolean = false,
    onMessage: () -> Unit = {},
    showSafetyMenu: Boolean = false,
    onReport: () -> Unit = {},
    onBlock: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ProfileTopBar(
                name = state.name,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                showSafetyMenu = showSafetyMenu,
                onReport = onReport,
                onBlock = onBlock,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.details is DetailsState.Unavailable) {
            UnavailableBody(onBack = onBack, modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = Spacing.md,
                        top = innerPadding.calculateTopPadding(),
                        end = Spacing.md,
                        bottom = innerPadding.calculateBottomPadding() + Spacing.xl,
                    ),
            ) {
                item(key = "hero") {
                    ExpertProfileHero(
                        name = state.name,
                        title = state.title,
                        isTitlePending = state.isTitlePending,
                        imageUrl = state.imageUrl,
                        rating = state.rating,
                    )
                }
                item(key = "details") {
                    DetailsSection(
                        state = state,
                        onBookSession = onBookSession,
                        onRetry = onRetry,
                        showBookSession = showBookSession,
                        showMessage = showMessage,
                        messageBusy = messageBusy,
                        onMessage = onMessage,
                    )
                }
                if (state.details !is DetailsState.Error) {
                    item(key = "reviews") { ReviewsSection(state.reviews) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    name: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    showSafetyMenu: Boolean,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.a11y_back),
                )
            }
        },
        actions = {
            if (showSafetyMenu) SafetyMenu(name = name, onReport = onReport, onBlock = onBlock)
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        scrollBehavior = scrollBehavior,
    )
}

/** The More options menu: Report this profile and Block {name} (D-07). Re-blocking is allowed, so no block state. */
@Composable
private fun SafetyMenu(
    name: String,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(MenuTarget)) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.profile_menu_more),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SafetyMenuItem(
                text = stringResource(R.string.profile_menu_report),
                icon = Icons.Outlined.Flag,
                onClick = {
                    expanded = false
                    onReport()
                },
            )
            SafetyMenuItem(
                text = stringResource(R.string.profile_menu_block, name),
                icon = Icons.Outlined.Block,
                onClick = {
                    expanded = false
                    onBlock()
                },
            )
        }
    }
}

@Composable
private fun SafetyMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = text, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = MenuTarget),
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
        colors =
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

/** D-13: a 404 replaces the whole body, hero included. */
@Composable
private fun UnavailableBody(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        EmptyState(
            icon = Icons.Outlined.PersonOff,
            heading = stringResource(R.string.expert_profile_unavailable_heading),
            body = stringResource(R.string.expert_profile_unavailable_body),
            modifier = Modifier.padding(top = Spacing.xxxl),
            action = EmptyStateAction(stringResource(R.string.expert_profile_go_back), onBack),
        )
    }
}

@Composable
private fun DetailsSection(
    state: ExpertProfileUiState,
    onBookSession: () -> Unit,
    onRetry: () -> Unit,
    showBookSession: Boolean,
    showMessage: Boolean,
    messageBusy: Boolean,
    onMessage: () -> Unit,
) {
    val kind =
        when (state.details) {
            DetailsState.Loading -> DetailsKind.Loading
            is DetailsState.Loaded -> DetailsKind.Loaded
            else -> DetailsKind.Error
        }
    Crossfade(
        targetState = kind,
        animationSpec = tween(SKELETON_FADE_MS),
        label = "profileDetails",
    ) { target ->
        when (target) {
            DetailsKind.Loading -> DetailsSkeleton()
            DetailsKind.Error ->
                Box(Modifier.fillMaxWidth().padding(top = Spacing.lg), contentAlignment = Alignment.TopCenter) {
                    ErrorState(onRetry = onRetry)
                }
            DetailsKind.Loaded ->
                (state.details as? DetailsState.Loaded)?.let { loaded ->
                    LoadedDetails(
                        details = loaded.details,
                        tags = state.tags,
                        onBookSession = onBookSession,
                        showBookSession = showBookSession,
                        showMessage = showMessage,
                        messageBusy = messageBusy,
                        onMessage = onMessage,
                    )
                }
        }
    }
}

@Composable
private fun DetailsSkeleton() {
    val loading = stringResource(R.string.content_loading)
    Column(Modifier.clearAndSetSemantics { contentDescription = loading }) {
        Spacer(Modifier.height(Spacing.md))
        ExpertProfileStatsBarSkeleton()
        Spacer(Modifier.height(Spacing.md))
        ExpertProfileActionButtonsSkeleton()
        Spacer(Modifier.height(Spacing.lg))
        ExpertProfileAboutCardSkeleton()
        Spacer(Modifier.height(Spacing.lg))
        ExpertProfileCardSkeleton(rows = 3, rowHeight = 56.dp)
    }
}

@Composable
private fun LoadedDetails(
    details: ProfileDetailsUi,
    tags: List<String>,
    onBookSession: () -> Unit,
    showBookSession: Boolean,
    showMessage: Boolean,
    messageBusy: Boolean,
    onMessage: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(Spacing.md))
        ExpertProfileStatsBar(details.experienceYears, details.totalBookings)
        Spacer(Modifier.height(Spacing.md))
        ExpertProfileActionButtons(
            isBookable = details.isBookable,
            onBookSession = onBookSession,
            showBookSession = showBookSession,
            showMessage = showMessage,
            messageBusy = messageBusy,
            onMessage = onMessage,
        )
        val bio = details.bio
        if (!bio.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.lg))
            SectionAppear { ExpertProfileAboutCard(bio = bio) }
        }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.lg))
            SectionAppear { ExpertProfileExpertiseSection(tags = tags) }
        }
        if (details.topics.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.lg))
            SectionAppear { ExpertProfileTopicsCard(topics = details.topics) }
        }
    }
}

@Composable
private fun ReviewsSection(reviews: ReviewsState) {
    val kind =
        when {
            reviews is ReviewsState.Loading -> ReviewsKind.Loading
            reviews is ReviewsState.Loaded && reviews.items.isNotEmpty() -> ReviewsKind.Shown
            else -> ReviewsKind.Absent
        }
    Crossfade(
        targetState = kind,
        animationSpec = tween(SKELETON_FADE_MS),
        label = "profileReviews",
    ) { target ->
        when (target) {
            ReviewsKind.Loading ->
                ExpertProfileReviewsCardSkeleton(Modifier.padding(top = Spacing.lg))
            ReviewsKind.Shown ->
                (reviews as? ReviewsState.Loaded)?.let {
                    SectionAppear(Modifier.padding(top = Spacing.lg)) {
                        ExpertProfileReviewsCard(reviews = it.items)
                    }
                }
            ReviewsKind.Absent -> Unit
        }
    }
}

@Preview(name = "ExpertProfileContent")
@Composable
private fun ExpertProfileContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileContent(
                state =
                    ExpertProfileUiState(
                        expertId = "1",
                        name = "Ada Lovelace",
                        title = "Mathematician",
                        imageUrl = null,
                        tags = listOf("AI", "Math"),
                        rating = HeroRating.Shown(4.8, 7),
                        details =
                            DetailsState.Loaded(
                                ProfileDetailsUi(
                                    experienceYears = 7,
                                    totalBookings = 12,
                                    bio = "Pioneer of early computing.",
                                    topics = listOf(TopicRowUi("1", "Career advice", 30, null, PriceDisplay.Free)),
                                    isBookable = true,
                                ),
                            ),
                        reviews = ReviewsState.Loaded(listOf(ReviewRowUi("1", "Grace", 5, "Great."))),
                    ),
                onBack = {},
                onBookSession = {},
                onRetry = {},
            )
        }
    }
}
