import SwiftUI

/// A single detail field shown in the per-category detail editor —
/// either a multi-select chip group or a free-text note.
enum DetailField: Identifiable {
    case chipGroup(label: String, options: [String])
    case freeText(label: String, placeholder: String)

    var id: String { "field-\(label)" }

    var label: String {
        switch self {
        case .chipGroup(let label, _): return label
        case .freeText(let label, _): return label
        }
    }
}

/// One of the selectable need categories (or the always-present "Basics" category),
/// matching the 14 categories from the Manual flow checklist mockup.
struct NeedCategory: Identifiable {
    let id: String
    let title: String
    let symbol: String
    let tint: Color
    /// The "Add: X · Y · Z" hint text shown on the summary card before any details are filled in.
    let detailHint: String
    let detailFields: [DetailField]
}

extension NeedCategory: Equatable, Hashable {
    static func == (lhs: NeedCategory, rhs: NeedCategory) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

extension NeedCategory {
    /// Always shown first in the details review, never part of the selectable checklist.
    static let basics = NeedCategory(
        id: "basics",
        title: "Basics",
        symbol: "person.text.rectangle.fill",
        tint: .gray,
        detailHint: "Add: Location · Age · Language · +2 more",
        detailFields: [
            .chipGroup(label: "Location", options: [
                "San Francisco", "Alameda (Oakland)", "Contra Costa", "San Mateo",
                "Solano", "Marin", "Santa Clara", "Sonoma", "Outside these areas", "Unknown"
            ]),
            .chipGroup(label: "Age", options: ["Under 18", "18–24", "25–54", "55–64", "65+", "Unknown"]),
            .chipGroup(label: "Language", options: [
                "English", "Spanish", "Cantonese", "Mandarin", "Portuguese", "Other language", "Unknown"
            ]),
            .chipGroup(label: "Accessibility", options: [
                "Wheelchair access", "Mobility assistance", "ASL interpretation",
                "Visual assistance", "Other accessibility need", "Unknown"
            ]),
            .chipGroup(label: "Veteran status", options: [
                "Veteran", "Active duty", "National Guard / Reserve",
                "Military family member", "Not a veteran", "Unknown"
            ])
        ]
    )

    private static func genericFields(urgencyLabel: String = "Urgency") -> [DetailField] {
        [
            .chipGroup(label: urgencyLabel, options: ["Routine", "Needs support soon", "Urgent"]),
            .freeText(label: "Notes", placeholder: "Anything else that could help")
        ]
    }

    /// The 14 selectable need categories, in the same order as the Manual flow checklist mockup.
    static let all: [NeedCategory] = [
        NeedCategory(
            id: "shelter", title: "Shelter", symbol: "house.fill", tint: .orange,
            detailHint: "Add: Housing status · Sleeping location · Pets",
            detailFields: [
                .chipGroup(label: "Housing status", options: [
                    "Unsheltered", "Sheltered", "Doubled up", "At risk of losing housing", "Unknown"
                ]),
                .chipGroup(label: "Sleeping location", options: ["Street", "Vehicle", "Encampment", "Shelter", "Other"]),
                .chipGroup(label: "Pets", options: ["Has pets", "No pets", "Unknown"])
            ]
        ),
        NeedCategory(
            id: "foodSupplies", title: "Food & Supplies", symbol: "bag.fill", tint: .green,
            detailHint: "Add: Food needs · Dietary needs · Pickup or delivery",
            detailFields: [
                .chipGroup(label: "Food needs", options: [
                    "Meals", "Groceries", "Baby formula & diapers", "Clothing", "Hygiene supplies"
                ]),
                .chipGroup(label: "Dietary needs", options: ["Vegetarian", "Vegan", "Halal", "Kosher", "Allergies", "None"]),
                .chipGroup(label: "Pickup or delivery", options: ["Pickup", "Delivery", "Either"])
            ]
        ),
        NeedCategory(
            id: "medicalDental", title: "Medical & Dental", symbol: "cross.case.fill", tint: .red,
            detailHint: "Add: Insurance · Urgency · Condition",
            detailFields: [
                .chipGroup(label: "Insurance", options: ["Has insurance", "Uninsured", "Medi-Cal", "Medicare", "Unknown"]),
                .chipGroup(label: "Urgency", options: ["Routine", "Needs care soon", "Urgent"]),
                .freeText(label: "Condition or notes", placeholder: "Anything else about the medical or dental need")
            ]
        ),
        NeedCategory(
            id: "mentalHealth", title: "Mental Health", symbol: "brain.head.profile", tint: .purple,
            detailHint: "Add: Support needed · Urgency · Insurance",
            detailFields: [
                .chipGroup(label: "Support needed", options: ["Counseling", "Therapy", "Psychiatric care", "Peer support", "Unknown"]),
                .chipGroup(label: "Urgency", options: ["Routine", "Needs support soon", "Crisis"]),
                .chipGroup(label: "Insurance", options: ["Has insurance", "Uninsured", "Medi-Cal", "Medicare", "Unknown"])
            ]
        ),
        NeedCategory(
            id: "substanceUseSupport", title: "Substance Use Support", symbol: "pills.fill", tint: .teal,
            detailHint: "Add: Support needed · Treatment preference · Insurance",
            detailFields: [
                .chipGroup(label: "Support needed", options: ["Detox", "Outpatient treatment", "Residential treatment", "Harm reduction", "Unknown"]),
                .chipGroup(label: "Treatment preference", options: ["Open to any", "Abstinence-based", "Medication-assisted", "Unknown"]),
                .chipGroup(label: "Insurance", options: ["Has insurance", "Uninsured", "Medi-Cal", "Medicare", "Unknown"])
            ]
        ),
        NeedCategory(
            id: "crisisSupport", title: "Crisis Support", symbol: "exclamationmark.triangle.fill", tint: .red,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "documentsID", title: "Documents & ID", symbol: "doc.text.fill", tint: .blue,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "benefitsIncome", title: "Benefits & Income", symbol: "banknote.fill", tint: .indigo,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "transportation", title: "Transportation", symbol: "bus.fill", tint: .cyan,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "veteranSupport", title: "Veteran Support", symbol: "shield.lefthalf.filled", tint: .brown,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "olderAdultsDisabilitySupport", title: "Older Adults & Disability Support", symbol: "figure.roll", tint: .yellow,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "legalAid", title: "Legal Aid", symbol: "building.columns.fill", tint: Color(UIColor.systemGray),
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "domesticViolenceSupport", title: "Domestic Violence Support", symbol: "hand.raised.fill", tint: .pink,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        ),
        NeedCategory(
            id: "youthServices", title: "Youth Services", symbol: "figure.child", tint: .mint,
            detailHint: "Add: Urgency · Notes",
            detailFields: genericFields()
        )
    ]
}
