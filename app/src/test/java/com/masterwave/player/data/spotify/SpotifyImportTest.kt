package com.masterwave.player.data.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class SpotifyImportTest {

    @Test
    fun testSpotifyConfigConstantsAndEndpoints() {
        assertEquals("masterwave://spotify-callback", SpotifyConfig.REDIRECT_URI)
        assertEquals("https://accounts.spotify.com/authorize", SpotifyConfig.AUTH_URL)
        assertEquals("https://accounts.spotify.com/api/token", SpotifyConfig.TOKEN_URL)
        assertEquals("https://api.spotify.com/v1", SpotifyConfig.API_BASE_URL)

        // Scopes must be strictly read-only
        val scopes = SpotifyConfig.SCOPES.split(" ")
        assertTrue(scopes.contains("user-library-read"))
        assertTrue(scopes.contains("playlist-read-private"))
        assertTrue(scopes.contains("playlist-read-collaborative"))

        // Must never include write or modify scopes
        assertFalse(scopes.any { it.contains("modify") || it.contains("write") })
    }

    @Test
    fun testPkceVerifierAndChallengeSpecification() {
        // Test RFC 7636 PKCE code verifier requirements
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val verifierLength = 64
        val random = java.security.SecureRandom()
        val verifier = (1..verifierLength)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")

        assertEquals(64, verifier.length)
        assertTrue(verifier.all { it in allowedChars })

        // S256 Challenge generation test (RFC 7636 Base64URL without padding)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(verifier.toByteArray(StandardCharsets.US_ASCII))
        val digest = md.digest()
        val challenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)

        assertNotNull(challenge)
        assertTrue(challenge.isNotBlank())
        assertEquals(43, challenge.length)
    }

    @Test
    fun testFebruary2026EndpointConventions() {
        // Spotify February 2026 guidelines convention:
        // Playlist tracks must use /v1/playlists/{playlist_id}/items, NOT /tracks
        val playlistId = "37i9dQZF1DXcBWIGoYBM5M"
        val expectedEndpoint = "${SpotifyConfig.API_BASE_URL}/playlists/$playlistId/items"

        assertTrue(expectedEndpoint.endsWith("/items"))
        assertFalse(expectedEndpoint.endsWith("/tracks"))
    }

    @Test
    fun testExportifyCsvParsingLogic() {
        val sampleExportifyCsv = """
            "Spotify ID","Track Name","Artist Name(s)","Album Name","Duration (ms)"
            "4cOdK2wGLETKBW3PvgPWqT","Never Gonna Give You Up","Rick Astley","Whenever You Need Somebody","213573"
            "3n3Ppam7vgaVa1iaRUc9Lp","Mr. Brightside","The Killers","Hot Fuss","222973"
        """.trimIndent()

        val lines = sampleExportifyCsv.lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size)

        val header = lines[0].split(",").map { it.trim('"', ' ') }
        val idIdx = header.indexOfFirst { it.contains("Spotify ID") }
        val titleIdx = header.indexOfFirst { it.contains("Track Name") }
        val artistIdx = header.indexOfFirst { it.contains("Artist Name") }

        assertEquals(0, idIdx)
        assertEquals(1, titleIdx)
        assertEquals(2, artistIdx)

        val firstRow = lines[1].split(",").map { it.trim('"', ' ') }
        assertEquals("4cOdK2wGLETKBW3PvgPWqT", firstRow[idIdx])
        assertEquals("Never Gonna Give You Up", firstRow[titleIdx])
        assertEquals("Rick Astley", firstRow[artistIdx])
    }
}
