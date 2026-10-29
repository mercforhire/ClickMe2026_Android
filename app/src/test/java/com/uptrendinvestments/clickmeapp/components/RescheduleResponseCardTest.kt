package com.uptrendinvestments.clickmeapp.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.DECLINE_REASON_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleSubmitting
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class RescheduleResponseCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val toronto = ZoneId.of("America/Toronto")

    private fun proposal(
        initiatedByExpert: Boolean = true,
        note: String? = "Can we move?",
        showsPaymentDisclosure: Boolean = false,
        chargesSoon: Boolean = false,
    ) = RescheduleProposalUi(
        currentStart = Instant.parse("2026-10-24T18:00:00Z"),
        proposedStart = Instant.parse("2026-10-28T18:00:00Z"),
        proposedEnd = Instant.parse("2026-10-28T19:00:00Z"),
        note = note,
        initiatedByExpert = initiatedByExpert,
        showsPaymentDisclosure = showsPaymentDisclosure,
        chargesSoon = chargesSoon,
    )

    private fun setCard(
        proposal: RescheduleProposalUi,
        submitting: RescheduleSubmitting = RescheduleSubmitting.None,
        onAccept: () -> Unit = {},
        onDecline: (String) -> Unit = {},
        viewer: RescheduleViewer = RescheduleViewer.Client,
    ) {
        rule.setContent {
            ClickMeTheme {
                RescheduleResponseCard(
                    proposal = proposal,
                    submitting = submitting,
                    onAccept = onAccept,
                    onDecline = onDecline,
                    viewer = viewer,
                    zone = toronto,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    private fun inSheet(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(RESCHEDULE_DECLINE_SHEET_TAG))

    private fun reasonText(): String? =
        rule
            .onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.EditableText)
            ?.text

    private fun dialogContains(text: String) = hasText(text, substring = true) and hasAnyAncestor(isDialog())

    @Test
    fun expertProposalShowsTimesNoteAndActions() {
        setCard(proposal())
        rule.onNodeWithText("Expert proposed a new time").assertExists()
        rule.onNodeWithText("Current time").assertExists()
        rule.onNodeWithText("Sat, Oct 24 · 2:00 PM").assertExists()
        rule.onNodeWithText("Proposed time").assertExists()
        rule.onNodeWithText("Wed, Oct 28 · 2:00 PM").assertExists()
        rule.onNodeWithText("Expert's message").assertExists()
        rule.onNodeWithText("Can we move?").assertExists()
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertIsEnabled()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).assertIsEnabled()
    }

    @Test
    fun acceptOpensConfirmDialogAndConfirmCallsOnAccept() {
        var accepted = 0
        setCard(proposal(), onAccept = { accepted++ })
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performClick()
        rule.onNodeWithText("Accept the proposed reschedule?").assertExists()
        rule.onNodeWithText("The session will move to the new time the expert proposed.").assertExists()
        rule.onNode(inDialog("Accept")).performClick()
        assertEquals(1, accepted)
        rule.onNodeWithText("Accept the proposed reschedule?").assertDoesNotExist()
    }

    @Test
    fun acceptDialogCancelDoesNotCallOnAccept() {
        var accepted = 0
        setCard(proposal(), onAccept = { accepted++ })
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performClick()
        rule.onNode(inDialog("Cancel")).performClick()
        assertEquals(0, accepted)
        rule.onNodeWithText("Accept the proposed reschedule?").assertDoesNotExist()
    }

    @Test
    fun acceptDialogDisclosureFollowsFlags() {
        setCard(proposal(showsPaymentDisclosure = true, chargesSoon = true))
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performClick()
        rule.onNode(dialogContains("Your card hold will be released.")).assertExists()
        rule.onNode(dialogContains("may be charged sooner")).assertExists()
    }

    @Test
    fun acceptDialogShowsDisclosureWithoutChargesSoonWhenOnlyDisclosure() {
        setCard(proposal(showsPaymentDisclosure = true, chargesSoon = false))
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performClick()
        rule.onNode(dialogContains("Your card hold will be released.")).assertExists()
        rule.onNode(dialogContains("may be charged sooner")).assertDoesNotExist()
    }

    @Test
    fun chargesSoonAloneAddsNeitherParagraph() {
        setCard(proposal(showsPaymentDisclosure = false, chargesSoon = true))
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performClick()
        rule.onNode(dialogContains("Your card hold will be released.")).assertDoesNotExist()
        rule.onNode(dialogContains("may be charged sooner")).assertDoesNotExist()
    }

    @Test
    fun declineSheetPassesTypedReason() {
        val reasons = mutableListOf<String>()
        setCard(proposal(), onDecline = { reasons += it })
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNodeWithText("Decline reschedule").assertExists()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG).performTextInput("Busy")
        rule.onNodeWithText("4/500").assertExists()
        rule.onNode(inSheet("Decline")).performClick()
        assertEquals(listOf("Busy"), reasons)
        rule.onNodeWithText("Decline reschedule").assertDoesNotExist()
    }

    @Test
    fun declineSheetKeepDoesNotCallOnDecline() {
        val reasons = mutableListOf<String>()
        setCard(proposal(), onDecline = { reasons += it })
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNode(inSheet("Keep")).performClick()
        assertEquals(emptyList<String>(), reasons)
        rule.onNodeWithText("Decline reschedule").assertDoesNotExist()
    }

    @Test
    fun declineReasonIsCappedAt500() {
        setCard(proposal())
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG).performTextInput("a".repeat(600))
        val length =
            rule
                .onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.EditableText)
                ?.text
                ?.length
        assertEquals(500, length)
    }

    @Test
    fun declineOpensSheetNotDialog() {
        setCard(proposal())
        rule.onNodeWithTag(RESCHEDULE_DECLINE_SHEET_TAG).assertDoesNotExist()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_SHEET_TAG).assertExists()
        rule.onNode(inSheet("Decline reschedule")).assertExists()
        // The title lives only inside the sheet: no AlertDialog carries a second copy (UI-SPEC R7).
        rule.onAllNodes(hasText("Decline reschedule")).assertCountEquals(1)
    }

    @Test
    fun declineSheetRestoresReason() {
        val restorer = StateRestorationTester(rule)
        restorer.setContent {
            ClickMeTheme {
                RescheduleResponseCard(
                    proposal = proposal(),
                    submitting = RescheduleSubmitting.None,
                    onAccept = {},
                    onDecline = {},
                    zone = toronto,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG).performTextInput("Can't make it")

        restorer.emulateSavedInstanceStateRestore()

        rule.onNodeWithTag(RESCHEDULE_DECLINE_SHEET_TAG).assertExists()
        assertEquals("Can't make it", reasonText())
    }

    @Test
    fun restoredReasonIsCapped() {
        val restored = DeclineReasonSaver.restore("a".repeat(DECLINE_REASON_MAX_CHARS + 100))
        assertEquals(DECLINE_REASON_MAX_CHARS, restored?.length)
        assertEquals("short", DeclineReasonSaver.restore("short"))
    }

    @Test
    fun keepCloseDoesNotDecline() {
        val reasons = mutableListOf<String>()
        setCard(proposal(), onDecline = { reasons += it })
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG).performTextInput("Busy")
        rule.onNode(inSheet("Keep")).performClick()
        assertEquals(emptyList<String>(), reasons)
        rule.onNodeWithTag(RESCHEDULE_DECLINE_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun acceptingDisablesBothButtonsAndDescribesState() {
        setCard(proposal(), submitting = RescheduleSubmitting.Accepting)
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertIsNotEnabled().assert(hasStateDescription("Accepting"))
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).assertIsNotEnabled()
    }

    @Test
    fun decliningDescribesDeclineButton() {
        setCard(proposal(), submitting = RescheduleSubmitting.Declining)
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).assertIsNotEnabled().assert(hasStateDescription("Declining"))
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertIsNotEnabled()
    }

    @Test
    fun clientProposalIsReadOnly() {
        setCard(proposal(initiatedByExpert = false, note = "Please"))
        rule.onNodeWithText("You proposed a new time").assertExists()
        rule.onNodeWithText("Your message").assertExists()
        rule.onNodeWithText("Awaiting expert's response").assertExists()
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertDoesNotExist()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).assertDoesNotExist()
    }

    @Test
    fun noteBlockIsHiddenWhenThereIsNoNote() {
        setCard(proposal(note = null))
        rule.onNodeWithText("Expert's message").assertDoesNotExist()
    }

    // ---- Expert viewer (Phase 11, D-18) ----

    @Test
    fun expertViewer_clientProposal_isActionable() {
        setCard(proposal(initiatedByExpert = false, note = "Please"), viewer = RescheduleViewer.Expert)
        rule.onNodeWithText("Client proposed a new time").assertExists()
        rule.onNodeWithText("Client's message").assertExists()
        rule.onNodeWithText("Please").assertExists()
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertIsEnabled()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).assertIsEnabled()
        rule.onNodeWithText("Awaiting client's response").assertDoesNotExist()
    }

    @Test
    fun expertViewer_ownProposal_isReadOnly() {
        setCard(proposal(initiatedByExpert = true, note = "Sorry"), viewer = RescheduleViewer.Expert)
        rule.onNodeWithText("You proposed a new time").assertExists()
        rule.onNodeWithText("Your message").assertExists()
        rule.onNodeWithText("Awaiting client's response").assertExists()
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertDoesNotExist()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).assertDoesNotExist()
    }

    @Test
    fun expertViewer_acceptDialog_hasNoPaymentText() {
        var accepted = 0
        setCard(
            proposal(initiatedByExpert = false, showsPaymentDisclosure = true, chargesSoon = true),
            onAccept = { accepted++ },
            viewer = RescheduleViewer.Expert,
        )
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performClick()
        rule.onNodeWithText("Accept the proposed reschedule?").assertExists()
        rule.onNodeWithText("The session will move to the new time your client proposed.").assertExists()
        rule.onNode(dialogContains("Your card hold will be released.")).assertDoesNotExist()
        rule.onNode(dialogContains("may be charged sooner")).assertDoesNotExist()
        rule.onNode(inDialog("Accept")).performClick()
        assertEquals(1, accepted)
    }

    @Test
    fun expertViewer_declineSheet_keepLabel() {
        val reasons = mutableListOf<String>()
        setCard(proposal(initiatedByExpert = false), onDecline = { reasons += it }, viewer = RescheduleViewer.Expert)
        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performClick()
        rule.onNode(inSheet("Keep session as-is")).assertExists()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG).performTextInput("a".repeat(600))
        val length =
            rule
                .onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.EditableText)
                ?.text
                ?.length
        assertEquals(500, length)
        rule.onNode(inSheet("Decline")).performClick()
        assertEquals(listOf("a".repeat(500)), reasons)
    }

    private fun setOutcome(
        dialog: RescheduleDialog,
        events: MutableList<String>,
    ) {
        rule.setContent {
            ClickMeTheme {
                RescheduleOutcomeDialog(
                    dialog = dialog,
                    onDismiss = { events += "dismiss" },
                    onAddPaymentMethod = { events += "add" },
                )
            }
        }
    }

    @Test
    fun paymentMethodNeededAddCallsAddThenDismiss() {
        val events = mutableListOf<String>()
        setOutcome(RescheduleDialog.PaymentMethodNeeded, events)
        rule.onNodeWithText("Add a payment method").assertExists()
        rule.onNodeWithText("Please add a payment method before accepting this reschedule.").assertExists()
        rule.onNodeWithText("Add a Payment Method").performClick()
        assertEquals(listOf("add", "dismiss"), events)
    }

    @Test
    fun paymentMethodNeededNotNowOnlyDismisses() {
        val events = mutableListOf<String>()
        setOutcome(RescheduleDialog.PaymentMethodNeeded, events)
        rule.onNodeWithText("Not now").performClick()
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun cardUnusableShowsItsMessageAndSameButtons() {
        val events = mutableListOf<String>()
        setOutcome(RescheduleDialog.CardUnusable, events)
        rule
            .onNodeWithText(
                "The card on file can't be used for this reschedule. Please add a new payment method and try again.",
            ).assertExists()
        rule.onNodeWithText("Add a Payment Method").assertExists()
        rule.onNodeWithText("Not now").assertExists()
    }

    @Test
    fun errorDialogShowsTitleMessageAndOkDismisses() {
        val events = mutableListOf<String>()
        setOutcome(RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_not_pending)), events)
        rule.onNodeWithText("Couldn't update booking").assertExists()
        rule.onNodeWithText("This reschedule is no longer pending.").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
