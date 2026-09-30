package org.sfcivictech.android.shared.resourcebinder.search

/**
 * One L2-normalized float vector per input text, in the same order.
 * Android-only for now: implemented by [org.sfcivictech.android.shared.resourcebinder.search.embedding.OnnxEmbeddingModel],
 * which runs the exported all-MiniLM-L6-v2 model on-device via
 * onnxruntime-android (NNAPI with CPU fallback). QueryEngine.kt depends on
 * this interface rather than the concrete implementation so an iOS backend
 * (e.g. Core ML) can be added later without touching the matching engine.
 */
interface EmbeddingProvider {
    fun encode(texts: List<String>): List<FloatArray>
}
