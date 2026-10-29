import SwiftUI

/// The "Default Review" mockup screen — draft care plan before finalizing. Resources render
/// as an honest empty state per category (no backend/mock data), while next steps, notes,
/// and provider contacts are fully editable client-side state.
struct CarePlanDraftScreen: View {
    @ObservedObject var profile: ClientProfile
    let onBack: () -> Void
    let onClose: () -> Void
    let onFinalize: () -> Void

    @State private var isEditingNotes: Bool = false
    @State private var isAddingStep: Bool = false
    @State private var newStepText: String = ""

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(currentStep: 2, totalSteps: 3, onBack: onBack, onClose: onClose)
                .padding(.bottom, 16)

            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Take a moment to review the generated care plan.")
                            .font(.system(size: 24, weight: .bold))
                        Text("You can edit any section before sharing it.")
                            .font(.system(size: 15))
                            .foregroundColor(.secondary)
                    }

                    summarySection
                    resourcesSection
                    nextStepsSection
                    notesSection
                    contactsSection
                }
                .padding(.bottom, 16)
            }

            PillButton(title: "Finalize care plan") {
                profile.seedNextStepsIfNeeded()
                onFinalize()
            }
            .padding(.top, 12)
        }
        .padding()
        .onAppear { isEditingNotes = !profile.notes.isEmpty }
    }

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text("Summary")
                    .font(.system(size: 17, weight: .semibold))
                Text("Shared with client")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Color.rbLinkBlue)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(Color.rbLinkBlue.opacity(0.12)))
            }

            Text(summaryText)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
        }
    }

    private var summaryText: String {
        guard !profile.selectedNeeds.isEmpty else {
            return "This care plan will include resources based on the needs you select."
        }
        let names = profile.selectedNeeds.map(\.title).joined(separator: ", ")
        return "This care plan includes places that may help today with \(names). Some locations may require calling before arrival."
    }

    private var resourcesSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Resources")
                .font(.system(size: 17, weight: .semibold))

            if profile.selectedNeeds.isEmpty {
                EmptyStateCard(symbol: "tray", message: "No needs selected yet.")
            } else {
                ForEach(profile.selectedNeeds) { category in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 6) {
                            Image(systemName: category.symbol)
                                .foregroundColor(category.tint)
                            Text(category.title)
                                .font(.system(size: 14, weight: .semibold))
                        }
                        EmptyStateCard(symbol: "magnifyingglass", message: "No resources matched yet for \(category.title).")
                    }
                }
            }
        }
    }

    private var nextStepsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Recommended next steps")
                    .font(.system(size: 17, weight: .semibold))
                Text("These steps were generated from the selected needs. Reorder or edit them if needed.")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            ForEach(Array(profile.nextSteps.enumerated()), id: \.element.id) { index, step in
                NextStepRow(
                    text: step.text,
                    canMoveUp: index > 0,
                    canMoveDown: index < profile.nextSteps.count - 1,
                    onMoveUp: { moveStep(from: index, to: index - 1) },
                    onMoveDown: { moveStep(from: index, to: index + 1) },
                    onRemove: { profile.nextSteps.remove(at: index) }
                )
            }

            if isAddingStep {
                HStack {
                    TextField("New step", text: $newStepText)
                        .padding(10)
                        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color(.systemBackground)))
                        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Color(UIColor.systemGray5), lineWidth: 1))
                    Button("Add") {
                        let trimmed = newStepText.trimmingCharacters(in: .whitespacesAndNewlines)
                        if !trimmed.isEmpty {
                            profile.nextSteps.append(CarePlanStep(text: trimmed))
                        }
                        newStepText = ""
                        isAddingStep = false
                    }
                    .font(.system(size: 14, weight: .semibold))
                }
            } else {
                Button {
                    isAddingStep = true
                } label: {
                    Label("Add step", systemImage: "plus")
                        .font(.system(size: 14, weight: .medium))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func moveStep(from: Int, to: Int) {
        profile.nextSteps.swapAt(from, to)
    }

    private var notesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Notes")
                .font(.system(size: 17, weight: .semibold))

            if isEditingNotes {
                TextField("Add a note", text: $profile.notes, axis: .vertical)
                    .lineLimit(2...5)
                    .padding(10)
                    .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color(.systemBackground)))
                    .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Color(UIColor.systemGray5), lineWidth: 1))
            } else {
                Button {
                    isEditingNotes = true
                } label: {
                    Label("Add note", systemImage: "plus")
                        .font(.system(size: 14, weight: .medium))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var contactsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Provider contacts")
                .font(.system(size: 17, weight: .semibold))

            ForEach($profile.emergencyContacts) { $contact in
                HStack(spacing: 8) {
                    TextField("Name", text: $contact.name)
                        .padding(10)
                        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color(.systemBackground)))
                        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Color(UIColor.systemGray5), lineWidth: 1))
                    TextField("Phone", text: $contact.phone)
                        .keyboardType(.phonePad)
                        .padding(10)
                        .frame(width: 130)
                        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color(.systemBackground)))
                        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Color(UIColor.systemGray5), lineWidth: 1))
                    Button {
                        profile.emergencyContacts.removeAll { $0.id == contact.id }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }

            Button {
                profile.emergencyContacts.append(EmergencyContact(name: "", phone: ""))
            } label: {
                Label("Add contact", systemImage: "plus")
                    .font(.system(size: 14, weight: .medium))
            }
            .buttonStyle(.plain)
        }
    }
}

private struct NextStepRow: View {
    let text: String
    let canMoveUp: Bool
    let canMoveDown: Bool
    let onMoveUp: () -> Void
    let onMoveDown: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(spacing: 2) {
                Button(action: onMoveUp) {
                    Image(systemName: "chevron.up")
                        .font(.system(size: 11, weight: .bold))
                }
                .buttonStyle(.plain)
                .disabled(!canMoveUp)
                .opacity(canMoveUp ? 1 : 0.25)

                Button(action: onMoveDown) {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 11, weight: .bold))
                }
                .buttonStyle(.plain)
                .disabled(!canMoveDown)
                .opacity(canMoveDown ? 1 : 0.25)
            }
            .foregroundColor(.secondary)
            .padding(.top, 3)

            Text(text)
                .font(.system(size: 14))
                .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.secondary)
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(UIColor.systemGray6))
        )
    }
}
