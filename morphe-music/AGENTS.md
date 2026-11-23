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

## This sandbox resets: push as you go

`/tmp` and `~/.gradle` are wiped on reset; `/workspace/project` survives. Anything only in `/tmp`
(baksmali trees, jars, scratch APKs) is gone. Commit **and push** after each change, and keep
release assets on GitHub rather than only on disk — the v4/v5/v6 APKs now live in releases for
exactly this reason.

## Gradle builds need the MorpheApp plugin, which is not publicly reachable

`:extensions:shared:testDebugUnitTest` needs the `app.morphe.patches` plugin. It is not on Maven
Central, Gradle Plugin Portal, or JitPack, and `maven.pkg.github.com/MorpheApp/registry` answers 401
for this token, so **a wiped Gradle cache cannot be rebuilt from scratch here**. `toolchain/m2/plugin.jar`
is a saved copy of the plugin, but installing it into `~/.m2` by hand is not enough: its POM must also
declare AGP, and guessing that pulls in a version conflict with the project's Gradle 9.8.2.

Consequence: re-running the unit tests after a reset is not currently possible. The recorded results
(2568 tests, 0 failures) are from a run that completed *after* the last source edit — see the test
result XML timestamps against `LocalDownloadManager.java`'s mtime before trusting them.

## Shipping a Java-level change in this sandbox — now solved (v5)

The earlier "blocked" note is obsolete. `:patches:build` still fails (forked patcher API), but the
Java extension itself compiles fine and the DEX can be swapped in with a **full baksmali/smali round
trip**. Verified end to end for v5.

Build the extension (works despite the broken `:patches` module):

```sh
./gradlew :extensions:shared:syncExtension     # NOT :bundle — packageDebugBundle fails on
                                               # "Version code not found in manifest"
# -> extensions/shared/build/morphe/extensions/shared.mpe (+ shared2.mpe)
```

Round trip: use `tools/dex-toolchain.sh`, which pins a **public** toolchain (org.smali 2.5.2 from
Maven Central) and downloads it on first use.

```sh
export JAVA_HOME=/workspace/project/toolchain/jdk-21.0.13.1+2
./tools/dex-toolchain.sh d -o sm10 classes10.dex
cp -a newmpe/app/morphe/extension/music/patches/downloads/. sm10/app/morphe/extension/music/patches/downloads/
./tools/dex-toolchain.sh a -o c10_new.dex sm10
```

Do **not** try to use the MorpheApp smali fork: it lives on a private registry that answers 401 here,
and the Gradle cache holding it is wiped on sandbox reset. The SDK ships `baksmali` but **not** the
assembler, so an SDK-only classpath cannot round trip. org.smali 2.5.2 is self-consistent, needs
`antlr-runtime 3.5.2` + Guava at runtime, and reads/writes the same dex 035 format as the APK.

Copy **only** the changed package directories, never the whole new tree: the freshly built `.mpe` is a
superset (58 classes also live in other DEX files, and duplicate definitions across DEX are a real
risk). After copying, diff the file lists — the only expected additions were three synthetic lambdas.

Then repack (`zipfile`, preserving entry order/attrs), `zipalign -p 4`, and sign **v2 only**:

```sh
apksigner sign --ks build/morphe.keystore --ks-pass pass:morphe123 --key-pass pass:morphe123 \
  --ks-key-alias morphe --v2-signing-enabled true --v3-signing-enabled false \
  --out ytm_signed_v5.apk ytm_aligned.apk
```

The keystore password is `morphe123`. Note the releases are **not** signed with one key: v2/v3 use
`97ae8fd1…`, v4 `53185b47…`, v5/v6 `9c70c674…` (checked with `apksigner verify --print-certs` on the
downloaded assets). So an in-place update only works v5 -> v6; going to v6 from v2/v3/v4 needs an
uninstall first.
`ytm_signed_v5.apk` — SHA-256 `7b5a3665be10a5788bb8300281dcd8ab3812b9263477e4c6cdb16bbe9d8b8240`.

