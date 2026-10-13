import SwiftUI

/// A category whose keywords were detected in the transcript, with the matched terms —
/// shown in the "Keyword Categories" breakdown on the describe/review screens.
struct KeywordCategoryBucket: Identifiable {
    let id = UUID()
    let title: String
    let symbol: String
    let tint: Color
    let terms: [String]
}

struct TranscriptKeywordAnalysis {
    let highlightedText: AttributedString
    /// NeedCategory ids detected in the transcript — used to preselect the needs checklist.
    let detectedNeedIDs: Set<String>
    let keywordCategories: [KeywordCategoryBucket]

    static let empty = TranscriptKeywordAnalysis(highlightedText: AttributedString(""), detectedNeedIDs: [], keywordCategories: [])
}

/// Lightweight local keyword matcher that stands in for the "AI-matched" need detection
/// described in the product pitch. Keyed directly to `NeedCategory.all` ids so a detected
/// need maps 1:1 onto a checklist item in `NeedsChecklistScreen`.
enum TranscriptKeywordAnalyzer {
    private struct CategoryKeywords {
        let categoryID: String
        let terms: [String]
    }

    private struct Match {
        let categoryID: String
        let term: String
        let range: Range<String.Index>
    }

    private static let keywordsByCategory: [CategoryKeywords] = [
        .init(categoryID: "shelter", terms: [
            "shelter", "housing", "homeless", "unhoused", "eviction", "evicted", "no place to stay", "sleeping outside"
        ]),
        .init(categoryID: "foodSupplies", terms: [
            "food", "hungry", "meals", "groceries", "diapers", "clothing", "hygiene"
        ]),
        .init(categoryID: "medicalDental", terms: [
            "medical", "dental", "doctor", "clinic", "medication", "medications", "prescription", "injury", "illness", "sick"
        ]),
        .init(categoryID: "mentalHealth", terms: [
            "anxiety", "anxious", "overwhelmed", "stress", "stressed", "depressed", "depression", "mental health", "therapy", "counseling"
        ]),
        .init(categoryID: "substanceUseSupport", terms: [
            "substance", "drug", "drugs", "alcohol", "addiction", "overdose", "sober", "relapse"
        ]),
        .init(categoryID: "crisisSupport", terms: [
            "crisis", "suicidal", "suicide", "self-harm", "unsafe", "abuse", "violence", "emergency"
        ]),
        .init(categoryID: "documentsID", terms: [
            "identification", "documents", "birth certificate", "social security card"
        ]),
        .init(categoryID: "benefitsIncome", terms: [
            "benefits", "snap", "ebt", "welfare", "income", "unemployment", "disability benefits", "calfresh"
        ]),
        .init(categoryID: "transportation", terms: [
            "transportation", "bus pass", "ride", "transit", "clipper card"
        ]),
        .init(categoryID: "veteranSupport", terms: [
            "veteran", "military", "service member", "va benefits", "deployed"
        ]),
        .init(categoryID: "olderAdultsDisabilitySupport", terms: [
            "elderly", "senior", "disability", "disabled", "wheelchair"
        ]),
        .init(categoryID: "legalAid", terms: [
            "legal", "lawyer", "attorney", "court", "eviction notice", "custody"
        ]),
        .init(categoryID: "domesticViolenceSupport", terms: [
            "domestic violence", "abusive partner", "restraining order"
        ]),
        .init(categoryID: "youthServices", terms: [
            "youth", "teen", "teenager", "minor", "foster care", "runaway"
        ])
    ]

    static func analyze(_ text: String) -> TranscriptKeywordAnalysis {
        let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedText.isEmpty else { return .empty }

        let matches = findMatches(in: trimmedText)
        let highlightedText = buildHighlightedText(from: trimmedText, matches: matches)

        let groupedMatches = Dictionary(grouping: matches, by: { $0.categoryID })
        let keywordCategories: [KeywordCategoryBucket] = NeedCategory.all.compactMap { category in
            guard let categoryMatches = groupedMatches[category.id] else { return nil }
            let matchedTerms = categoryMatches.map { $0.term }.uniquedPreservingOrder()
            return KeywordCategoryBucket(title: category.title, symbol: category.symbol, tint: category.tint, terms: matchedTerms)
        }

        return TranscriptKeywordAnalysis(
            highlightedText: highlightedText,
            detectedNeedIDs: Set(groupedMatches.keys),
            keywordCategories: keywordCategories
        )
    }

    private static func findMatches(in text: String) -> [Match] {
        var matches: [Match] = []
        let sortedCategories = keywordsByCategory.sorted { lhs, rhs in
            lhs.terms.joined(separator: " ").count > rhs.terms.joined(separator: " ").count
        }

        for category in sortedCategories {
            for term in category.terms.sorted(by: { $0.count > $1.count }) {
                for range in text.ranges(of: term) {
                    let overlaps = matches.contains { existing in existing.range.overlaps(range) }
                    guard !overlaps else { continue }
                    matches.append(Match(categoryID: category.categoryID, term: term, range: range))
                }
            }
        }

        return matches.sorted { $0.range.lowerBound < $1.range.lowerBound }
    }

    private static func buildHighlightedText(from text: String, matches: [Match]) -> AttributedString {
        var attributed = AttributedString(text)
        guard !matches.isEmpty else { return attributed }

        for match in matches {
            guard let lower = AttributedString.Index(match.range.lowerBound, within: attributed),
                  let upper = AttributedString.Index(match.range.upperBound, within: attributed),
                  let tint = NeedCategory.all.first(where: { $0.id == match.categoryID })?.tint else {
                continue
            }

            let range = lower..<upper
            attributed[range].backgroundColor = tint.opacity(0.22)
            attributed[range].foregroundColor = tint
            attributed[range].font = .body.bold()
        }

        return attributed
    }
}

private extension String {
    func ranges(of searchText: String) -> [Range<String.Index>] {
        guard !searchText.isEmpty else { return [] }

        var results: [Range<String.Index>] = []
        var searchStartIndex = startIndex

        while searchStartIndex < endIndex,
              let range = range(of: searchText, options: [.caseInsensitive, .diacriticInsensitive], range: searchStartIndex..<endIndex) {
            if isWordBoundaryMatch(range: range) {
                results.append(range)
            }
            searchStartIndex = range.upperBound
        }

        return results
    }

    private func isWordBoundaryMatch(range: Range<String.Index>) -> Bool {
        let before = range.lowerBound > startIndex ? self[index(before: range.lowerBound)] : nil
        let after = range.upperBound < endIndex ? self[range.upperBound] : nil

        let beforeIsBoundary = before.map { !$0.isLetter && !$0.isNumber } ?? true
        let afterIsBoundary = after.map { !$0.isLetter && !$0.isNumber } ?? true

        return beforeIsBoundary && afterIsBoundary
    }
}

private extension Array where Element == String {
    func uniquedPreservingOrder() -> [String] {
        var seen = Set<String>()
        var result: [String] = []

        for value in self {
            let lowered = value.lowercased()
            guard !seen.contains(lowered) else { continue }
            seen.insert(lowered)
            result.append(value)
        }

        return result
    }
}
