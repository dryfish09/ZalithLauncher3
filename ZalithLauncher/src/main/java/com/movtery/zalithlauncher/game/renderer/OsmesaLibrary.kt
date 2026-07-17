package com.movtery.zalithlauncher.game.renderer

import com.movtery.zalithlauncher.setting.AllSettings

fun osmesaLibrary(): String {
    return if (AllSettings.osmesaVersion.state == 23) "libOSMesa_2300d.so" else "libOSMesa_8.so"
}
