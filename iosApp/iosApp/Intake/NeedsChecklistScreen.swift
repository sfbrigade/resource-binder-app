import SwiftUI

/// Shared checklist used two ways: empty, as the Manual flow's first step
/// ("What does the client need today?"), or preselected from voice transcript
/// analysis, as the Voice flow's review step ("Review the needs we identified").
struct NeedsChecklistScreen: View {
    let title: String
    let subtitle: String
    let currentStep: Int
    let totalSteps: Int
    @Binding var selectedIDs: Set<String>
    let onBack: () -> Void
    let onClose: () -> Void
    let onContinue: () -> Void

    private var footerText: String {
        selectedIDs.isEmpty
            ? "Select at least one need to continue"
            : "\(selectedIDs.count) need\(selectedIDs.count == 1 ? "" : "s") selected"
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(currentStep: currentStep, totalSteps: totalSteps, onBack: onBack, onClose: onClose)
                .padding(.bottom, 20)

            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.system(size: 24, weight: .bold))
                Text(subtitle)
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 16)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach(NeedCategory.all) { category in
                        NeedRow(category: category, isSelected: selectedIDs.contains(category.id)) {
                            toggle(category.id)
                        }
                    }
                }
                .padding(.bottom, 12)
            }

            VStack(spacing: 10) {
                Text(footerText)
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                PillButton(title: "Continue", isEnabled: !selectedIDs.isEmpty) {
                    onContinue()
                }
            }
            .padding(.top, 12)
        }
        .padding()
    }

    private func toggle(_ id: String) {
        if selectedIDs.contains(id) {
            selectedIDs.remove(id)
        } else {
            selectedIDs.insert(id)
        }
    }
}

private struct NeedRow: View {
    let category: NeedCategory
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: category.symbol)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(category.tint)
                    .frame(width: 22)

                Text(category.title)
                    .font(.system(size: 16, weight: .regular))
                    .foregroundColor(.primary)

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 20))
                    .foregroundColor(isSelected ? Color.rbInk : Color(UIColor.systemGray4))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isSelected ? Color(UIColor.systemGray5).opacity(0.6) : Color(UIColor.systemGray6))
            )
        }
        .buttonStyle(.plain)
    }
}
