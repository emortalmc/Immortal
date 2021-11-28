package dev.emortal.immortal.config

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ConfigHelper {

    inline fun <reified T : Any> initConfigFile(path: Path, emptyObj: T, format: Json = prettyPrintFormat): T {
        return if (path.exists()) format.decodeFromString(path.readText()) else run {
            path.writeText(format.encodeToString(emptyObj))
            emptyObj
        }
    }

    inline fun <reified T : Any> writeObjectToPath(path: Path, emptyObj: T, format: Json = prettyPrintFormat) {
        path.writeText(format.encodeToString(emptyObj))
    }

    val prettyPrintFormat = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    val noPrettyPrintFormat = Json {
        encodeDefaults = true
        prettyPrint = false
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

}