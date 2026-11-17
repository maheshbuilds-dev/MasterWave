package com.masterwave.player.data.recommendation

data class CommunityPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val trackCount: Int,
    val artworkUrl: String,
    val searchQuery: String,
    val trendTag: String = "🔥 TRENDING",
    val updatedToday: String = "Updated Today"
)

object CommunityPlaylistsProvider {

    fun getPlaylistsForLanguage(language: String): List<CommunityPlaylist> {
        return when (language) {
            "Tamil" -> listOf(
                CommunityPlaylist(
                    id = "pl_tm_viral_50",
                    title = "Kollywood Viral 50",
                    description = "The most streamed Tamil chartbusters & movie anthems",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&fit=crop",
                    searchQuery = "trending viral tamil hits 2026 songs",
                    trendTag = "🔥 #1 IN TAMIL NADU"
                ),
                CommunityPlaylist(
                    id = "pl_tm_anirudh",
                    title = "Anirudh Ravichander: Complete Universe",
                    description = "High-octane bass, rock guitars & iconic signature hooks",
                    trackCount = 50,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("anirudh"),
                    searchQuery = "Anirudh Ravichander best songs 2026",
                    trendTag = "⚡ POPULAR"
                ),
                CommunityPlaylist(
                    id = "pl_tm_sai",
                    title = "Sai Abhyankkar & New Wave Indie",
                    description = "Aane Wala Star, Katchi Sera, Pavazha Malli & modern pop",
                    trackCount = 35,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("sai abhyankkar"),
                    searchQuery = "Sai Abhyankkar songs Aane Wala Star Katchi Sera",
                    trendTag = "📈 RISING VIRAL"
                ),
                CommunityPlaylist(
                    id = "pl_tm_melodies",
                    title = "Tamil Melodies Uncut",
                    description = "Soulful violins, gentle strings & heartfelt lyrics",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&fit=crop",
                    searchQuery = "tamil melody songs soulful hits",
                    trendTag = "✨ EDITORS' CHOICE"
                ),
                CommunityPlaylist(
                    id = "pl_tm_yuvan",
                    title = "Yuvan Shankar Raja 2000s Nostalgia",
                    description = "Unmatched drug-like BGM, melodic soul & timeless tracks",
                    trackCount = 50,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("yuvan shankar raja"),
                    searchQuery = "Yuvan Shankar Raja 2000s hits songs",
                    trendTag = "👑 NOSTALGIA"
                ),
                CommunityPlaylist(
                    id = "pl_tm_acoustic",
                    title = "Tamil Chill Sunset Sessions",
                    description = "Intimate unplugged guitars, rain vibes & peaceful calm",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&fit=crop",
                    searchQuery = "tamil unplugged chill songs",
                    trendTag = "☕ RELAX"
                ),
                CommunityPlaylist(
                    id = "pl_tm_kuthu",
                    title = "Tamil Kuthu & Festival Beats",
                    description = "Massive dhol beats, festival energy & dance anthems",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&fit=crop",
                    searchQuery = "tamil kuthu dance party songs",
                    trendTag = "💃 DANCE"
                ),
                CommunityPlaylist(
                    id = "pl_tm_madras_hiphop",
                    title = "Madras Hip-Hop & Rap Cypher",
                    description = "Street poetry, fast flows & hard 808 basslines",
                    trackCount = 38,
                    artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&fit=crop",
                    searchQuery = "tamil hip hop rap independent songs",
                    trendTag = "🎙️ STREET CYPHER"
                ),
                CommunityPlaylist(
                    id = "pl_tm_santhosh",
                    title = "Santhosh Narayanan Indie Vault",
                    description = "Unique brass arrangements, horn sections & raw emotion",
                    trackCount = 42,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("santhosh narayanan"),
                    searchQuery = "Santhosh Narayanan hits songs",
                    trendTag = "🎸 INDIE"
                ),
                CommunityPlaylist(
                    id = "pl_tm_arr",
                    title = "A.R. Rahman 90s Kollywood Classics",
                    description = "Revolutionary orchestrations that transformed world music",
                    trackCount = 55,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("ar rahman"),
                    searchQuery = "AR Rahman 90s tamil evergreen hits",
                    trendTag = "🏆 GOLD"
                ),
                CommunityPlaylist(
                    id = "pl_tm_harris",
                    title = "Harris Jayaraj Romantic Melodies",
                    description = "Guitars, whistle tunes & smooth breeze romance",
                    trackCount = 45,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("harris jayaraj"),
                    searchQuery = "Harris Jayaraj love melody songs",
                    trendTag = "❤️ ROMANCE"
                ),
                CommunityPlaylist(
                    id = "pl_tm_lofi",
                    title = "Kolly Chill & Lo-Fi Beats",
                    description = "Slowed, reverbed & atmospheric late night mixes",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&fit=crop",
                    searchQuery = "tamil lofi chill beats songs",
                    trendTag = "🌙 MIDNIGHT"
                ),
                CommunityPlaylist(
                    id = "pl_tm_workout",
                    title = "Tamil Power Gym Workout",
                    description = "High BPM anthems to power through your sets",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&fit=crop",
                    searchQuery = "tamil high energy workout gym songs",
                    trendTag = "⚡ WORKOUT"
                ),
                CommunityPlaylist(
                    id = "pl_tm_ilaiyaraaja",
                    title = "Ilaiyaraaja Gold Symphony",
                    description = "The Maestro's eternal string sections and soul",
                    trackCount = 60,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("ilaiyaraaja"),
                    searchQuery = "Ilaiyaraaja golden hits songs",
                    trendTag = "👑 MAESTRO"
                ),
                CommunityPlaylist(
                    id = "pl_tm_sid_sriram",
                    title = "Sid Sriram: Soul of Tamil Cinema",
                    description = "Carnatic cadences blended into modern romantic pop",
                    trackCount = 40,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("sid sriram"),
                    searchQuery = "Sid Sriram tamil hit songs",
                    trendTag = "💖 VIRAL HITS"
                ),
                CommunityPlaylist(
                    id = "pl_tm_indie_fresh",
                    title = "Tamil Indie Wave: Fresh Releases",
                    description = "New independent voices from Chennai, Coimbatore & abroad",
                    trackCount = 35,
                    artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&fit=crop",
                    searchQuery = "tamil independent indie music 2026",
                    trendTag = "🌟 NEW RELEASES"
                )
            )
            "Punjabi" -> listOf(
                CommunityPlaylist(
                    id = "pl_pb_viral_50",
                    title = "Punjabi Virals 2026",
                    description = "Top 50 trending Punjabi chartbusters across the globe",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&fit=crop",
                    searchQuery = "punjabi trending hits 2026 songs",
                    trendTag = "🔥 #1 GLOBAL PUNJABI"
                ),
                CommunityPlaylist(
                    id = "pl_pb_karan_aujla",
                    title = "Karan Aujla: Four You & Beyond",
                    description = "Geet'an Di Machine with relentless flow & massive bass",
                    trackCount = 45,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("karan aujla"),
                    searchQuery = "Karan Aujla hits songs",
                    trendTag = "⚡ POPULAR"
                ),
                CommunityPlaylist(
                    id = "pl_pb_diljit",
                    title = "Diljit Dosanjh Stadium Flow",
                    description = "Born to Shine, G.O.A.T., Lover & worldwide festival bangers",
                    trackCount = 50,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("diljit dosanjh"),
                    searchQuery = "Diljit Dosanjh best songs",
                    trendTag = "👑 GOAT"
                ),
                CommunityPlaylist(
                    id = "pl_pb_sidhu",
                    title = "Sidhu Moose Wala: Forever Legends",
                    description = "Immortal voice of Punjab, revolutionary folk-drill",
                    trackCount = 50,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("sidhu moose wala"),
                    searchQuery = "Sidhu Moose Wala best songs",
                    trendTag = "🏆 IMMORTAL"
                ),
                CommunityPlaylist(
                    id = "pl_pb_drill",
                    title = "Urban Punjabi Drill & Sub-Bass",
                    description = "UK Drill bounce, heavy sliding 808s & aggressive flows",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&fit=crop",
                    searchQuery = "punjabi drill rap bass songs",
                    trendTag = "🏎️ CAR BASS"
                ),
                CommunityPlaylist(
                    id = "pl_pb_shubh_ap",
                    title = "Shubh & AP Dhillon Wave",
                    description = "Smooth melancholic trap cadences & modern Punjabi pop",
                    trackCount = 35,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("ap dhillon"),
                    searchQuery = "Shubh AP Dhillon songs",
                    trendTag = "📈 CHARTBUSTERS"
                ),
                CommunityPlaylist(
                    id = "pl_pb_pind",
                    title = "Pind Di Hawa: Folk & Heritage",
                    description = "Tumbi, dholak, sarangi & raw rural Punjabi poetry",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&fit=crop",
                    searchQuery = "punjabi folk traditional songs",
                    trendTag = "🌾 FOLK"
                ),
                CommunityPlaylist(
                    id = "pl_pb_bhangra",
                    title = "Bhangra Power Anthems 2026",
                    description = "High octane wedding beats, dhol drops & pure celebration",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&fit=crop",
                    searchQuery = "bhangra power wedding dance songs",
                    trendTag = "💃 BHANGRA"
                ),
                CommunityPlaylist(
                    id = "pl_pb_bpraak",
                    title = "B Praak & Jaani Heartbreak Anthems",
                    description = "Soul-shattering emotional poetry & orchestral crescendos",
                    trackCount = 42,
                    artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&fit=crop",
                    searchQuery = "B Praak Jaani sad songs",
                    trendTag = "💔 SAD ANTHEMS"
                ),
                CommunityPlaylist(
                    id = "pl_pb_night_drive",
                    title = "Late Night Punjabi Drive",
                    description = "Cruising tunes, smooth melodies & atmospheric sub-bass",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&fit=crop",
                    searchQuery = "punjabi late night drive cruising songs",
                    trendTag = "🌙 NIGHT DRIVE"
                ),
                CommunityPlaylist(
                    id = "pl_pb_desi_crew",
                    title = "Desi Crew Banger Collection",
                    description = "Crisp dhol rhythm sections & modern Punjabi groove",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&fit=crop",
                    searchQuery = "Desi Crew hit songs",
                    trendTag = "🎧 HIT PRODUCERS"
                ),
                CommunityPlaylist(
                    id = "pl_pb_lofi",
                    title = "Punjabi Lo-Fi & Chill Sessions",
                    description = "Intimate covers, slowed & reverb sunset mixes",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&fit=crop",
                    searchQuery = "punjabi lofi chill songs",
                    trendTag = "☕ CHILL"
                ),
                CommunityPlaylist(
                    id = "pl_pb_honey_badshah",
                    title = "Yo Yo & Badshah Club Classics",
                    description = "The era-defining anthems that started the party wave",
                    trackCount = 45,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("badshah"),
                    searchQuery = "Honey Singh Badshah hit party songs",
                    trendTag = "🎉 PARTY"
                ),
                CommunityPlaylist(
                    id = "pl_pb_workout",
                    title = "Punjabi Gym Beast Mode",
                    description = "Hard beats & unstoppable energy to destroy PRs",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&fit=crop",
                    searchQuery = "punjabi gym workout high energy songs",
                    trendTag = "⚡ WORKOUT"
                ),
                CommunityPlaylist(
                    id = "pl_pb_amrit_jordan",
                    title = "Amrit Maan & Jordan Sandhu Swag",
                    description = "Heavy lyrics, cultural proudness & village pride",
                    trackCount = 38,
                    artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600&fit=crop",
                    searchQuery = "Amrit Maan Jordan Sandhu songs",
                    trendTag = "🤠 SWAG"
                ),
                CommunityPlaylist(
                    id = "pl_pb_acoustic",
                    title = "Punjabi Unplugged Sessions",
                    description = "Pure passionate vocals & intimate guitar strings",
                    trackCount = 35,
                    artworkUrl = "https://images.unsplash.com/photo-1464375117522-1311d6a5b81f?w=600&fit=crop",
                    searchQuery = "punjabi unplugged songs",
                    trendTag = "✨ UNPLUGGED"
                )
            )
            "Telugu" -> listOf(
                CommunityPlaylist(
                    id = "pl_tl_viral_50",
                    title = "Tollywood Mass Anthems 2026",
                    description = "High-voltage mass beats & blockbuster chartbusters",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&fit=crop",
                    searchQuery = "telugu trending mass hits 2026 songs",
                    trendTag = "🔥 #1 TOLLYWOOD"
                ),
                CommunityPlaylist(
                    id = "pl_tl_dsp_thaman",
                    title = "DSP & Thaman Storm Beats",
                    description = "Non-stop dance grooves & chartbuster stadium sounds",
                    trackCount = 50,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("devi sri prasad"),
                    searchQuery = "thaman dsp telugu party songs",
                    trendTag = "⚡ BEAT STORM"
                ),
                CommunityPlaylist(
                    id = "pl_tl_romantic",
                    title = "Telugu Romantic Melodies",
                    description = "Heartwarming melodies, soulful flutes & love poetry",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&fit=crop",
                    searchQuery = "telugu love melody songs",
                    trendTag = "💖 LOVE"
                ),
                CommunityPlaylist(
                    id = "pl_tl_keeravaani",
                    title = "M.M. Keeravaani Cinema Gold",
                    description = "Oscar-winning orchestrations & grand epic compositions",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&fit=crop",
                    searchQuery = "MM Keeravaani classic hits songs",
                    trendTag = "🏆 OSCAR WINNER"
                ),
                CommunityPlaylist(
                    id = "pl_tl_ram_miriyala",
                    title = "Ram Miriyala Folk Grooves",
                    description = "Earthy rhythms, celebration & catchy grassroots hooks",
                    trackCount = 35,
                    artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&fit=crop",
                    searchQuery = "Ram Miriyala telugu songs",
                    trendTag = "🪘 FOLK"
                ),
                CommunityPlaylist(
                    id = "pl_tl_hesham",
                    title = "Hesham Abdul Wahab Chill Melodies",
                    description = "Gentle chords, intimate vocals & breeze romance",
                    trackCount = 35,
                    artworkUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&fit=crop",
                    searchQuery = "Hesham Abdul Wahab telugu songs",
                    trendTag = "✨ CHILL"
                ),
                CommunityPlaylist(
                    id = "pl_tl_party",
                    title = "Tollywood Party & DJ Blast",
                    description = "Fast dholak beats, whistle tunes & mass celebrations",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&fit=crop",
                    searchQuery = "telugu dj party dance songs",
                    trendTag = "🎉 PARTY"
                ),
                CommunityPlaylist(
                    id = "pl_tl_sid_sriram",
                    title = "Sid Sriram Telugu Essentials",
                    description = "Inkem Inkem, Samajavaragamana & timeless romantic gems",
                    trackCount = 42,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("sid sriram"),
                    searchQuery = "Sid Sriram telugu songs",
                    trendTag = "❤️ SOUL"
                ),
                CommunityPlaylist(
                    id = "pl_tl_drill_rap",
                    title = "Hyderabad Drill & Hip Hop",
                    description = "Independent Telugu rap, hard flows & modern street vibes",
                    trackCount = 38,
                    artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&fit=crop",
                    searchQuery = "telugu hip hop rap songs",
                    trendTag = "🎙️ STREET"
                ),
                CommunityPlaylist(
                    id = "pl_tl_lofi",
                    title = "Telugu Lo-Fi Relax & Chill",
                    description = "Mellow beats & reverbed melodies for studying",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&fit=crop",
                    searchQuery = "telugu lofi chill songs",
                    trendTag = "☕ STUDY"
                ),
                CommunityPlaylist(
                    id = "pl_tl_anurag",
                    title = "Anurag Kulkarni Power Anthems",
                    description = "Massive vocal power & energetic Tollywood hits",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&fit=crop",
                    searchQuery = "Anurag Kulkarni telugu songs",
                    trendTag = "🔥 ENERGY"
                ),
                CommunityPlaylist(
                    id = "pl_tl_night_drive",
                    title = "Telugu Roadtrip & Highway Drive",
                    description = "Smooth cruising beats for late night highways",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&fit=crop",
                    searchQuery = "telugu travel cruising driving songs",
                    trendTag = "🚗 ROADTRIP"
                ),
                CommunityPlaylist(
                    id = "pl_tl_workout",
                    title = "Tollywood Gym Power Motivation",
                    description = "Heavy basslines & mass beats to push through limits",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&fit=crop",
                    searchQuery = "telugu workout gym gym songs",
                    trendTag = "⚡ GYM"
                ),
                CommunityPlaylist(
                    id = "pl_tl_classics",
                    title = "Telugu Golden Classics: SPB & Ghantasala",
                    description = "Timeless melodies from the greatest legends of cinema",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1464375117522-1311d6a5b81f?w=600&fit=crop",
                    searchQuery = "SP Balasubrahmanyam telugu golden hits",
                    trendTag = "👑 CLASSICS"
                ),
                CommunityPlaylist(
                    id = "pl_tl_acoustic",
                    title = "Telugu Unplugged Sessions",
                    description = "Intimate live strings and vocals with minimal production",
                    trackCount = 35,
                    artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&fit=crop",
                    searchQuery = "telugu unplugged live songs",
                    trendTag = "✨ UNPLUGGED"
                ),
                CommunityPlaylist(
                    id = "pl_tl_indie",
                    title = "Telugu Indie Waves 2026",
                    description = "Fresh independent releases pushing boundaries",
                    trackCount = 35,
                    artworkUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&fit=crop",
                    searchQuery = "telugu independent indie songs 2026",
                    trendTag = "🌟 NEW VOICES"
                )
            )
            "Global Hits" -> listOf(
                CommunityPlaylist(
                    id = "pl_gl_viral_50",
                    title = "Global Viral 50",
                    description = "The most played viral tracks across international charts",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&fit=crop",
                    searchQuery = "top 50 global viral hits songs 2026",
                    trendTag = "🔥 #1 WORLDWIDE"
                ),
                CommunityPlaylist(
                    id = "pl_gl_billboard",
                    title = "Today's Top Hits: Billboard 100",
                    description = "The absolute biggest songs on the planet right now",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&fit=crop",
                    searchQuery = "billboard top 100 pop hits 2026",
                    trendTag = "⚡ POPULAR"
                ),
                CommunityPlaylist(
                    id = "pl_gl_synthwave",
                    title = "Synthwave & Cyberpunk Night Drive",
                    description = "Retro analog synths, outrun neon & late night driving",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&fit=crop",
                    searchQuery = "synthwave outrun retro electro night drive",
                    trendTag = "🏎️ SYNTHWAVE"
                ),
                CommunityPlaylist(
                    id = "pl_gl_lofi",
                    title = "Deep Focus & Study Lo-Fi Beats",
                    description = "Mellow instrumental chillhop to keep you in the zone",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&fit=crop",
                    searchQuery = "lofi hip hop beats study work calm",
                    trendTag = "☕ STUDY & FOCUS"
                ),
                CommunityPlaylist(
                    id = "pl_gl_pop_rising",
                    title = "Pop Rising: Future Chartbusters",
                    description = "Catchy melodies, infectious choruses & rising stars",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&fit=crop",
                    searchQuery = "pop rising new music hits 2026",
                    trendTag = "📈 RISING"
                ),
                CommunityPlaylist(
                    id = "pl_gl_hiphop",
                    title = "Hip Hop Central & Trap Bangers",
                    description = "Heavy 808s, sharp hi-hats & lyricism from top rappers",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&fit=crop",
                    searchQuery = "hip hop rap trap bangers 2026",
                    trendTag = "🎙️ HIP HOP"
                ),
                CommunityPlaylist(
                    id = "pl_gl_indie_pop",
                    title = "Indie Pop & Dreamy Bedroom Beats",
                    description = "Warm guitars, vintage vocals & soft melancholic vibes",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&fit=crop",
                    searchQuery = "indie pop bedroom dream pop songs",
                    trendTag = "✨ INDIE"
                ),
                CommunityPlaylist(
                    id = "pl_gl_rock",
                    title = "Rock Legends: Timeless Anthems",
                    description = "Riffs, drums & stadium choruses that live forever",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&fit=crop",
                    searchQuery = "classic rock legendary stadium songs",
                    trendTag = "🎸 ROCK"
                ),
                CommunityPlaylist(
                    id = "pl_gl_acoustic",
                    title = "Morning Coffee & Soul",
                    description = "Peaceful morning fingerpicking & smooth keys to start your day",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1464375117522-1311d6a5b81f?w=600&fit=crop",
                    searchQuery = "morning coffee peaceful chill songs",
                    trendTag = "🌅 MORNING"
                ),
                CommunityPlaylist(
                    id = "pl_gl_edm",
                    title = "EDM Festival Mainstage",
                    description = "Massive drops, festival energy & floor-shaking bass",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&fit=crop",
                    searchQuery = "edm festival electro house dance songs",
                    trendTag = "⚡ EDM"
                ),
                CommunityPlaylist(
                    id = "pl_gl_rnb",
                    title = "R&B Slow Jamz & Late Night Soul",
                    description = "Sensual vocals, silky grooves & smooth late night vibes",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&fit=crop",
                    searchQuery = "rnb soul slow jams chill songs",
                    trendTag = "💖 R&B"
                ),
                CommunityPlaylist(
                    id = "pl_gl_workout",
                    title = "Workout Beast Mode: High BPM",
                    description = "Electrifying tempo & bass to destroy training limits",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&fit=crop",
                    searchQuery = "high energy gym workout motivation songs",
                    trendTag = "💪 BEAST MODE"
                ),
                CommunityPlaylist(
                    id = "pl_gl_melancholy",
                    title = "Late Night Melancholy & Rain",
                    description = "Reflective sad pop, gentle piano & emotional healing",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600&fit=crop",
                    searchQuery = "sad emotional pop unplugged songs",
                    trendTag = "🌧️ RAIN"
                ),
                CommunityPlaylist(
                    id = "pl_gl_ambient",
                    title = "Chillout Lounge & Ambient Space",
                    description = "Ethereal pads, zero distractions & meditative soundscapes",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&fit=crop",
                    searchQuery = "ambient chillout lounge relaxing music",
                    trendTag = "🧘 CALM"
                ),
                CommunityPlaylist(
                    id = "pl_gl_tiktok",
                    title = "Viral TikTok & Reels Hits 2026",
                    description = "The catchiest trending sounds exploding across video feeds",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&fit=crop",
                    searchQuery = "viral tiktok reels trending songs 2026",
                    trendTag = "📱 VIRAL VIDEO"
                ),
                CommunityPlaylist(
                    id = "pl_gl_nostalgia",
                    title = "2000s Pop & Nostalgia Gold",
                    description = "The iconic millennial radio anthems that defined an era",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&fit=crop",
                    searchQuery = "2000s pop nostalgia throwback hits",
                    trendTag = "📻 NOSTALGIA"
                )
            )
            else -> listOf( // Hindi default
                CommunityPlaylist(
                    id = "pl_hi_viral_50",
                    title = "Bollywood Chartbusters 2026",
                    description = "The most played Hindi cinema anthems & viral tracks",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&fit=crop",
                    searchQuery = "bollywood latest trending hits 2026 songs",
                    trendTag = "🔥 #1 IN INDIA"
                ),
                CommunityPlaylist(
                    id = "pl_hi_arijit",
                    title = "Arijit Singh: Soulful Romantics",
                    description = "Emotional soul masterpieces from the voice of a generation",
                    trackCount = 50,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("arijit singh"),
                    searchQuery = "Arijit Singh best romantic songs",
                    trendTag = "⚡ POPULAR"
                ),
                CommunityPlaylist(
                    id = "pl_hi_dhh",
                    title = "Desi Hip Hop Underground",
                    description = "KR\$NA, Seedhe Maut, DIVINE, Raftaar & raw gully poetry",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&fit=crop",
                    searchQuery = "desi hip hop rap viral songs",
                    trendTag = "🎙️ DHH CYPHER"
                ),
                CommunityPlaylist(
                    id = "pl_hi_shashwat",
                    title = "Shashwat Sachdev Power Anthems",
                    description = "Lutt Le Gaya, Jaiye Sajana, Uri score & modern cinematic bass",
                    trackCount = 35,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("shashwat sachdev"),
                    searchQuery = "Shashwat Sachdev songs Dhurandhar Uri",
                    trendTag = "🎬 CINEMATIC"
                ),
                CommunityPlaylist(
                    id = "pl_hi_90s",
                    title = "Bollywood 90s Golden Nostalgia",
                    description = "Udit Narayan, Kumar Sanu, Alka Yagnik & timeless melodies",
                    trackCount = 60,
                    artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&fit=crop",
                    searchQuery = "90s bollywood evergreen romantic songs",
                    trendTag = "👑 90s NOSTALGIA"
                ),
                CommunityPlaylist(
                    id = "pl_hi_pritam",
                    title = "Pritam: The Hitmaker Vault",
                    description = "Guitar hooks, whistle cadences & unstoppable chartbusters",
                    trackCount = 45,
                    artworkUrl = ArtistPhotoProvider.getPhotoUrl("pritam"),
                    searchQuery = "Pritam bollywood hit songs",
                    trendTag = "🎸 HITMAKER"
                ),
                CommunityPlaylist(
                    id = "pl_hi_acoustic_sunset",
                    title = "Hindi Sunset Lo-Fi Sessions",
                    description = "Warm coffee, rain vibes & gentle unplugged melodies",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&fit=crop",
                    searchQuery = "hindi unplugged sunset songs",
                    trendTag = "☕ CHILL"
                ),
                CommunityPlaylist(
                    id = "pl_hi_dance",
                    title = "Bollywood Dance & Party Bangers",
                    description = "Dhol drops, electro-punjabi beats & wedding madness",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&fit=crop",
                    searchQuery = "bollywood party dance wedding songs",
                    trendTag = "🎉 PARTY"
                ),
                CommunityPlaylist(
                    id = "pl_hi_sufi",
                    title = "Sufi Reverie & Ghazal Soul",
                    description = "Nusrat, Rahat, Kailash Kher & transcendental Sufi poetry",
                    trackCount = 38,
                    artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&fit=crop",
                    searchQuery = "sufi ghazal soul songs hindi",
                    trendTag = "🕊️ SUFI"
                ),
                CommunityPlaylist(
                    id = "pl_hi_lofi",
                    title = "Midnight Hindi Lo-Fi Chill",
                    description = "Slowed, reverbed & atmospheric late night bedroom mixes",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&fit=crop",
                    searchQuery = "hindi lofi chill beats",
                    trendTag = "🌙 MIDNIGHT"
                ),
                CommunityPlaylist(
                    id = "pl_hi_indie",
                    title = "Indie India Underground",
                    description = "Prateek Kuhad, Anuv Jain, When Chai Met Toast & modern indie",
                    trackCount = 42,
                    artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&fit=crop",
                    searchQuery = "indian indie independent songs",
                    trendTag = "✨ INDIE"
                ),
                CommunityPlaylist(
                    id = "pl_hi_heartbreak",
                    title = "Soulful Bollywood Heartbreak",
                    description = "Melancholy violins & emotional heartbreak poetry",
                    trackCount = 45,
                    artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600&fit=crop",
                    searchQuery = "sad emotional heartbreak bollywood songs",
                    trendTag = "💔 EMOTIONAL"
                ),
                CommunityPlaylist(
                    id = "pl_hi_coke_studio",
                    title = "Coke Studio & MTV Unplugged",
                    description = "Epic live orchestrations, fusion instruments & legendary jams",
                    trackCount = 36,
                    artworkUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&fit=crop",
                    searchQuery = "coke studio unplugged live fusion songs",
                    trendTag = "🎺 LIVE FUSION"
                ),
                CommunityPlaylist(
                    id = "pl_hi_workout",
                    title = "Bollywood EDM Workout Storm",
                    description = "Remixed basslines & relentless tempo for the gym",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&fit=crop",
                    searchQuery = "bollywood edm remix gym workout songs",
                    trendTag = "⚡ WORKOUT"
                ),
                CommunityPlaylist(
                    id = "pl_hi_rain",
                    title = "Bollywood Monsoon Rain Melodies",
                    description = "Romance in the rain, soulful flutes & nostalgia",
                    trackCount = 40,
                    artworkUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&fit=crop",
                    searchQuery = "bollywood monsoon rain love songs",
                    trendTag = "🌧️ MONSOON"
                ),
                CommunityPlaylist(
                    id = "pl_hi_retro",
                    title = "Retro Legends: Kishore & Lata Gold",
                    description = "Pancham Da, Kishore Kumar, Lata Mangeshkar & RD Burman",
                    trackCount = 50,
                    artworkUrl = "https://images.unsplash.com/photo-1464375117522-1311d6a5b81f?w=600&fit=crop",
                    searchQuery = "Kishore Kumar RD Burman Lata golden hits",
                    trendTag = "👑 RETRO GOLD"
                )
            )
        }
    }
}
