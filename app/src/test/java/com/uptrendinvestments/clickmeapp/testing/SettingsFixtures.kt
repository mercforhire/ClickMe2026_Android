package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionsData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertPersonalInfo
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfessionalDetails
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.FaqArticlePreview
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PaginationMeta
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileAddress
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import java.time.Instant

/*
 * Phase 14 payload builders shared by the settings, payouts and help view-model tests. Defaults mirror a realistic
 * server body; override only what a test cares about.
 */

/** The caller's own profile as `GET /user/profile` returns it. */
fun userProfileData(
    firstName: String? = "Ana",
    lastName: String? = "Lee",
    email: String? = "ana@example.com",
    bio: String? = "",
    jobTitle: String? = null,
    company: String? = null,
    city: String? = null,
    stateProvince: String? = null,
    country: String? = "CAN",
    languages: List<UserProfileData.LanguageEntry> =
        listOf(UserProfileData.LanguageEntry(id = "en", label = "English")),
): UserProfileData =
    UserProfileData(
        personalDetails =
            UserProfileData.PersonalDetails(
                firstName = firstName,
                lastName = lastName,
                email = email,
                bio = bio,
            ),
        professionalDetails = UserProfileData.ProfessionalDetails(jobTitle = jobTitle, company = company),
        location = UserProfileData.Location(city = city, stateProvince = stateProvince, country = country),
        languages = languages,
    )

/** The expert's own profile as `GET /expert/profile` returns it, with the Phase 14 meeting link. */
fun expertProfileData(
    meetingUrl: String? = null,
    tags: List<ExpertTag> = listOf(ExpertTag(id = "tax", label = "Tax", isPrimary = true)),
): ExpertProfileData =
    ExpertProfileData(
        setupCompleted = true,
        profileId = "p1",
        personalInfo = ExpertPersonalInfo(firstName = "Ana", lastName = "Lee", email = "ana@example.com", bio = ""),
        expertiseTags = tags,
        professionalDetails = ExpertProfessionalDetails(meetingUrl = meetingUrl),
    )

/** One earnings ledger row; amounts are MINOR units. Gross and commission are derived from [net]. */
fun earningsTransaction(
    id: String = "e1",
    net: Long = 6375L,
    currency: String = "CAD",
    status: String? = "settled_direct",
    topic: String? = "Career Advice",
    client: String? = "Liam Carter",
    sessionDate: Instant? = Instant.parse("2026-07-30T14:00:00Z"),
    createdAt: Instant = Instant.parse("2026-07-30T10:00:00Z"),
): EarningsTransactionItem =
    EarningsTransactionItem(
        id = id,
        bookingId = "b-$id",
        sessionDate = sessionDate,
        topicTitle = topic,
        clientName = client,
        grossAmount = net + net / 5,
        commissionAmount = net / 5,
        netAmount = net,
        currency = currency,
        payoutStatus = status,
        processedAt = null,
        createdAt = createdAt,
    )

/** One page of the ledger. */
fun transactionsPage(
    items: List<EarningsTransactionItem> = listOf(earningsTransaction()),
    page: Int = 1,
    hasMore: Boolean = false,
): EarningsTransactionsData =
    EarningsTransactionsData(
        transactions = items,
        pagination =
            PaginationMeta(
                currentPage = page,
                totalPages = if (hasMore) page + 1 else page,
                totalCount = items.size,
                hasMore = hasMore,
            ),
    )

/** The payout summary header; amounts are MINOR units. */
fun payoutSummary(
    available: Long = 12_000L,
    pending: Long = 0L,
    currency: String = "CAD",
    nextPayoutDate: String? = null,
    payoutMethod: String? = "bank_transfer",
): PayoutSummaryData =
    PayoutSummaryData(
        availableAmount = available,
        pendingAmount = pending,
        currency = currency,
        nextPayoutDate = nextPayoutDate,
        payoutMethod = payoutMethod,
    )

/** Stripe Connect status. */
fun connectStatus(
    chargesEnabled: Boolean = true,
    currentlyDue: List<String> = emptyList(),
): ConnectStatusData = ConnectStatusData(chargesEnabled = chargesEnabled, currentlyDue = currentlyDue)

/** The tax profile; the defaults are the server's all-empty body. */
fun taxProfile(
    tinOnFile: Boolean = false,
    legalName: String? = null,
    address: TaxProfileAddress? = null,
    dob: String? = null,
    financialAccountIdentifier: String? = null,
): TaxProfileData =
    TaxProfileData(
        tinOnFile = tinOnFile,
        legalName = legalName,
        address = address,
        dob = dob,
        financialAccountIdentifier = financialAccountIdentifier,
    )

fun faqArticle(
    id: String = "a1",
    question: String = "How do I get paid?",
    snippet: String? = null,
): FaqArticlePreview = FaqArticlePreview(id = id, question = question, snippet = snippet)

fun faqCategory(
    id: String = "c1",
    title: String = "Payments",
    order: Int = 1,
    articles: List<FaqArticlePreview> = emptyList(),
): FaqCategory = FaqCategory(id = id, title = title, displayOrder = order, featuredArticles = articles)

fun faqsPayload(vararg categories: FaqCategory): FaqsPayload = FaqsPayload(categories = categories.toList())
