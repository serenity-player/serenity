package io.github.serenity_player.serenity.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.serenity_player.serenity.library.ArtistUi
import io.github.serenity_player.serenity.library.PageStack
import io.github.serenity_player.serenity.player.NowPlayingViewModel
import io.github.serenity_player.serenity.player.PlayRequest

/** Artist page: Play/Shuffle the artist, then their albums and singles (tap to open). */
@Composable
fun ArtistScreen(a: ArtistUi, stack: PageStack, np: NowPlayingViewModel, modifier: Modifier = Modifier) {
    PagedList(
        pager = a.pager,
        emptyText = "No albums found.",
        modifier = modifier,
        header = {
            PageHeader(
                title = a.artist.name,
                subtitle = "Albums and singles",
                artUrl = a.artist.artUrl,
                circle = true,
                onBack = stack::back,
                onPlay = { np.play(PlayRequest.Context(a.artist.uri)) },
                onShuffle = { np.play(PlayRequest.Context(a.artist.uri), shuffle = true) },
            )
        },
    ) { _, album, _ ->
        MediaRow(
            title = album.name,
            subtitle = listOfNotNull(
                album.year,
                album.trackCount.takeIf { it > 0 }?.let { "$it tracks" },
            ).joinToString(" · "),
            artUrl = album.artUrl,
            onClick = { stack.openAlbum(album) },
        )
    }
}
