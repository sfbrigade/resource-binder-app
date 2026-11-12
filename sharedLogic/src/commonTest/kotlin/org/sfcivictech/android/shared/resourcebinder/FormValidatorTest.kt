package org.sfcivictech.android.shared.resourcebinder

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormValidatorTest {

    @Test
    fun fullNameAcceptsARealName() {
        assertTrue(FormValidator.isValidFullName("Nikolai K"))
    }

    @Test
    fun fullNameRejectsASingleCharacter() {
        assertFalse(FormValidator.isValidFullName("1"))
    }

    @Test
    fun fullNameRejectsWhitespaceOnly() {
        assertFalse(FormValidator.isValidFullName("   "))
    }

    @Test
    fun fullNameRejectsDigitsOnly() {
        assertFalse(FormValidator.isValidFullName("12345"))
    }

    @Test
    fun emailAcceptsAWorkEmail() {
        assertTrue(FormValidator.isValidEmail("nikolai@civic-tech.design"))
    }

    @Test
    fun emailRejectsAMissingTopLevelDomain() {
        assertFalse(FormValidator.isValidEmail("nikolai@1234567"))
    }

    @Test
    fun emailRejectsEmpty() {
        assertFalse(FormValidator.isValidEmail(""))
    }

    @Test
    fun emailIgnoresSurroundingWhitespace() {
        assertTrue(FormValidator.isValidEmail("  nikolai@civic-tech.design "))
    }

    @Test
    fun organizationAcceptsADepartmentName() {
        assertTrue(FormValidator.isValidOrganization("City Outreach Department"))
    }

    @Test
    fun organizationRejectsDigitsOnly() {
        assertFalse(FormValidator.isValidOrganization("1234567"))
    }

    @Test
    fun organizationRejectsASingleCharacter() {
        assertFalse(FormValidator.isValidOrganization("A"))
    }
}
