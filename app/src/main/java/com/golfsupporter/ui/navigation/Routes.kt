package com.golfsupporter.ui.navigation

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val HISTORY = "history"
    const val ROUND = "round/{sessionId}"
    const val INTERSTITIAL = "interstitial/{sessionId}"
    const val RESULT = "result/{sessionId}"

    fun round(sessionId: String) = "round/$sessionId"
    fun interstitial(sessionId: String) = "interstitial/$sessionId"
    fun result(sessionId: String) = "result/$sessionId"

    const val ARG_SESSION_ID = "sessionId"
}
