# Morphe Music — build notes

Patched YouTube Music APK built from the RVX / `anddea/revanced-patches` bundle.

## Repository (upstream of this work)
`limonadeneon999-ux/wow`, cloned at `/workspace/project/wow`.

- `main` @ `578431f` — only the `SystemMonitorApp 2` Swift project and macOS CI. **No Morphe Music source.**
  Tagged `ytm-ultimate-v2`, which is misleading: that tag points at a commit with no music code.
- `origin/morphe-music-rvx` @ `86ae2b1` — the real work. Morphe Music lives under `morphe-music/`
  (a full RVX checkout); `SystemMonitorApp 2` is untouched there.
- Tags `ytm-ultimate-v2` (on main) and `ytm-ultimate-v3` (on `morphe-music-rvx`).
- Releases publish `ytm_ultimate_v2.apk` / `ytm_ultimate_v3.apk` (both 87 680 336 bytes).

The patcher workspace at `/tmp/rvx-src`, `/tmp/cli`, `/tmp/out` **no longer exists** — `/tmp` was wiped.
Everything needed to rebuild is on `morphe-music-rvx` under `morphe-music/`.

## Source layout on `morphe-music-rvx`
Download pipeline — `morphe-music/extensions/shared/src/main/java/app/morphe/extension/music/patches/downloads/`:
`LocalDownloadManager`, `Mp3Transcoder`, `AudioTagger`, `NativeAudioStore`, `NativeDownloads`,
`PlaylistDownloads`, `TopTracks`, `TrackMetadata`, `DownloadsConfig`, `DownloadedMark`.

Note the naming differs from the earlier local work: `TopTracks` (not `AutoDownloader`/`PlayHistory`),
`DownloadsConfig.DownloadFormat`/`DownloadQuality` (not separate `DownloadFormat`/`DownloadQuality`
classes), and no `Id3Writer`/`StreamSelector`/`Mp4Muxer` — ID3 writing is inside `Mp3Transcoder`.

Settings keys on the branch: `morphe_music_download_format`, `morphe_music_download_quality`,
`morphe_music_auto_download_top_tracks`, `morphe_music_auto_download_top_tracks_count`
(`SliderConfig(1, 1000, 1, "tracks")`, default 10), `morphe_music_auto_download_wifi_only`.

## Build
```sh
cd /workspace/project/wow/morphe-music
export JAVA_HOME=/workspace/project/toolchain/jdk-21.0.13.1+2
export ANDROID_HOME=/workspace/project/toolchain/sdk
./gradlew :patches:build
```
`extensions/shared/build.gradle.kts` renames each DEX to `shared<N>.mpe` because the patcher merges
one DEX per extension. A `javax.sound.sampled.AudioFormat` stub exists because jump3r imports it.

## Toolchain
- JDK 21: `toolchain/jdk-21.0.13.1+2`
- Android SDK: `toolchain/sdk` (build-tools 35.0.0 for `apksigner`, `zipalign`, `aapt2`, `dexdump`)
- `unzip` is absent; use Python `zipfile` to inspect APKs.

## Unit tests
`:extensions:shared:testReleaseUnitTest` does not exist — use the debug variant:
```sh
./gradlew :extensions:shared:testDebugUnitTest --tests "*downloads*"
```
The `lyrics` and YouTube caption suites fail with `Method i in android.util.Log not mocked` —
pre-existing, they need Robolectric or a `returnDefaultValues` unit-test option.

**Gradle cannot run in this sandbox at all**: `settings.gradle.kts` applies
`id("app.morphe.patches") version "2.0.0"`, which only exists in the private
`maven.pkg.github.com/MorpheApp/registry` and 404s with the available token. Every `./gradlew`
invocation therefore fails during settings evaluation, before any task runs.

Workaround used to validate tests: compile only the JDK-pure classes with `javac` and run JUnit
directly (no Android jar needed when the class under test avoids `android.*`):
```sh
curl -sLO https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
curl -sLO https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
$JAVA_HOME/bin/javac -d /tmp/tout -cp "junit-4.13.2.jar:hamcrest-core-1.3.jar" <sources...>
$JAVA_HOME/bin/java -cp "/tmp/tout:junit-4.13.2.jar:hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore <test classes...>
```
`AudioTagger` and `DownloadsConfig` are JDK-pure and are covered this way
(`AudioTaggerTest` 14 tests, `DownloadsConfigTest` 7 tests, all passing).

## Known patch issues on 9.15.51
- `Visual preferences icons for YouTube Music` fails: missing `pref_key_parent_tools_icon.xml`.
- Unsupported by version: compact dialog, hide handle, smooth transition animation, restore old
  comments popup panels, spoof app version.

## Root cause of the "slider resets at startup" bug (release `ytm-ultimate-v3`)
`Resetting out-of-range slider setting: morphe_music_auto_download_top_tracks_count` at startup.
The v3 APK was **not rebuilt** — its DEX was hand-edited:

