import SwiftUI

// MARK: - Color tokens

extension Color {
    /// The near-black used for primary pill buttons and headline text across the mockups.
    static let rbInk = Color(red: 0.12, green: 0.13, blue: 0.15)
    static let rbLinkBlue = Color(red: 0.26, green: 0.40, blue: 1.0)
}

// MARK: - Pill buttons

/// The dark, full-width capsule CTA used for every primary action in the mockups
/// ("Next", "Get started", "Continue", "Find resources", "Finalize care plan"...).
struct PillButton: View {
    let title: String
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(isEnabled ? .white : Color(UIColor.systemGray4))
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    Capsule()
                        .fill(isEnabled ? Color.rbInk : Color(UIColor.systemGray5))
                )
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
    }
}

/// The light-gray capsule used for secondary actions ("Try another email", "Back to editing").
struct SecondaryPillButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(.primary)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(
                    Capsule()
                        .fill(Color(UIColor.systemGray5).opacity(0.6))
                )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Section card

/// A rounded, shadowed white container used to group content blocks throughout the flow.
struct SectionCard<Content: View>: View {
    var cornerRadius: CGFloat = 20
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
                    .shadow(color: Color.black.opacity(0.05), radius: 6, x: 0, y: 2)
            )
    }
}

// MARK: - Flow header (back chevron / progress dots / close)

/// The nav row that tops every multi-step screen in the intake and care-plan flows:
/// a circular back button on the left, step progress dots in the middle, and a close (X) on the right.
struct FlowHeader: View {
    let currentStep: Int
    let totalSteps: Int
    var onBack: (() -> Void)?
    var onClose: (() -> Void)?

    var body: some View {
        HStack {
            if let onBack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.primary)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Color(.systemGray6)))
                }
                .buttonStyle(.plain)
            } else {
                Color.clear.frame(width: 36, height: 36)
            }

            Spacer()

            HStack(spacing: 6) {
                ForEach(0..<max(totalSteps, 1), id: \.self) { index in
                    Capsule()
                        .fill(index == currentStep ? Color.rbInk : Color(UIColor.systemGray4))
                        .frame(width: index == currentStep ? 18 : 6, height: 6)
                }
            }

            Spacer()

            if let onClose {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Color(.systemGray6)))
                }
                .buttonStyle(.plain)
            } else {
                Color.clear.frame(width: 36, height: 36)
            }
        }
    }
}

// MARK: - Chip toggle (multi-select fields)

/// A single toggleable capsule chip, used for Location/Age/Language/Accessibility/Veteran-status
/// style multi-select fields in the details editor.
struct ChipToggle: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(isSelected ? .white : .primary)
                .padding(.horizontal, 14)
                .padding(.vertical, 9)
                .background(
                    Capsule()
                        .fill(isSelected ? Color.rbInk : Color(UIColor.systemGray6))
                )
        }
        .buttonStyle(.plain)
    }
}

/// A wrapping layout of `ChipToggle`s for a single detail field.
struct ChipFlowField: View {
    let label: String
    let options: [String]
    @Binding var selection: Set<String>

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.secondary)

            FlexibleChipLayout(options: options) { option in
                ChipToggle(title: option, isSelected: selection.contains(option)) {
                    if selection.contains(option) {
                        selection.remove(option)
                    } else {
                        selection.insert(option)
                    }
                }
            }
        }
    }
}

/// Simple wrapping flow layout for chips (Apple's `Layout` protocol).
struct FlexibleChipLayout<Data: RandomAccessCollection, ItemView: View>: View where Data.Element: Hashable {
    let options: Data
    let itemBuilder: (Data.Element) -> ItemView

    init(options: Data, @ViewBuilder itemBuilder: @escaping (Data.Element) -> ItemView) {
        self.options = options
        self.itemBuilder = itemBuilder
    }

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(Array(options), id: \.self) { option in
                itemBuilder(option)
            }
        }
    }
}

/// Minimal left-to-right wrapping flow layout.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var currentRowWidth: CGFloat = 0
        var currentRowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0
        var totalWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentRowWidth + size.width > maxWidth, currentRowWidth > 0 {
                totalHeight += currentRowHeight + spacing
                totalWidth = max(totalWidth, currentRowWidth)
                currentRowWidth = 0
                currentRowHeight = 0
            }
            currentRowWidth += size.width + (currentRowWidth > 0 ? spacing : 0)
            currentRowHeight = max(currentRowHeight, size.height)
        }
        totalHeight += currentRowHeight
        totalWidth = max(totalWidth, currentRowWidth)

        return CGSize(width: maxWidth.isFinite ? maxWidth : totalWidth, height: totalHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var origin = bounds.origin
        var currentRowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if origin.x + size.width > bounds.maxX, origin.x > bounds.minX {
                origin.x = bounds.minX
                origin.y += currentRowHeight + spacing
                currentRowHeight = 0
            }
            subview.place(at: origin, anchor: .topLeading, proposal: ProposedViewSize(size))
            origin.x += size.width + spacing
            currentRowHeight = max(currentRowHeight, size.height)
        }
    }
}

// MARK: - Empty state card

/// Used in the Care Plan screens where a category has no matched resources yet.
struct EmptyStateCard: View {
    var symbol: String = "tray"
    let message: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(.secondary)
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(UIColor.systemGray6))
        )
    }
}

// MARK: - Loading spinner screen

/// Full-screen centered spinner used for the brief transitional states
/// ("Sending secure link...", "Signing you in...", "Finding resources...").
struct LoadingTransitionScreen: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .controlSize(.large)
            Text(message)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white)
    }
}

/// A `LoadingTransitionScreen` that advances on its own after a short delay — used for the
/// "Signing you in..." and "Finding resources..." transitional states, which have no real
/// backend request to wait on.
struct AutoAdvanceLoadingScreen: View {
    let message: String
    var delay: Double = 1.2
    let onComplete: () -> Void

    var body: some View {
        LoadingTransitionScreen(message: message)
            .task {
                try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                onComplete()
            }
    }
}
