package io.github.serenity_player.serenity.data.repository

import io.github.serenity_player.serenity.data.api.ImageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageSelectionTest {
    private val images = listOf(
        ImageDto("big", 640, 640),
        ImageDto("medium", 300, 300),
        ImageDto("small", 64, 64),
    )

    @Test
    fun picksTheSmallestImageThatIsBigEnough() {
        assertEquals("medium", images.best(minWidth = 300))
        assertEquals("big", images.best(minWidth = 600))
    }

    @Test
    fun fallsBackToTheFirstImageWhenNoneIsBigEnough() {
        assertEquals("small", listOf(ImageDto("small", 64, 64)).best(minWidth = 300))
    }

    @Test
    fun ignoresBlankUrlsAndHandlesEmptyLists() {
        assertEquals("ok", listOf(ImageDto("", 640, 640), ImageDto("ok", 640, 640)).best())
        assertNull(emptyList<ImageDto>().best())
    }

    @Test
    fun imagesWithoutAWidthAreStillUsable() {
        assertEquals("only", listOf(ImageDto("only", null, null)).best())
    }
}
