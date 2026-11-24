package com.masterwave.player.data.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeHelperTest {

    @Test
    fun extractRoomCode_fromDirect6Digits() {
        val code = QrCodeHelper.extractRoomCode("482193")
        assertEquals("482193", code)
    }

    @Test
    fun extractRoomCode_fromHttpsUrl() {
        val code = QrCodeHelper.extractRoomCode("https://masterwave.vercel.app/join?code=705624")
        assertEquals("705624", code)
    }

    @Test
    fun extractRoomCode_fromCustomSchemeUrl() {
        val code = QrCodeHelper.extractRoomCode("masterwave://join?code=193847")
        assertEquals("193847", code)
    }

    @Test
    fun extractRoomCode_fromTextWithSurroundingMessage() {
        val code = QrCodeHelper.extractRoomCode("Hey listen together with code 839201 in MasterWave")
        assertEquals("839201", code)
    }

    @Test
    fun extractRoomCode_invalidShortOrLongDigits() {
        assertNull(QrCodeHelper.extractRoomCode("123"))
        assertNull(QrCodeHelper.extractRoomCode("12345"))
        assertNull(QrCodeHelper.extractRoomCode("abcdef"))
        assertNull(QrCodeHelper.extractRoomCode(""))
        assertNull(QrCodeHelper.extractRoomCode(null))
    }

    @Test
    fun buildInviteUrl_constructsCorrectHttpsUrl() {
        val url = QrCodeHelper.buildInviteUrl("482193")
        assertEquals("https://masterwave.vercel.app/join?code=482193", url)
    }

    @Test
    fun buildCustomSchemeUrl_constructsCorrectDeepLink() {
        val url = QrCodeHelper.buildCustomSchemeUrl("482193")
        assertEquals("masterwave://join?code=482193", url)
    }
}