- `classes10.dex` differs from the v2 APK by exactly **one code byte** (`0x1faf6f`: `0x59` → `0x8f`).
  That byte is the high half of a `const-wide/high16` literal, changing the setting's max from
  `100.0` to `992.0`. It cannot encode `1000.0`: that value needs a full 64-bit `const-wide`
  (`0x408F400000000000`), not a high16 immediate. So the intended 1000 was written as the nearest
  high16 approximation.
- `Settings.<clinit>` in the v3 DEX has only **5** `SeekBarPreference.register` calls, and none of
  them references `AUTO_DOWNLOAD_TOP_TRACKS_COUNT` (the field is only ever `sput` in `<clinit>`,
  never `sget`). So no `SliderConfig` is registered for the count key at runtime, and
  `Setting.java:519` logs the out-of-range reset.
- `classes8.dex` differs only in `PatchStatus.PatchedTime` and the DEX checksum/signature.
- The source is fine: preference XML (`SeekBarPreference`, `key`, `dependency`) and the
  `*_entry_values` arrays (`OPUS/MP3/MP4`, `LOW/STANDARD/HIGH`) in the APK are correct.

The real fix must come from a **fresh `:patches:build` + patcher run**, not from editing DEX bytes.
That is blocked here by the private plugin registry (see Unit tests above).

### Fixed build: `ytm_ultimate_v4.apk`
Built by patching the v3 `classes10.dex` with apktool 2.11.1 (baksmali + smali round trip), then
`zipalign -p 4` and `apksigner` (v2 scheme only). SHA-256 `522e339c37e87c1b807e1f97dba5fa6356cb7ad5590596ba62a89a4545f4de28`.

Two smali edits in `app/morphe/extension/music/settings/Settings.smali`:
1. `const-wide/high16 v28, 0x408f000000000000L # 992.0` → `const-wide v28, 0x408f400000000000L # 1000.0`.
2. Appended a 6th `SeekBarPreference.register(new SeekBarConfig(AUTO_DOWNLOAD_TOP_TRACKS_COUNT,
   1, 1000, 1, "tracks"))` **at the end of `<clinit>`, just before `return-void`**. It must come
   after the `sput-object` that assigns the field — inserting it in the source's static block
   position reads a null field, because `<clinit>` has not reached the assignment yet.

Verification of the rebuilt APK: 74 577 classes in all 10 DEX (identical to v3), 6 register calls,
`#double 1000` present, zip CRC clean, v2 signature valid, zipalign OK, badging unchanged
(`anddea.youtube.music` 91551240 / 9.15.51).

**Install note**: v4 is signed with a *new* 4096-bit RSA key (`CN=Morphe, OU=Morphe Music`), not the
key used for v2/v3, so it cannot upgrade an existing install. Uninstall the old app first, which
drops local downloads and settings.

Source-level cleanup still needed (not required for the fixed APK): in
`music/settings/Settings.java` the static block at line 389 references
`AUTO_DOWNLOAD_TOP_TRACKS_COUNT` declared at line 431 — an *illegal forward reference* that makes
the class uncompilable as written. The registration belongs after the field declarations, or the
count setting should move above the static block.

APK audit of v3 that came back clean: v2 signature scheme valid (CN=Morphe, RSA 4096), zipalign
`-p 4` OK, all 5814 entries CRC-valid, 10 DEX headers self-consistent, no duplicate zip entries,
resources and preference XML consistent.

## Download format/quality was never applied to stream selection (fixed in `fabd3fb`)
`LocalDownloadManager.parseStream` filtered audio formats and then took
`.max(Comparator.comparingInt(Format::getBitrate))`, ignoring `Settings.DOWNLOAD_FORMAT`
completely. Consequences before the fix:
- MP4 and OPUS selections had no effect on which stream was fetched.
- The MP4 "quality" setting could never pick a bitrate.
- MP3 worked only because the transcoder re-encoded whatever AAC stream arrived.

Now the container follows the setting: OPUS → WebM/Opus stream, MP3 and MP4 → AAC (`audio/mp4`).
MP3 keeps the highest source (LAME sets the output bitrate itself); MP4 is a passthrough, so the
quality picks the closest bitrate with a tie-break toward the higher one. Null settings fall back to
MP3/STANDARD because the enums can be absent on an older patched install (same guard `TopTracks`
already used).

`PlaylistDownloads.enqueue` calls `LocalDownloadManager.download(...)` per track, so the playlist
button inherits this behaviour — no separate code path needed.

Tests: `StreamSelectionTest` mirrors the filter/comparator and pins all of the above (28 tests total
across the three downloads test classes).

## Shipping a Java-level change in this sandbox
Blocked. `maven.pkg.github.com/MorpheApp/registry` answers **401** for the `app.morphe.patches`
plugin with the available token, so `:patches:build` cannot run and `LocalDownloadManager` cannot be
recompiled into a DEX here.

The v4 APK was produced by smali-editing the released v3 `classes10.dex`, which only worked because
that fix was a two-instruction change. A Java-level change like the `parseStream` fix needs either
the plugin registry, or a manual javac build of the extension against `android.jar` + protobuf +
jump3r followed by a DEX swap.

