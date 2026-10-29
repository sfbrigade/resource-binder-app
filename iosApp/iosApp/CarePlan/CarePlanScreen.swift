import SwiftUI

/// The final "Care plan #00001" screen — read view of the finalized plan with
/// Back to editing / Share care plan actions.
struct CarePlanScreen: View {
    @ObservedObject var profile: ClientProfile
    let planNumber: String
    let preparedByName: String
    let organizationName: String
    let onBackToEditing: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                circleButton(systemName: "chevron.left", action: onBackToEditing)
                Spacer()
                circleButton(systemName: "xmark", action: onClose)
            }
            .padding(.bottom, 12)

            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Care plan #\(planNumber)")
                            .font(.system(size: 22, weight: .bold))
                        Text("Prepared today by \(preparedByName)")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                        Text(organizationName)
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                    }

                    Text(introText)
                        .font(.system(size: 14))
                        .foregroundColor(.primary)

                    todaysPlanSection
                    resourcesSection
                    notesSection
                    contactsSection
                }
                .padding(.bottom, 16)
            }

            VStack(spacing: 10) {
                SecondaryPillButton(title: "Back to editing", action: onBackToEditing)
                ShareLink(item: shareText) {
                    Text("Share care plan")
                        .font(.system(size: 17, weight: .medium))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Capsule().fill(Color.rbInk))
                }
            }
            .padding(.top, 12)
        }
        .padding()
    }

    private func circleButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.primary)
                .frame(width: 36, height: 36)
                .background(Circle().fill(Color(.systemGray6)))
        }
        .buttonStyle(.plain)
    }

    private var introText: String {
        guard !profile.selectedNeeds.isEmpty else {
            return "We prepared this plan based on the needs you shared today."
        }
        let names = profile.selectedNeeds.map(\.title).joined(separator: ", ")
        return "We prepared this plan based on the needs you shared today. It includes places that may be able to help with \(names). Some locations ask that you call before visiting."
    }

    private var todaysPlanSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Today's plan")
                .font(.system(size: 17, weight: .semibold))

            if profile.nextSteps.isEmpty {
                Text("No steps added yet.")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            } else {
                ForEach(profile.nextSteps) { step in
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.secondary)
                            .padding(.top, 2)
                        Text(step.text)
                            .font(.system(size: 14))
                    }
                }
            }
        }
    }

    private var resourcesSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Resources (0)")
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

    private var notesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Notes")
                .font(.system(size: 17, weight: .semibold))
            Text(profile.notes.isEmpty ? "No notes added." : profile.notes)
                .font(.system(size: 14))
                .foregroundColor(profile.notes.isEmpty ? .secondary : .primary)
        }
    }

    private var contactsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Emergency contacts")
                .font(.system(size: 17, weight: .semibold))

            if profile.emergencyContacts.isEmpty {
                Text("No contacts added.")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            } else {
                ForEach(profile.emergencyContacts) { contact in
                    Text("\(contact.name.isEmpty ? "Unnamed contact" : contact.name) · \(contact.phone.isEmpty ? "No phone" : contact.phone)")
                        .font(.system(size: 14))
                }
            }
        }
    }

    private var shareText: String {
        var lines = ["Care plan #\(planNumber)", "Prepared today by \(preparedByName)", organizationName, ""]
        lines.append(introText)
        lines.append("")
        lines.append("Today's plan:")
        if profile.nextSteps.isEmpty {
            lines.append("- No steps added yet.")
        } else {
            for step in profile.nextSteps {
                lines.append("- \(step.text)")
            }
        }
        if !profile.notes.isEmpty {
            lines.append("")
            lines.append("Notes: \(profile.notes)")
        }
        return lines.joined(separator: "\n")
    }
}
