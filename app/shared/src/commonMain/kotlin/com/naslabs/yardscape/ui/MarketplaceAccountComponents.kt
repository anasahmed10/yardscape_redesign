package com.naslabs.yardscape.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val MARKETPLACE_ACCOUNT_MINIMUM_TARGET_DP = 48

internal enum class MarketplaceAccountLayout {
    Compact,
    Expanded,
}

internal enum class MarketplaceAccountSurface {
    SignedOut,
    Profile,
    Expired,
}

internal enum class MarketplaceAccountSettingsStyle {
    GroupedCard,
}

internal data class MarketplaceAccountPresentation(
    val surface: MarketplaceAccountSurface,
    val heading: String,
    val minimumInteractiveTarget: Dp = MARKETPLACE_ACCOUNT_MINIMUM_TARGET_DP.dp,
)

internal fun marketplaceAccountPresentation(state: MockAccountState): MarketplaceAccountPresentation = when (state.sessionStatus) {
    MockSessionStatus.SignedOut -> MarketplaceAccountPresentation(
        surface = MarketplaceAccountSurface.SignedOut,
        heading = "Browse without an account",
    )
    MockSessionStatus.Expired -> MarketplaceAccountPresentation(
        surface = MarketplaceAccountSurface.Expired,
        heading = "Session expired safely",
    )
    MockSessionStatus.SignedIn -> MarketplaceAccountPresentation(
        surface = MarketplaceAccountSurface.Profile,
        heading = "Your marketplace profile",
    )
}

internal fun marketplaceAccountLayoutFor(width: Dp): MarketplaceAccountLayout =
    if (width >= 760.dp) MarketplaceAccountLayout.Expanded else MarketplaceAccountLayout.Compact

internal fun marketplaceAccountContentWidthFor(availableWidth: Dp): Dp = minOf(availableWidth, 960.dp)

internal val marketplaceAccountSettingsStyle: MarketplaceAccountSettingsStyle =
    MarketplaceAccountSettingsStyle.GroupedCard
