package org.sfcivictech.android.shared.resourcebinder.search.embedding

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * On-device check that the bundled ONNX model + tokenizer actually produce
 * usable embeddings: asset loading, tokenization, ONNX Runtime session,
 * and NNAPI/CPU fallback all working end to end.
 */
class OnnxEmbeddingModelTest {

    private fun dot(a: FloatArray, b: FloatArray): Double {
        var sum = 0.0
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    private fun norm(a: FloatArray): Double = sqrt(dot(a, a))

    @Test
    fun encodeProducesNormalizedVectorsAndSensibleSimilarity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = OnnxEmbeddingModel.load(context)
        try {
            val texts = listOf(
                "emergency shelter for a family with young kids",
                "housing help for families with children",
                "job training program for veterans",
            )
            val vectors = model.encode(texts)

            assertEquals(3, vectors.size)
            for (v in vectors) {
                assertEquals(384, v.size)
                // Graph already L2-normalizes -- confirm that held on-device too.
                assertTrue("expected unit norm, got ${norm(v)}", kotlin.math.abs(norm(v) - 1.0) < 1e-3)
            }

            val simShelterHousing = dot(vectors[0], vectors[1])
            val simShelterJobs = dot(vectors[0], vectors[2])
            assertTrue(
                "expected the two housing-related prompts to be more similar " +
                    "to each other ($simShelterHousing) than either is to the " +
                    "unrelated jobs prompt ($simShelterJobs)",
                simShelterHousing > simShelterJobs,
            )
        } finally {
            model.close()
        }
    }
}
