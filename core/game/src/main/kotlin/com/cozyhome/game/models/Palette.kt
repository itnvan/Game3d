package com.cozyhome.game.models

import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.rgb

object Palette {
    val GRASS = rgb(0x86DC5E)
    val GRASS_DARK = rgb(0x62C24A)
    val GRASS_FAR = rgb(0x9BE27A)
    val PATH = rgb(0xF5E2B8)
    val PATH_EDGE = rgb(0xE6CC98)
    val STONE = rgb(0xD9D4CF)
    val STONE_DARK = rgb(0xB9B2AB)
    val SOIL = rgb(0x9C6B4A)
    val SOIL_WET = rgb(0x6E4632)
    val WOOD = rgb(0xD9A066)
    val WOOD_LIGHT = rgb(0xEBC48F)
    val WOOD_DARK = rgb(0x9B6A43)
    val WHITE = rgb(0xFFFFFF)
    val OFFWHITE = rgb(0xFBF7EF)
    val INK = rgb(0x3B3355)
    val GLASS = rgb(0xCDEFFF)
    val WATER = rgb(0x5CC8F2)
    val WATER_DEEP = rgb(0x3BA7DB)
    val TRACK = rgb(0xF08A6C)
    val TRACK_LINE = rgb(0xFFF4EA)
    val COURT = rgb(0xFFB27A)
    val COURT_KEY = rgb(0x8ED1FC)
    val ROOF = rgb(0xF2727F)
    val ROOF_DARK = rgb(0xD95564)
    val TRUNK = rgb(0x9A6B4F)
    val LEAF = rgb(0x52C46E)
    val LEAF_LIGHT = rgb(0x7EDC86)
    val LEAF_DEEP = rgb(0x3FA85E)
    val BLOSSOM = rgb(0xFFB7D0)
    val METAL = rgb(0xC9D3DD)
    val ORANGE = rgb(0xFF9F43)
    val RED = rgb(0xFF5D6C)
    val YELLOW = rgb(0xFFD54F)
    val PINK = rgb(0xFF9AB8)
    val MINT = rgb(0x6EE7B7)
    val SKY = rgb(0x7DD3FC)
    val LILAC = rgb(0xC4B5FD)
    val PEACH = rgb(0xFFC9A3)
    val CORAL = rgb(0xFB7185)
    val NAVY = rgb(0x4B5D9E)

    val SKIN = listOf(rgb(0xFFE0C7), rgb(0xF6C9A8), rgb(0xE8B08A), rgb(0xC98E66), rgb(0xA0694A), rgb(0x7A4E36))
    val HAIR = listOf(rgb(0x2E2433), rgb(0x6B3E2E), rgb(0xA8653A), rgb(0xF2C46B), rgb(0xFF8FB1), rgb(0x7AB8FF), rgb(0xB79CFF), rgb(0xF0F0F0))
    val SHIRT = listOf(rgb(0xFFD166), rgb(0x6EE7B7), rgb(0x7DD3FC), rgb(0xFF8FA3), rgb(0xC4B5FD), rgb(0xFFFFFF), rgb(0xFF9F43), rgb(0x34D399))
    val PANTS = listOf(rgb(0x5B7FD6), rgb(0x3B3355), rgb(0xF5E2B8), rgb(0x8E6CEF), rgb(0x56B98A), rgb(0xE86A7A))
    val SHOES = listOf(rgb(0xFFFFFF), rgb(0xFF6B6B), rgb(0x3B3355), rgb(0xFFC94D), rgb(0x56CCF2))
    const val HAIR_STYLES = 5
    const val HATS = 4

    val WALLS = listOf(rgb(0xFFF3DC), rgb(0xD8F5E6), rgb(0xDDF0FF), rgb(0xFFE1E6), rgb(0xEDE4FF), rgb(0xFFEBC2), rgb(0xE9F0D8))
    val FLOORS = listOf(
        rgb(0xE8BE88) to rgb(0xDDAE76),
        rgb(0xB98258) to rgb(0xA87350),
        rgb(0xF6F2EA) to rgb(0xE4DED3),
        rgb(0xBFEBD8) to rgb(0xA9DEC7),
        rgb(0xF4D6DE) to rgb(0xEBC2CD),
    )
}
