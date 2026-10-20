package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.realtime.ProcessForeground
import kotlinx.coroutines.flow.MutableStateFlow

/** Test [ProcessForeground]: flip [isForeground] to simulate the app moving between foreground and background. */
class FakeProcessForeground(
    initial: Boolean = true,
) : ProcessForeground {
    override val isForeground = MutableStateFlow(initial)
}
