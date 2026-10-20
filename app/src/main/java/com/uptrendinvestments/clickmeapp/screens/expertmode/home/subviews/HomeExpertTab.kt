package com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.navigation.TabDefinition
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoot : NavKey

@Serializable
data object ExpertBookingsRoot : NavKey

@Serializable
data object TopicsRoot : NavKey

@Serializable
data object ExpertChatsRoot : NavKey

@Serializable
data object ExpertProfileRoot : NavKey

/** Expert shell tabs, in iOS HomeExpertTab order. */
val expertTabs: List<TabDefinition> =
    listOf(
        TabDefinition(
            key = DashboardRoot,
            labelRes = R.string.tab_dashboard,
            icon = Icons.Outlined.Home,
            selectedIcon = Icons.Filled.Home,
        ),
        TabDefinition(
            key = ExpertBookingsRoot,
            labelRes = R.string.tab_bookings,
            icon = Icons.Outlined.CalendarMonth,
            selectedIcon = Icons.Filled.CalendarMonth,
        ),
        TabDefinition(
            key = TopicsRoot,
            labelRes = R.string.tab_topics,
            icon = Icons.Outlined.EditNote,
            selectedIcon = Icons.Filled.EditNote,
        ),
        TabDefinition(
            key = ExpertChatsRoot,
            labelRes = R.string.tab_chats,
            icon = Icons.Outlined.ChatBubbleOutline,
            selectedIcon = Icons.Filled.ChatBubble,
        ),
        TabDefinition(
            key = ExpertProfileRoot,
            labelRes = R.string.tab_profile,
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person,
        ),
    )

val expertStartTab = expertTabs.first().key
