package com.naslabs.yardscape.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naslabs.yardscape.data.PublicEventDetail
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import yardscape.app.shared.generated.resources.Res
import yardscape.app.shared.generated.resources.marketplace_book_market
import yardscape.app.shared.generated.resources.marketplace_clothing_market
import yardscape.app.shared.generated.resources.marketplace_flea_market
import yardscape.app.shared.generated.resources.marketplace_furniture_market
import yardscape.app.shared.generated.resources.marketplace_garage_sale
import yardscape.app.shared.generated.resources.marketplace_swap_market

internal enum class ShopperMarketplaceLayout {
    Compact,
    Expanded,
}

internal fun shopperMarketplaceLayoutFor(width: Dp): ShopperMarketplaceLayout =
    if (width >= 760.dp) ShopperMarketplaceLayout.Expanded else ShopperMarketplaceLayout.Compact

internal enum class ShopperArtworkResource(
    val resourceName: String,
    val contentDescription: String,
) {
    GarageSale(
        resourceName = "marketplace_garage_sale",
        contentDescription = "Table filled with second-hand kitchenware and toys",
    ),
    SwapMarket(
        resourceName = "marketplace_swap_market",
        contentDescription = "People browse kitchenware at a community swap market",
    ),
    FleaMarket(
        resourceName = "marketplace_flea_market",
        contentDescription = "Shoppers browse vintage furniture at an outdoor market",
    ),
    FurnitureMarket(
        resourceName = "marketplace_furniture_market",
        contentDescription = "Outdoor sale with chairs, tables, and vintage goods",
    ),
    ClothingMarket(
        resourceName = "marketplace_clothing_market",
        contentDescription = "Colorful clothing and household goods at an outdoor market",
    ),
    BookMarket(
        resourceName = "marketplace_book_market",
        contentDescription = "Books arranged for browsing at a flea market",
    ),
}

internal data class ShopperArtwork(
    val resource: ShopperArtworkResource,
    val contentDescription: String = resource.contentDescription,
)

internal data class ShopperEventArtworkPresentation(
    val eventId: String,
    val photoReference: String?,
) {
    val artwork: ShopperArtwork
        get() = shopperArtworkFor(eventId = eventId, photoReference = photoReference)
}

internal fun BrowseEventItem.toShopperEventArtworkPresentation(): ShopperEventArtworkPresentation =
    ShopperEventArtworkPresentation(eventId = id, photoReference = photoReference)

internal fun PublicEventDetail.toShopperEventArtworkPresentation(): ShopperEventArtworkPresentation =
    ShopperEventArtworkPresentation(eventId = id, photoReference = photos.firstOrNull()?.url)

/** Uses the same local artwork mapping as a shopper-facing event before publishing. */
internal fun HostPublicPreview.toShopperEventArtworkPresentation(eventId: String): ShopperEventArtworkPresentation =
    hostArtworkPresentationFor(draftId = eventId, photoReference = photoReferences.firstOrNull())

/**
 * Keeps host picker, selected-photo, and preview artwork stable while a draft is reordered.
 * The fallback draft key is intentionally independent of a list position or UI section.
 */
internal fun hostArtworkPresentationFor(
    draftId: String?,
    photoReference: String?,
): ShopperEventArtworkPresentation = ShopperEventArtworkPresentation(
    eventId = draftId ?: "new-host-draft",
    photoReference = photoReference,
)

/**
 * Resolves public event photo references to bundled artwork. No remote media is loaded from this
 * component, so an event's public surface never makes a network request or exposes its source URL.
 */
internal fun shopperArtworkFor(eventId: String, photoReference: String?): ShopperArtwork {
    val resource = when (photoReference) {
        "seed://maple-ridge-driveway",
        "mock://host-photo/driveway",
        -> ShopperArtworkResource.GarageSale

        "seed://marin-tools-records",
        "mock://host-photo/tools",
        -> ShopperArtworkResource.FleaMarket

        "mock://host-photo/furniture" -> ShopperArtworkResource.FurnitureMarket
        "mock://host-photo/kids" -> ShopperArtworkResource.ClothingMarket
        else -> ShopperArtworkResource.entries[stableArtworkIndex(eventId, photoReference)]
    }
    return ShopperArtwork(resource = resource)
}

private fun stableArtworkIndex(eventId: String, photoReference: String?): Int {
    val stableKey = "$eventId|${photoReference.orEmpty()}"
    val hash = stableKey.fold(0) { result, character -> (result * 31) + character.code }
    return (hash and Int.MAX_VALUE) % ShopperArtworkResource.entries.size
}

@Composable
internal fun ShopperEventArtwork(
    presentation: ShopperEventArtworkPresentation,
    modifier: Modifier = Modifier,
    height: Dp = 216.dp,
) {
    val artwork = presentation.artwork
    Image(
        painter = painterResource(artwork.resource.drawableResource()),
        contentDescription = artwork.contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium),
    )
}

@Composable
internal fun ShopperSectionHeader(
    title: String,
    supportingText: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.extraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier
                    .yardScapeInteractiveTarget()
                    .semantics { contentDescription = actionLabel },
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun ShopperStatePanel(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    statusMessageKind: YardScapeStatusMessageKind? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (statusMessageKind == null) Modifier else Modifier.yardScapeStatusAnnouncement(statusMessageKind),
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.62f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(YardScapeDesign.spacing.large),
            verticalArrangement = Arrangement.spacedBy(YardScapeDesign.spacing.small),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier
                        .yardScapeInteractiveTarget()
                        .semantics { contentDescription = actionLabel },
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ShopperArtworkResource.drawableResource(): DrawableResource = when (this) {
    ShopperArtworkResource.GarageSale -> Res.drawable.marketplace_garage_sale
    ShopperArtworkResource.SwapMarket -> Res.drawable.marketplace_swap_market
    ShopperArtworkResource.FleaMarket -> Res.drawable.marketplace_flea_market
    ShopperArtworkResource.FurnitureMarket -> Res.drawable.marketplace_furniture_market
    ShopperArtworkResource.ClothingMarket -> Res.drawable.marketplace_clothing_market
    ShopperArtworkResource.BookMarket -> Res.drawable.marketplace_book_market
}
