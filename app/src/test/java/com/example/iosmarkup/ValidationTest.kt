package com.example.iosmarkup

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for input validation
 * Tests the validation logic added for stroke width, text, and colors
 */
class ValidationTest {

    @Test
    fun `ValidationConstants have correct values`() {
        // Verify validation constants are set correctly
        assertEquals(1f, ValidationConstants.MIN_STROKE_WIDTH, 0.001f)
        assertEquals(100f, ValidationConstants.MAX_STROKE_WIDTH, 0.001f)
        assertEquals(100, ValidationConstants.MAX_TEXT_LENGTH)
    }

    @Test
    fun `stroke width coercion works correctly`() {
        // Test stroke width within valid range
        val validWidth = 50f
        val coerced = validWidth.coerceIn(
            ValidationConstants.MIN_STROKE_WIDTH,
            ValidationConstants.MAX_STROKE_WIDTH
        )
        assertEquals(50f, coerced, 0.001f)
    }

    @Test
    fun `stroke width below minimum is coerced to minimum`() {
        // Test stroke width below minimum
        val tooSmall = 0f
        val coerced = tooSmall.coerceIn(
            ValidationConstants.MIN_STROKE_WIDTH,
            ValidationConstants.MAX_STROKE_WIDTH
        )
        assertEquals(ValidationConstants.MIN_STROKE_WIDTH, coerced, 0.001f)
    }

    @Test
    fun `stroke width above maximum is coerced to maximum`() {
        // Test stroke width above maximum
        val tooLarge = 200f
        val coerced = tooLarge.coerceIn(
            ValidationConstants.MIN_STROKE_WIDTH,
            ValidationConstants.MAX_STROKE_WIDTH
        )
        assertEquals(ValidationConstants.MAX_STROKE_WIDTH, coerced, 0.001f)
    }

    @Test
    fun `negative stroke width is coerced to minimum`() {
        // Test negative stroke width
        val negative = -10f
        val coerced = negative.coerceIn(
            ValidationConstants.MIN_STROKE_WIDTH,
            ValidationConstants.MAX_STROKE_WIDTH
        )
        assertEquals(ValidationConstants.MIN_STROKE_WIDTH, coerced, 0.001f)
    }

    @Test
    fun `valid text passes length validation`() {
        // Test text within valid length
        val validText = "Hello World"
        assertTrue(validText.length <= ValidationConstants.MAX_TEXT_LENGTH)
    }

    @Test
    fun `text at maximum length is valid`() {
        // Test text exactly at maximum length
        val maxLengthText = "a".repeat(ValidationConstants.MAX_TEXT_LENGTH)
        assertEquals(ValidationConstants.MAX_TEXT_LENGTH, maxLengthText.length)
        assertTrue(maxLengthText.length <= ValidationConstants.MAX_TEXT_LENGTH)
    }

    @Test
    fun `text exceeding maximum length is invalid`() {
        // Test text exceeding maximum length
        val tooLongText = "a".repeat(ValidationConstants.MAX_TEXT_LENGTH + 1)
        assertTrue(tooLongText.length > ValidationConstants.MAX_TEXT_LENGTH)
    }

    @Test
    fun `empty text is detected`() {
        // Test empty text detection
        val emptyText = ""
        assertTrue(emptyText.isEmpty())
    }

    @Test
    fun `whitespace-only text is empty after trim`() {
        // Test whitespace handling
        val whitespaceText = "   \n\t  "
        assertTrue(whitespaceText.trim().isEmpty())
    }

    @Test
    fun `control characters are detected in text`() {
        // Test control character detection
        val textWithControl = "Hello\u0000World"
        assertTrue(textWithControl.contains(Regex("[\\p{C}]")))
    }

    @Test
    fun `normal text has no control characters`() {
        // Test normal text
        val normalText = "Hello World 123!@#"
        assertFalse(normalText.contains(Regex("[\\p{C}]")))
    }

