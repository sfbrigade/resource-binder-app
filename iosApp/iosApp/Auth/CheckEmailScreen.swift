import SwiftUI

/// Mirrors the mockup's "Check your email" state, including the resend cooldown.
/// Since there's no real mail backend, a clearly-labeled demo action stands in for
/// actually tapping the link in an email client.
struct CheckEmailScreen: View {
    let email: String
    let onChangeEmail: () -> Void
    let onContinue: () -> Void

    @State private var secondsRemaining: Int = 27
    @State private var countdownTask: Task<Void, Never>?

    private var canResend: Bool { secondsRemaining <= 0 }

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.white.ignoresSafeArea()

                VStack(spacing: 18) {
                    envelopeIcon

                    Text("Check your email")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.black)

                    Text("We've sent a secure sign-in link to **\(email)**. Open it on this device to continue.")
                        .font(.system(size: 15, weight: .regular))
                        .multilineTextAlignment(.center)
                        .foregroundColor(Color(UIColor.systemGray2))
                        .lineSpacing(3)
                        .padding(.horizontal, 18)

                    HStack(spacing: 20) {
                        Button(action: onChangeEmail) {
                            Text("Change email")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.primary)
                        }
                        .buttonStyle(.plain)

                        Button {
                            startCountdown()
                        } label: {
                            Text(canResend ? "Resend email" : "Resend in \(secondsRemaining)s")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(canResend ? Color.rbLinkBlue : Color(UIColor.systemGray3))
                        }
                        .buttonStyle(.plain)
                        .disabled(!canResend)
                    }
                    .padding(.top, 4)

                    Spacer(minLength: 28)

                    VStack(spacing: 10) {
                        Text("DEMO")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.secondary)
                            .tracking(1.2)

                        PillButton(title: "I opened the link on this device") {
                            onContinue()
                        }
                    }
                }
                .frame(maxWidth: 330)
                .padding(.horizontal, 24)
                .padding(.bottom, max(proxy.safeAreaInsets.bottom, 14) + 4)
            }
        }
        .onAppear { startCountdown() }
        .onDisappear { countdownTask?.cancel() }
    }

    private var envelopeIcon: some View {
        Circle()
            .fill(Color(UIColor.systemGray6))
            .frame(width: 78, height: 78)
            .overlay(
                Image(systemName: "envelope.badge.fill")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundColor(Color.rbInk)
            )
    }

    private func startCountdown() {
        countdownTask?.cancel()
        secondsRemaining = 27
        countdownTask = Task {
            while secondsRemaining > 0 {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if Task.isCancelled { return }
                secondsRemaining -= 1
            }
        }
    }
}
