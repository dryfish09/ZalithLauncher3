
# Zalith Launcher 2+ (PLUS)

> **⚠️ UNOFFICIAL MODIFIED VERSION**
>
> This project will soon be changed its name to: TreeLauncher (in summer, when school and exams are done) and ill scope for better features and new ui, codebase then.)
> 
> This is an unofficial fork of [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2). This project is **not affiliated with or endorsed by the official Zalith Launcher project**.

**Zalith Launcher 2+** is a community-modified launcher for **Android devices** tailored for [Minecraft: Java Edition](https://www.minecraft.net/). It builds upon the foundation of [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2), utilizing [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk/app_pojavlauncher/src/main/jni) as its core launching engine with a modern UI built using **Jetpack Compose** and **Material Design 3**.

## 📋 What's New in This Fork?

This fork aims to enhance and customize the original Zalith Launcher 2 experience. Some key improvements include:

- [x] Cape system
- [x] Fixes
- [x] Offline accounts
- [x] Chroma names
- [x] Shortcuts in main screen
- [x] Importing/exporting settings
- [x] Importing/exporting accounts along with capes and skins

**Soon:**
- [ ] Friend system
- [ ] First ever performance update


## 🔗 Upstream Project

This project is derived from the excellent work of the Zalith Launcher team:
- **Original Project:** [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2)
- **Original License:** GPL-3.0

Please visit the upstream project if you want the official, unmodified version.

## 🌐 Language and Translation Support

This fork uses translations of Zalith Launcher 2. To contribute translations or improvements, please consider contributing upstream to the [Zalith Launcher 2 Weblate project](https://hosted.weblate.org/projects/zalithlauncher2).

## 📦 Build Instructions (For Developers)

### Requirements

* Android Studio **Bumblebee** or newer
* Android SDK:
  * **Minimum API level**: 26
  * **Target API level**: 35
* JDK 11

### Build Steps

```bash
git clone https://github.com/Star1xr/ZalithLauncher2Plus.git
# Open the project in Android Studio and build
```

## 📜 License

This project is licensed under the **[GPL-3.0 license](LICENSE)**, inherited from the upstream Zalith Launcher 2 project.

### Important Terms

**Contributions and Attribution**
   - All modifications are clearly documented and attributed to this fork
   - The upstream project is properly credited

## 📦 Open Source Libraries and Licenses

This project inherits all dependencies from Zalith Launcher 2. Please refer to the original project's [README](https://github.com/ZalithLauncher/ZalithLauncher2/blob/main/README.md) for the complete list of open source libraries and their licenses.

## 🤝 Contributing

| Library                               | Copyright                                                                                                     | License              | Official Link                                                                     |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------|----------------------|-----------------------------------------------------------------------------------|
| androidx-constraintlayout-compose     | Copyright © The Android Open Source Project                                                                   | Apache 2.0           | [Link](https://developer.android.com/develop/ui/compose/layouts/constraintlayout) |
| androidx-material-icons-core          | Copyright © The Android Open Source Project                                                                   | Apache 2.0           | [Link](https://developer.android.com/jetpack/androidx/releases/compose-material)  |
| androidx-material-icons-extended      | Copyright © The Android Open Source Project                                                                   | Apache 2.0           | [Link](https://developer.android.com/jetpack/androidx/releases/compose-material)  |
| ANGLE                                 | Copyright 2018 The ANGLE Project Authors                                                                      | BSD 3-Clause License | [Link](http://angleproject.org/)                                                  |
| Apache Commons Codec                  | -                                                                                                             | Apache 2.0           | [Link](https://commons.apache.org/proper/commons-codec)                           |
| Apache Commons Compress               | -                                                                                                             | Apache 2.0           | [Link](https://commons.apache.org/proper/commons-compress)                        |
| Apache Commons IO                     | -                                                                                                             | Apache 2.0           | [Link](https://commons.apache.org/proper/commons-io)                              |
| ByteHook                              | Copyright © 2020-2024 ByteDance, Inc.                                                                         | MIT License          | [Link](https://github.com/bytedance/bhook)                                        |
| BuildKeys                             | Copyright © 2026 MovTery                                                                                      | Aoache 2.0           | [Link](https://github.com/MovTery/BuildKeys)                                      |
| Coil Compose                          | Copyright © 2025 Coil Contributors                                                                            | Apache 2.0           | [Link](https://github.com/coil-kt/coil)                                           |
| Coil Gifs                             | Copyright © 2025 Coil Contributors                                                                            | Apache 2.0           | [Link](https://github.com/coil-kt/coil)                                           |
| Coil SVG                              | Copyright © 2025 Coil Contributors                                                                            | Apache 2.0           | [Link](https://github.com/coil-kt/coil)                                           |
| Fishnet                               | Copyright © 2025 Kyant                                                                                        | Apache 2.0           | [Link](https://github.com/Kyant0/Fishnet)                                         |
| gl4es_extra_extra                     | Copyright © 2016-2018 Sebastien Chevalier; Copyright (c) 2013-2016 Ryan Hileman                               | MIT License          | [Link](https://github.com/PojavLauncherTeam/gl4es_extra_extra)                    |
| Gson                                  | Copyright © 2008 Google Inc.                                                                                  | Apache 2.0           | [Link](https://github.com/google/gson)                                            |
| kotlinx.coroutines                    | Copyright © 2000-2020 JetBrains s.r.o.                                                                        | Apache 2.0           | [Link](https://github.com/Kotlin/kotlinx.coroutines)                              |
| ktor-client-cio                       | Copyright © 2000-2023 JetBrains s.r.o.                                                                        | Apache 2.0           | [Link](https://ktor.io)                                                           |
| ktor-client-content-negotiation       | Copyright © 2000-2023 JetBrains s.r.o.                                                                        | Apache 2.0           | [Link](https://ktor.io)                                                           |
| ktor-client-core                      | Copyright © 2000-2023 JetBrains s.r.o.                                                                        | Apache 2.0           | [Link](https://ktor.io)                                                           |
| ktor-http                             | Copyright © 2000-2023 JetBrains s.r.o.                                                                        | Apache 2.0           | [Link](https://ktor.io)                                                           |
| ktor-serialization-kotlinx-json       | Copyright © 2000-2023 JetBrains s.r.o.                                                                        | Apache 2.0           | [Link](https://ktor.io)                                                           |
| LWJGL - Lightweight Java Game Library | Copyright © 2012-present Lightweight Java Game Library All rights reserved.                                   | BSD 3-Clause License | [Link](https://github.com/LWJGL/lwjgl3)                                           |
| material-color-utilities              | Copyright 2021 Google LLC                                                                                     | Apache 2.0           | [Link](https://github.com/material-foundation/material-color-utilities)           |
| Maven Artifact                        | Copyright © The Apache Software Foundation                                                                    | Apache 2.0           | [Link](https://github.com/apache/maven/tree/maven-3.9.9/maven-artifact)           |
| Media3                                | Copyright © The Android Open Source Project                                                                   | Apache 2.0           | [Link](https://developer.android.com/jetpack/androidx/releases/media3)            |
| Mesa                                  | Copyright © The Mesa Authors                                                                                  | MIT License          | [Link](https://mesa3d.org/)                                                       |
| MMKV                                  | Copyright © 2018 THL A29 Limited, a Tencent company.                                                          | BSD 3-Clause License | [Link](https://github.com/Tencent/MMKV)                                           |
| Navigation 3                          | Copyright © The Android Open Source Project                                                                   | Apache 2.0           | [Link](https://developer.android.com/jetpack/androidx/releases/navigation3)       |
| NBT                                   | Copyright © 2016 - 2020 Querz                                                                                 | MIT License          | [Link](https://github.com/Querz/NBT)                                              |
| NG-GL4ES                              | Copyright © 2016-2018 Sebastien Chevalier; Copyright © 2013-2016 Ryan Hileman; Copyright (c) 2025-2026 BZLZHH | MIT License          | [Link](https://github.com/BZLZHH/NG-GL4ES)                                        |
| OkHttp                                | Copyright © 2019 Square, Inc.                                                                                 | Apache 2.0           | [Link](https://github.com/square/okhttp)                                          |
| Okio                                  | Copyright © 2013 Square, Inc.                                                                                 | Apache 2.0           | [Link](https://square.github.io/okio/)                                            |
| Process Phoenix                       | Copyright © 2015 Jake Wharton                                                                                 | Apache 2.0           | [Link](https://github.com/JakeWharton/ProcessPhoenix)                             |
| proxy-client-android                  | -                                                                                                             | LGPL-3.0 License     | [Link](https://github.com/TouchController/TouchController)                        |
| Reorderable                           | Copyright © 2023 Calvin Liang                                                                                 | Apache 2.0           | [Link](https://github.com/Calvin-LL/Reorderable)                                  |
| skinview3d                            | Copyright © 2014-2018 Kent Rasmussen; Copyright © 2017-2022 Haowei Wen, Sean Boult and contributors           | MIT License          | [Link](https://github.com/bs-community/skinview3d)                                |
| sora-editor                           | Copyright © 1991, 1999 Free Software Foundation, Inc.                                                         | LGPL-2.1 License     | [Link](https://github.com/Rosemoe/sora-editor)                                    |
| StringFog                             | Copyright © 2016-2023, Megatron King                                                                          | Apache 2.0           | [Link](https://github.com/MegatronKing/StringFog)                                 |
| XZ for Java                           | Copyright © The XZ for Java authors and contributors                                                          | 0BSD License         | [Link](https://tukaani.org/xz/java.html)                                          |