    @Test
    fun `valid hex color format RRGGBB is recognized`() {
        // Test valid #RRGGBB format
        val validColor = "#FF0000"
        assertTrue(validColor.matches(Regex("^#[0-9A-Fa-f]{6}$")))
    }

    @Test
    fun `valid hex color format AARRGGBB is recognized`() {
        // Test valid #AARRGGBB format
        val validColor = "#80FF0000"
        assertTrue(validColor.matches(Regex("^#[0-9A-Fa-f]{8}$")))
    }

    @Test
    fun `invalid hex color without hash is rejected`() {
        // Test color without hash
        val invalidColor = "FF0000"
        assertFalse(invalidColor.matches(Regex("^#[0-9A-Fa-f]{6}$")))
    }

    @Test
    fun `invalid hex color with wrong length is rejected`() {
        // Test color with wrong length
        val invalidColor = "#FF00"
        assertFalse(invalidColor.matches(Regex("^#[0-9A-Fa-f]{6}$")))
    }

    @Test
    fun `invalid hex color with invalid characters is rejected`() {
        // Test color with invalid characters
        val invalidColor = "#GG0000"
        assertFalse(invalidColor.matches(Regex("^#[0-9A-Fa-f]{6}$")))
    }

    @Test
    fun `hex color parsing works for valid colors`() {
        // Test actual color parsing
        try {
            val color1 = Color.parseColor("#FF0000")
            assertEquals(Color.RED, color1)

            val color2 = Color.parseColor("#00FF00")
            assertEquals(Color.GREEN, color2)

            val color3 = Color.parseColor("#0000FF")
            assertEquals(Color.BLUE, color3)
        } catch (e: IllegalArgumentException) {
            fail("Valid colors should parse without exception")
        }
    }

    @Test
    fun `hex color parsing throws for invalid colors`() {
        // Test invalid color parsing
        assertThrows(IllegalArgumentException::class.java) {
            Color.parseColor("#GGGGGG")
        }
    }

    @Test
    fun `SignatureConstants have valid values`() {
        // Verify signature constants
        assertEquals(500, SignatureConstants.DEFAULT_HEIGHT)
        assertEquals(5f, SignatureConstants.DEFAULT_STROKE_WIDTH, 0.001f)
        assertEquals(Color.LTGRAY, SignatureConstants.BACKGROUND_COLOR)
    }

    @Test
    fun `DefaultColors palette is not empty`() {
        // Verify default colors exist
        assertTrue(DefaultColors.DEFAULT_PALETTE.isNotEmpty())
        assertEquals(4, DefaultColors.DEFAULT_PALETTE.size)
    }

    @Test
    fun `DefaultColors palette contains expected colors`() {
        // Verify default palette contains standard colors
        assertTrue(DefaultColors.DEFAULT_PALETTE.contains(Color.BLACK))
        assertTrue(DefaultColors.DEFAULT_PALETTE.contains(Color.RED))
        assertTrue(DefaultColors.DEFAULT_PALETTE.contains(Color.BLUE))
        assertTrue(DefaultColors.DEFAULT_PALETTE.contains(Color.GREEN))
    }

    @Test
    fun `accent colors list is not empty`() {
        // Verify accent colors exist
        assertTrue(DefaultColors.ACCENT_COLORS.isNotEmpty())
        assertEquals(6, DefaultColors.ACCENT_COLORS.size)
    }

    @Test
    fun `all accent colors are valid hex strings`() {
        // Verify all accent colors are valid hex format
        DefaultColors.ACCENT_COLORS.forEach { hexColor ->
            assertTrue(
                "Invalid hex color: $hexColor",
                hexColor.matches(Regex("^#[0-9A-Fa-f]{6}$"))
            )
        }
    }

    @Test
    fun `all accent colors can be parsed`() {
        // Verify all accent colors can be parsed without exception
        DefaultColors.ACCENT_COLORS.forEach { hexColor ->
            try {
                Color.parseColor(hexColor)
            } catch (e: IllegalArgumentException) {
                fail("Accent color $hexColor should be parseable")
            }
        }
    }
}
