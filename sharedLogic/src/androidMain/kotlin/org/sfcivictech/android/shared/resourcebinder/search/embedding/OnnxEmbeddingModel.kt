package org.sfcivictech.android.shared.resourcebinder.search.embedding

/*
 * Offline sentence-embedding model for sentence-transformers/all-MiniLM-L6-v2,
 * running on-device via ONNX Runtime for Android. No network calls, no
 * downloaded weights: model_int8.onnx and vocab.txt are bundled as assets
 * (see sharedLogic/build.gradle.kts's androidResources block) and read from
 * AssetManager at load time. Tokenization is WordPieceTokenizer.kt, pure
 * Kotlin rather than a native binding -- see its header for why.
 *
 * The exported ONNX graph already includes mean-pooling and L2
 * normalization -- verified bit-for-bit against the real Python
 * sentence-transformers output at export time (cosine similarity 1.0, max
 * abs diff ~1e-7, pure float32 rounding noise). That means encode() needs
 * no pooling/normalization math of its own: it's tokenize -> run -> read
 * `sentence_embedding` straight out.
 *
 * The bundled weights are INT8 dynamically-quantized (~23MB vs ~90MB fp32),
 * which is a real, measurable accuracy tradeoff, not a free win: cosine
 * similarity against the fp32 model ran ~0.95-0.98 across a handful of
 * sample prompts during export, not 1.0. Fine for retrieval-style semantic
 * matching.
 */

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.Closeable
import java.nio.LongBuffer
import org.sfcivictech.android.shared.resourcebinder.search.EmbeddingProvider

class OnnxEmbeddingModel private constructor(
    private val session: OrtSession,
    private val tokenizer: WordPieceTokenizer,
) : EmbeddingProvider, Closeable {

    companion object {
        private const val MODEL_ASSET = "model_int8.onnx"
        private const val VOCAB_ASSET = "vocab.txt"

        /** Loads the bundled model + tokenizer from the app's assets. Offline; no I/O beyond that. */
        fun load(context: Context): OnnxEmbeddingModel {
            val env = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }

            // Prefer NNAPI hardware acceleration where the device supports
            // it; ONNX Runtime falls back to its default CPU/XNNPACK
            // execution providers on its own for any node NNAPI can't run.
            // If NNAPI fails to register at all (e.g. an emulator image
            // with no NNAPI HAL), fall back to a plain CPU-only session
            // rather than failing to load entirely.
            val session = try {
                val options = OrtSession.SessionOptions()
                options.addNnapi()
                env.createSession(modelBytes, options)
            } catch (e: Exception) {
                env.createSession(modelBytes, OrtSession.SessionOptions())
            }

            val vocabText = context.assets.open(VOCAB_ASSET).use { it.reader(Charsets.UTF_8).readText() }
            val tokenizer = WordPieceTokenizer.fromVocabText(vocabText)

            return OnnxEmbeddingModel(session, tokenizer)
        }
    }

    /** One L2-normalized 384-dim float embedding per input text, same order. */
    override fun encode(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) return emptyList()
        val env = OrtEnvironment.getEnvironment()
        val encodings = tokenizer.encodeBatch(texts)

        val seqLength = encodings[0].ids.size
        val batchSize = encodings.size
        val idsBuffer = LongBuffer.allocate(batchSize * seqLength)
        val maskBuffer = LongBuffer.allocate(batchSize * seqLength)
        for (encoding in encodings) {
            idsBuffer.put(encoding.ids)
            maskBuffer.put(encoding.attentionMask)
        }
        idsBuffer.rewind()
        maskBuffer.rewind()

        val shape = longArrayOf(batchSize.toLong(), seqLength.toLong())
        OnnxTensor.createTensor(env, idsBuffer, shape).use { idsTensor ->
            OnnxTensor.createTensor(env, maskBuffer, shape).use { maskTensor ->
                val inputs = mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor)
                session.run(inputs).use { results ->
                    @Suppress("UNCHECKED_CAST")
                    val output = results.get("sentence_embedding").get().value as Array<FloatArray>
                    return output.toList()
                }
            }
        }
    }

    override fun close() {
        session.close()
    }
}
