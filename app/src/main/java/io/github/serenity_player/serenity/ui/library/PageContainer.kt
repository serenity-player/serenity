package io.github.serenity_player.serenity.ui.library

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.serenity_player.serenity.library.ArtistUi
import io.github.serenity_player.serenity.library.DetailUi
import io.github.serenity_player.serenity.library.Page
import io.github.serenity_player.serenity.library.PageStack
import io.github.serenity_player.serenity.player.NowPlayingViewModel

private data class PageState(val page: Page?, val depth: Int)

/**
 * Shows [root] (a tab's own content) or the top page of its stack. Pushing slides forward, popping slides
 * back, and the system back gesture previews the pop by shrinking the page as you drag.
 */
@Composable
fun PageContainer(
    pages: List<Page>,
    stack: PageStack,
    np: NowPlayingViewModel,
    modifier: Modifier = Modifier,
    root: @Composable () -> Unit,
) {
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = pages.isNotEmpty()) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            stack.back() // gesture committed
        } finally {
            backProgress = 0f // committed or cancelled
        }
    }

    AnimatedContent(
        targetState = PageState(pages.lastOrNull(), pages.size),
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = 1f - 0.08f * backProgress
                scaleX = scale
                scaleY = scale
                alpha = 1f - 0.25f * backProgress
            },
        transitionSpec = {
            if (targetState.depth > initialState.depth) {
                (slideInHorizontally(tween(300)) { it / 5 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { -it / 5 } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally(tween(300)) { -it / 5 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { it / 5 } + fadeOut(tween(200)))
            }
        },
        label = "pages",
    ) { state ->
        when (val page = state.page) {
            null -> root()
            is DetailUi -> DetailScreen(page, np, onBack = stack::back)
            is ArtistUi -> ArtistScreen(page, stack, np)
        }
    }
}
