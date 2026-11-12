package org.sfcivictech.android.shared.resourcebinder.careplan

// The catalog the manual flow ships with. To add a need, a detail or a sheet category,
// edit the tables below; Catalog refuses to build if they disagree with each other.
object CarePlanCatalog {
    val default: Catalog = Catalog(
        needs = listOf(
            Need("shelter", "Shelter"),
            Need("food_supplies", "Food & Supplies"),
            Need("medical_dental", "Medical & Dental"),
            Need("mental_health", "Mental Health"),
            Need("substance_use", "Substance Use"),
            Need("crisis_support", "Crisis Support"),
            Need("documents_id", "Documents / ID"),
            Need("benefits_income", "Benefits & Income"),
            Need("transportation", "Transportation"),
            Need("veterans", "Veterans"),
            Need("seniors_disability", "Seniors & Disability"),
            Need("legal_aid", "Legal Aid"),
            Need("domestic_violence", "Domestic Violence"),
            Need("youth_services", "Youth Services"),
        ),
        // listed by sheet category; the order here is the order of the sheet
        details = listOf(
            Detail("age", "Age", "Demographics", listOf("Under 18", "18-25", "26-54", "55-64", "65+")),
            Detail("gender", "Gender", "Demographics", listOf("Woman", "Man", "Non-binary", "Another gender")),
            Detail(
                "language", "Language", "Demographics",
                listOf("English", "Spanish", "Cantonese", "Mandarin", "Portuguese", "Other language"),
            ),
            Detail(
                "veteran_status", "Veteran Status", "Demographics",
                listOf("Veteran", "Active duty", "National Guard / Reserve", "Military family member", "Not a veteran"),
            ),

            Detail(
                "location_area", "Location / Area", "Location",
                listOf(
                    "San Francisco", "Alameda (Oakland)", "Contra Costa", "Marin",
                    "San Mateo", "Santa Clara", "Solano", "Sonoma",
                ),
            ),
            Detail(
                "sleeping_location", "Sleeping Location", "Location",
                listOf("Car", "Street", "Shelter", "Encampment", "Other"),
            ),
            Detail(
                "transportation", "Transportation", "Location",
                listOf("Needs a ride", "Has a car", "Uses public transit"),
            ),

            Detail(
                "housing_status", "Housing Status", "Housing",
                listOf("Unsheltered", "Sheltered", "At risk of losing housing"),
            ),
            Detail("couples", "Couples", "Housing", listOf("Single", "With partner")),
            Detail("pets", "Pets", "Housing", listOf("No pets", "Dog", "Cat", "Other pet")),
            Detail("wheelchair_accessible", "Wheelchair Accessible", "Housing", listOf("Required", "Not required")),

            Detail("insurance", "Insurance", "Health", listOf("Medi-Cal", "Medicare", "Private", "Uninsured")),
            Detail("primary_care", "Primary Care", "Health", listOf("Has a provider", "Needs a provider")),
            Detail("current_crisis", "Current Crisis", "Health", listOf("Yes", "No")),
            Detail("substance_use", "Substance Use", "Health", listOf("Active use", "In recovery", "None")),
            Detail("disability", "Disability", "Health", listOf("Physical", "Mental health", "Developmental", "None")),
            Detail(
                "accessibility", "Accessibility", "Health",
                listOf(
                    "Wheelchair access", "Mobility assistance", "ASL interpretation",
                    "Visual assistance", "Other accessibility need",
                ),
            ),

            Detail("snap", "SNAP", "Income & Benefits", listOf("Receiving", "Applying", "Not receiving")),
            Detail("ssi", "SSI", "Income & Benefits", listOf("Receiving", "Applying", "Not receiving")),
            Detail(
                "employment_status", "Employment Status", "Income & Benefits",
                listOf("Employed", "Unemployed", "Seeking work", "Unable to work"),
            ),
            Detail(
                "income", "Income", "Income & Benefits",
                listOf("No income", "Under \$1,000 a month", "\$1,000 or more a month"),
            ),

            Detail("has_id", "Has ID", "Documents / ID", listOf("Yes", "No", "Expired")),
            Detail(
                "immigration", "Immigration", "Documents / ID",
                listOf("Citizen", "Permanent resident", "Visa holder", "Undocumented", "Prefer not to say"),
            ),
            Detail(
                "documents_needed", "Documents Needed", "Documents / ID",
                listOf("ID", "Birth certificate", "Social Security card", "Proof of income"),
            ),

            Detail("youth", "Youth", "Family", listOf("Minor in household", "Transition-age youth")),
            Detail("seniors", "Seniors", "Family", listOf("Yes", "No")),
            Detail(
                "domestic_violence", "Domestic Violence", "Family",
                listOf("Currently at risk", "Past experience", "No"),
            ),
            Detail("family_support", "Family Support", "Family", listOf("Has support", "Limited support", "No support")),
        ),
        // each need's details in card order
        links = mapOf(
            "shelter" to listOf(
                "housing_status", "sleeping_location", "couples", "pets", "veteran_status", "gender", "has_id",
            ),
            "food_supplies" to listOf("snap", "income", "transportation"),
            "medical_dental" to listOf("insurance", "primary_care", "disability", "transportation"),
            "mental_health" to listOf("insurance", "current_crisis", "substance_use", "primary_care"),
            "substance_use" to listOf("substance_use", "current_crisis", "insurance"),
            "crisis_support" to listOf("current_crisis", "domestic_violence", "sleeping_location"),
            "documents_id" to listOf("has_id", "documents_needed", "immigration"),
            "benefits_income" to listOf("insurance", "snap", "ssi", "employment_status", "income"),
            "transportation" to listOf("transportation", "housing_status", "disability"),
            "veterans" to listOf("veteran_status", "employment_status", "income"),
            "seniors_disability" to listOf("disability", "wheelchair_accessible", "seniors"),
            "legal_aid" to listOf("immigration", "documents_needed", "has_id"),
            "domestic_violence" to listOf("domestic_violence", "couples", "family_support"),
            "youth_services" to listOf("youth", "family_support", "has_id"),
        ),
        // for each linked detail, the needs that link it, owner first
        priorities = mapOf(
            "housing_status" to listOf("shelter", "transportation"),
            "sleeping_location" to listOf("shelter", "crisis_support"),
            "couples" to listOf("shelter", "domestic_violence"),
            "pets" to listOf("shelter"),
            "veteran_status" to listOf("veterans", "shelter"),
            "gender" to listOf("shelter"),
            "has_id" to listOf("documents_id", "shelter", "legal_aid", "youth_services"),
            "snap" to listOf("benefits_income", "food_supplies"),
            "income" to listOf("benefits_income", "veterans", "food_supplies"),
            "transportation" to listOf("transportation", "food_supplies", "medical_dental"),
            "insurance" to listOf("benefits_income", "mental_health", "medical_dental", "substance_use"),
            "primary_care" to listOf("medical_dental", "mental_health"),
            "disability" to listOf("seniors_disability", "medical_dental", "transportation"),
            "current_crisis" to listOf("crisis_support", "mental_health", "substance_use"),
            "substance_use" to listOf("substance_use", "mental_health"),
            "domestic_violence" to listOf("domestic_violence", "crisis_support"),
            "documents_needed" to listOf("documents_id", "legal_aid"),
            "immigration" to listOf("legal_aid", "documents_id"),
            "ssi" to listOf("benefits_income"),
            "employment_status" to listOf("benefits_income", "veterans"),
            "wheelchair_accessible" to listOf("seniors_disability"),
            "seniors" to listOf("seniors_disability"),
            "family_support" to listOf("domestic_violence", "youth_services"),
            "youth" to listOf("youth_services"),
        ),
        universalDetailIds = listOf("location_area", "age", "language", "accessibility"),
    )
}
