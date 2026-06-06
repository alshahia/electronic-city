package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * D9.2 — shared empty-state slot. Replaces the per-screen ad-hoc
 * `Box { Column { Icon / Spacer / Text / Spacer / Text } }` block
 * that previously lived in 5 screens (ProductsScreen, DiscountsScreen,
 * FavoritesScreen, CartScreen, AccountScreen x3).
 *
 * Layout:
 *  - Illustration: optional `Painter`, 120dp tall, centered, with
 *    24dp vertical breathing room. `contentDescription = null`
 *    (decorative — the title does the screen-reader work).
 *  - Title: `headlineSmall` 18sp ExtraBold, `onSurfaceVariant` at
 *    70% alpha, max 2 lines ellipsised.
 *  - Subtitle: `bodyMedium` 12sp, `onSurfaceVariant` at 60% alpha,
 *    max 2 lines ellipsised. Optional.
 *  - Action: optional trailing composable (used by the cart empty
 *    state for the "Browse store offerings" button).
 *
 * Caller wraps this in a `Box(... .weight(1f))` (or a `fillMaxSize`)
 * to center it vertically inside the available space.
 *
 * Why a shared composable: 5 of 7 call sites were copy-paste
 * variations with subtle font-size and alpha inconsistencies
 * (13sp vs 14sp vs 16sp, alpha 0.7 vs 0.8). Centralizing the
 * typography and spacing in one place makes the visual rhythm
 * uniform and gives a designer a single file to revise.
 */
@Composable
fun EmptyState(
    illustration: Painter?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (illustration != null) {
            // Decorative: contentDescription is null because the title
            // already conveys the meaning for screen readers.
            Image(
                painter = illustration,
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .heightIn(min = 96.dp, max = 120.dp)
                    .size(120.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            // No illustration: leave a small leading spacer so the
            // text isn't crammed against the top edge.
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                action()
            }
        }
    }
}
