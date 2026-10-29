package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R

/**
 * The four reasons the backend accepts as `reason_category` on `DELETE user/account`. Ports iOS
 * `DeleteAccountReason`; [wire] is the exact server value. None is preselected on the form.
 */
enum class DeleteAccountReason(
    val wire: String,
    @StringRes val label: Int,
) {
    TooExpensive("too_expensive", R.string.delete_account_reason_too_expensive),
    FoundAlternative("found_alternative", R.string.delete_account_reason_found_alternative),
    PrivacyConcerns("privacy_concerns", R.string.delete_account_reason_privacy_concerns),
    Other("other", R.string.delete_account_reason_other),
}
