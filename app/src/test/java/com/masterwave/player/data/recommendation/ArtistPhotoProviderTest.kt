package com.masterwave.player.data.recommendation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistPhotoProviderTest {
    @Test
    fun verifiedArtistAliasesResolveWithoutMatchingUnrelatedNames() {
        assertNotNull(ArtistPhotoProvider.getPhotoUrlOrNull("KK"))
        assertNotNull(ArtistPhotoProvider.getPhotoUrlOrNull("Arijit Singh, Shreya Ghoshal"))
        assertNull(ArtistPhotoProvider.getPhotoUrlOrNull("King Gnu"))
    }
}
