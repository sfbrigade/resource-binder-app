import Foundation

/// A single actionable item in the care plan's "Recommended next steps" list.
/// Reorderable, removable, and user-addable — pure client-side state, no backend.
struct CarePlanStep: Identifiable, Equatable {
    let id = UUID()
    var text: String
}

struct EmergencyContact: Identifiable, Equatable {
    let id = UUID()
    var name: String
    var phone: String
}
