import SwiftUI

/// Landing screen between auth and intake — bridges the mockup's separate Voice flow
/// and Manual flow exports into one explicit entry point.
struct IntakeChoiceScreen: View {
    let onChooseVoice: () -> Void
    let onChooseManual: () -> Void

    var body: some View {
        VStack(spacing: 28) {
            Spacer(minLength: 12)

            VStack(spacing: 10) {
                Text("How would you like to start?")
                    .font(.system(size: 26, weight: .bold))
                    .multilineTextAlignment(.center)

                Text("Describe the client's situation with your voice, or enter what you know manually. You can always add more later.")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 12)
            }

            VStack(spacing: 16) {
                IntakeChoiceCard(
                    icon: "waveform",
                    tint: .accentColor,
                    title: "Describe with voice",
                    subtitle: "Speak naturally — we'll detect the needs.",
                    action: onChooseVoice
                )
                IntakeChoiceCard(
                    icon: "checklist",
                    tint: .green,
                    title: "Enter needs manually",
                    subtitle: "Select from a checklist of needs.",
                    action: onChooseManual
                )
            }

            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.top, 40)
    }
}

private struct IntakeChoiceCard: View {
    let icon: String
    let tint: Color
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                ZStack {
                    Circle().fill(tint.opacity(0.15)).frame(width: 52, height: 52)
                    Image(systemName: icon)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(tint)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.primary)
                    Text(subtitle)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
                    .shadow(color: Color.black.opacity(0.05), radius: 6, x: 0, y: 2)
            )
        }
        .buttonStyle(.plain)
    }
}
