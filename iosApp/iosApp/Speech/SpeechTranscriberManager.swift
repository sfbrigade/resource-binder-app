import Foundation
import TranscriptionKit

@MainActor
class SpeechTranscriberManager: ObservableObject, @MainActor TranscriberDelegate {
    @Published var transcription: String = ""
    @Published var errorMessage: String? = nil
    @Published var supportsOnDeviceRecognition: Bool? = nil

    private var transcriber = Transcriber()

    var error: (any Error)? {
        didSet {
            errorMessage = error?.localizedDescription
        }
    }

    func start() async throws {
        // Reset
        transcription = ""
        error = nil

        if transcriber.recognizer == nil {
            transcriber.recognizer = AppleRecognizer()
            transcriber.delegate = self
            transcriber.reset()
        }

        try transcriber.startRecording()
    }

    func stop() {
        transcriber.stopRecording()
    }

    // MARK: - TranscriberDelegate

    func transcriberDidRecord(_ transcriber: Transcriber, seconds: TimeInterval, data: [Float]) {

    }

    func transcriberDidFailToRecord(_ transcriber: Transcriber, error: any Error) {
        print(error)
    }

    func transcriberDidFinishRecording(_ transcriber: Transcriber, seconds: TimeInterval) {

    }

    func transcriberDidTransformBuffer(_ transcriber: Transcriber,  data: [Float]) {

    }

    func transcriberDidFinishRecording(_ transcriber: Transcriber, duration seconds: TimeInterval) {

    }

    func transcriberDidPlay(_ transcriber: Transcriber, seconds: TimeInterval) {

    }

    func transcriberDidFinishPlaying(_ transcriber: Transcriber, successfully: Bool, error: Error?) {

    }

    func transcriberDidFinishRecognition(_ transcriber: Transcriber, error: Error?) {

    }

    func transcriberDidRequestSpeechAuthorization(_ transcriber: Transcriber, status: TranscriberAuthorizationStatus) {
        switch status {
        case .granted:
            do {
                try transcriber.startRecording()
            } catch {
                print(error)
            }
        default:
            break
        }
    }

    func transcriberDidRequestRecordAuthorization(_ transcriber: Transcriber, status: TranscriberAuthorizationStatus) {
        switch status {
        case .granted:
            do {
                try transcriber.startRecording()
            } catch {
                print(error)
            }
        default:
            break
        }
    }

    func transcriberDidRecognize(_ transcriber: Transcriber, text: String, fileId: String, transcriptId: String,
                                 metadata: [String: Any], isFinal: Bool) {
        transcription = text
    }
}