**v7 was built without Gradle at all.** `:extensions:shared:syncExtension` also fails now: the
`app.morphe.patches:2.0.0` plugin is still unreachable (401), so *no* Gradle task starts, including
`syncExtension`. Compile the two changed files with plain `javac` against the classes left by the last
successful build, then dex them with `d8` and splice the result into `classes10.dex`:

```sh
export JAVA_HOME=/workspace/project/toolchain/jdk-21.0.13.1+2
BT=/workspace/project/toolchain/sdk/build-tools/35.0.0
CP=extensions/shared/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:\
/workspace/project/toolchain/sdk/platforms/android-35/android.jar:\
/workspace/project/toolchain/sdk/cmdline-tools/latest/lib/external/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar:\
/workspace/project/toolchain/sdk/cmdline-tools/latest/lib/external/com/google/protobuf/protobuf-java/3.19.3/protobuf-java-3.19.3.jar:\
extensions/shared/libs/jump3r-1.0.5.jar
$JAVA_HOME/bin/javac -nowarn -proc:none -sourcepath /tmp/stubs -cp "$CP" -d /tmp/out \
  extensions/shared/src/main/java/app/morphe/extension/music/patches/downloads/{LocalDownloadManager,TopTracks}.java
$JAVA_HOME/bin/java -cp $BT/lib/d8.jar com.android.tools.r8.D8 --min-api 26 --output ./d8out /tmp/out/.../*.class
```

Two gotchas that cost time:

- `androidx.annotation.Nullable`/`NonNull` are **not** on any disk jar here. Write the two annotations
  as source stubs in `/tmp/stubs` and pass `-sourcepath /tmp/stubs`. Do not glob every jar into the
  classpath: `android-stubs-src.jar` matches `*.jar` and poisons the compile.
- `d8` emits a `com/android/tools/r8/RecordTag` class. **It already exists in `classes9.dex`**, so
  never copy that directory in — duplicate class definitions across DEX are a real install-time risk.
  Splice only `app/morphe/extension/music/patches/downloads/`.

The DEX that *defines* these classes is `classes10.dex`. A raw byte search for `TopTracks` also hits
other DEX files, but those are string references; confirm with baksmali before splicing.

`/workspace/project/build/morphe.keystore` survives resets — `build/morphe.keystore` inside the repo
does not. `apksigner` is a shell wrapper that needs `java` on `PATH`, not just `JAVA_HOME`.

### Changes in v7 (on top of v6)

1. **Plays were counted two or three times.** `MediaSession` republishes metadata per song (title,
   then artwork, then duration), and the `onSetMetadata` hook scored each republication. Dedup by
   `lastScoredVideoId`, plus a single-thread executor so two rapid track changes cannot lose an
   increment in the SharedPreferences read-modify-write.
2. **Auto-downloads ran on the unbounded shared pool.** `Utils.backgroundThreadPool` is
   `ThreadPoolExecutor(3, Integer.MAX_VALUE, …, SynchronousQueue)`, and each download opens
   `MAX_CONNECTIONS = 6` sockets. A top-N catch-up queued hundreds of tasks. They now go through
   `AUTO_DOWNLOAD_THREAD`, a single-thread executor; manual downloads keep the shared pool.
3. **Wi-Fi-only was sampled once, at enqueue time.** Now `requireWifi` is threaded down to
   `RangeQueue.checkStopped()` and `downloadUnknownLength`, so a queue that outlives the connection
   aborts instead of falling back to mobile data. `isOnWifi` also requires
   `NET_CAPABILITY_NOT_METERED` (0xb), so a phone hotspot no longer counts as Wi-Fi.
4. **`downloadUnknownLength` got the same headers as `openAudioConnection`** — `Referer` and
   `setInstanceFollowRedirects(true)`. This is the likely cause of 403 on streams without a
   Content-Length.
