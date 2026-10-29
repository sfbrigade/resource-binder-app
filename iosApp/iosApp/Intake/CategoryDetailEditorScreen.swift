import SwiftUI

/// Generic, config-driven detail form for a single category — chip-group and/or
/// free-text fields, defined by `NeedCategory.detailFields`.
struct CategoryDetailEditorScreen: View {
    let category: NeedCategory
    @ObservedObject var profile: ClientProfile
    let onBack: () -> Void
    let onSave: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(currentStep: 0, totalSteps: 1, onBack: onBack, onClose: nil)
                .padding(.bottom, 20)

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 6) {
                    Image(systemName: category.symbol)
                        .foregroundColor(category.tint)
                    Text(category.title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(category.tint)
                }

                Text("Add anything you know to improve the matches.")
                    .font(.system(size: 22, weight: .bold))
                Text("All details are optional.")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 20)

            ScrollView {
                VStack(alignment: .leading, spacing: 26) {
                    ForEach(category.detailFields) { field in
                        fieldView(field)
                    }
                }
                .padding(.bottom, 12)
            }

            PillButton(title: "Save changes") {
                onSave()
            }
            .padding(.top, 12)
        }
        .padding()
    }

    @ViewBuilder
    private func fieldView(_ field: DetailField) -> some View {
        switch field {
        case .chipGroup(let label, let options):
            ChipFlowField(label: label, options: options, selection: chipBinding(field))
        case .freeText(let label, let placeholder):
            VStack(alignment: .leading, spacing: 10) {
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
                TextField(placeholder, text: textBinding(field), axis: .vertical)
                    .lineLimit(3...6)
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(Color(.systemBackground))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Color(UIColor.systemGray5), lineWidth: 1)
                    )
            }
        }
    }

    private func chipBinding(_ field: DetailField) -> Binding<Set<String>> {
        Binding(
            get: { profile.chips(for: category, field: field) },
            set: { profile.setChips($0, for: category, field: field) }
        )
    }

    private func textBinding(_ field: DetailField) -> Binding<String> {
        Binding(
            get: { profile.text(for: category, field: field) },
            set: { profile.setText($0, for: category, field: field) }
        )
    }
}
