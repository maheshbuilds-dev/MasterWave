package com.masterwave.player.data.recommendation

import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intelligent Language & Viral Music Recommendation Engine:
 * Analyzes the song currently playing, detects its language and cultural genre,
 * and immediately fetches real viral, most-heard, trending tracks in that language.
 */
object LanguageMusicRecommender {

    fun detectLanguage(track: Track?): String {
        if (track == null) return "Hindi"

        val combined = "${track.title} ${track.artist} ${track.album}".lowercase()

        // 1. Unicode Script Detection
        for (char in combined) {
            val code = char.code
            when (code) {
                in 0x0900..0x097F -> return "Hindi" // Devanagari
                in 0x0A00..0x0A7F -> return "Punjabi" // Gurmukhi
                in 0x0B80..0x0BFF -> return "Tamil" // Tamil
                in 0x0C00..0x0C7F -> return "Telugu" // Telugu
                in 0x0D00..0x0D7F -> return "Malayalam" // Malayalam
                in 0x0C80..0x0CFF -> return "Kannada" // Kannada
            }
        }

        // 2. Artist & Keyword Signatures: Punjabi
        val punjabiKeywords = listOf(
            "karan aujla", "diljit", "shubh", "ap dhillon", "sidhu", "amrit maan",
            "b praak", "honey singh", "kr\$na", "dhanda", "raftaar", "badshah",
            "jass manak", "mankirt", "jordan sandhu", "gippy", "nimrat", "sukhe",
            "gurdas maan", "bohemia", "deep jandu", "cheema", "arjan dhillon",
            "prophec", "gurinder gill", "tarseem", "punjabi", "bhangra", "jatt", "pind"
        )
        if (punjabiKeywords.any { combined.contains(it) }) return "Punjabi"

        // 3. Artist & Keyword Signatures: Tamil
        val tamilKeywords = listOf(
            "anirudh", "sai abhyankkar", "a.r. rahman", "ar rahman", "yuvan",
            "harris jayaraj", "sid sriram", "santhosh narayanan", "dhibu", "g.v. prakash",
            "hiphop tamizha", "udhiram", "aane wala", "kadhai", "kanmani", "tamil",
            "kollywood", "naan", "unnakul", "uyire"
        )
        if (tamilKeywords.any { combined.contains(it) }) return "Tamil"

        // 4. Artist & Keyword Signatures: Telugu
        val teluguKeywords = listOf(
            "dsp", "devi sri prasad", "thaman", "ram miriyala", "hesham", "anurag kulkarni",
            "chaitan", "keeravaani", "telugu", "tollywood", "prema", "nenu", "samajavaragamana",
            "raanu", "inkem"
        )
        if (teluguKeywords.any { combined.contains(it) }) return "Telugu"

        // 5. Artist & Keyword Signatures: English / Global
        val englishKeywords = listOf(
            "whitney houston", "weeknd", "taylor swift", "drake", "billie eilish",
            "post malone", "bruno mars", "eminem", "ed sheeran", "coolio", "dua lipa",
            "justin bieber", "travis scott", "kendrick", "gangsta", "maroon 5",
            "coldplay", "imagine dragons", "ariana grande", "charlie puth", "pop", "hip hop"
        )
        if (englishKeywords.any { combined.contains(it) }) return "Global Hits"

        // 6. Bollywood / Hindi Default
        val hindiKeywords = listOf(
            "arijit", "sachdev", "shashwat", "pritam", "shreya ghoshal", "atif aslam",
            "udit narayan", "kumar sanu", "neha kakkar", "jubin", "sonu nigam",
            "alka yagnik", "vishal mishra", "mohit chauhan", "armaan malik",
            "darshan raval", "bairan", "sheesha", "parvati", "bollywood", "hindi",
            "pyaar", "dil", "ishq", "tum", "hum", "sajna", "mere"
        )
        if (hindiKeywords.any { combined.contains(it) }) return "Hindi"

        return "Hindi"
    }

    fun getSearchQuery(language: String): String {
        return when (language) {
            "Punjabi" -> "Trending Punjabi viral hits 2026 songs"
            "Tamil" -> "Trending Tamil viral hits 2026 songs"
            "Telugu" -> "Trending Telugu viral hits 2026 songs"
            "Malayalam" -> "Trending Malayalam hits 2026 songs"
            "Global Hits" -> "Top 50 viral hits songs 2026"
            else -> "Top Bollywood trending viral hits 2026 songs"
        }
    }

    suspend fun getLanguageViralTracks(
        repository: MusicRepository,
        currentTrack: Track?,
        languageHint: String? = null
    ): Pair<String, List<Track>> = withContext(Dispatchers.IO) {
        val lang = languageHint?.takeIf { it.isNotBlank() }?.let {
            if (it.equals("English", ignoreCase = true)) "Global Hits" else it
        } ?: detectLanguage(currentTrack)
        val query = getSearchQuery(lang)
        try {
            val searchRes = repository.search(query)
            val songs = searchRes.songs.filter { it.title.isNotBlank() }
            if (songs.isNotEmpty()) {
                Pair(lang, songs)
            } else {
                Pair(lang, repository.getTrending())
            }
        } catch (e: Exception) {
            Pair(lang, repository.getTrending())
        }
    }
}
