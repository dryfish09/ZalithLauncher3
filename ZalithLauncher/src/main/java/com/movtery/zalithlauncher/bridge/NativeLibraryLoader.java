/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.bridge;

import android.util.Log;

public class NativeLibraryLoader {
    private static final String TAG = "NativeLibraryLoader";

    /**
     * Android 14 (API 34) üzerinde ReplayMod video export ederken
     * "cannot locate symbol native_handle_create referenced by libandroid.so"
     * hatası alınıyor. Bunun sebebi, native_handle_create'in Android 14'te
     * libnativewindow.so'da tanımlı olması ve FFmpeg dlopen edildiğinde
     * linker'ın bu sembolü çözümleyememesidir.
     * <p>
     * Çözüm: Oyun başlamadan önce, ZLBridge.dlopenGlobalSphal ile
     * (android_dlopen_ext + RTLD_GLOBAL) bu kütüphaneleri sphal/default
     * namespace'inde yükleyip sembolleri süreç genelinde görünür hale getiririz.
     * <p>
     * Bazı cihazlarda (ColorOS/RealmeUI/HyperOS) linker namespace'leri
     * normal dlopen'ı engelleyebilir; bu yüzden android_dlopen_ext ile
     * doğrudan doğru namespace hedeflenir.
     * <p>
     * ÖNEMLİ: System.loadLibrary (RTLD_LOCAL) KULLANMIYORUZ. Bir kütüphane
     * RTLD_LOCAL ile yüklendikten sonra RTLD_GLOBAL'a çevrilemez.
     * Bu yüzden sadece RTLD_GLOBAL ile yükleme yapıyoruz.
     */
    public static void reloadFFmpegSystemDependenciesGlobally() {
        dlopenSystemLibGlobally("libcutils.so");
        dlopenSystemLibGlobally("libandroid.so");
        dlopenSystemLibGlobally("libmediandk.so");
        dlopenSystemLibGlobally("libnativewindow.so");
    }

    private static void dlopenSystemLibGlobally(String libName) {
        try {
            boolean ok = ZLBridge.dlopenGlobalSphal(libName);
            if (ok) {
                Log.i(TAG, "Globally loaded: " + libName);
                return;
            }
            Log.w(TAG, "ZLBridge.dlopenGlobalSphal failed for " + libName + " (no fallback)");
        } catch (Exception e) {
            Log.w(TAG, "Error globally loading " + libName + " via dlopenGlobalSphal", e);
        }
    }

    public static void loadPojavLib() {
        System.loadLibrary("pojavexec");
    }

    public static void loadExitHookLib() {
        System.loadLibrary("exithook");
    }

    public static void loadPojavAWTLib() {
        System.loadLibrary("pojavexec_awt");
    }
}
