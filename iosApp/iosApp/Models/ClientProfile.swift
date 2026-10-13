import Foundation

/// The single source of truth threaded through the whole intake → review → care plan flow:
/// which needs were selected, what details were filled in for each, and the resulting draft plan.
final class ClientProfile: ObservableObject {
    @Published var selectedNeedIDs: Set<String> = []

    /// Keyed by "<categoryID>|<fieldLabel>" -> the selected chip options for that field.
    @Published var chipAnswers: [String: Set<String>] = [:]
    /// Keyed the same way, for free-text fields.
    @Published var textAnswers: [String: String] = [:]

    /// The raw transcript/typed description that seeded this intake, kept for the care plan summary.
    @Published var situationText: String = ""

    @Published var nextSteps: [CarePlanStep] = []
    @Published var notes: String = ""
    @Published var emergencyContacts: [EmergencyContact] = []

    var selectedNeeds: [NeedCategory] {
        NeedCategory.all.filter { selectedNeedIDs.contains($0.id) }
    }

    /// Basics first, then every selected need — the exact ordering used by the details review screen.
    var reviewCategories: [NeedCategory] {
        [.basics] + selectedNeeds
    }

    private func key(_ category: NeedCategory, _ field: DetailField) -> String {
        "\(category.id)|\(field.label)"
    }

    func chips(for category: NeedCategory, field: DetailField) -> Set<String> {
        chipAnswers[key(category, field)] ?? []
    }

    func setChips(_ chips: Set<String>, for category: NeedCategory, field: DetailField) {
        chipAnswers[key(category, field)] = chips
    }

    func text(for category: NeedCategory, field: DetailField) -> String {
        textAnswers[key(category, field)] ?? ""
    }

    func setText(_ text: String, for category: NeedCategory, field: DetailField) {
        textAnswers[key(category, field)] = text
    }

    /// Short "X, Y · Z" style summary of what's been filled in for a category, or nil if nothing yet.
    func detailSummary(for category: NeedCategory) -> String? {
        var parts: [String] = []
        for field in category.detailFields {
            switch field {
            case .chipGroup:
                let selected = chips(for: category, field: field)
                if !selected.isEmpty {
                    parts.append(selected.sorted().joined(separator: ", "))
                }
            case .freeText:
                let value = text(for: category, field: field).trimmingCharacters(in: .whitespacesAndNewlines)
                if !value.isEmpty {
                    parts.append(value)
                }
            }
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    func hasAnyDetails(for category: NeedCategory) -> Bool {
        detailSummary(for: category) != nil
    }

    /// Populates "Recommended next steps" from the selected needs, if it hasn't been seeded yet.
    func seedNextStepsIfNeeded() {
        guard nextSteps.isEmpty else { return }
        nextSteps = selectedNeeds.map { category in
            CarePlanStep(text: "Contact a \(category.title) provider to confirm availability and next steps.")
        }
    }

    func resetIntake() {
        selectedNeedIDs = []
        chipAnswers = [:]
        textAnswers = [:]
        situationText = ""
        nextSteps = []
        notes = ""
        emergencyContacts = []
    }
}
