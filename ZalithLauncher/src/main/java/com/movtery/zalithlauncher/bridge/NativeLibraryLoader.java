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
     * Android 14 (API 34) üzerinde bazı cihazlarda, Replay Mod ile video
     * dışa aktarılırken libffmpeg.so üzerinden linker şu hatayı veriyordu:
     * "cannot locate symbol native_handle_create referenced by libandroid.so"
     * <p>
     * readelf ile bakıldığında libffmpeg.so'nun kendisi libandroid'e bağımlı
     * görünmüyor, ancak FFmpeg'in altında kullandığı bazı alt kütüphaneler
     * (MediaCodec/AImage tabanlı donanım kodlayıcı yolları) çalışma zamanında
     * bu sembolleri dolaylı olarak tetikliyor. Sorun, bu sistem kütüphanelerinin
     * FFmpeg dlopen edilene kadar süreç içinde henüz yüklenmemiş/bağlanmamış
     * olmasından kaynaklanıyor.
     * <p>
     * Çözüm: oyun süreci başlamadan (yani {@link ZLBridge} ilk dokunulup asıl
     * pojavexec/awt kütüphaneleri yüklenmeden) hemen önce, Java katmanında
     * System.loadLibrary ile bu sistem kütüphanelerini zorla önden yükleyip
     * sembollerini sürecin genelinde çözümlenebilir hale getiriyoruz.
     * <p>
     * Bazı cihazlarda/mimarilerde bu kütüphanelerden biri bulunamayabilir ya da
     * zaten yüklenmiş olabilir; bu durumda oyunun başlamasını engellememesi için
     * her biri ayrı ayrı, birbirinden bağımsız olarak try/catch içinde yükleniyor.
     */
    public static void preloadFFmpegSystemDependencies() {
        loadSystemLibraryQuietly("cutils");
        loadSystemLibraryQuietly("android");
        loadSystemLibraryQuietly("mediandk");
    }

    /**
     * System.loadLibrary kullanır (RTLD_LOCAL). Aşağıdaki yöntem,
     * ZLBridge.dlopen (RTLD_GLOBAL) ile aynı kütüphaneleri yeniden
     * yükleyerek sembollerin süreç genelinde görünür olmasını sağlar.
     * Bu, FFmpeg gibi daha sonra dlopen ile yüklenen kütüphanelerin
     * native_handle_create vb. sembolleri bulamamasını engeller.
     */
    public static void reloadFFmpegSystemDependenciesGlobally() {
        dlopenSystemLibGlobally("libcutils.so");
        dlopenSystemLibGlobally("libandroid.so");
        dlopenSystemLibGlobally("libmediandk.so");
    }

    private static void dlopenSystemLibGlobally(String libName) {
        try {
            boolean ok = ZLBridge.dlopen(libName);
            if (ok) {
                Log.i(TAG, "Globally loaded: " + libName);
            } else {
                Log.w(TAG, "Failed to globally load: " + libName);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error globally loading: " + libName, e);
        }
    }

    private static void loadSystemLibraryQuietly(String libraryName) {
        try {
            System.loadLibrary(libraryName);
            Log.i(TAG, "Preloaded system library: lib" + libraryName + ".so");
        } catch (UnsatisfiedLinkError | SecurityException e) {
            //bazı cihazlarda/mimarilerde bu kütüphane bulunamayabilir veya erişilemez olabilir,
            //bu durumda sessizce günlüğe yazıp devam ediyoruz; bu yüzden oyunun başlamasını engellememeli
            Log.w(TAG, "Failed to preload system library: lib" + libraryName + ".so", e);
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
