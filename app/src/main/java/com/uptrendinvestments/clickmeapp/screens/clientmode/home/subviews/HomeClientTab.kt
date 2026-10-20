package com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.navigation.TabDefinition
import kotlinx.serialization.Serializable

@Serializable
data object ExploreRoot : NavKey

@Serializable
data object SearchRoot : NavKey

@Serializable
data object ClientBookingsRoot : NavKey

@Serializable
data object ClientChatsRoot : NavKey

@Serializable
data object ClientProfileRoot : NavKey

/** Client shell tabs, in iOS HomeClientTab order. */
val clientTabs: List<TabDefinition> =
    listOf(
        TabDefinition(
            key = ExploreRoot,
            labelRes = R.string.tab_explore,
            icon = Icons.Outlined.AutoAwesome,
            selectedIcon = Icons.Filled.AutoAwesome,
        ),
        TabDefinition(
            key = SearchRoot,
            labelRes = R.string.tab_search,
            icon = Icons.Outlined.Search,
            selectedIcon = Icons.Filled.Search,
        ),
        TabDefinition(
            key = ClientBookingsRoot,
            labelRes = R.string.tab_bookings,
            icon = Icons.Outlined.CalendarMonth,
            selectedIcon = Icons.Filled.CalendarMonth,
        ),
        TabDefinition(
            key = ClientChatsRoot,
            labelRes = R.string.tab_chats,
            icon = Icons.Outlined.ChatBubbleOutline,
            selectedIcon = Icons.Filled.ChatBubble,
        ),
        TabDefinition(
            key = ClientProfileRoot,
            labelRes = R.string.tab_profile,
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person,
        ),
    )

val clientStartTab = clientTabs.first().key
