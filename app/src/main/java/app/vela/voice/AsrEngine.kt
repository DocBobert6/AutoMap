package app.vela.voice

import android.content.Context
import java.io.File

// File-level consts: enum entries are initialized BEFORE the companion object, so the constructor
// can't read companion members - these must live at file scope. [AsrEngine.VAD] re-exports VAD_FILE
// for callers outside this file.
private val ASR_BASE: String get() = app.vela.core.config.VelaLinks.asrModels()
private const val VAD_FILE = "silero_vad.onnx"

/**
 * The on-device speech-to-text engines Vela can download for voice search (tier-1). All run through
 * the bundled sherpa-onnx runtime ([AsrRecognizer]) with Silero VAD; nothing leaves the phone and no
 * account or third-party voice app is needed. Each engine is an OPTIONAL one-time download hosted on
 * the `asr-models` GitHub release (like the `tts-runtime`/`routing-graphs` infra releases - the
 * assets live nowhere else), extracted to `filesDir/asr/<id>/`.
 *
 * Five engines, because they trade off differently and the user picks:
 *  - [WHISPER_TINY] - the English default. 99-language Whisper tiny (int8); covers every language
 *    Vela's UI supports on paper, but outside English it is too weak to use.
 *  - [WHISPER_BASE] - the default for every other language: the same 99 languages, usable in them,
 *    91 MB. [WHISPER_SMALL] is the same again, better and 222 MB, for whoever wants it.
 *  - [SENSE_VOICE] - FunAudioLLM SenseVoice. More accurate + faster than Whisper tiny, but only for
 *    English, Chinese, Cantonese, Japanese, Korean. Great if you speak one of those.
 *  - [MOONSHINE] - Useful Sensors Moonshine tiny. Lowest latency, but ENGLISH ONLY.
 *
 * Whisper stays the default so no language silently regresses; the other two are opt-in via the
 * voice-search engine picker in Settings. This object holds only metadata + a cheap install check
 * and the selected-engine preference; loading + running a model lives in [AsrRecognizer] (`:app`).
 */
