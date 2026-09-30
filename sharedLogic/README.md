# sharedLogic

Kotlin Multiplatform module shared between the Android and iOS apps.

## Search (`org.sfcivictech.android.shared.resourcebinder.search`)

Android port of [`backend/query_services.py`](../backend/query_services.py):
semantic, free-text search over San Francisco human-services data (HSDS CSV
package), ranked by embedding similarity and filtered/ordered by distance
from the caller's current location.

- **`EmbeddingProvider`** (`commonMain`) -- one L2-normalized float vector
  per input text. Pure interface, no platform dependency, so an iOS backend
  can be added later without touching the matching engine.
- **`QueryServices.kt`** (`androidMain`) -- `loadDataset()`, spelling
  correction against the dataset's own vocabulary, embedding-cache
  round-trip, geo-filtering, and `query()`, the entry point. Uses
  `java.io.File` and Apache Commons CSV, so it's JVM/Android-only for now
  (not yet portable to `commonMain`/iOS).
- **`search/embedding/OnnxEmbeddingModel.kt`** (`androidMain`) -- the
  `EmbeddingProvider` implementation: runs `sentence-transformers/all-MiniLM-L6-v2`,
  exported to ONNX and INT8-quantized, fully offline on-device via
  `onnxruntime-android` (NNAPI with CPU fallback). The model
  (`src/androidMain/assets/model_int8.onnx`, ~22MB) and its vocabulary
  (`vocab.txt`) are bundled app assets -- no network call, no download, ever.
- **`search/embedding/WordPieceTokenizer.kt`** (`androidMain`) -- a
  from-scratch pure-Kotlin BERT/WordPiece tokenizer. Not a native binding:
  `ai.djl.huggingface:tokenizers` only ships native binaries for desktop
  platforms, not Android's ABI, and `UnsatisfiedLinkError`s on-device.

Cosine similarity only means anything when both the prompt and the corpus
are embedded with the exact same model/weights/quantization, so swapping in
a different embedding model means re-exporting for both sides together, not
picking a "better" model for one side only.

### Tests

- `androidDeviceTest` (real device/emulator, via `AndroidJUnitRunner`):
  `QueryEngineOnDeviceTest` runs the full pipeline -- dataset loading, the
  taxonomy join, spelling correction, the embedding-cache round-trip, and
  geo-filtering -- against a real HSDS sample package
  (`src/androidDeviceTest/assets/hsds_csv_sample/`).
  `embedding/OnnxEmbeddingModelTest` checks the raw model + tokenizer path
  (asset loading, tokenization, ONNX Runtime session, NNAPI/CPU fallback)
  in isolation.

  ```
  ./gradlew :sharedLogic:connectedDebugAndroidTest
  ```

### Updating the embedding model

1. Re-export from a BERT/WordPiece-family `sentence-transformers` model to
   ONNX with mean-pooling + L2-normalization baked into the graph (output
   node `sentence_embedding`, inputs `input_ids`/`attention_mask`), then
   INT8-quantize.
2. Replace `src/androidMain/assets/model_int8.onnx` and `vocab.txt`.
3. Delete any existing `service_embeddings.json` cache (or call
   `buildOrLoadServiceEmbeddings(..., rebuild = true)`) -- the cache is only
   keyed on service IDs, not model identity or vector dimension, so a stale
   cache from a different model will silently corrupt similarity scores
   rather than fail loudly.
4. A model using SentencePiece/BPE instead of WordPiece (most non-BERT
   architectures) needs a new tokenizer written in `androidMain` --
   `WordPieceTokenizer.kt`'s algorithm and hardcoded special-token IDs are
   BERT-specific.
