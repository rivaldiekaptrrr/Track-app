package com.trackit.app.nfc.parser

/**
 * Pemetaan kode stasiun KRL Commuter Line (KMT - Kartu Multi Trip)
 * berdasarkan hasil reverse engineering eSaldo Info (com.work.cardinfo.emoney.utilities.b).
 */
object KmtStationData {

    private val stations: Map<Int, String> = mapOf(
        1 to "Stasiun KRL Tanah Abang",
        257 to "Stasiun KRL Bogor",
        258 to "Stasiun KRL Cilebut",
        259 to "Stasiun KRL Bojonggede",
        260 to "Stasiun KRL Citayam",
        261 to "Stasiun KRL Depok",
        262 to "Stasiun KRL Depok Baru",
        263 to "Stasiun KRL Pondok Cina",
        264 to "Stasiun KRL Univ. Indonesia",
        265 to "Stasiun KRL Univ. Pancasila",
        266 to "Stasiun KRL Lenteng Agung",
        267 to "Stasiun KRL Tanjung Barat",
        268 to "Stasiun KRL Pasar Minggu",
        275 to "Stasiun KRL Pasar Minggu Baru",
        276 to "Stasiun KRL Duren Kalibata",
        277 to "Stasiun KRL Cawang",
        278 to "Stasiun KRL Tebet",
        279 to "Stasiun KRL Manggarai",
        280 to "Stasiun KRL Cikini",
        281 to "Stasiun KRL Gondangdia",
        282 to "Stasiun KRL Juanda",
        283 to "Stasiun KRL Sawah Besar",
        284 to "Stasiun KRL Mangga Besar",
        285 to "Stasiun KRL Jayakarta",
        286 to "Stasiun KRL Jakarta Kota",
        287 to "Stasiun KRL Bekasi",
        294 to "Stasiun KRL Kranji",
        295 to "Stasiun KRL Cakung",
        296 to "Stasiun KRL Klender Baru",
        297 to "Stasiun KRL Buaran",
        304 to "Stasiun KRL Klender",
        305 to "Stasiun KRL Jatinegara",
        306 to "Stasiun KRL Tangerang",
        307 to "Stasiun KRL Karet",
        308 to "Stasiun KRL Sudirman",
        313 to "Stasiun KRL Tangerang",
        329 to "Stasiun KRL Tanah Abang",
        330 to "Stasiun KRL Palmerah",
        331 to "Stasiun KRL Kebayoran",
        332 to "Stasiun KRL Pondok Ranji",
        333 to "Stasiun KRL Jurang Mangu",
        334 to "Stasiun KRL Sudimara",
        335 to "Stasiun KRL Rawabuntu",
        342 to "Stasiun KRL Serpong",
        343 to "Stasiun KRL Cisauk",
        344 to "Stasiun KRL Cicayur",
        345 to "Stasiun KRL Parung Panjang",
        352 to "Stasiun KRL Cilejit",
        353 to "Stasiun KRL Daru",
        354 to "Stasiun KRL Tenjo",
        355 to "Stasiun KRL Tigaraksa",
        356 to "Stasiun KRL Maja",
        357 to "Stasiun KRL Citeras",
        358 to "Stasiun KRL Rangkasbitung",
        374 to "Stasiun KRL Bekasi Timur",
        376 to "Stasiun KRL Cikarang"
    )

    fun getStationName(stationId: Int): String? {
        return stations[stationId]
    }
}

