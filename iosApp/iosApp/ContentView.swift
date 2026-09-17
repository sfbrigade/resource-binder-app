import SwiftUI
import SharedLogic
import Speech
import AVFoundation
import UIKit

struct ContentView: View {
    enum Stage {
        case onboarding
        case auth
        case describe
        case review
    }
    
    @State private var stage: Stage = .onboarding
    @State private var onboardingPageIndex: Int = 0
    @State private var emailAddress: String = ""
    @State private var situationText: String = ""
    @State private var isRecording: Bool = false
    @State private var detectedNeeds: [String] = []
    @State private var clientDetails: [String] = []
    @State private var showSecondaryActions: Bool = false
    @State private var showAlert: Bool = false
    @State private var alertMessage: String = ""
    @State private var showSubmissionConfirmation: Bool = false
    @State private var keywordCategories: [KeywordCategoryBucket] = []
    @State private var highlightedSituationText: AttributedString = AttributedString("")
    @StateObject private var transcriberManager = SpeechTranscriberManager()
    
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
            Color(uiColor: stage == .onboarding ? UIColor.systemBackground : UIColor.systemGroupedBackground)
                .ignoresSafeArea()
            
            switch stage {
            case .onboarding:
                onboardingScreen
                    .transition(.asymmetric(insertion: .scale.combined(with: .opacity), removal: .opacity))
            case .auth:
                authScreen
                    .transition(.asymmetric(insertion: .opacity.combined(with: .scale), removal: .opacity))
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
    
    // MARK: - Onboarding Screen
    var onboardingScreen: some View {
        OnboardingScreen(
            pageIndex: $onboardingPageIndex,
            pages: onboardingPages,
            onSkip: { stage = .auth },
            onFinish: { stage = .auth }
        )
        .onAppear {
            onboardingPageIndex = 0
        }
    }

    // MARK: - Auth Screen
    var authScreen: some View {
        AuthScreen(
            emailAddress: $emailAddress,
            onRequestAccess: { stage = .describe },
            onSkip: { stage = .describe }
        )
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
                        detectNeedsAndDetails()
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
                
                // On-device availability indicator
                HStack(spacing: 8) {
                    Text("Offline speech:")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                    if transcriberManager.supportsOnDeviceRecognition == nil {
                        Text("Checking...")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                    } else if transcriberManager.supportsOnDeviceRecognition == true {
                        Text("Available")
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(.green)
                    } else {
                        Text("Unavailable — will use cloud if needed")
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(.orange)
                    }
                    Spacer()
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
        ReviewScreenContent(
            situationText: situationText,
            highlightedSituationText: highlightedSituationText,
            detectedNeeds: detectedNeeds,
            clientDetails: clientDetails,
            keywordCategories: keywordCategories,
            showSecondaryActions: showSecondaryActions,
            onEdit: {
                withAnimation {
                    stage = .describe
                    showSecondaryActions = false
                }
            },
            onToggleSecondaryActions: {
                withAnimation {
                    showSecondaryActions.toggle()
                }
            }
        )
        .onAppear {
            refreshReviewAnalysis()
            withAnimation(.easeInOut.delay(0.3)) {
                showSecondaryActions = false
            }
        }
        .onChange(of: situationText) { _ in
            refreshReviewAnalysis()
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
        situationText = transcriberManager.transcription
        detectNeedsAndDetails()
        withAnimation {
            isRecording = false
            stage = .review
        }
    }

    private func refreshReviewAnalysis() {
        detectNeedsAndDetails()
    }
    
    // MARK: - Detect Needs & Client Details (Simulated)
    func detectNeedsAndDetails() {
        let analysis = TranscriptKeywordAnalyzer.analyze(situationText)
        highlightedSituationText = analysis.highlightedText
        detectedNeeds = analysis.detectedNeeds
        clientDetails = analysis.clientDetails
        keywordCategories = analysis.keywordCategories
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

private struct ReviewScreenContent: View {
    let situationText: String
    let highlightedSituationText: AttributedString
    let detectedNeeds: [String]
    let clientDetails: [String]
    let keywordCategories: [KeywordCategoryBucket]
    let showSecondaryActions: Bool
    let onEdit: () -> Void
    let onToggleSecondaryActions: () -> Void

    private var displayedSituationText: AttributedString {
        highlightedSituationText.characters.isEmpty ? AttributedString(situationText) : highlightedSituationText
    }

    var body: some View {
        VStack(spacing: 20) {
            ReviewSituationSection(situationText: situationText, displayedSituationText: displayedSituationText)
            ReviewChipSection(title: "Detected Needs", chipColor: .accentColor, iconName: "staroflife.fill", items: detectedNeeds)
            ReviewChipSection(title: "Client Details", chipColor: .green, iconName: "person.fill", items: clientDetails)

            if !keywordCategories.isEmpty {
                KeywordCategoriesSection(keywordCategories: keywordCategories)
            }

            Spacer()

            if showSecondaryActions {
                ReviewSecondaryActionsView()
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            ReviewFooterActions(onEdit: onEdit, onToggleSecondaryActions: onToggleSecondaryActions, showSecondaryActions: showSecondaryActions)
        }
        .padding()
        .frame(maxWidth: 600)
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Color(.systemBackground))
                .shadow(color: Color.black.opacity(0.12), radius: 18, x: 0, y: 8)
        )
    }
}

private struct ReviewSituationSection: View {
    let situationText: String
    let displayedSituationText: AttributedString

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Review needs & details")
                .font(.title2.weight(.bold))
                .foregroundColor(.primary)

            if !situationText.isEmpty {
                Text("Situation:")
                    .font(.headline)
                    .foregroundColor(.secondary)

                Text(displayedSituationText)
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
    }
}

private struct ReviewChipSection: View {
    let title: String
    let chipColor: Color
    let iconName: String
    let items: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.headline)
                .foregroundColor(.secondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(items, id: \.self) { item in
                        ChipView(title: item, color: chipColor, iconName: iconName)
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ChipView: View {
    let title: String
    let color: Color
    let iconName: String

    var body: some View {
        Capsule()
            .fill(color.opacity(0.15))
            .overlay(
                HStack(spacing: 6) {
                    Image(systemName: iconName)
                        .foregroundColor(color)
                        .font(.system(size: 14))
                    Text(title)
                        .foregroundColor(color)
                        .font(.subheadline.weight(.semibold))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
            )
            .fixedSize()
    }
}

private struct KeywordCategoriesSection: View {
    let keywordCategories: [KeywordCategoryBucket]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Keyword Categories")
                .font(.headline)
                .foregroundColor(.secondary)

            ForEach(keywordCategories) { bucket in
                KeywordCategoryCard(bucket: bucket)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct KeywordCategoryCard: View {
    let bucket: KeywordCategoryBucket

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: bucket.symbol)
                    .foregroundColor(bucket.tint)
                Text(bucket.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.primary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(bucket.terms, id: \.self) { term in
                        Text(term)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(bucket.tint)
                            .padding(.vertical, 6)
                            .padding(.horizontal, 10)
                            .background(
                                Capsule()
                                    .fill(bucket.tint.opacity(0.14))
                            )
                    }
                }
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(.secondarySystemBackground))
        )
    }
}

private struct ReviewFooterActions: View {
    let onEdit: () -> Void
    let onToggleSecondaryActions: () -> Void
    let showSecondaryActions: Bool

    var body: some View {
        HStack(spacing: 20) {
            Button(action: onEdit) {
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

            Button(action: onToggleSecondaryActions) {
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
}

private struct ReviewSecondaryActionsView: View {
    var body: some View {
        VStack(spacing: 14) {
            Button(action: {}) {
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

            Button(action: {}) {
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
}

// MARK: - Local keyword analysis

private struct KeywordCategoryBucket: Identifiable {
    let id = UUID()
    let title: String
    let symbol: String
    let tint: Color
    let terms: [String]
}

private struct TranscriptKeywordAnalysis {
    let highlightedText: AttributedString
    let detectedNeeds: [String]
    let clientDetails: [String]
    let keywordCategories: [KeywordCategoryBucket]

    static let empty = TranscriptKeywordAnalysis(
        highlightedText: AttributedString(""),
        detectedNeeds: [],
        clientDetails: [],
        keywordCategories: []
    )
}

private enum TranscriptKeywordAnalyzer {
    private struct CategoryDefinition {
        let title: String
        let symbol: String
        let tint: Color
        let terms: [String]
        let needsLabel: String
        let detailLabel: String
    }

    private struct Match {
        let category: CategoryDefinition
        let term: String
        let range: Range<String.Index>
    }

    private static let categories: [CategoryDefinition] = [
        .init(
            title: "Emotional Support",
            symbol: "heart.text.square.fill",
            tint: .pink,
            terms: ["anxiety", "anxious", "overwhelmed", "stress", "stressed", "panic", "worried", "depressed", "depression"],
            needsLabel: "Emotional support",
            detailLabel: "Possible emotional distress"
        ),
        .init(
            title: "Medication",
            symbol: "pills.fill",
            tint: .blue,
            terms: ["medication", "medications", "medicine", "dose", "dosage", "prescription", "refill", "side effects"],
            needsLabel: "Medication info",
            detailLabel: "Mentions medication concerns"
        ),
        .init(
            title: "Safety & Crisis",
            symbol: "exclamationmark.shield.fill",
            tint: .red,
            terms: ["unsafe", "crisis", "suicidal", "suicide", "self-harm", "overdose", "abuse", "violence"],
            needsLabel: "Safety planning",
            detailLabel: "Potential safety concern"
        ),
        .init(
            title: "Housing & Basic Needs",
            symbol: "house.fill",
            tint: .orange,
            terms: ["housing", "rent", "eviction", "homeless", "unhoused", "food", "shelter", "utilities"],
            needsLabel: "Basic needs support",
            detailLabel: "Housing or essential needs mentioned"
        ),
        .init(
            title: "Care Coordination",
            symbol: "person.2.fill",
            tint: .green,
            terms: ["appointment", "referral", "therapy", "therapist", "counseling", "case management", "transportation", "resources", "follow-up"],
            needsLabel: "Care coordination",
            detailLabel: "Needs follow-up or referral support"
        ),
        .init(
            title: "Social Support",
            symbol: "person.3.fill",
            tint: .purple,
            terms: ["family", "caregiver", "support", "isolated", "lonely", "isolation"],
            needsLabel: "Social support",
            detailLabel: "Mentions family or support network"
        )
    ]

    static func analyze(_ text: String) -> TranscriptKeywordAnalysis {
        let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedText.isEmpty else {
            return .empty
        }

        let matches = findMatches(in: trimmedText)
        let highlightedText = buildHighlightedText(from: trimmedText, matches: matches)

        let groupedMatches = Dictionary(grouping: matches, by: { $0.category.title })
        let keywordCategories = categories.compactMap { category -> KeywordCategoryBucket? in
            guard let categoryMatches = groupedMatches[category.title] else { return nil }
            let matchedTerms = categoryMatches
                .map { $0.term }
                .uniquedPreservingOrder()
            return KeywordCategoryBucket(title: category.title, symbol: category.symbol, tint: category.tint, terms: matchedTerms)
        }

        var detectedNeeds = keywordCategories.compactMap { bucket in
            categories.first(where: { $0.title == bucket.title })?.needsLabel
        }
        if detectedNeeds.isEmpty {
            detectedNeeds = ["General support"]
        }

        let clientDetails = keywordCategories.compactMap { bucket in
            categories.first(where: { $0.title == bucket.title })?.detailLabel
        }

        return TranscriptKeywordAnalysis(
            highlightedText: highlightedText,
            detectedNeeds: detectedNeeds,
            clientDetails: clientDetails,
            keywordCategories: keywordCategories
        )
    }

    private static func findMatches(in text: String) -> [Match] {
        var matches: [Match] = []
        let sortedCategories = categories.sorted { lhs, rhs in
            lhs.terms.joined(separator: " ").count > rhs.terms.joined(separator: " ").count
        }

        for category in sortedCategories {
            for term in category.terms.sorted(by: { $0.count > $1.count }) {
                for range in text.ranges(of: term) {
                    let overlaps = matches.contains { existing in
                        existing.range.overlaps(range)
                    }
                    guard !overlaps else { continue }
                    matches.append(Match(category: category, term: term, range: range))
                }
            }
        }

        return matches.sorted { $0.range.lowerBound < $1.range.lowerBound }
    }

    private static func buildHighlightedText(from text: String, matches: [Match]) -> AttributedString {
        var attributed = AttributedString(text)
        guard !matches.isEmpty else { return attributed }

        for match in matches {
            guard let lower = AttributedString.Index(match.range.lowerBound, within: attributed),
                  let upper = AttributedString.Index(match.range.upperBound, within: attributed) else {
                continue
            }

            let range = lower..<upper
            attributed[range].backgroundColor = match.category.tint.opacity(0.22)
            attributed[range].foregroundColor = match.category.tint
            attributed[range].font = .body.bold()
        }

        return attributed
    }
}

private extension String {
    func ranges(of searchText: String) -> [Range<String.Index>] {
        guard !searchText.isEmpty else { return [] }

        var results: [Range<String.Index>] = []
        var searchStartIndex = startIndex

        while searchStartIndex < endIndex,
              let range = range(of: searchText, options: [.caseInsensitive, .diacriticInsensitive], range: searchStartIndex..<endIndex) {
            if isWordBoundaryMatch(range: range, searchText: searchText) {
                results.append(range)
            }
            searchStartIndex = range.upperBound
        }

        return results
    }

    private func isWordBoundaryMatch(range: Range<String.Index>, searchText: String) -> Bool {
        let before = range.lowerBound > startIndex ? self[index(before: range.lowerBound)] : nil
        let after = range.upperBound < endIndex ? self[range.upperBound] : nil

        let beforeIsBoundary = before.map { !$0.isLetter && !$0.isNumber } ?? true
        let afterIsBoundary = after.map { !$0.isLetter && !$0.isNumber } ?? true

        if searchText.contains(" ") {
            return beforeIsBoundary && afterIsBoundary
        }

        return beforeIsBoundary && afterIsBoundary
    }
}

private extension Array where Element == String {
    func uniquedPreservingOrder() -> [String] {
        var seen = Set<String>()
        var result: [String] = []

        for value in self {
            let lowered = value.lowercased()
            guard !seen.contains(lowered) else { continue }
            seen.insert(lowered)
            result.append(value)
        }

        return result
    }
}

struct RoundedCapsuleBackground: View {
    var color: Color
    var body: some View {
        Capsule()
            .fill(color)
    }
}

private struct OnboardingPageData {
    let title: String
    let subtitle: String
    let buttonTitle: String
}

private struct OnboardingScreen: View {
    @Binding var pageIndex: Int
    let pages: [OnboardingPageData]
    let onSkip: () -> Void
    let onFinish: () -> Void

    private var currentPage: OnboardingPageData {
        pages[min(max(pageIndex, 0), pages.count - 1)]
    }

    private var isLastPage: Bool {
        pageIndex == pages.count - 1
    }

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 0) {
                HStack {
                    if pageIndex > 0 {
                        Button {
                            withAnimation(.easeInOut(duration: 0.25)) {
                                pageIndex = max(pageIndex - 1, 0)
                            }
                        } label: {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.primary)
                                .frame(width: 44, height: 44)
                                .background(
                                    Circle()
                                        .fill(Color(.systemGray6))
                                )
                        }
                        .buttonStyle(.plain)
                    } else {
                        Color.clear.frame(width: 44, height: 44)
                    }

                    Spacer()

                    Button(action: onSkip) {
                        Text("Skip")
                            .font(.body)
                            .foregroundColor(.primary)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 24)
                .padding(.top, 8)

                Spacer(minLength: 0)

                OnboardingIllustrationView()
                    .frame(height: proxy.size.height * 0.34)
                    .padding(.horizontal, 24)

                Spacer(minLength: 0)

                VStack(spacing: 14) {
                    Text(currentPage.title)
                        .font(.system(size: 29, weight: .bold, design: .default))
                        .multilineTextAlignment(.center)
                        .foregroundColor(.black)
                        .lineSpacing(1.5)

                    Text(currentPage.subtitle)
                        .font(.system(size: 16, weight: .regular))
                        .multilineTextAlignment(.center)
                        .foregroundColor(Color(UIColor.systemGray2))
                        .lineSpacing(4)
                        .padding(.horizontal, 18)
                }
                .padding(.horizontal, 12)

                Spacer(minLength: 0)

                OnboardingProgressDots(currentIndex: pageIndex, count: pages.count)
                    .padding(.bottom, 18)

                Button {
                    withAnimation(.easeInOut(duration: 0.28)) {
                        if isLastPage {
                            onFinish()
                        } else {
                            pageIndex += 1
                        }
                    }
                } label: {
                    Text(currentPage.buttonTitle)
                        .font(.system(size: 17, weight: .regular))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            Capsule()
                                .fill(Color(red: 0.12, green: 0.13, blue: 0.15))
                        )
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 24)
                .padding(.bottom, max(proxy.safeAreaInsets.bottom, 12) + 4)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
            .ignoresSafeArea()
        }
    }
}

private struct OnboardingProgressDots: View {
    let currentIndex: Int
    let count: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<count, id: \.self) { index in
                Capsule()
                    .fill(index == currentIndex ? Color(red: 0.15, green: 0.16, blue: 0.18) : Color(UIColor.systemGray4))
                    .frame(width: index == currentIndex ? 20 : 7, height: 7)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: currentIndex)
    }
}

private struct OnboardingIllustrationView: View {
    private let columns = 12
    private let rows = 10

    var body: some View {
        VStack(spacing: 6) {
            ForEach(0..<rows, id: \.self) { row in
                HStack(spacing: 6) {
                    ForEach(0..<columns, id: \.self) { column in
                        RoundedRectangle(cornerRadius: 2.5, style: .continuous)
                            .fill(squareColor(for: row, column: column))
                            .frame(width: 12, height: 12)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, 18)
    }

    private func squareColor(for row: Int, column: Int) -> Color {
        (row + column).isMultiple(of: 2)
            ? Color(UIColor.systemGray5).opacity(0.8)
            : Color(UIColor.systemGray4).opacity(0.28)
    }
}

private struct AuthScreen: View {
    @Binding var emailAddress: String
    let onRequestAccess: () -> Void
    let onSkip: () -> Void

    private enum DisplayState {
        case form
        case accountNotFound
    }

    @State private var displayState: DisplayState = .form
    @State private var showNetworkBanner: Bool = false
    @State private var hasShownNetworkBannerForCurrentEmail: Bool = false

    private var trimmedEmail: String {
        emailAddress.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isValidEmailFormat: Bool {
        let parts = trimmedEmail.split(separator: "@")
        guard parts.count == 2 else { return false }
        return parts[1].contains(".") && !parts[0].isEmpty && !parts[1].isEmpty
    }

    private var isWorkEmail: Bool {
        let lowercased = trimmedEmail.lowercased()
        return lowercased.hasSuffix("@civic-tech.design") || lowercased.hasSuffix("@resource-binder.app")
    }

    private var canSubmit: Bool {
        isValidEmailFormat
    }

    private var inlineValidationMessage: String? {
        guard !trimmedEmail.isEmpty, !isValidEmailFormat else { return nil }
        return "Enter a valid email."
    }

    private var titleTopSpacing: CGFloat {
        displayState == .accountNotFound ? 96 : 140
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                Color.white
                    .ignoresSafeArea()

                authBody(proxyHeight: proxy.size.height)
                .padding(.horizontal, 24)
                .padding(.bottom, max(proxy.safeAreaInsets.bottom, 14) + 4)

                if showNetworkBanner && displayState == .form {
                    VStack {
                        topBanner
                            .padding(.horizontal, 18)
                            .padding(.top, max(proxy.safeAreaInsets.top, 12))
                        Spacer()
                    }
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.28), value: displayState)
            .animation(.easeInOut(duration: 0.28), value: showNetworkBanner)
        }
    }

    @ViewBuilder
    private func authBody(proxyHeight: CGFloat) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: titleTopSpacing)

            switch displayState {
            case .form:
                formContent(proxyHeight: proxyHeight)
            case .accountNotFound:
                accountNotFoundContent
            }

            Spacer(minLength: 0)
        }
    }

    private var topBanner: some View {
        HStack(spacing: 10) {
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(.red)
                .font(.system(size: 18, weight: .semibold))

            Text("No internet connection. Try again.")
                .font(.system(size: 13.5, weight: .medium))
                .foregroundColor(.primary)

            Spacer(minLength: 0)
        }
        .padding(.vertical, 11)
        .padding(.horizontal, 14)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.11), radius: 10, x: 0, y: 4)
        )
    }

    private func formContent(proxyHeight: CGFloat) -> some View {
        VStack(spacing: 0) {
            authTitle

            Spacer(minLength: 24)

            VStack(alignment: .leading, spacing: 8) {
                emailLabel
                emailField

                if let inlineValidationMessage {
                    Text(inlineValidationMessage)
                        .font(.system(size: 12, weight: .regular))
                        .foregroundColor(.red)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Spacer(minLength: proxyHeight * 0.22)

            primaryActionButton

            secondaryLinkButton
        }
        .onChange(of: emailAddress) { _ in
            showNetworkBanner = false
            hasShownNetworkBannerForCurrentEmail = false
            if displayState == .accountNotFound {
                displayState = .form
            }
        }
    }

    private var authTitle: some View {
        VStack(spacing: 16) {
            Text("Welcome back!")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.black)

            Text("Sign in using your work email. We'll send you a secure link to continue.")
                .font(.system(size: 15, weight: .regular))
                .foregroundColor(Color(UIColor.systemGray2))
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .padding(.horizontal, 8)
        }
        .frame(maxWidth: 330)
    }

    private var emailLabel: some View {
        HStack(spacing: 0) {
            Text("Email Address")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.black)
            Text("*")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.red)
        }
    }

    private var emailField: some View {
        TextField("Your email", text: $emailAddress)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
            .keyboardType(.emailAddress)
            .padding(.horizontal, 12)
            .frame(height: 32)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color.white)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(inlineValidationMessage == nil ? Color(UIColor.systemGray5) : Color.red, lineWidth: 1)
            )
    }

    private var primaryActionButton: some View {
        Button {
            showNetworkBanner = false

            if !isValidEmailFormat {
                displayState = .form
                return
            }

            if !isWorkEmail {
                displayState = .accountNotFound
                return
            }

            if !hasShownNetworkBannerForCurrentEmail {
                hasShownNetworkBannerForCurrentEmail = true
                showNetworkBanner = true
                return
            }

            onRequestAccess()
        } label: {
            Text("Email me a sign-in link")
                .font(.system(size: 17, weight: .regular))
                .foregroundColor(canSubmit ? .white : Color(UIColor.systemGray4))
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(
                    Capsule()
                        .fill(canSubmit ? Color(red: 0.12, green: 0.13, blue: 0.15) : Color(UIColor.systemGray5))
                )
        }
        .buttonStyle(.plain)
        .disabled(!canSubmit)
        .padding(.top, 8)
    }

    private var secondaryLinkButton: some View {
        Button {
            displayState = .accountNotFound
        } label: {
            Text("Need access? Request")
                .font(.system(size: 17, weight: .regular))
                .foregroundColor(Color(red: 0.26, green: 0.40, blue: 1.0))
        }
        .buttonStyle(.plain)
        .padding(.top, 24)
    }

    private var accountNotFoundContent: some View {
        VStack(spacing: 18) {
            AccountNotFoundIcon()

            Text("We couldn't find your account")
                .font(.system(size: 24, weight: .bold))
                .multilineTextAlignment(.center)
                .foregroundColor(.black)

            Text("Make sure you're using your work email or request access.")
                .font(.system(size: 15, weight: .regular))
                .multilineTextAlignment(.center)
                .foregroundColor(Color(UIColor.systemGray2))
                .lineSpacing(3)
                .padding(.horizontal, 18)

            Spacer(minLength: 28)

            VStack(spacing: 10) {
                Button {
                    displayState = .form
                    showNetworkBanner = false
                    hasShownNetworkBannerForCurrentEmail = false
                } label: {
                    Text("Try another email")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(
                            Capsule()
                                .fill(Color(UIColor.systemGray5).opacity(0.5))
                        )
                }
                .buttonStyle(.plain)

                Button {
                    onSkip()
                } label: {
                    Text("Request access")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(
                            Capsule()
                                .fill(Color(red: 0.12, green: 0.13, blue: 0.15))
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: 330)
    }
}

private struct AccountNotFoundIcon: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 14, style: .continuous)
            .fill(
                LinearGradient(
                    colors: [Color(UIColor.systemGray6), Color(UIColor.systemGray5).opacity(0.4)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay(
                VStack(spacing: 4) {
                    ForEach(0..<6, id: \.self) { row in
                        HStack(spacing: 4) {
                            ForEach(0..<6, id: \.self) { column in
                                RoundedRectangle(cornerRadius: 1.5, style: .continuous)
                                    .fill((row + column).isMultiple(of: 2) ? Color.white.opacity(0.68) : Color.clear)
                                    .frame(width: 6, height: 6)
                            }
                        }
                    }
                }
                .padding(12)
            )
            .frame(width: 78, height: 78)
    }
}

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
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        let inputNode = audioEngine.inputNode
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()

        guard let request = recognitionRequest else {
            throw NSError(domain: "SpeechTranscriberManager", code: 3, userInfo: [NSLocalizedDescriptionKey: "Unable to create recognition request."])
        }

        request.shouldReportPartialResults = true

        // Prefer on-device recognition when available; otherwise allow server-side fallback.
        if #available(iOS 13.0, *) {
            if let recognizer = speechRecognizer {
                if recognizer.supportsOnDeviceRecognition {
                    // Request on-device processing when supported to keep speech local.
                    request.requiresOnDeviceRecognition = true
                } else {
                    // On-device model isn't available for this locale/device — allow server-side fallback.
                    // if it cant use the local servers then must use the Apple's servers if needed.
                    print("On-device recognition unavailable for locale; allowing server-side fallback.")
                    supportsOnDeviceRecognition = false
                }
            }
        } else {
            // On older iOS versions, `requiresOnDeviceRecognition` isn't supported — allow fallback.
            print("On-device recognition API not available on this iOS version; allowing server-side processing.")
            supportsOnDeviceRecognition = false
        }

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
