import SwiftUI
import SharedLogic

/// Root coordinator for the whole app. Owns the `Stage` state machine and the
/// shared `ClientProfile`/`SpeechTranscriberManager` objects, and simply switches
/// between the screen views defined in Onboarding/, Auth/, Intake/, and CarePlan/.
struct ContentView: View {
    enum Stage: Equatable {
        case onboarding
        case signIn
        case signUp
        case checkEmail
        case signingIn
        case faceID
        case intakeChoice
        case voiceDescribe
        case needsChecklist
        case detailsReview
        case categoryEditor
        case generatingResources
        case carePlanDraft
        case carePlan
    }

    enum IntakeSource {
        case voice
        case manual
    }

    @State private var stage: Stage = .onboarding
    @State private var onboardingPageIndex: Int = 0
    @State private var emailAddress: String = ""
    @State private var situationText: String = ""
    @State private var isRecording: Bool = false
    @State private var intakeSource: IntakeSource = .manual
    @State private var editingCategory: NeedCategory?
    @State private var showAlert: Bool = false
    @State private var alertMessage: String = ""

    @StateObject private var transcriberManager = SpeechTranscriberManager()
    @StateObject private var profile = ClientProfile()

    private let onboardingPages: [OnboardingPageData] = [
        .init(
            title: "Help people find\nthe right support faster",
            subtitle: "Create care plans in minutes by capturing the situation and matching the right resources.",
            buttonTitle: "Next"
        ),
        .init(
            title: "Speak or type what\nyou know",
            subtitle: "Describe the client's situation with your voice or enter information manually. You can edit everything before sharing.",
            buttonTitle: "Next"
        ),
        .init(
            title: "AI helps. You decide",
            subtitle: "We'll identify needs, detect important details, and recommend resources. You stay in control of every decision.",
            buttonTitle: "Next"
        ),
        .init(
            title: "Built for the field",
            subtitle: "Work online or offline. Your plans stay on your device and sync automatically when you're connected.",
            buttonTitle: "Get started"
        )
    ]

    var body: some View {
        ZStack {
            Color(uiColor: .systemGroupedBackground)
                .ignoresSafeArea()

            Group {
                switch stage {
                case .onboarding:
                    OnboardingScreen(
                        pageIndex: $onboardingPageIndex,
                        pages: onboardingPages,
                        onSkip: { stage = .signIn },
                        onFinish: { stage = .signIn }
                    )
                case .signIn:
                    SignInScreen(
                        emailAddress: $emailAddress,
                        onContinue: { stage = .checkEmail },
                        onRequestAccess: { stage = .intakeChoice },
                        onCreateAccount: { stage = .signUp }
                    )
                case .signUp:
                    SignUpScreen(
                        onBack: { stage = .signIn },
                        onDone: { stage = .signIn }
                    )
                case .checkEmail:
                    CheckEmailScreen(
                        email: emailAddress,
                        onChangeEmail: { stage = .signIn },
                        onContinue: { stage = .signingIn }
                    )
                case .signingIn:
                    AutoAdvanceLoadingScreen(message: "Signing you in...") {
                        stage = .faceID
                    }
                case .faceID:
                    FaceIDScreen(
                        onEnabled: { stage = .intakeChoice },
                        onSkip: { stage = .intakeChoice }
                    )
                case .intakeChoice:
                    IntakeChoiceScreen(
                        onChooseVoice: {
                            intakeSource = .voice
                            profile.resetIntake()
                            situationText = ""
                            stage = .voiceDescribe
                        },
                        onChooseManual: {
                            intakeSource = .manual
                            profile.resetIntake()
                            stage = .needsChecklist
                        }
                    )
                case .voiceDescribe:
                    VoiceDescribeScreen(
                        situationText: $situationText,
                        isRecording: $isRecording,
                        transcriberManager: transcriberManager,
                        onBack: { stage = .intakeChoice },
                        onClose: { stage = .intakeChoice },
                        onStartRecording: startRecording,
                        onStopRecording: stopRecording,
                        onReviewNeeds: proceedFromVoiceDescribe
                    )
                case .needsChecklist:
                    NeedsChecklistScreen(
                        title: intakeSource == .voice ? "Review the needs we identified" : "What does the client need today?",
                        subtitle: intakeSource == .voice ? "Add or remove anything before continuing." : "Select all that apply.",
                        currentStep: intakeSource == .voice ? 1 : 0,
                        totalSteps: intakeSource == .voice ? 3 : 2,
                        selectedIDs: $profile.selectedNeedIDs,
                        onBack: { stage = intakeSource == .voice ? .voiceDescribe : .intakeChoice },
                        onClose: { stage = .intakeChoice },
                        onContinue: { stage = .detailsReview }
                    )
                case .detailsReview:
                    DetailsReviewScreen(
                        mode: intakeSource == .voice ? .review : .addDetails,
                        categories: profile.reviewCategories,
                        profile: profile,
                        currentStep: intakeSource == .voice ? 2 : 1,
                        totalSteps: intakeSource == .voice ? 3 : 2,
                        onBack: { stage = .needsChecklist },
                        onClose: { stage = .intakeChoice },
                        onEditCategory: { category in
                            editingCategory = category
                            stage = .categoryEditor
                        },
                        onFindResources: { stage = .generatingResources }
                    )
                case .categoryEditor:
                    if let category = editingCategory {
                        CategoryDetailEditorScreen(
                            category: category,
                            profile: profile,
                            onBack: { stage = .detailsReview },
                            onSave: { stage = .detailsReview }
                        )
                    }
                case .generatingResources:
                    AutoAdvanceLoadingScreen(message: "Finding resources...") {
                        profile.seedNextStepsIfNeeded()
                        stage = .carePlanDraft
                    }
                case .carePlanDraft:
                    CarePlanDraftScreen(
                        profile: profile,
                        onBack: { stage = .detailsReview },
                        onClose: { stage = .intakeChoice },
                        onFinalize: { stage = .carePlan }
                    )
                case .carePlan:
                    CarePlanScreen(
                        profile: profile,
                        planNumber: "00001",
                        preparedByName: preparedByName,
                        organizationName: "Resource_Binder",
                        onBackToEditing: { stage = .carePlanDraft },
                        onClose: { stage = .intakeChoice }
                    )
                }
            }
            .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                                     removal: .move(edge: .leading).combined(with: .opacity)))
        }
        .animation(.easeInOut(duration: 0.35), value: stage)
        .onChange(of: transcriberManager.transcription) { newTranscription in
            if isRecording {
                situationText = newTranscription
            }
        }
        .onChange(of: transcriberManager.errorMessage) { newErrorMessage in
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

    private var preparedByName: String {
        let local = emailAddress.split(separator: "@").first.map(String.init) ?? ""
        let cleaned = local.replacingOccurrences(of: ".", with: " ").replacingOccurrences(of: "_", with: " ")
        let capitalized = cleaned
            .split(separator: " ")
            .map { $0.capitalized }
            .joined(separator: " ")
        return capitalized.isEmpty ? "Case Manager" : capitalized
    }

    // MARK: - Recording

    private func startRecording() {
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

    private func stopRecording() {
        transcriberManager.stop()
        situationText = transcriberManager.transcription
        withAnimation {
            isRecording = false
        }
    }

    private func proceedFromVoiceDescribe() {
        profile.situationText = situationText
        let analysis = TranscriptKeywordAnalyzer.analyze(situationText)
        profile.selectedNeedIDs = analysis.detectedNeedIDs
        withAnimation {
            stage = .needsChecklist
        }
    }
}

#Preview {
    ContentView()
}