5. **Smaller robustness fixes** — formats with `getBitrate() == 0` are filtered out (they would pass
   the `<= bitrateKbps*1000` ceiling), `isInTop` rejects a score `<= 0` (rank 0 when every neighbour
   is also zero), and `sweepOrphanedTemporaryFiles()` deletes `rvx-audio-*.part` left by a kill.

`ytm_ultimate_v7.apk` — SHA-256 `83662c11b446a4e6bd0d42cad4ad65c7923799e44a5e2ba9695af85d5d6c6c01`,
86 525 396 bytes, same `9c70c674…` signer as v5/v6.

**What "tested" means here.** Gradle cannot run, so `./gradlew test` is unavailable. The JUnit tests
were executed by compiling `TopTracksTest` against the real `TopTracks.class` with a 20-line local
`org.junit` stub and running it reflectively: 10/10 pass. Nothing was installed on a device, so
runtime behaviour on a phone is *unverified* — say so plainly rather than implying a device test.

Verification that caught nothing this time but is worth repeating: all 10 DEX headers/checksums valid,
0 unresolved class references, `apksigner verify` OK, `zipalign -c 4` OK, badging unchanged
(`anddea.youtube.music` 91551240 / 9.15.51), 5816 zip entries CRC-clean.

### Changes in v5 (on top of v4)

1. **`TopTracks` rank logic** — `isInTop`/`rankOf` compared against the *lowest* score in the top set
   (usually 0), so the "top N" filter matched everything. Now computes the real rank.
   `autoDownloadCurrentTop` also downloaded N *new* tracks instead of the N most played.
2. **Main-thread I/O removed** — `LocalDownloadManager.onSetMetadata` runs on the main thread for every
   track change and called `TopTracks.onTrackPlayed`, which does SharedPreferences reads plus one
   `openFileDescriptor` per ranked song. Now dispatched to the background pool (ANR risk).
3. **Permission prompt off the main thread** — added `NativeDownloads.hasStoragePermission()` (silent)
   and used it on the auto-download/quiet paths, which can run on any thread. `ensureStoragePermission()`
   (may show a dialog + toast) stays on the main thread for manual downloads.

Tests: 2565 pass (`:extensions:shared:testDebugUnitTest`), including the new `TopTracksTest`.

## v6 — quality actually applies to OPUS (and is a ceiling, not a nearest match)

Found while auditing feature B: `parseStream` treated only MP4 as passthrough, so **OPUS ignored
the quality setting entirely** and always took the largest opus stream.

The obvious fix ("treat OPUS as passthrough too") is not enough. YouTube only offers a few rungs
(opus ≈48/70/160 kbps, AAC ≈128/256), and the original comparator was a *nearest bitrate* match.
A 128 kbps request is equidistant from 48 and 160, so the tie-break picked 160 — meaning LOW and HIGH
both downloaded the same file. A unit test caught this, not review.

So passthrough now treats the quality as a **ceiling**: take the highest rung that does not exceed the
target, and fall back to the smallest rung if every rung is above it. MP3 is unchanged — LAME sets the
output bitrate, so it still always takes the best source. `DownloadsConfig` documents this.

Also in v6: `Collectors` import added, and the three `StreamSelectionTest` expectations that encoded
the old nearest-match behaviour were rewritten (plus one new case for the all-rungs-above-ceiling
fallback). Tests: **2568 pass**, 0 failures.

`ytm_signed_v6.apk` — SHA-256 `0a0956395997e5b005457f9d4d9504d5f0988224502b249a9b7a15e5db33ca68`,
same key as v5 (so v5 -> v6 updates in place), 5816 entries CRC-clean, `classes10.dex` 7 048 116
bytes, 0 unresolved references.

**Not validated at runtime**: this sandbox has `adb` but no emulator image and no device, so the APK is
verified statically only (signature, alignment, DEX checksums, reference resolution, bytecode inspection
of `parseStream`). It has not been installed or launched.
