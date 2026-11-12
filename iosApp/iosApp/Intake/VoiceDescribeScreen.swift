import SwiftUI
import UIKit

/// Restyled version of the original merged describe+record screen, matching the
/// "Voice flow" mockup's header (back/progress/close) and CTA copy.
struct VoiceDescribeScreen: View {
    @Binding var situationText: String
    @Binding var isRecording: Bool
    @ObservedObject var transcriberManager: SpeechTranscriberManager

    let onBack: () -> Void
    let onClose: () -> Void
    let onStartRecording: () -> Void
    let onStopRecording: () -> Void
    let onReviewNeeds: () -> Void

    private var hasText: Bool {
        !situationText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        VStack(spacing: 24) {
            FlowHeader(currentStep: 0, totalSteps: 3, onBack: onBack, onClose: onClose)

            VStack(alignment: .leading, spacing: 8) {
                Text("Describe the client's situation")
                    .font(.title2.weight(.bold))
                    .foregroundColor(.primary)

                Text("Include what they need and any details that could improve the matches.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

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

                offlineSpeechIndicator
            }

            recordButton

            if !isRecording && hasText {
                PillButton(title: "Review needs & details") {
                    endEditing()
                    onReviewNeeds()
                }
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }

            Spacer()
        }
        .padding()
        .animation(.easeInOut(duration: 0.2), value: hasText)
    }

    private var offlineSpeechIndicator: some View {
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

    private var recordButton: some View {
        Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                if isRecording {
                    onStopRecording()
                } else {
                    onStartRecording()
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
        .buttonStyle(.plain)
    }

    private func endEditing() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}
