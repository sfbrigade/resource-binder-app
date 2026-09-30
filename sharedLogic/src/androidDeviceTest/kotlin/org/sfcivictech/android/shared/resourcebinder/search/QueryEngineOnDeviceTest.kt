package org.sfcivictech.android.shared.resourcebinder.search

/*
 * End-to-end check of the real matching engine (QueryServices.kt) running
 * on-device against OnnxEmbeddingModel, using a real HSDS CSV package
 * bundled as a test asset (hsds_csv_sample/).
 *
 * Exercises dataset loading, the taxonomy join, spelling correction against
 * the dataset's own vocabulary, the service_embeddings.json cache
 * round-trip, and geo-filtering -- all on a real device/emulator via
 * androidDeviceTest, not just a JVM-host unit test.
 *
 * Assets aren't on the regular filesystem, so the CSV package is copied out
 * to the instrumentation target's cacheDir first -- loadDataset()/File()
 * need a real path.
 */

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.search.embedding.OnnxEmbeddingModel

class QueryEngineOnDeviceTest {

    private val csvTables = listOf(
        "organizations", "services", "locations", "addresses", "phones",
        "service_at_locations", "service_taxonomy", "taxonomy_terms",
    )

    private lateinit var packageDir: File

    // Downtown SF, same default origin used by the sample data itself.
    private val sfLat = 37.7749
    private val sfLon = -122.4194

    @Before
    fun copyRealHsdsPackageOutOfAssets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        packageDir = File(context.cacheDir, "hsds_csv_sample").apply { mkdirs() }
        for (table in csvTables) {
            val dest = File(packageDir, "$table.csv")
            context.assets.open("hsds_csv_sample/$table.csv").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    @After
    fun cleanUp() {
        packageDir.deleteRecursively()
    }

    private fun loadModel() = OnnxEmbeddingModel.load(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun loadsRealDatasetAndCachesEmbeddings() {
        val dataset: Dataset = loadDataset(packageDir)
        assertTrue("expected services to load from the real CSV package", dataset.services.isNotEmpty())

        val model = loadModel()
        try {
            val cacheFile = File(packageDir, "service_embeddings.json")
            assertTrue("cache should not exist before the first build", !cacheFile.exists())

            val built = buildOrLoadServiceEmbeddings(dataset, packageDir, model, rebuild = true)
            assertEquals(dataset.services.size, built.size)
            assertTrue("cache file should be written after building", cacheFile.exists())

            // Second call with rebuild = false should read the cache rather
            // than re-running the model -- same vectors back out.
            val cached = buildOrLoadServiceEmbeddings(dataset, packageDir, model)
            assertEquals(built.size, cached.size)
            for (i in built.indices) {
                assertTrue(
                    "cached vector $i should match the freshly-built one",
                    built[i].contentEquals(cached[i]),
                )
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun queryAgainstRealDataRetrievesAFamilyShelterNearDowntownSf() {
        val dataset = loadDataset(packageDir)
        val model = loadModel()
        try {
            val embeddings = buildOrLoadServiceEmbeddings(dataset, packageDir, model, rebuild = true)
            val result = query(
                "family shelter",
                sfLat, sfLon, model,
                packageDir = packageDir, dataset = dataset, serviceEmbeddings = embeddings,
            )

            assertTrue("expected at least one result for \"family shelter\"", result.results.isNotEmpty())
            val top = result.results.first()
            assertTrue(
                "expected the top result to actually be shelter-related, was " +
                    "\"${top.serviceName}\" categories=${top.categories}",
                top.serviceName.contains("shelter", ignoreCase = true) ||
                    top.categories.any { it.contains("shelter", ignoreCase = true) },
            )
            assertTrue(
                "top result should be within the default 30-mile radius, was ${top.distanceMiles}",
                top.distanceMiles <= DEFAULT_RADIUS_MILES,
            )
        } finally {
            model.close()
        }
    }

    @Test
    fun typoInPromptIsCorrectedAgainstTheRealDatasetVocabulary() {
        val dataset = loadDataset(packageDir)
        val model = loadModel()
        try {
            val embeddings = buildOrLoadServiceEmbeddings(dataset, packageDir, model, rebuild = true)
            val result = query(
                "famly shelder", // "family shelter" with two typos
                sfLat, sfLon, model,
                packageDir = packageDir, dataset = dataset, serviceEmbeddings = embeddings,
            )

            assertNotNull("expected the typo'd prompt to be corrected", result.interpretedQuery)
            assertTrue(
                "expected \"shelter\" in the corrected prompt, got \"${result.interpretedQuery}\"",
                result.interpretedQuery!!.contains("shelter", ignoreCase = true),
            )
        } finally {
            model.close()
        }
    }

    @Test
    fun radiusFilterExcludesServicesOutsideIt() {
        val dataset = loadDataset(packageDir)
        val model = loadModel()
        try {
            val embeddings = buildOrLoadServiceEmbeddings(dataset, packageDir, model, rebuild = true)
            // Null Island -- nowhere near any real SF service in the sample data.
            val result = query(
                "family shelter",
                0.0, 0.0, model,
                radiusMiles = 1.0,
                packageDir = packageDir, dataset = dataset, serviceEmbeddings = embeddings,
            )

            assertEquals(
                "expected no results within 1 mile of Null Island",
                0,
                result.count,
            )
        } finally {
            model.close()
        }
    }
}
