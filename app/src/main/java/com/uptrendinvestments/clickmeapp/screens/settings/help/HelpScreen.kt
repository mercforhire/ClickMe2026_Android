package com.uptrendinvestments.clickmeapp.screens.settings.help

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.model.responses.FaqArticlePreview
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.supportEmailIntent
import com.uptrendinvestments.clickmeapp.screens.settings.help.subviews.HelpContactRow
import com.uptrendinvestments.clickmeapp.screens.settings.help.subviews.HelpFaqCard
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val SkeletonHeaderWidth = 200.dp
private val SkeletonHeaderHeight = 20.dp
private val SkeletonCardHeight = 56.dp

/** Saves the ids of the expanded categories across recreation. */
private val ExpandedIdsSaver =
    listSaver<SnapshotStateList<String>, String>(
        save = { it.toList() },
        restore = { it.toMutableStateList() },
    )

/**
 * Port of iOS `HelpView`: the FAQ categories from the server, expandable in place, and an Email Us row. The mail
 * intent is started from the Activity context (PROF-05).
 */
@Composable
fun HelpScreen(
    viewModel: HelpViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    HelpContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = {},
        onRefreshFailureShown = {},
        launchEmail = context::startActivity,
        modifier = modifier,
    )
}

/**
 * Stateless Help screen under a "Help" bar. [launchEmail] is the seam that starts the mail intent, so tests can stand
 * in for the platform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpContent(
    state: HelpUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureShown: () -> Unit,
    launchEmail: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedIds = rememberSaveable(saver = ExpandedIdsSaver) { mutableStateListOf<String>() }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.help_title), onBack = onBack) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                when (val load = state.load) {
                    HelpLoad.Loading -> item(key = "loading") { HelpSkeleton() }
                    is HelpLoad.Failed -> item(key = "contact") { ContactSection { launchEmail(supportEmailIntent()) } }
                    is HelpLoad.Loaded -> {
                        if (load.categories.isNotEmpty()) {
                            item(key = "faq") {
                                FaqSection(
                                    categories = load.categories,
                                    expandedIds = expandedIds,
                                )
                            }
                        }
                        item(key = "contact") { ContactSection { launchEmail(supportEmailIntent()) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = Spacing.md).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FaqSection(
    categories: List<FaqCategory>,
    expandedIds: SnapshotStateList<String>,
) {
    Column {
        SectionHeader(stringResource(R.string.help_faq_section))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            categories.forEach { category ->
                HelpFaqCard(
                    category = category,
                    expanded = category.id in expandedIds,
                    onToggle = {
                        if (!expandedIds.remove(category.id)) expandedIds.add(category.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ContactSection(onEmailUs: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.help_contact_section))
        HelpContactRow(onClick = onEmailUs)
    }
}

/** A header bar and four cards, then a header bar and one card, matching the loaded layout. */
@Composable
private fun HelpSkeleton() {
    SkeletonGroup {
        Column {
            SkeletonHeaderBar()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(FAQ_SKELETON_CARDS) { SkeletonCard() }
            }
            Spacer(Modifier.height(Spacing.lg))
            SkeletonHeaderBar()
            SkeletonCard()
        }
    }
}

private const val FAQ_SKELETON_CARDS = 4

@Composable
private fun SkeletonCard() {
    SkeletonBlock(
        modifier = Modifier.fillMaxWidth().height(SkeletonCardHeight),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun SkeletonHeaderBar() {
    SkeletonBlock(Modifier.padding(bottom = Spacing.md).size(SkeletonHeaderWidth, SkeletonHeaderHeight))
}

@Preview(name = "Help loaded")
@Composable
private fun HelpLoadedPreview() {
    ClickMeTheme {
        HelpContent(
            state =
                HelpUiState(
                    load =
                        HelpLoad.Loaded(
                            listOf(
                                FaqCategory(
                                    id = "c1",
                                    title = "Account",
                                    featuredArticles =
                                        listOf(FaqArticlePreview(id = "a1", question = "How do I reset my password?")),
                                ),
                            ),
                        ),
                ),
            onBack = {},
            onRetry = {},
            onRefresh = {},
            onRefreshFailureShown = {},
            launchEmail = {},
        )
    }
}
