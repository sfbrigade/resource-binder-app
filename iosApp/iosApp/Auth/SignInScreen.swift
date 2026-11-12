import SwiftUI

struct SignInScreen: View {
    @Binding var emailAddress: String
    /// Valid work email submitted — move to the "check your email" state.
    let onContinue: () -> Void
    /// "Request access" from the account-not-found state.
    let onRequestAccess: () -> Void
    let onCreateAccount: () -> Void

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

            Spacer(minLength: proxyHeight * 0.16)

            primaryActionButton

            secondaryLinks
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

            onContinue()
        } label: {
            Text("Email me a sign-in link")
                .font(.system(size: 17, weight: .regular))
                .foregroundColor(canSubmit ? .white : Color(UIColor.systemGray4))
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(
                    Capsule()
                        .fill(canSubmit ? Color.rbInk : Color(UIColor.systemGray5))
                )
        }
        .buttonStyle(.plain)
        .disabled(!canSubmit)
        .padding(.top, 8)
    }

    private var secondaryLinks: some View {
        VStack(spacing: 14) {
            Button {
                displayState = .accountNotFound
            } label: {
                Text("Need access? Request")
                    .font(.system(size: 17, weight: .regular))
                    .foregroundColor(Color.rbLinkBlue)
            }
            .buttonStyle(.plain)

            Button(action: onCreateAccount) {
                Text("New here? ")
                    .foregroundColor(Color(UIColor.systemGray2))
                + Text("Create an account")
                    .foregroundColor(Color.rbLinkBlue)
            }
            .font(.system(size: 15, weight: .regular))
            .buttonStyle(.plain)
        }
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

                Button(action: onRequestAccess) {
                    Text("Request access")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(
                            Capsule()
                                .fill(Color.rbInk)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: 330)
    }
}

struct AccountNotFoundIcon: View {
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
