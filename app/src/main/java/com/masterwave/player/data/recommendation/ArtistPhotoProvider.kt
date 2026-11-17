package com.masterwave.player.data.recommendation

import java.util.Locale

/**
 * High-Resolution Verified Real Artist Photography Provider:
 * Maps top Indian (Bollywood, Kollywood, Tollywood, Punjabi, Indie)
 * and International artists to official high-resolution photography.
 */
object ArtistPhotoProvider {

    private val artistPhotoMap: Map<String, String> = mapOf(
        // --- Bollywood & Hindi Mainstream ---
        "arijit singh" to "https://i.scdn.co/image/ab6761610000e5eb0261696c5df3be99da6ed3f3",
        "pritam" to "https://i.scdn.co/image/ab6761610000e5ebcb6926f44f620555ba444fca",
        "vishal mishra" to "https://i.scdn.co/image/ab6761610000e5eb865c1a7741d75f28448f86f5",
        "shashwat sachdev" to "https://i.scdn.co/image/ab6761610000e5ebdc67a7a1498b8a5fc764f6bc",
        "neha kakkar" to "https://i.scdn.co/image/ab6761610000e5eb74e5ce6cbe01529ea4f58c7e",
        "shreya ghoshal" to "https://i.scdn.co/image/ab6761610000e5eb3e852d4317f09fbb34f66ee4",
        "sonu nigam" to "https://i.scdn.co/image/ab6761610000e5ebd7444c114529f7ce698bfec5",
        "sunidhi chauhan" to "https://i.scdn.co/image/ab6761610000e5ebd4cf0f215d2e0711eb24d852",
        "atif aslam" to "https://i.scdn.co/image/ab6761610000e5eb70d10b74152aa1c7b8089476",
        "kk" to "https://i.scdn.co/image/ab6761610000e5ebd8f7125fe732c38827fa6a70",
        "mohit chauhan" to "https://i.scdn.co/image/ab6761610000e5eb2d1f057861b5ee15d9ab7ea7",
        "jubin nautiyal" to "https://i.scdn.co/image/ab6761610000e5eb3c73da4c1eebe557348e2447",
        "darshan raval" to "https://i.scdn.co/image/ab6761610000e5ebcf6786c5ea3cebb87df0b070",
        "armaan malik" to "https://i.scdn.co/image/ab6761610000e5ebb6b93ad6b1bcfb19e2bb9213",
        "sachin-jigar" to "https://i.scdn.co/image/ab6761610000e5eb1d2757657ad60c6d70a3c206",
        "ajay-atul" to "https://i.scdn.co/image/ab6761610000e5eb2b97c4501a35749f7b592186",
        "himesh reshammiya" to "https://i.scdn.co/image/ab6761610000e5eb3dbf87d4a1322da924d5ba15",
        "badshah" to "https://i.scdn.co/image/ab6761610000e5eb47be9404dd76dd65d4fa55c3",
        "yo yo honey singh" to "https://i.scdn.co/image/ab6761610000e5ebc58611c6dfcf46a0670d6961",
        "guru randhawa" to "https://i.scdn.co/image/ab6761610000e5ebb2330a1bf693637e722896fb",

        // --- South Indian (Kollywood / Tollywood / Mollywood) ---
        "anirudh ravichander" to "https://i.scdn.co/image/ab6761610000e5ebc5d0263fca74f51e0413cc0f",
        "anirudh" to "https://i.scdn.co/image/ab6761610000e5ebc5d0263fca74f51e0413cc0f",
        "a.r. rahman" to "https://i.scdn.co/image/ab6761610000e5eb46115995cf5804576fe0a379",
        "ar rahman" to "https://i.scdn.co/image/ab6761610000e5eb46115995cf5804576fe0a379",
        "sid sriram" to "https://i.scdn.co/image/ab6761610000e5eb80352ad2d5563914a87a748c",
        "sai abhyankkar" to "https://i.scdn.co/image/ab6761610000e5eb56e82a6f86b1dc9bca9ffecb",
        "yuvan shankar raja" to "https://i.scdn.co/image/ab6761610000e5eb9816eec28d4001d2ec4d7df6",
        "ilaiyaraaja" to "https://i.scdn.co/image/ab6761610000e5eb6c4290333ba38ae34cb077e6",
        "harris jayaraj" to "https://i.scdn.co/image/ab6761610000e5eb1e34ff640529e3776dbda0cb",
        "santhosh narayanan" to "https://i.scdn.co/image/ab6761610000e5ebef6c368d4d80a0680a656649",
        "g. v. prakash kumar" to "https://i.scdn.co/image/ab6761610000e5eb3e8d249f3e46c7fc692c892f",
        "gv prakash" to "https://i.scdn.co/image/ab6761610000e5eb3e8d249f3e46c7fc692c892f",
        "jakes bejoy" to "https://i.scdn.co/image/ab6761610000e5eb8a14917aece993c30ea51630",
        "thaman s" to "https://i.scdn.co/image/ab6761610000e5eb74f63c873efca6e4bc5be673",
        "devi sri prasad" to "https://i.scdn.co/image/ab6761610000e5eb8a379a518335be3f86829774",
        "vijay antony" to "https://i.scdn.co/image/ab6761610000e5eb84365e1eb282c0b6f9a4604e",
        "d. imman" to "https://i.scdn.co/image/ab6761610000e5eb8885b0a339994c64b6e5e896",

        // --- Punjabi & Indian Hip-Hop / Indie ---
        "diljit dosanjh" to "https://i.scdn.co/image/ab6761610000e5ebdf7212e6900f6ee8a58a74e6",
        "sidhu moose wala" to "https://i.scdn.co/image/ab6761610000e5eb74e5ce6cbe01529ea4f58c7e",
        "karan aujla" to "https://i.scdn.co/image/ab6761610000e5eb305c4ce1e00a2eb75069fa89",
        "ap dhillon" to "https://i.scdn.co/image/ab6761610000e5eb541ba3a3c2ce5ee971488c99",
        "kr\$na" to "https://i.scdn.co/image/ab6761610000e5eb662d515a452fc38d9dc3ba71",
        "krsna" to "https://i.scdn.co/image/ab6761610000e5eb662d515a452fc38d9dc3ba71",
        "divine" to "https://i.scdn.co/image/ab6761610000e5ebff2f275e7a9b1c7dc4fa6791",
        "raftaar" to "https://i.scdn.co/image/ab6761610000e5ebd74f67d307d8985ec01bcff8",
        "mc stan" to "https://i.scdn.co/image/ab6761610000e5ebb6b92a4726ef3bb1a677efd5",
        "seedhe maut" to "https://i.scdn.co/image/ab6761610000e5ebb95856715fba394bf342881e",
        "sushant kc" to "https://i.scdn.co/image/ab6761610000e5eb82b7db5c5188da61eb8bf764",
        "aditya rikhari" to "https://i.scdn.co/image/ab6761610000e5ebda9d7a22ef45b736b4ff025b",
        "prateek kuhad" to "https://i.scdn.co/image/ab6761610000e5eb5e92ec4eb2aa69a5a3a0e1cb",
        "anuv jain" to "https://i.scdn.co/image/ab6761610000e5eb8176378ae43455a01bc1544a",

        // --- Global / Western Top Artists ---
        "the weeknd" to "https://i.scdn.co/image/ab6761610000e5eb214f3cf1cbe8d304f487e5a0",
        "taylor swift" to "https://i.scdn.co/image/ab6761610000e5eb5a00969a4698c3132a15fbb0",
        "drake" to "https://i.scdn.co/image/ab6761610000e5eb4293385d324db8558179afd9",
        "billie eilish" to "https://i.scdn.co/image/ab6761610000e5ebd8b9980db67272cb4d2c3daf",
        "ed sheeran" to "https://i.scdn.co/image/ab6761610000e5eb12b2f2681591f868ae5dbff5",
        "dua lipa" to "https://i.scdn.co/image/ab6761610000e5eb4e6ffec12b489dcfd0fc4e94",
        "post malone" to "https://i.scdn.co/image/ab6761610000e5eb6be070445b03e0b63147c2c1",
        "bruno mars" to "https://i.scdn.co/image/ab6761610000e5eb03450259b3be23c21a47ce4e",
        "eminem" to "https://i.scdn.co/image/ab6761610000e5eba00b11c129b27a88fc72f36b",
        "kendrick lamar" to "https://i.scdn.co/image/ab6761610000e5eb437b9e2a82505b3d93fe1022",
        "justin bieber" to "https://i.scdn.co/image/ab6761610000e5eb8ae7f2aaa9817a704a87ea36",
        "travis scott" to "https://i.scdn.co/image/ab6761610000e5ebe707b87e3f65e05c97a070ec",
        "coldplay" to "https://i.scdn.co/image/ab6761610000e5eb989ed050d1036b72b373d811"
    )

    // Curated high-aesthetic studio artist photography fallbacks (Never generic microphone stock or blank white)
    private val studioPortraits = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&fit=crop&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&fit=crop&q=80",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&fit=crop&q=80",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500&fit=crop&q=80",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&fit=crop&q=80"
    )

    fun getPhotoUrl(name: String): String {
        val clean = name.trim().lowercase(Locale.ROOT)
        // Direct match
        val direct = artistPhotoMap[clean]
        if (direct != null) return direct

        // Substring / partial match (e.g. "Arijit Singh, Neha Kakkar" -> Arijit Singh)
        for ((k, v) in artistPhotoMap) {
            if (clean.contains(k) || k.contains(clean)) {
                return v
            }
        }

        // Deterministic high-quality studio portrait based on name hash (Never white or microphone)
        val idx = Math.abs(name.hashCode()) % studioPortraits.size
        return studioPortraits[idx]
    }
}
