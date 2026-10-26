@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.requests.CreateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.TopicHourlyRateBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MinSessionAmount
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicPayload
import com.uptrendinvestments.clickmeapp.model.responses.PricingLimits
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertiseTagsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.ExpertTopicsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicEvents
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicSavedKind
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import com.uptrendinvestments.clickmeapp.testing.topicItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class TopicEditorViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun meWith(default: String = "CAD") =
        MeData(
            id = "u1",
            email = "a@b.co",
            roles = listOf("expert"),
            pricingLimits =
                PricingLimits(
                    MinSessionAmount(
                        byCurrency = mapOf("CAD" to 500, "USD" to 500),
                        defaultMinorUnits = 500,
                        defaultCurrency = default,
                    ),
                ),
        )

    private fun tag(
        id: String,
        label: String,
    ) = ExpertiseTagItem(id, label)

    private inner class Env(
        scope: TestScope,
    ) {
        val api = FakeClickMeApi()
        val events = TopicEvents()
        val userManager =
            UserManager(
                api,
                FakeSecureTokenStore(TokenPair("access-1", "refresh-1", 1_900_000_000L)),
                SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp)),
                SessionEvents(),
                PushTokenRegistrar(),
                scope.backgroundScope,
                main.testDispatcher,
            )

        init {
            api.expertProfileHandler = { profile("tax" to "Tax") }
        }

        fun profile(vararg tags: Pair<String, String>) =
            ExpertProfileData(setupCompleted = true, expertiseTags = tags.map { ExpertTag(it.first, it.second) })

        /** Puts [me] into the in-memory `/me` through the real refresh path. */
        suspend fun loadMe(me: MeData = meWith()) {
            api.meHandler = { me }
            userManager.refreshMe()
        }

        fun vm(
            key: TopicEditorKey = TopicEditorKey.newTopic(),
            handle: SavedStateHandle = SavedStateHandle(),
        ) = TopicEditorViewModel(
            key,
            handle,
            ExpertTopicsRepository(api),
            ExpertiseTagsRepository(api),
            userManager,
            events,
        )
    }

    private fun editKey(
        amount: Long = 1250,
        currency: String = "CAD",
        isFree: Boolean = false,
        duration: Int? = 90,
        icon: String? = "finance",
        description: String? = "D",
        tags: List<ExpertiseTagItem> = emptyList(),
    ) = TopicEditorKey.forTopic(
        topicItem(
            amount = amount,
            currency = currency,
            isFree = isFree,
            durationMins = duration,
            iconSlug = icon,
            description = description,
            tags = tags,
        ),
    )

    // ---- seeding ----

    @Test
    fun editSeedFillsEveryFieldAndFormatsThePrice() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm(editKey())
            advanceUntilIdle()
            val s = vm.uiState.value
            assertTrue(s.isEdit)
            assertEquals("Tax planning", s.title)
            assertEquals("D", s.description)
            assertEquals(90, s.durationMins)
            assertEquals("12.50", s.priceText)
            assertEquals("CAD", s.currency)
            assertEquals("finance", s.iconSlug)
            assertEquals(500, s.floorMinor)
        }

    @Test
    fun aFreeSeedShowsABlankPrice() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm(editKey(isFree = true))
            advanceUntilIdle()
            assertEquals("", vm.uiState.value.priceText)
        }

    @Test
    fun aNonCuratedSeedIconLeadsTheIconOptions() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm(editKey(icon = "real-estate"))
            advanceUntilIdle()
            assertEquals(
                "real-estate",
                vm.uiState.value.iconOptions
                    .first(),
            )
        }

    @Test
    fun newTopicStartsEmptyWithSixtyMinutesAndTheDefaultCurrency() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe(meWith("USD"))
            val vm = env.vm()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertFalse(s.isEdit)
            assertEquals(60, s.durationMins)
            assertEquals("", s.priceText)
            assertEquals("USD", s.currency)
        }

    @Test
    fun newTopicFallsBackToCadWhenMeCannotLoad() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.api.meHandler = { throw env.api.httpError(503, """{"message":"down"}""") }
            val vm = env.vm()
            advanceUntilIdle()
            assertEquals("CAD", vm.uiState.value.currency)
            assertNull(vm.uiState.value.floorMinor)
        }

    @Test
    fun newTopicCurrencyFollowsMeWhenItArrivesLater() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.api.meHandler = { meWith("USD") }
            val vm = env.vm()
            assertEquals("CAD", vm.uiState.value.currency)
            advanceUntilIdle()
            assertEquals("USD", vm.uiState.value.currency)
            assertEquals(500, vm.uiState.value.floorMinor)
        }

    @Test
    fun anEditedTopicKeepsItsCurrencyWhenMeArrives() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.api.meHandler = { meWith("USD") }
            val vm = env.vm(editKey(currency = "CAD"))
            advanceUntilIdle()
            assertEquals("CAD", vm.uiState.value.currency)
        }

    @Test
    fun meIsNotRefreshedWhenItAlreadyHoldsLimits() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            assertEquals(1, env.api.meCalls)
            env.vm()
            advanceUntilIdle()
            assertEquals(1, env.api.meCalls)
        }

    @Test
    fun meIsRefreshedOnceWhenLimitsAreMissing() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.api.meHandler = { meWith() }
            val vm = env.vm()
            advanceUntilIdle()
            assertEquals(1, env.api.meCalls)
            assertEquals(500, vm.uiState.value.floorMinor)
        }

    // ---- price floor ----

    @Test
    fun aPaidPriceBelowTheFloorShowsLiveAndClearsWhenRaised() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm()
            advanceUntilIdle()
            vm.onPriceChange("4.99")
            assertEquals(PriceFieldError.BelowFloor(500, "CAD"), vm.uiState.value.priceError)
            vm.onPriceChange("5")
            assertNull(vm.uiState.value.priceError)
            vm.onPriceChange("0")
            assertNull(vm.uiState.value.priceError)
            vm.onPriceChange("")
            assertNull(vm.uiState.value.priceError)
        }

    @Test
    fun priceInputIsFilteredToTheCurrencyDigits() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm()
            advanceUntilIdle()
            vm.onPriceChange("12,505")
            assertEquals("12,50", vm.uiState.value.priceText)
        }

    @Test
    fun withNoFloorTheLiveCheckIsSkipped() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.api.meHandler = { throw env.api.httpError(503, """{"message":"down"}""") }
            val vm = env.vm()
            advanceUntilIdle()
            vm.onPriceChange("1")
            assertNull(vm.uiState.value.priceError)
        }

    // ---- duration and icon ----

    @Test
    fun durationStepsByThirtyWithinTheRange() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm()
            advanceUntilIdle()
            vm.increaseDuration()
            assertEquals(90, vm.uiState.value.durationMins)

            val low = env.vm(editKey(duration = 30))
            low.decreaseDuration()
            assertEquals(30, low.uiState.value.durationMins)

            val high = env.vm(editKey(duration = 480))
            high.increaseDuration()
            assertEquals(480, high.uiState.value.durationMins)
        }

    @Test
    fun aStoredDurationAboveTheCapIsShownAndKeptOnAnUnrelatedEdit() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.updateTopicHandler = { _, _ -> saved() }
            val vm = readyVm(env, editKey(duration = 600))
            assertEquals(600, vm.uiState.value.durationMins)
            vm.onTitleChange("Tax planning 2")
            vm.save()
            advanceUntilIdle()
            val (_, request) = env.api.updateTopicRequests.single()
            assertNull(request.durationMins)
        }

    @Test
    fun anOffGridStoredDurationIsKeptUntilTheUserStepsIt() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.updateTopicHandler = { _, _ -> saved() }
            val vm = readyVm(env, editKey(duration = 45))
            assertEquals(45, vm.uiState.value.durationMins)
            vm.increaseDuration()
            assertEquals(60, vm.uiState.value.durationMins)
            vm.save()
            advanceUntilIdle()
            val (_, request) = env.api.updateTopicRequests.single()
            assertEquals(60, request.durationMins)
        }

    @Test
    fun tappingTheSelectedIconClearsIt() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm()
            advanceUntilIdle()
            vm.toggleIcon("tech")
            assertEquals("tech", vm.uiState.value.iconSlug)
            vm.toggleIcon("tech")
            assertNull(vm.uiState.value.iconSlug)
        }

    // ---- tags ----

    @Test
    fun profileTagsLoadSortedAndSelectionsNotOnTheProfileAreDropped() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.expertProfileHandler = { env.profile("tax" to "Tax", "audit" to "Audit", "law" to "Law") }
            val vm = env.vm(editKey(tags = listOf(tag("tax", "Tax"), tag("gone", "Gone"))))
            assertEquals(TopicTagsLoad.Loading, vm.uiState.value.tags)
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(listOf("Audit", "Law", "Tax"), s.profileTags.map { it.label })
            assertTrue(s.tags is TopicTagsLoad.Loaded)
            assertEquals(listOf("tax"), s.selectedTags.map { it.id })
        }

    @Test
    fun aFourthTagIsIgnored() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.expertProfileHandler = { env.profile("a" to "A", "b" to "B", "c" to "C", "d" to "D") }
            val vm = env.vm()
            advanceUntilIdle()
            listOf("a" to "A", "b" to "B", "c" to "C", "d" to "D").forEach { vm.toggleTag(tag(it.first, it.second)) }
            assertEquals(
                listOf("a", "b", "c"),
                vm.uiState.value.selectedTags
                    .map { it.id },
            )
            assertTrue(vm.uiState.value.atCap)
        }

    @Test
    fun theTagFilterKeepsSelectedTagsVisible() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.expertProfileHandler = { env.profile("tax" to "Tax", "audit" to "Audit", "law" to "Law") }
            val vm = env.vm(editKey(tags = listOf(tag("tax", "Tax"))))
            advanceUntilIdle()
            vm.onTagFilterChange("au")
            assertEquals(
                listOf("Tax", "Audit"),
                vm.uiState.value.visibleTags
                    .map { it.label },
            )
        }

    @Test
    fun retryTagsReloadsAfterAFailedLoad() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.expertProfileHandler = { throw env.api.httpError(500, """{"message":"boom"}""") }
            val vm = env.vm(editKey(tags = listOf(tag("tax", "Tax"))))
            advanceUntilIdle()
            assertEquals(TopicTagsLoad.Failed, vm.uiState.value.tags)
            assertEquals(
                listOf("tax"),
                vm.uiState.value.selectedTags
                    .map { it.id },
            )

            env.api.expertProfileHandler = { env.profile("tax" to "Tax") }
            vm.retryTags()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.tags is TopicTagsLoad.Loaded)
        }

    private fun selectedIds(vm: TopicEditorViewModel): List<String> {
        val selected = vm.uiState.value.selectedTags
        return selected.map { it.id }
    }

    @Test
    fun aTagRemovedWhileTheProfileTagsFailedIsNotSent() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.expertProfileHandler = { throw env.api.httpError(500, """{"message":"boom"}""") }
            env.api.updateTopicHandler = { _, _ -> saved() }
            val vm = readyVm(env, editKey(tags = listOf(tag("tax", "Tax"), tag("law", "Law"))))
            assertEquals(TopicTagsLoad.Failed, vm.uiState.value.tags)
            assertEquals(listOf("tax", "law"), selectedIds(vm))
            vm.toggleTag(tag("law", "Law"))
            assertEquals(listOf("tax"), selectedIds(vm))
            vm.save()
            advanceUntilIdle()
            val (_, request) = env.api.updateTopicRequests.single()
            assertEquals(listOf("tax"), request.expertiseTagIds)
        }

    @Test
    fun tagsSavedInTheExpertiseSheetBecomeTheAvailableTags() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = env.vm(editKey(tags = listOf(tag("tax", "Tax"))))
            advanceUntilIdle()
            vm.onExpertiseSaved(listOf(tag("law", "Law"), tag("new", "New")))
            val s = vm.uiState.value
            assertEquals(listOf("Law", "New"), s.profileTags.map { it.label })
            assertTrue(s.selectedTags.isEmpty())
        }

    // ---- saved state ----

    @Test
    fun savedStateOverridesTheSeedAndEditsWriteBack() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val handle =
                SavedStateHandle(
                    mapOf(
                        TopicEditorViewModel.KEY_TITLE to "Restored",
                        TopicEditorViewModel.KEY_PRICE to "7",
                    ),
                )
            val vm = env.vm(editKey(), handle)
            advanceUntilIdle()
            assertEquals("Restored", vm.uiState.value.title)
            assertEquals("7", vm.uiState.value.priceText)

            vm.onTitleChange("Edited")
            vm.onPriceChange("9.5")
            assertEquals("Edited", handle.get<String>(TopicEditorViewModel.KEY_TITLE))
            assertEquals("9.5", handle.get<String>(TopicEditorViewModel.KEY_PRICE))
        }

    @Test
    fun restoredValuesPassThroughTheSameFilters() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val handle = SavedStateHandle(mapOf(TopicEditorViewModel.KEY_PRICE to "1x2.999"))
            val vm = env.vm(handle = handle)
            advanceUntilIdle()
            assertEquals("12.99", vm.uiState.value.priceText)
        }

    // ---- save ----

    private fun apiError(
        status: Int,
        code: String,
        message: String,
        errors: String = "",
    ) = FakeClickMeApi().httpError(
        status,
        """{"status":"error","code":"$code","message":"$message"$errors}""",
    )

    private fun saved(id: String = "t1") = MyExpertTopicPayload(topicItem(id = id))

    private fun TestScope.readyVm(
        env: Env,
        key: TopicEditorKey = TopicEditorKey.newTopic(),
    ): TopicEditorViewModel {
        val vm = env.vm(key)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun aBlankTitleShowsTheRequiredErrorAndSendsNothing() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = readyVm(env)
            vm.save()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.topic_editor_required_error), s.titleError)
            assertEquals(EditorField.Title, s.focusTarget)
            assertEquals(0, env.api.createTopicCalls)
            vm.onFocusHandled()
            assertNull(vm.uiState.value.focusTarget)
            vm.onTitleChange("x")
            assertNull(vm.uiState.value.titleError)
        }

    @Test
    fun anInvalidPriceFocusesThePriceField() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.onPriceChange(".")
            assertNull(vm.uiState.value.priceError)
            vm.save()
            advanceUntilIdle()
            assertEquals(PriceFieldError.Invalid, vm.uiState.value.priceError)
            assertEquals(EditorField.Price, vm.uiState.value.focusTarget)
            assertEquals(0, env.api.createTopicCalls)
        }

    @Test
    fun aBelowFloorPriceBlocksSaveAndTitleWinsTheFocus() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val vm = readyVm(env)
            vm.onPriceChange("4.99")
            vm.save()
            advanceUntilIdle()
            assertEquals(PriceFieldError.BelowFloor(500, "CAD"), vm.uiState.value.priceError)
            assertEquals(EditorField.Title, vm.uiState.value.focusTarget)
            vm.onTitleChange("Tax")
            vm.save()
            advanceUntilIdle()
            assertEquals(EditorField.Price, vm.uiState.value.focusTarget)
            assertEquals(0, env.api.createTopicCalls)
        }

    @Test
    fun aFreeNewTopicSendsTheFloorAsTheWireAmount() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.expertProfileHandler = { env.profile("TAX" to "Tax") }
            env.api.createTopicHandler = { saved() }
            val vm = readyVm(env)
            vm.onTitleChange("  Tax planning  ")
            vm.onDescriptionChange("   ")
            vm.toggleIcon("finance")
            vm.toggleTag(tag("TAX", "Tax"))
            vm.save()
            advanceUntilIdle()
            assertEquals(
                listOf(
                    CreateExpertTopicRequest(
                        title = "Tax planning",
                        description = null,
                        durationMins = 60,
                        isFree = true,
                        hourlyRate = TopicHourlyRateBody(500, "CAD"),
                        iconSlug = "finance",
                        expertiseTagIds = listOf("tax"),
                    ),
                ),
                env.api.createTopicRequests,
            )
        }

    @Test
    fun withNoLimitsAFreeTopicSendsOneHundred() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.api.meHandler = { throw env.api.httpError(503, """{"message":"down"}""") }
            env.api.createTopicHandler = { saved() }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.save()
            advanceUntilIdle()
            assertEquals(
                TopicHourlyRateBody(100, "CAD"),
                env.api.createTopicRequests
                    .single()
                    .hourlyRate,
            )
        }

    @Test
    fun aPaidNewTopicSendsTheParsedAmount() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.createTopicHandler = { saved() }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.onPriceChange("12.50")
            vm.save()
            advanceUntilIdle()
            val request = env.api.createTopicRequests.single()
            assertEquals(TopicHourlyRateBody(1250, "CAD"), request.hourlyRate)
            assertFalse(request.isFree)
        }

    @Test
    fun editSendsAFullBodyWithClearedFieldsAndKeepsItsCurrency() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe(meWith("CAD"))
            env.api.updateTopicHandler = { _, _ -> saved() }
            val vm = readyVm(env, editKey(currency = "USD"))
            vm.toggleIcon("finance")
            vm.onDescriptionChange("")
            vm.save()
            advanceUntilIdle()
            val expected =
                UpdateExpertTopicRequest(
                    title = "Tax planning",
                    description = null,
                    durationMins = null,
                    isFree = false,
                    hourlyRate = TopicHourlyRateBody(1250, "USD"),
                    iconSlug = null,
                    expertiseTagIds = emptyList(),
                )
            assertEquals(listOf("t1" to expected), env.api.updateTopicRequests)
            assertEquals(0, env.api.createTopicCalls)
        }

    @Test
    fun anUnchangedDurationIsLeftOutOfThePatch() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.updateTopicHandler = { _, _ -> saved() }
            val vm = readyVm(env, editKey(duration = 90))
            vm.onTitleChange("Tax planning 2")
            vm.save()
            advanceUntilIdle()
            val (_, request) = env.api.updateTopicRequests.single()
            assertNull(request.durationMins)
        }

    @Test
    fun aChangedDurationIsSentInThePatch() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.updateTopicHandler = { _, _ -> saved() }
            val vm = readyVm(env, editKey(duration = 90))
            vm.increaseDuration()
            vm.save()
            advanceUntilIdle()
            val (_, request) = env.api.updateTopicRequests.single()
            assertEquals(120, request.durationMins)
        }

    @Test
    fun successEmitsTheSavedEventAndFinishes() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val gate = CompletableDeferred<MyExpertTopicPayload>()
            env.api.createTopicHandler = { gate.await() }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.save()
            runCurrent()
            assertTrue(vm.uiState.value.isSaving)
            assertFalse(vm.uiState.value.finished)
            gate.complete(saved("new1"))
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isSaving)
            assertTrue(vm.uiState.value.finished)
            val event = env.events.events.first()
            assertEquals(TopicSavedKind.Added, event.kind)
            assertEquals("new1", event.topic.id)
        }

    @Test
    fun editSuccessEmitsAnUpdatedEvent() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.updateTopicHandler = { _, _ -> saved("t1") }
            val vm = readyVm(env, editKey())
            vm.save()
            advanceUntilIdle()
            assertEquals(
                TopicSavedKind.Updated,
                env.events.events
                    .first()
                    .kind,
            )
            assertTrue(vm.uiState.value.finished)
        }

    @Test
    fun aSecondSaveWhileOneIsInFlightSendsNothingAndEditsAreIgnored() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val gate = CompletableDeferred<MyExpertTopicPayload>()
            env.api.createTopicHandler = { gate.await() }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.save()
            runCurrent()
            vm.save()
            vm.onTitleChange("Other")
            vm.onPriceChange("9")
            runCurrent()
            assertEquals(1, env.api.createTopicCalls)
            assertEquals("Tax", vm.uiState.value.title)
            assertEquals("", vm.uiState.value.priceText)
            gate.complete(saved())
            advanceUntilIdle()
        }

    @Test
    fun priceBelowMinimumFromTheServerLandsOnThePriceField() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.createTopicHandler = {
                throw apiError(422, "PRICE_BELOW_MINIMUM", "A paid session must be at least CA\$5.")
            }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.onPriceChange("6")
            vm.save()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(PriceFieldError.Server("A paid session must be at least CA\$5."), s.priceError)
            assertEquals(EditorField.Price, s.focusTarget)
            assertFalse(s.finished)
            assertFalse(s.isSaving)
            assertNull(s.dialog)
            vm.onPriceChange("7")
            assertNull(vm.uiState.value.priceError)
        }

    @Test
    fun aValidationErrorOpensTheDialogWithTheFieldMessageAndKeepsTheFields() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            val text = "One or more expertise tags are not in your profile"
            val fieldErrors = ""","errors":[{"field":"expertise_tag_ids","message":"$text"}]"""
            env.api.createTopicHandler = { throw apiError(422, "VALIDATION_ERROR", "Validation failed", fieldErrors) }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.onPriceChange("6")
            vm.save()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(FieldError.Text(text), s.dialog)
            assertEquals("Tax", s.title)
            assertEquals("6", s.priceText)
            assertFalse(s.finished)
            assertFalse(s.isSaving)
            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun aNetworkFailureOpensTheNetworkDialog() =
        runTest(main.testDispatcher) {
            val env = Env(this)
            env.loadMe()
            env.api.createTopicHandler = { throw IOException("offline") }
            val vm = readyVm(env)
            vm.onTitleChange("Tax")
            vm.save()
            advanceUntilIdle()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialog)
            assertFalse(vm.uiState.value.isSaving)
        }
}
