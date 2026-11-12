import SwiftUI

/// The "Add details to improve the matches" / "Review the details we identified" screen —
/// one card per category (Basics always first), summarizing what's filled in with a chevron
/// through to `CategoryDetailEditorScreen`.
struct DetailsReviewScreen: View {
    enum Mode {
        case addDetails
        case review

        var headline: String {
            switch self {
            case .addDetails: return "Add details to improve\nthe matches"
            case .review: return "Review the details we identified"
            }
        }

        var subtitle: String {
            switch self {
            case .addDetails: return "Optional — you can find resources now."
            case .review: return "Add, change, or remove anything before finding resources."
            }
        }
    }

    let mode: Mode
    let categories: [NeedCategory]
    @ObservedObject var profile: ClientProfile
    let currentStep: Int
    let totalSteps: Int
    let onBack: () -> Void
    let onClose: () -> Void
    let onEditCategory: (NeedCategory) -> Void
    let onFindResources: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(currentStep: currentStep, totalSteps: totalSteps, onBack: onBack, onClose: onClose)
                .padding(.bottom, 20)

            VStack(alignment: .leading, spacing: 6) {
                Text(mode.headline)
                    .font(.system(size: 24, weight: .bold))
                Text(mode.subtitle)
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 16)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach(categories) { category in
                        CategorySummaryRow(category: category, summary: profile.detailSummary(for: category)) {
                            onEditCategory(category)
                        }
                    }
                }
                .padding(.bottom, 12)
            }

            PillButton(title: "Find resources") {
                onFindResources()
            }
            .padding(.top, 12)
        }
        .padding()
    }
}

private struct CategorySummaryRow: View {
    let category: NeedCategory
    let summary: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: category.symbol)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(category.tint)
                    .frame(width: 22)
                    .padding(.top, 2)

                VStack(alignment: .leading, spacing: 4) {
                    Text(category.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(summary ?? "No details added")
                        .font(.system(size: 13))
                        .foregroundColor(summary == nil ? .secondary : .primary)

                    if summary == nil {
                        Text(category.detailHint)
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
                    .padding(.top, 2)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
            )
        }
        .buttonStyle(.plain)
    }
}
