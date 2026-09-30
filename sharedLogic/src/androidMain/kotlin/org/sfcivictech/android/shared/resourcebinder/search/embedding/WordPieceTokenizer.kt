package org.sfcivictech.android.shared.resourcebinder.search.embedding

/*
 * Pure-Kotlin BERT/WordPiece tokenizer for all-MiniLM-L6-v2, matching the
 * exported tokenizer_config.json exactly (do_lower_case=true,
 * do_basic_tokenize=true, strip_accents=null [-> stripped, since
 * do_lower_case=true], tokenize_chinese_chars=true, max_length=128,
 * unk/cls/sep/pad = [UNK]=100/[CLS]=101/[SEP]=102/[PAD]=0).
 *
 * Deliberately not a native binding: ai.djl.huggingface:tokenizers (the
 * standard JVM HuggingFace tokenizer) only ships native libraries for
 * desktop platforms (linux-x86_64/linux-aarch64/osx-aarch64/win-x86_64 --
 * verified by inspecting its jar directly), not Android's bionic-libc ABI,
 * so it UnsatisfiedLinkError's at runtime on-device. WordPiece itself is a
 * small, well-defined algorithm (greedy longest-match subword splitting
 * against a fixed vocabulary), so implementing it directly in pure Kotlin
 * sidesteps the native-library problem entirely: it runs identically on
 * Android's ART and any desktop JVM, no native ABI to match at all.
 *
 * Reference: Google's original BERT tokenization.py (BasicTokenizer +
 * WordpieceTokenizer). This implements the same two-stage algorithm.
 */

import java.text.Normalizer

private const val MAX_INPUT_CHARS_PER_WORD = 100

private val CJK_RANGES = listOf(
    0x4E00..0x9FFF, 0x3400..0x4DBF, 0x20000..0x2A6DF, 0x2A700..0x2B73F,
    0x2B740..0x2B81F, 0x2B820..0x2CEAF, 0xF900..0xFAFF, 0x2F800..0x2FA1F,
)

private fun isChineseChar(cp: Int): Boolean = CJK_RANGES.any { cp in it }

private fun isControlChar(cp: Int): Boolean {
    if (cp == '\t'.code || cp == '\n'.code || cp == '\r'.code) return false
    val type = Character.getType(cp)
    return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
}

private fun isPunctuationChar(cp: Int): Boolean {
    // BERT's original also special-cases ASCII symbols that Unicode doesn't
    // classify as "P" (general punctuation) -- e.g. ^ $ ` -- so those ranges
    // are checked explicitly before falling back to the Unicode category.
    if ((cp in 33..47) || (cp in 58..64) || (cp in 91..96) || (cp in 123..126)) return true
    val type = Character.getType(cp)
    return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
        type == Character.DASH_PUNCTUATION.toInt() ||
        type == Character.START_PUNCTUATION.toInt() ||
        type == Character.END_PUNCTUATION.toInt() ||
        type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
        type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
        type == Character.OTHER_PUNCTUATION.toInt()
}

/** Clean control chars, split CJK chars out as their own tokens, lowercase + strip accents, split on whitespace/punctuation. */
private fun basicTokenize(text: String): List<String> {
    val cleaned = StringBuilder()
    for (cp in text.codePoints()) {
        when {
            cp == 0 || cp == 0xFFFD || isControlChar(cp) -> {}
            Character.isWhitespace(cp) -> cleaned.append(' ')
            isChineseChar(cp) -> {
                cleaned.append(' ')
                cleaned.appendCodePoint(cp)
                cleaned.append(' ')
            }
            else -> cleaned.appendCodePoint(cp)
        }
    }

    val tokens = mutableListOf<String>()
    for (whitespaceToken in cleaned.toString().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }) {
        // do_lower_case=true, strip_accents=null -> stripped: NFD-decompose
        // then drop combining marks, matching BasicTokenizer._run_strip_accents.
        val lowered = whitespaceToken.lowercase()
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val noAccents = StringBuilder()
        for (cp in decomposed.codePoints()) {
            if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) noAccents.appendCodePoint(cp)
        }

        var current = StringBuilder()
        for (cp in noAccents.toString().codePoints()) {
            if (isPunctuationChar(cp)) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current = StringBuilder()
                }
                tokens.add(String(Character.toChars(cp)))
            } else {
                current.appendCodePoint(cp)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
    }
    return tokens
}

class WordPieceTokenizer(private val vocab: Map<String, Int>) {

    companion object {
        const val PAD_TOKEN_ID = 0
        const val UNK_TOKEN_ID = 100
        const val CLS_TOKEN_ID = 101
        const val SEP_TOKEN_ID = 102
        const val MAX_LENGTH = 128

        fun fromVocabText(vocabText: String): WordPieceTokenizer {
            val vocab = HashMap<String, Int>()
            vocabText.lineSequence().forEachIndexed { index, line ->
                if (line.isNotEmpty()) vocab[line] = index
            }
            return WordPieceTokenizer(vocab)
        }
    }

    private fun wordpieceIds(word: String): List<Int> {
        if (word.length > MAX_INPUT_CHARS_PER_WORD) return listOf(UNK_TOKEN_ID)

        val ids = mutableListOf<Int>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var matchedId: Int? = null
            while (start < end) {
                var piece = word.substring(start, end)
                if (start > 0) piece = "##$piece"
                val id = vocab[piece]
                if (id != null) {
                    matchedId = id
                    break
                }
                end--
            }
            if (matchedId == null) return listOf(UNK_TOKEN_ID)
            ids.add(matchedId)
            start = end
        }
        return ids
    }

    data class Encoded(val ids: LongArray, val attentionMask: LongArray)

    /** Tokenizes each text to [CLS] + wordpieces + [SEP], padded to the batch's shared length (capped at MAX_LENGTH). */
    fun encodeBatch(texts: List<String>): List<Encoded> {
        val contentIdsPerText = texts.map { text ->
            val ids = mutableListOf<Int>()
            for (word in basicTokenize(text)) ids.addAll(wordpieceIds(word))
            // -2 reserves room for [CLS]/[SEP].
            if (ids.size > MAX_LENGTH - 2) ids.subList(0, MAX_LENGTH - 2) else ids
        }
        val batchLength = (contentIdsPerText.maxOfOrNull { it.size } ?: 0) + 2

        return contentIdsPerText.map { contentIds ->
            val ids = LongArray(batchLength)
            val mask = LongArray(batchLength)
            ids[0] = CLS_TOKEN_ID.toLong()
            mask[0] = 1
            var i = 1
            for (id in contentIds) {
                ids[i] = id.toLong()
                mask[i] = 1
                i++
            }
            ids[i] = SEP_TOKEN_ID.toLong()
            mask[i] = 1
            i++
            while (i < batchLength) {
                ids[i] = PAD_TOKEN_ID.toLong()
                mask[i] = 0
                i++
            }
            Encoded(ids, mask)
        }
    }
}