enum class AsrEngine(
    val id: String,
    /** Proper-noun engine name shown in the picker (not localized). */
    val displayName: String,
    /** sherpa-onnx `modelType` string the recognizer passes through. */
    val modelType: String,
    val sizeMb: Int,
    /** Asset on the `asr-models` release. */
    val url: String,
    /** Every file that must be present + non-empty for the engine to count as installed. A missing
     *  file reads as not-installed, so a partial or aborted download self-heals (re-download). */
    val files: List<String>,
) {
    WHISPER_TINY(
        id = "whisper-tiny",
        displayName = "Whisper tiny",
        modelType = "whisper",
        sizeMb = 58,
        url = "$ASR_BASE/vela-asr-whisper-tiny.tar.gz",
        files = listOf("tiny-encoder.int8.onnx", "tiny-decoder.int8.onnx", "tiny-tokens.txt", VAD_FILE),
    ),
    WHISPER_BASE(
        id = "whisper-base",
        displayName = "Whisper base",
        modelType = "whisper",
        sizeMb = 91,
        url = "$ASR_BASE/vela-asr-whisper-base.tar.gz",
        files = listOf("base-encoder.int8.onnx", "base-decoder.int8.onnx", "base-tokens.txt", VAD_FILE),
    ),
    WHISPER_SMALL(
        id = "whisper-small",
        displayName = "Whisper small",
        modelType = "whisper",
        sizeMb = 222,
        url = "$ASR_BASE/vela-asr-whisper-small.tar.gz",
        files = listOf("small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt", VAD_FILE),
    ),
    SENSE_VOICE(
        id = "sensevoice",
        displayName = "SenseVoice",
        modelType = "sense_voice",
        sizeMb = 154,
        url = "$ASR_BASE/vela-asr-sensevoice.tar.gz",
        files = listOf("model.int8.onnx", "tokens.txt", VAD_FILE),
    ),
    MOONSHINE(
        id = "moonshine",
        displayName = "Moonshine",
        modelType = "moonshine",
        sizeMb = 101,
        url = "$ASR_BASE/vela-asr-moonshine.tar.gz",
        files = listOf(
            "preprocess.onnx", "encode.int8.onnx", "uncached_decode.int8.onnx",
            "cached_decode.int8.onnx", "tokens.txt", VAD_FILE,
        ),
    ),
    ;

    /** The Whisper model files' prefix (`tiny-encoder.int8.onnx`), null for another model. */
    val whisperPrefix: String? get() = if (modelType == "whisper") id.removePrefix("whisper-") else null

    /** `filesDir/asr/<id>/` - the extracted archive's single top-level folder. */
    fun dir(context: Context): File = File(context.filesDir, "asr/$id")

    /** True only when EVERY model file is present and non-empty. Pure file check, no model load, so
     *  it's safe from the UI thread / availability gates. */
    fun isInstalled(context: Context): Boolean {
        val d = dir(context)
        return files.all { File(d, it).length() > 0L }
    }

    /** Whether this engine can actually recognize [lang] (an app language code, e.g. "en"/"he"/"ja").
     *  Whisper covers everything; SenseVoice only en/zh/ja/ko/yue; Moonshine only English. Used to
     *  fall back off a picked engine that can't do the current language. */
    fun supportsLanguage(lang: String): Boolean = when (this) {
        WHISPER_TINY, WHISPER_BASE, WHISPER_SMALL -> true
        SENSE_VOICE -> lang in SENSE_VOICE_LANGS
        MOONSHINE -> lang == "en"
    }

    companion object {
        /** Silero VAD, shipped inside every engine archive so each is self-contained. */
        const val VAD = VAD_FILE
        /** The languages SenseVoice recognizes (its own codes). */
        val SENSE_VOICE_LANGS = setOf("zh", "en", "ja", "ko", "yue")
        private const val PREFS = "vela_settings"
        private const val PREF_ENGINE = "asr_engine"

        /** The engine offered when nothing is installed: [defaultFor] the app language. Read
         *  through `Locale.getDefault()`, which is the app language once `AppLocale.wrap` has run. */
        val DEFAULT: AsrEngine get() = defaultFor(java.util.Locale.getDefault().language)

        /** Whisper tiny for English, where it is good and smallest; Whisper base for any other
         *  language, where tiny is too weak to use (issue 798). Never SenseVoice or Moonshine: those
         *  are opt-in for the languages they cover. */
        fun defaultFor(lang: String): AsrEngine = if (lang == "en") WHISPER_TINY else WHISPER_BASE

        fun byId(id: String?): AsrEngine? = entries.firstOrNull { it.id == id }

        fun installed(context: Context): List<AsrEngine> = entries.filter { it.isInstalled(context) }

        /** Any engine downloaded at all → voice search is available. */
        fun anyInstalled(context: Context): Boolean = entries.any { it.isInstalled(context) }

        /** The engine to actually run: the user's pick if it's still installed, else the first
         *  installed engine (so deleting the picked one degrades gracefully), else the default. */
        fun active(context: Context): AsrEngine {
            val picked = byId(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_ENGINE, null),
            )
            if (picked != null && picked.isInstalled(context)) return picked
            return installed(context).firstOrNull() ?: DEFAULT
        }

        fun setActive(context: Context, engine: AsrEngine) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(PREF_ENGINE, engine.id).apply()

        /** The engine to actually RUN for [lang]: the [active] pick if it supports the language, else
         *  Whisper (the multilingual default) when it's installed, so dictating in a language the
         *  picked engine can't do (e.g. SenseVoice picked, Hebrew spoken) still works instead of
         *  returning garbage. Falls back to the pick as a last resort if Whisper isn't installed.
         *  Note: the Settings picker's "Active" still reflects the user's raw pick ([active]); this
         *  only changes which model the recognizer loads for the current language. */
        fun forRecognition(context: Context, lang: String): AsrEngine {
            val picked = active(context)
            if (picked.supportsLanguage(lang)) return picked
            return WHISPER_TINY.takeIf { it.isInstalled(context) } ?: picked
        }
    }
}
