import SwiftUI
import SharedLogic
import Speech
import AVFoundation

struct ContentView: View {
    enum Stage {
        case welcome
        case describe
        case review
    }
    
    @State private var stage: Stage = .welcome
    @State private var situationText: String = ""
    @State private var isRecording: Bool = false
    @State private var detectedNeeds: [String] = []
    @State private var clientDetails: [String] = []
    @State private var showSecondaryActions: Bool = false
    @State private var showAlert: Bool = false
    @State private var alertMessage: String = ""
    @State private var showSubmissionConfirmation: Bool = false
    @StateObject private var transcriberManager = SpeechTranscriberManager()
    
    private let exampleSituation = """
    Client feels anxious and overwhelmed after recent diagnosis. Needs emotional support and info on medication.
    """
    
    var body: some View {
        ZStack {
            Color(uiColor: UIColor.systemGroupedBackground)
                .ignoresSafeArea()
            
            switch stage {
            case .welcome:
                welcomeCard
                    .transition(.asymmetric(insertion: .scale.combined(with: .opacity), removal: .opacity))
            case .describe:
                describeScreen
                    .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                                            removal: .move(edge: .leading).combined(with: .opacity)))
            case .review:
                reviewScreen
                    .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                                            removal: .move(edge: .leading).combined(with: .opacity)))
            }
            
            if showSubmissionConfirmation {
                submissionConfirmationOverlay
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.4), value: stage)
        .padding()
        .onChange(of: transcriberManager.transcription) { oldValue, newTranscription in
            if isRecording {
                situationText = newTranscription
            }
        }
        .onChange(of: transcriberManager.errorMessage) { oldValue, newErrorMessage in
            if let errorMessage = newErrorMessage {
                alertMessage = errorMessage
                showAlert = true
                isRecording = false
            }
        }
        .alert(isPresented: $showAlert) {
            Alert(title: Text("Speech Recognition Error"),
                  message: Text(alertMessage),
                  dismissButton: .default(Text("OK")) {
                    transcriberManager.error = nil
                    transcriberManager.errorMessage = nil
                  })
        }
    }
    
    // MARK: - Welcome Card
    var welcomeCard: some View {
        Button {
            withAnimation {
                stage = .describe
            }
        } label: {
            VStack(spacing: 12) {
                Image(systemName: "plus.circle.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 64, height: 64)
                    .foregroundColor(Color.accentColor.opacity(0.7))
                    .shadow(radius: 3)
                Text("Start new care plan")
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.primary.opacity(0.8))
            }
            .frame(maxWidth: 280, minHeight: 180)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color(.secondarySystemBackground))
                    .shadow(color: Color.black.opacity(0.1), radius: 12, x: 0, y: 6)
            )
            .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .accessibilityAddTraits(.isButton)
        }
        .buttonStyle(PlainButtonStyle())
    }
    
    // MARK: - Describe Screen
    var describeScreen: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Describe the situation")
                    .font(.title2.weight(.bold))
                    .foregroundColor(.primary)
                
                TextEditor(text: $situationText)
                    .padding(12)
                    .frame(minHeight: 150, maxHeight: 180)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color(.systemBackground))
                            .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 2)
                    )
                    .overlay(
                        Group {
                            if situationText.isEmpty {
                                Text("Type or record your notes here...")
                                    .foregroundColor(.secondary)
                                    .padding(16)
                                    .padding(.top, 4)
                                    .allowsHitTesting(false)
                                    .font(.body)
                            }
                        }, alignment: .topLeading
                    )
                    .accessibilityLabel("Situation description")
                    .accessibilityHint("You can type or record your notes about the situation.")
                    .disableAutocorrection(true)
                
                if !isRecording && !situationText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Button {
                        // End editing and move to review screen with confirmation
                        endEditing()
                        withAnimation {
                            stage = .review
                            showSubmissionConfirmation = true
                        }
                        detectNeedsAndDetails()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                            withAnimation {
                                showSubmissionConfirmation = false
                            }
                        }
                    } label: {
                        Text("Submit")
                            .font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .fill(Color.accentColor.opacity(0.85))
                            )
                            .foregroundColor(.white)
                    }
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
            }
            
            recordButton
            
            Spacer()
        }
        .padding()
    }
    
    var recordButton: some View {
        Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                if isRecording {
                    stopRecording()
                } else {
                    startRecording()
                }
            }
        } label: {
            ZStack {
                Circle()
                    .fill(isRecording ? Color.red.opacity(0.85) : Color.accentColor.opacity(0.85))
                    .frame(width: 88, height: 88)
                    .shadow(color: isRecording ? Color.red.opacity(0.6) : Color.accentColor.opacity(0.5), radius: 12, x: 0, y: 6)
                    .scaleEffect(isRecording ? 1.15 : 1.0)
                    .animation(.easeInOut(duration: 0.6).repeatForever(autoreverses: true), value: isRecording)
                
                Image(systemName: isRecording ? "waveform.circle.fill" : "mic.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 42, height: 42)
                    .foregroundColor(.white)
                    .shadow(radius: 2)
            }
        }
        .accessibilityLabel(isRecording ? "Stop recording" : "Start recording")
        .accessibilityHint("Tap to \(isRecording ? "stop" : "start") voice recording of the situation")
        .buttonStyle(PlainButtonStyle())
    }
    
    // MARK: - Review Screen
    var reviewScreen: some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 14) {
                Text("Review needs & details")
                    .font(.title2.weight(.bold))
                    .foregroundColor(.primary)
                
                if !situationText.isEmpty {
                    Text("Situation:")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Text(situationText)
                        .font(.body)
                        .foregroundColor(.primary)
                        .padding(12)
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(Color(.systemBackground))
                                .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 2)
                        )
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            VStack(alignment: .leading, spacing: 16) {
                Text("Detected Needs")
                    .font(.headline)
                    .foregroundColor(.secondary)
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 14) {
                        ForEach(detectedNeeds, id: \.self) { need in
                            Capsule()
                                .fill(Color.accentColor.opacity(0.15))
                                .overlay(
                                    HStack(spacing: 6) {
                                        Image(systemName: "staroflife.fill")
                                            .foregroundColor(Color.accentColor)
                                            .font(.system(size: 14))
                                        Text(need)
                                            .foregroundColor(Color.accentColor)
                                            .font(.subheadline.weight(.semibold))
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                )
                                .fixedSize()
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            VStack(alignment: .leading, spacing: 16) {
                Text("Client Details")
                    .font(.headline)
                    .foregroundColor(.secondary)
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 14) {
                        ForEach(clientDetails, id: \.self) { detail in
                            Capsule()
                                .fill(Color.green.opacity(0.15))
                                .overlay(
                                    HStack(spacing: 6) {
                                        Image(systemName: "person.fill")
                                            .foregroundColor(Color.green)
                                            .font(.system(size: 14))
                                        Text(detail)
                                            .foregroundColor(Color.green)
                                            .font(.subheadline.weight(.semibold))
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                )
                                .fixedSize()
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            Spacer()
            
            if showSecondaryActions {
                secondaryActions
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
            
            HStack(spacing: 20) {
                Button {
                    withAnimation {
                        stage = .describe
                        showSecondaryActions = false
                    }
                } label: {
                    HStack {
                        Image(systemName: "pencil")
                        Text("Edit")
                    }
                    .font(.headline)
                    .foregroundColor(.accentColor)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 20)
                    .background(
                        RoundedCapsuleBackground(color: Color.accentColor.opacity(0.15))
                    )
                }
                .buttonStyle(PlainButtonStyle())
                .accessibilityLabel("Edit situation and details")
                
                Button {
                    withAnimation {
                        showSecondaryActions.toggle()
                    }
                } label: {
                    HStack {
                        Image(systemName: "plus.circle")
                        Text(showSecondaryActions ? "Hide Details" : "Add Details")
                    }
                    .font(.headline)
                    .foregroundColor(.green)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 20)
                    .background(
                        RoundedCapsuleBackground(color: Color.green.opacity(0.15))
                    )
                }
                .buttonStyle(PlainButtonStyle())
                .accessibilityLabel(showSecondaryActions ? "Hide additional details" : "Add additional details")
            }
        }
        .padding()
        .frame(maxWidth: 600)
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Color(.systemBackground))
                .shadow(color: Color.black.opacity(0.12), radius: 18, x: 0, y: 8)
        )
        .onAppear {
            detectNeedsAndDetails()
            withAnimation(.easeInOut.delay(0.3)) {
                showSecondaryActions = false
            }
        }
        .onChange(of: situationText) { oldValue, newValue in
            detectNeedsAndDetails()
        }
    }
    
    var secondaryActions: some View {
        VStack(spacing: 14) {
            Button {
                // Placeholder for Add Details action
            } label: {
                HStack {
                    Image(systemName: "plus.bubble.fill")
                    Text("Add more client details")
                }
                .font(.body.weight(.semibold))
                .foregroundColor(.blue)
                .padding(.vertical, 10)
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color.blue.opacity(0.1))
                )
            }
            .buttonStyle(PlainButtonStyle())
            
            Button {
                // Placeholder for Find Resources action
            } label: {
                HStack {
                    Image(systemName: "magnifyingglass.circle.fill")
                    Text("Find resources")
                }
                .font(.body.weight(.semibold))
                .foregroundColor(.orange)
                .padding(.vertical, 10)
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color.orange.opacity(0.1))
                )
            }
            .buttonStyle(PlainButtonStyle())
        }
        .padding(.top, 12)
    }
    
    // MARK: - Recording using SpeechTranscriberManager
    func startRecording() {
        situationText = ""
        Task {
            do {
                try await transcriberManager.start()
                withAnimation {
                    isRecording = true
                }
            } catch {
                alertMessage = error.localizedDescription
                showAlert = true
                isRecording = false
            }
        }
    }
    
    func stopRecording() {
        transcriberManager.stop()
        withAnimation {
            isRecording = false
            stage = .review
        }
    }
    
    // MARK: - Detect Needs & Client Details (Simulated)
    func detectNeedsAndDetails() {
        // Basic simulation of detection based on situationText (case insensitive)
        let lowerText = situationText.lowercased()
        
        var needs = [String]()
        if lowerText.contains("emotional") || lowerText.contains("anxious") || lowerText.contains("anxiety") {
            needs.append("Emotional support")
        }
        if lowerText.contains("medication") || lowerText.contains("medications") || lowerText.contains("medicine") {
            needs.append("Medication info")
        }
        if lowerText.contains("overwhelmed") || lowerText.contains("stress") {
            needs.append("Anxiety management")
        }
        
        if needs.isEmpty && !situationText.isEmpty {
            needs.append("General support")
        }
        
        detectedNeeds = needs
        
        var details = [String]()
        // For simulation, use example details if some keywords match
        if lowerText.contains("anxiety") {
            details.append("Diagnosis: Anxiety")
        }
        if lowerText.contains("age") {
            // Extract age if mentioned? For now, just add placeholder
            details.append("Age: 32")
        }
        if lowerText.contains("medications") || lowerText.contains("medication") {
            details.append("Medications: None")
        }
        clientDetails = details
    }
    
    // MARK: - Submission Confirmation Overlay
    var submissionConfirmationOverlay: some View {
        VStack {
            Spacer()
            HStack(spacing: 12) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.system(size: 28, weight: .semibold))
                    .shadow(radius: 3)
                Text("Submitted!")
                    .font(.headline.weight(.semibold))
                    .foregroundColor(.green)
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 24)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Color(.systemBackground))
                    .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
            )
            .padding(.bottom, 40)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
    
    private func endEditing() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

struct RoundedCapsuleBackground: View {
    var color: Color
    var body: some View {
        Capsule()
            .fill(color)
    }
}

@MainActor
class SpeechTranscriberManager: ObservableObject {
    @Published var transcription: String = ""
    @Published var errorMessage: String? = nil

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

        // Setup audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        let inputNode = audioEngine.inputNode
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()

        guard let request = recognitionRequest else {
            throw NSError(domain: "SpeechTranscriberManager", code: 3, userInfo: [NSLocalizedDescriptionKey: "Unable to create recognition request."])
        }

        request.shouldReportPartialResults = true

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
                        self?.error = error
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

        // Deactivate audio session
        Task {
            let audioSession = AVAudioSession.sharedInstance()
            try? audioSession.setActive(false)
        }
    }
}

#Preview {
    ContentView()
}
