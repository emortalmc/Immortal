package dev.emortal.immortal.util

import dev.emortal.immortal.util.SmallCapsTranslatorUtil.font

fun String.smallcaps(): String {
    val translated = this.lowercase().map {
        if (it.code < 97 || it.code > 122) {
            it
        } else {
            font[it.code - 97]
        }
    }
    return translated.joinToString(separator = "")
}

object SmallCapsTranslatorUtil {
    val font = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀѕᴛᴜᴠᴡхʏᴢ".toList()
}