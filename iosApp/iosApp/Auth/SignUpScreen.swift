import SwiftUI

/// New screen — no mockup existed for Sign Up (the export only captured an empty frame),
/// so this mirrors SignInScreen's visual language: white background, centered title block,
/// bordered fields, dark capsule primary action.
struct SignUpScreen: View {
    let onBack: () -> Void
    let onDone: () -> Void

    @State private var fullName: String = ""
    @State private var emailAddress: String = ""
    @State private var organization: String = ""
    @State private var didSubmit: Bool = false

    private var trimmedEmail: String {
        emailAddress.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isValidEmailFormat: Bool {
        let parts = trimmedEmail.split(separator: "@")
        guard parts.count == 2 else { return false }
        return parts[1].contains(".") && !parts[0].isEmpty && !parts[1].isEmpty
    }

    private var canSubmit: Bool {
        !fullName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && isValidEmailFormat
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                Color.white.ignoresSafeArea()

                VStack {
                    FlowHeader(currentStep: 0, totalSteps: 1, onBack: didSubmit ? nil : onBack, onClose: nil)
                        .padding(.horizontal, 24)
                        .padding(.top, max(proxy.safeAreaInsets.top, 12))

                    Spacer(minLength: 24)

                    if didSubmit {
                        confirmationContent
                    } else {
                        formContent
                    }

                    Spacer(minLength: 0)
                }
                .padding(.bottom, max(proxy.safeAreaInsets.bottom, 14) + 4)
            }
            .animation(.easeInOut(duration: 0.28), value: didSubmit)
        }
    }

    private var formContent: some View {
        VStack(spacing: 0) {
            VStack(spacing: 16) {
                Text("Create your account")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.black)

                Text("Request access with your work email. We'll notify you once you're approved.")
                    .font(.system(size: 15, weight: .regular))
                    .foregroundColor(Color(UIColor.systemGray2))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .padding(.horizontal, 8)
            }
            .frame(maxWidth: 330)
            .padding(.horizontal, 24)

            Spacer(minLength: 28)

            VStack(alignment: .leading, spacing: 18) {
                labeledField(title: "Full Name", required: true, text: $fullName, placeholder: "Your name", keyboard: .default)
                labeledField(title: "Work Email", required: true, text: $emailAddress, placeholder: "you@organization.org", keyboard: .emailAddress)
                labeledField(title: "Organization", required: false, text: $organization, placeholder: "Where you work", keyboard: .default)
            }
            .padding(.horizontal, 24)

            Spacer(minLength: 32)

            VStack(spacing: 14) {
                PillButton(title: "Create account", isEnabled: canSubmit) {
                    didSubmit = true
                }

                Button {
                    onBack()
                } label: {
                    Text("Already have an account? ")
                        .foregroundColor(Color(UIColor.systemGray2))
                    + Text("Sign in")
                        .foregroundColor(Color.rbLinkBlue)
                }
                .font(.system(size: 15, weight: .regular))
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 24)
        }
    }

    private func labeledField(title: String, required: Bool, text: Binding<String>, placeholder: String, keyboard: UIKeyboardType) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 0) {
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.black)
                if required {
                    Text("*")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.red)
                }
            }

            TextField(placeholder, text: text)
                .textInputAutocapitalization(keyboard == .emailAddress ? .never : .words)
                .autocorrectionDisabled(keyboard == .emailAddress)
                .keyboardType(keyboard)
                .padding(.horizontal, 12)
                .frame(height: 40)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Color(UIColor.systemGray5), lineWidth: 1)
                )
        }
    }

    private var confirmationContent: some View {
        VStack(spacing: 18) {
            AccountNotFoundIcon()

            Text("Request sent")
                .font(.system(size: 24, weight: .bold))
                .multilineTextAlignment(.center)
                .foregroundColor(.black)

            Text("We'll email \(trimmedEmail.isEmpty ? "you" : trimmedEmail) once your account is approved.")
                .font(.system(size: 15, weight: .regular))
                .multilineTextAlignment(.center)
                .foregroundColor(Color(UIColor.systemGray2))
                .lineSpacing(3)
                .padding(.horizontal, 18)

            Spacer(minLength: 28)

            PillButton(title: "Back to sign in") {
                onDone()
            }
            .padding(.horizontal, 24)
        }
        .frame(maxWidth: 330)
    }
}
