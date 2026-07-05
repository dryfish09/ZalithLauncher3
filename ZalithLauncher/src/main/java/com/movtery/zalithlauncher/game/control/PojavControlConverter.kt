package com.movtery.zalithlauncher.game.control

import android.util.DisplayMetrics
import androidx.compose.ui.graphics.Color
import com.movtery.layer_controller.EDITOR_VERSION
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonShape
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.ButtonStyle
import com.movtery.layer_controller.data.TextAlignment
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.TranslatableString
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayer
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.utils.randomUUID
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.long

private const val POJAV_MARGIN_DP = 2f

fun isPojavFormat(jsonString: String): Boolean {
    return jsonString.contains("\"mControlDataList\"")
}

fun convertPojavToZalith(jsonString: String, displayMetrics: DisplayMetrics): ControlLayout {
    val root = kotlinx.serialization.json.Json.parseToJsonElement(jsonString).jsonObject
    val buttons = root["mControlDataList"]?.jsonArray ?: throw IllegalArgumentException("Not a Pojav layout")

    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
    val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density

    val pojavStyleUuid = randomUUID()

    val zalithButtons = buttons.mapNotNull { btnObj ->
        val btn = btnObj.jsonObject
        val name = btn["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val w = (btn["width"]?.jsonPrimitive?.int ?: 50).toFloat()
        val h = (btn["height"]?.jsonPrimitive?.int ?: 50).toFloat()
        val rawX = btn["dynamicX"]?.jsonPrimitive?.content ?: "${pojavMargin}"
        val rawY = btn["dynamicY"]?.jsonPrimitive?.content ?: "${pojavMargin}"

        val xDp = evalPojavExpr(rawX, w, h, screenWidthDp, screenHeightDp)
        val yDp = evalPojavExpr(rawY, w, h, screenWidthDp, screenHeightDp)

        val xPct = ((xDp / screenWidthDp) * 10000).toInt().coerceIn(0, 10000)
        val yPct = ((yDp / screenHeightDp) * 10000).toInt().coerceIn(0, 10000)

        val keycodes = btn["keycodes"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
        val primaryKey = keycodes.getOrNull(0) ?: -1
        val isToggle = btn["isToggle"]?.jsonPrimitive?.boolean ?: false
        val isPenetrable = btn["passThruEnabled"]?.jsonPrimitive?.boolean ?: false

        val events = pojavKeyToClickEvents(primaryKey)

        com.movtery.layer_controller.data.NormalData(
            text = TranslatableString(name, emptyList()),
            uuid = randomUUID(),
            position = ButtonPosition(x = xPct, y = yPct),
            buttonSize = ButtonSize(
                type = ButtonSize.Type.Dp,
                widthDp = w.coerceAtLeast(5f),
                heightDp = h.coerceAtLeast(5f),
                widthPercentage = 1400,
                heightPercentage = 1400,
                widthReference = ButtonSize.Reference.ScreenHeight,
                heightReference = ButtonSize.Reference.ScreenHeight
            ),
            buttonStyle = pojavStyleUuid,
            textAlignment = TextAlignment.Center,
            visibilityType = VisibilityType.ALWAYS,
            _clickEvents = events,
            isSwipple = false,
            isPenetrable = isPenetrable,
            isToggleable = isToggle
        )
    }

    val bg = Color(0x4D000000)
    val pressedBg = Color(0x80000000)
    val fg = Color.White

    val styleConfig = ButtonStyle.StyleConfig(
        alpha = 1f,
        pressedAlpha = 1f,
        backgroundColor = bg,
        pressedBackgroundColor = pressedBg,
        contentColor = fg,
        pressedContentColor = fg,
        fontSize = null,
        pressedFontSize = null,
        borderWidth = 0,
        pressedBorderWidth = 0,
        borderColor = fg,
        pressedBorderColor = fg,
        borderRadius = ButtonShape(8f),
        pressedBorderRadius = ButtonShape(8f)
    )

    val style = ButtonStyle(
        name = "Pojav",
        uuid = pojavStyleUuid,
        animateSwap = false,
        commonStyle = true,
        lightStyle = styleConfig,
        darkStyle = styleConfig
    )

    return ControlLayout(
        info = ControlLayout.Info(
            name = TranslatableString("Pojav", emptyList()),
            author = TranslatableString("PojavLauncherTeam", emptyList()),
            description = TranslatableString("PojavLauncher touch controls", emptyList()),
            versionCode = 0,
            versionName = "1.0"
        ),
        layers = listOf(
            ControlLayer(
                name = "controls",
                uuid = randomUUID(),
                hide = false,
                hideWhenMouse = true,
                hideWhenGamepad = true,
                visibilityType = VisibilityType.ALWAYS,
                normalButtons = zalithButtons,
                textBoxes = emptyList()
            )
        ),
        styles = listOf(style),
        special = ControlLayout.Special(),
        editorVersion = EDITOR_VERSION
    )
}

private val pojavMargin = POJAV_MARGIN_DP

private fun evalPojavExpr(expr: String, btnW: Float, btnH: Float, sw: Float, sh: Float): Float {
    val margin = POJAV_MARGIN_DP
    var s = expr
    s = s.replace("\${margin}", margin.toString())
    s = s.replace("\${width}", btnW.toString())
    s = s.replace("\${height}", btnH.toString())
    s = s.replace("\${screen_width}", sw.toString())
    s = s.replace("\${screen_height}", sh.toString())
    s = s.replace("\${right}", (sw - btnW - margin).toString())
    s = s.replace("\${bottom}", (sh - btnH - margin).toString())
    return evaluateSimple(s)
}

private fun evaluateSimple(expr: String): Float {
    val cleaned = expr.replace(" ", "")
    if (cleaned.all { it.isDigit() || it == '.' }) return cleaned.toFloat()

    return try {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (c in cleaned) {
            if (c in "+-*/()") {
                if (current.isNotEmpty()) { tokens.add(current.toString()); current.clear() }
                tokens.add(c.toString())
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())

        var idx = 0
        fun parseExpr(): Float {
            var result = parseTerm()
            while (idx < tokens.size && (tokens[idx] == "+" || tokens[idx] == "-")) {
                val op = tokens[idx++]
                val rhs = parseTerm()
                result = if (op == "+") result + rhs else result - rhs
            }
            return result
        }
        fun parseTerm(): Float {
            var result = parseFactor()
            while (idx < tokens.size && (tokens[idx] == "*" || tokens[idx] == "/")) {
                val op = tokens[idx++]
                val rhs = parseFactor()
                result = if (op == "*") result * rhs else result / rhs
            }
            return result
        }
        fun parseFactor(): Float {
            if (idx >= tokens.size) return 0f
            val t = tokens[idx]
            if (t == "(") { idx++; val v = parseExpr(); idx++; return v }
            if (t == "-") { idx++; return -parseFactor() }
            idx++
            return t.toFloatOrNull() ?: 0f
        }
        parseExpr()
    } catch (_: Exception) {
        0f
    }
}

private fun pojavKeyToClickEvents(keycode: Int): List<ClickEvent> {
    val keyName = when (keycode) {
        -1 -> return listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_ime"))
        -2 -> return listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_menu"))
        -3 -> return listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "GLFW_MOUSE_BUTTON_LEFT"))
        -4 -> return listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "GLFW_MOUSE_BUTTON_RIGHT"))
        -5 -> return listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_menu"))
        32 -> "GLFW_KEY_SPACE"
        65 -> "GLFW_KEY_A"
        68 -> "GLFW_KEY_D"
        69 -> "GLFW_KEY_E"
        83 -> "GLFW_KEY_S"
        84 -> "GLFW_KEY_T"
        87 -> "GLFW_KEY_W"
        258 -> "GLFW_KEY_TAB"
        292 -> "GLFW_KEY_F3"
        294 -> "GLFW_KEY_F5"
        340 -> "GLFW_KEY_LEFT_SHIFT"
        341 -> "GLFW_KEY_LEFT_CONTROL"
        342 -> "GLFW_KEY_LEFT_ALT"
        346 -> "GLFW_KEY_RIGHT_SHIFT"
        347 -> "GLFW_KEY_RIGHT_CONTROL"
        348 -> "GLFW_KEY_RIGHT_ALT"
        else -> "GLFW_KEY_UNKNOWN"
    }
    return listOf(ClickEvent(ClickEvent.Type.Key, keyName))
}
