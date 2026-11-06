package com.uptrendinvestments.clickmeapp.config

/** Static external links (port of iOS `AppLinks`). Not switched per environment. */
object AppLinks {
    const val PRIVACY_POLICY = "https://www.uptrendinvestments.net/clickme/privacy.html"

    /**
     * Support contact. iOS HelpView has `support@clickme.app`, which the owner
     * confirmed is wrong (2026-11-06); this matches the published privacy policy.
     */
    const val SUPPORT_EMAIL = "clickmeapp@uptrendinvestments.net"
}
