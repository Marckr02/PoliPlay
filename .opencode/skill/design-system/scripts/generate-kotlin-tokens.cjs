#!/usr/bin/env node
/**
 * Kotlin Token Generator - Generates Kotlin token files from JSON config
 */

const fs = require('fs');
const path = require('path');

/**
 * Generate the Tokens.kt file from tokens.json config
 */
function generateTokensFile(outputPath) {
    const tokenContent = `package com.example.campuspocket.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DESIGN TOKENS - Three-Layer Architecture
 * 
 * Layer 1: PRIMITIVE - Raw values (colors, spacing, typography scales)
 * Layer 2: SEMANTIC - Purpose-driven aliases (primary, surface, error, etc.)
 * Layer 3: COMPONENT - Component-specific tokens (button-bg, card-elevation, etc.)
 * 
 * NEVER use raw values in components. Always reference semantic or component tokens.
 * This enables theming, dark mode, and brand changes from a single source.
 */

// ═══════════════════════════════════════════════════════════════
// LAYER 1: PRIMITIVE TOKENS - Raw Values
// ═══════════════════════════════════════════════════════════════

object PrimitiveTokens {

    // Color Primitives - Cold Luxury Palette
    object Color {
        // Brand Blue - Primary Accent
        val Blue50 = Color(0xFFEFF6FF)
        val Blue100 = Color(0xFFDBEAFE)
        val Blue200 = Color(0xFFBFDBFE)
        val Blue300 = Color(0xFF93C5FD)
        val Blue400 = Color(0xFF60A5FA)
        val Blue500 = Color(0xFF3B82F6)  // Primary brand
        val Blue600 = Color(0xFF2563EB)
        val Blue700 = Color(0xFF1D4ED8)
        val Blue800 = Color(0xFF1E40AF)
        val Blue900 = Color(0xFF1E3A8A)
        val Blue950 = Color(0xFF172554)

        // Neutral Scale - Cool Slate
        val Slate50 = Color(0xFFF8FAFC)
        val Slate100 = Color(0xFFF1F5F9)
        val Slate200 = Color(0xFFE2E8F0)
        val Slate300 = Color(0xFFCBD5E1)
        val Slate400 = Color(0xFF94A3B8)
        val Slate500 = Color(0xFF64748B)
        val Slate600 = Color(0xFF475569)
        val Slate700 = Color(0xFF334155)
        val Slate800 = Color(0xFF1E293B)
        val Slate900 = Color(0xFF0F172A)
        val Slate950 = Color(0xFF020617)

        // Semantic Color Primitives
        val Emerald500 = Color(0xFF10B981)  // Success
        val Emerald600 = Color(0xFF059669)
        val Amber500 = Color(0xFFF59E0B)   // Warning
        val Amber600 = Color(0xFFD97706)
        val Rose500 = Color(0xFFF43F5E)    // Error (not generic red)
        val Rose600 = Color(0xFFE11D48)

        // White / Black
        val White = Color.White
        val Black = Color.Black
    }

    // Spacing Scale - 4dp base unit, exponential progression
    object Space {
        val XXS = 2.dp
        val XS = 4.dp
        val SM = 8.dp
        val MD = 16.dp
        val LG = 24.dp
        val XL = 32.dp
        val XXL = 48.dp
        val XXXL = 64.dp
        val XXXXL = 96.dp
    }

    // Typography Scale - Using Geist + JetBrains Mono
    object Typography {
        // Font Sizes (sp)
        val DisplayLarge = 57.sp
        val DisplayMedium = 45.sp
        val DisplaySmall = 36.sp
        val HeadlineLarge = 32.sp
        val HeadlineMedium = 28.sp
        val HeadlineSmall = 24.sp
        val TitleLarge = 22.sp
        val TitleMedium = 16.sp
        val TitleSmall = 14.sp
        val BodyLarge = 16.sp
        val BodyMedium = 14.sp
        val BodySmall = 12.sp
        val LabelLarge = 14.sp
        val LabelMedium = 12.sp
        val LabelSmall = 11.sp
    }

    // Border Radius Scale
    object Radius {
        val None = 0.dp
        val XS = 4.dp
        val SM = 8.dp
        val MD = 12.dp
        val LG = 16.dp
        val XL = 20.dp
        val XXL = 24.dp
        val Full = 9999.dp
    }

    // Shadow/Elevation Scale
    object Elevation {
        val Level0 = 0.dp
        val Level1 = 1.dp
        val Level2 = 4.dp
        val Level3 = 8.dp
        val Level4 = 16.dp
        val Level5 = 24.dp
    }

    // Animation Durations
    object Motion {
        val Instant = 0
        val Fast = 150
        val Normal = 250
        val Slow = 350
        val Slower = 500
    }

    // Easing Curves
    object Easing {
        val Standard = "cubic-bezier(0.16, 1, 0.3, 1)"
        val Emphasized = "cubic-bezier(0.2, 0, 0, 1)"
        val Accelerated = "cubic-bezier(0.4, 0, 1, 1)"
        val SpringGentle = "spring(0.3, 0.9)"
        val SpringBouncy = "spring(0.4, 0.6)"
    }
}

// ═══════════════════════════════════════════════════════════════
// LAYER 2: SEMANTIC TOKENS - Purpose-Driven Aliases
// ═══════════════════════════════════════════════════════════════

object SemanticTokens {
    object Light {
        // Brand
        val Primary = PrimitiveTokens.Color.Blue600
        val PrimaryContainer = PrimitiveTokens.Color.Blue100
        val OnPrimary = PrimitiveTokens.Color.White
        val OnPrimaryContainer = PrimitiveTokens.Color.Blue900

        // Background & Surface
        val Background = PrimitiveTokens.Color.Slate50
        val Surface = PrimitiveTokens.Color.White
        val SurfaceVariant = PrimitiveTokens.Color.Slate100
        
        // On Colors
        val OnBackground = PrimitiveTokens.Color.Slate900
        val OnSurface = PrimitiveTokens.Color.Slate900
        val OnSurfaceVariant = PrimitiveTokens.Color.Slate600

        // Outline
        val Outline = PrimitiveTokens.Color.Slate300
        val OutlineVariant = PrimitiveTokens.Color.Slate200

        // State
        val Success = PrimitiveTokens.Color.Emerald600
        val SuccessContainer = Color(0xFFD1FAE5)
        val OnSuccess = PrimitiveTokens.Color.White
        val OnSuccessContainer = Color(0xFF064E2B)

        val Warning = PrimitiveTokens.Color.Amber600
        val WarningContainer = Color(0xFFFEF3C7)
        val OnWarning = PrimitiveTokens.Color.Slate900
        val OnWarningContainer = Color(0xFF78350F)

        val Error = PrimitiveTokens.Color.Rose600
        val ErrorContainer = Color(0xFFFCE7E8)
        val OnError = PrimitiveTokens.Color.White
        val OnErrorContainer = Color(0xFF991B1B)

        // Inverse
        val InverseSurface = PrimitiveTokens.Color.Slate900
        val InverseOnSurface = PrimitiveTokens.Color.Slate50
        val InversePrimary = PrimitiveTokens.Color.Blue400
    }

    object Dark {
        // Brand
        val Primary = PrimitiveTokens.Color.Blue400
        val PrimaryContainer = PrimitiveTokens.Color.Blue900.copy(alpha = 0.3f)
        val OnPrimary = PrimitiveTokens.Color.Slate950
        val OnPrimaryContainer = PrimitiveTokens.Color.Blue100

        // Background & Surface
        val Background = PrimitiveTokens.Color.Slate950
        val Surface = PrimitiveTokens.Color.Slate900
        val SurfaceVariant = PrimitiveTokens.Color.Slate800

        // On Colors
        val OnBackground = PrimitiveTokens.Color.Slate50
        val OnSurface = PrimitiveTokens.Color.Slate50
        val OnSurfaceVariant = PrimitiveTokens.Color.Slate400

        // Outline
        val Outline = PrimitiveTokens.Color.Slate700
        val OutlineVariant = PrimitiveTokens.Color.Slate800

        // State
        val Success = PrimitiveTokens.Color.Emerald500
        val SuccessContainer = Color(0xFF064E2B)
        val OnSuccess = PrimitiveTokens.Color.Slate950
        val OnSuccessContainer = Color(0xFFD1FAE5)

        val Warning = PrimitiveTokens.Color.Amber500
        val WarningContainer = Color(0xFF78350F)
        val OnWarning = PrimitiveTokens.Color.Slate950
        val OnWarningContainer = Color(0xFFFEF3C7)

        val Error = PrimitiveTokens.Color.Rose500
        val ErrorContainer = Color(0xFF991B1B)
        val OnError = PrimitiveTokens.Color.Slate950
        val OnErrorContainer = Color(0xFFFCE7E8)

        // Inverse
        val InverseSurface = PrimitiveTokens.Color.Slate50
        val InverseOnSurface = PrimitiveTokens.Color.Slate900|◊|        val InversePrimary = PrimitiveTokens.Color.Blue600
    }

    // Spacing Semantic Aliases
    object Space {
        val InsetXS = PrimitiveTokens.Space.XS
        val InsetSM = PrimitiveTokens.Space.SM
        val InsetMD = PrimitiveTokens.Space.MD
        val InsetLG = PrimitiveTokens.Space.LG
        val InsetXL = PrimitiveTokens.Space.XL
        val InsetXXL = PrimitiveTokens.Space.XXL

        val GapXS = PrimitiveTokens.Space.XS
        val GapSM = PrimitiveTokens.Space.SM
        val GapMD = PrimitiveTokens.Space.MD
        val GapLG = PrimitiveTokens.Space.LG
        val GapXL = PrimitiveTokens.Space.XL

        val StackXS = PrimitiveTokens.Space.XS
        val StackSM = PrimitiveTokens.Space.SM
        val StackMD = PrimitiveTokens.Space.MD
        val StackLG = PrimitiveTokens.Space.LG
    }

    // Radius Semantic Aliases
    object Radius {
        val ComponentXS = PrimitiveTokens.Radius.XS
        val ComponentSM = PrimitiveTokens.Radius.SM
        val ComponentMD = PrimitiveTokens.Radius.MD
        val ComponentLG = PrimitiveTokens.Radius.LG
        val ComponentXL = PrimitiveTokens.Radius.XL
        val ComponentFull = PrimitiveTokens.Radius.Full
    }

    // Elevation Semantic Aliases
    object Elevation {
        val Resting = PrimitiveTokens.Elevation.Level1
        val Raised = PrimitiveTokens.Elevation.Level2
        val Floating = PrimitiveTokens.Elevation.Level3
        val Overlay = PrimitiveTokens.Elevation.Level4
        val Popover = PrimitiveTokens.Elevation.Level5
    }

    // Motion Semantic Aliases
    object Motion {
        val Micro = PrimitiveTokens.Motion.Fast
        val Transition = PrimitiveTokens.Motion.Normal
        val Expand = PrimitiveTokens.Motion.Slow
        val Page = PrimitiveTokens.Motion.Slower
    }
}

// ═══════════════════════════════════════════════════════════════
// LAYER 3: COMPONENT TOKENS - Component-Specific
// ═══════════════════════════════════════════════════════════════

object ComponentTokens {
    // Import semantic tokens for reference
    // Button tokens would go here...
}