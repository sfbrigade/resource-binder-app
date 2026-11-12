import SwiftUI
import LocalAuthentication

/// New screen — no mockup existed for Face ID (the export only captured an empty frame).
/// Offers a real biometric opt-in via LocalAuthentication, with a graceful fallback
/// when biometrics aren't available (e.g. simulator with no enrolled face).
struct FaceIDScreen: View {
    let onEnabled: () -> Void
    let onSkip: () -> Void

    @State private var isAuthenticating = false
    @State private var errorMessage: String?

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 18) {
                Spacer()

                Circle()
                    .fill(Color(UIColor.systemGray6))
                    .frame(width: 96, height: 96)
                    .overlay(
                        Image(systemName: "faceid")
                            .font(.system(size: 40, weight: .medium))
                            .foregroundColor(Color.rbInk)
                    )

                Text("Unlock faster next time")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.black)

                Text("Use Face ID to sign back in instantly instead of waiting on an email link.")
                    .font(.system(size: 15, weight: .regular))
                    .foregroundColor(Color(UIColor.systemGray2))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .padding(.horizontal, 18)

                if let errorMessage {
                    Text(errorMessage)
                        .font(.system(size: 13, weight: .regular))
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 18)
                }

                Spacer()

                VStack(spacing: 12) {
                    PillButton(title: isAuthenticating ? "Verifying..." : "Enable Face ID", isEnabled: !isAuthenticating) {
                        authenticate()
                    }
                    SecondaryPillButton(title: "Not now") {
                        onSkip()
                    }
                }
            }
            .frame(maxWidth: 330)
            .padding(.horizontal, 24)
            .padding(.bottom, max(proxy.safeAreaInsets.bottom, 14) + 4)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
        }
    }

    private func authenticate() {
        let context = LAContext()
        var evaluationError: NSError?

        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &evaluationError) else {
            // Biometrics unavailable (e.g. simulator with no enrolled face) — don't block the demo.
            onEnabled()
            return
        }

        isAuthenticating = true
        errorMessage = nil

        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Unlock Resource_Binder") { success, error in
            DispatchQueue.main.async {
                isAuthenticating = false
                if success {
                    onEnabled()
                } else {
                    errorMessage = error?.localizedDescription ?? "Face ID failed. Try again or skip for now."
                }
            }
        }
    }
}
