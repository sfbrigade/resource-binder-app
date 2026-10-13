import SwiftUI

struct OnboardingPageData {
    let title: String
    let subtitle: String
    let buttonTitle: String
}

struct OnboardingScreen: View {
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
                .padding(.top, max(proxy.safeAreaInsets.top, 12) + 8)

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
                                .fill(Color.rbInk)
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
