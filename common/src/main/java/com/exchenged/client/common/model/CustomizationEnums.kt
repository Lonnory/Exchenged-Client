package com.exchenged.client.common.model

import com.exchenged.client.common.R

enum class AppTheme(val labelRes: Int) {
    PURPLE_BLUE(R.string.theme_purple_blue),
    OCEAN(R.string.theme_ocean),
    BLACK_WHITE(R.string.theme_black_white),
    AMOLED(R.string.theme_amoled),
    LIGHT(R.string.theme_light),
    RED_GRAY(R.string.theme_red_gray),
    DYNAMIC(R.string.theme_dynamic)
}

enum class AppWallpaper(val labelRes: Int) {
    NONE(R.string.wallpaper_none),
    GRADIENT(R.string.wallpaper_gradient),
    LIQUID_GLASS(R.string.wallpaper_liquid_glass),
    EMOJI_WORKSHOP(R.string.wallpaper_emoji_workshop)
}

enum class AppLanguage(val code: String, val labelRes: Int) {
    RUSSIAN("ru", R.string.russian)
}

enum class PingType {
    ICMP, GET
}
