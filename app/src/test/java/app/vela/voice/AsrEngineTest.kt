package app.vela.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The engine offered first follows the app language (issue 798), and the three Whisper sizes
 *  share one recognizer branch keyed by their file prefix. */
class AsrEngineTest {
    @Test fun englishIsOfferedTiny() = assertEquals(AsrEngine.WHISPER_TINY, AsrEngine.defaultFor("en"))

    @Test fun otherLanguagesAreOfferedBase() {
        for (lang in listOf("de", "fr", "es", "ru", "he", "ja", "zh", "")) {
            assertEquals(lang, AsrEngine.WHISPER_BASE, AsrEngine.defaultFor(lang))
        }
    }

    @Test fun whisperPrefixNamesTheFiles() {
        for (e in listOf(AsrEngine.WHISPER_TINY, AsrEngine.WHISPER_BASE, AsrEngine.WHISPER_SMALL)) {
            val p = e.whisperPrefix!!
            assertTrue(e.name, "$p-encoder.int8.onnx" in e.files)
            assertTrue(e.name, "$p-decoder.int8.onnx" in e.files)
            assertTrue(e.name, "$p-tokens.txt" in e.files)
            assertTrue(e.name, e.supportsLanguage("de"))
        }
        assertNull(AsrEngine.SENSE_VOICE.whisperPrefix)
        assertNull(AsrEngine.MOONSHINE.whisperPrefix)
    }

    @Test fun theBiggerWhispersHaveTheirOwnAssets() {
        assertTrue(AsrEngine.WHISPER_BASE.url.endsWith("/vela-asr-whisper-base.tar.gz"))
        assertTrue(AsrEngine.WHISPER_SMALL.url.endsWith("/vela-asr-whisper-small.tar.gz"))
        assertEquals(91, AsrEngine.WHISPER_BASE.sizeMb)
        assertEquals(222, AsrEngine.WHISPER_SMALL.sizeMb)
    }
}
