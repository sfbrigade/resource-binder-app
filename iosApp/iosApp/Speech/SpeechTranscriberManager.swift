import Foundation
import Speech
import AVFoundation

@MainActor
class SpeechTranscriberManager: ObservableObject {
    @Published var transcription: String = ""
    @Published var errorMessage: String? = nil
    @Published var supportsOnDeviceRecognition: Bool? = nil

    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    var error: (any Error)? {
        didSet {
            errorMessage = error?.localizedDescription
        }
    }

    func start() async throws {
        // Reset
        transcription = ""
        error = nil

        // Request authorization
        let status = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { authStatus in
                continuation.resume(returning: authStatus)
            }
        }

        guard status == .authorized else {
            throw NSError(domain: "SpeechTranscriberManager", code: 1, userInfo: [NSLocalizedDescriptionKey: "Speech recognition authorization not granted."])
        }

        // Setup speech recognizer
        speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
        guard let recognizer = speechRecognizer else {
            throw NSError(domain: "SpeechTranscriberManager", code: 2, userInfo: [NSLocalizedDescriptionKey: "Speech recognition is not available."])
        }
        // Expose whether this recognizer supports on-device models for the chosen locale
        if #available(iOS 13.0, *) {
            supportsOnDeviceRecognition = recognizer.supportsOnDeviceRecognition
        } else {
            supportsOnDeviceRecognition = false
        }

        // Setup audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        // `setActive` can block on the audio daemon, which triggers an "UI
        // unresponsiveness" warning when called on the main thread, so hop off it.
        try await Task.detached(priority: .userInitiated) {
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        }.value

        let inputNode = audioEngine.inputNode
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()

        guard let request = recognitionRequest else {
            throw NSError(domain: "SpeechTranscriberManager", code: 3, userInfo: [NSLocalizedDescriptionKey: "Unable to create recognition request."])
        }

        request.shouldReportPartialResults = true

        // Deliberately NOT setting `requiresOnDeviceRecognition = true` here: `supportsOnDeviceRecognition`
        // reports `true` on the Simulator (and sometimes on real devices before the on-device model has
        // finished downloading) even when on-device recognition can't actually run, which makes
        // `recognitionTask` fail immediately with "Failed to initialize recognizer". Leaving this unset
        // lets iOS use on-device recognition when it's truly available and fall back to the server
        // transparently otherwise — `supportsOnDeviceRecognition` above is kept purely as an info display.

        recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
            if let result = result {
                DispatchQueue.main.async {
                    self?.transcription = result.bestTranscription.formattedString
                }
            }
            if let error = error {
                let nsError = error as NSError
                // Ignore cancellation errors - they occur when endAudio() is called
                if nsError.code != NSUserCancelledError && nsError.localizedDescription != "Recognition request was cancelled." {
                    DispatchQueue.main.async {
                        if nsError.localizedDescription == "Failed to initialize recognizer" {
                            // This exact message is Apple's own error, surfaced when SFSpeechRecognizer's
                            // backing service can't start at all — in practice this is almost always the
                            // iOS Simulator, which doesn't support live speech recognition regardless of
                            // on-device/server settings or permissions. Give a clearer, actionable message
                            // instead of the generic one.
                            self?.error = NSError(
                                domain: "SpeechTranscriberManager",
                                code: 4,
                                userInfo: [NSLocalizedDescriptionKey: "Speech recognition isn't available in the iOS Simulator. Please test voice recording on a real device."]
                            )
                        } else {
                            self?.error = error
                        }
                    }
                }
            }
        }

        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            request.append(buffer)
        }

        audioEngine.prepare()
        try audioEngine.start()
    }

    func stop() {
        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
        }
        // Signal end of audio without cancelling the task, to avoid error
        recognitionRequest?.endAudio()

        // Let the task finish naturally instead of cancelling
        recognitionRequest = nil
        recognitionTask = nil
        speechRecognizer = nil

        // Deactivate audio session off the main thread to avoid blocking the UI
        Task.detached(priority: .userInitiated) {
            let audioSession = AVAudioSession.sharedInstance()
            try? audioSession.setActive(false)
        }
    }
}
