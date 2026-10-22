# CampusPocket Component Specifications

Detailed specs for core components with states and variants, mapped to tokens.

**Design System:** Cold Luxury (Slate + Blue)
**Theme:** Material 3 with dynamic color fallback

## Button

### Variants

| Variant | Background | Text | Border | Use Case |
|---------|------------|------|--------|----------|
| Primary | Light.Primary | Light.OnPrimary | none | Main CTA, primary actions |
| Secondary | Light.PrimaryContainer | Light.OnPrimaryContainer | none | Secondary actions |
| Outlined | Light.Surface | Light.Primary | Light.Outline | Tertiary actions |
| Ghost | Light.Surface | Light.Primary | none | Minimal actions |
| Danger | Light.Error | Light.OnError | none | Destructive actions |

### Sizes

| Size | Height | Padding X | Padding Y | Font Size | Icon Size |
|------|--------|-----------|-----------|-----------|-----------|
| sm | ComponentTokens.Button.HeightSM | ComponentTokens.Button.PaddingHorizontalSM | ComponentTokens.Button.PaddingVertical | ComponentTokens.Button.FontSize | ComponentTokens.IconSize.MD |
| default | ComponentTokens.Button.Height | ComponentTokens.Button.PaddingHorizontal | ComponentTokens.Button.PaddingVertical | ComponentTokens.Button.FontSize | ComponentTokens.IconSize.MD |
| lg | ComponentTokens.Button.HeightLG | ComponentTokens.Button.PaddingHorizontal | ComponentTokens.Button.PaddingVertical | ComponentTokens.Button.FontSize | ComponentTokens.IconSize.LG |

### States

| State | Background | Text | Opacity | Cursor |
|-------|------------|------|---------|--------|
| default | ComponentTokens.Button.PrimaryBg | ComponentTokens.Button.PrimaryText | 1 | pointer |
| hover | ComponentTokens.Button.PrimaryBgHover | ComponentTokens.Button.PrimaryText | 1 | pointer |
| pressed | ComponentTokens.Button.PrimaryBgPressed | ComponentTokens.Button.PrimaryText | 1 | pointer |
| focus | ComponentTokens.Surface (with 4px border Primary) | ComponentTokens.Button.PrimaryText | 1 | pointer |
| disabled | ComponentTokens.Button.PrimaryBgDisabled | ComponentTokens.Button.PrimaryTextDisabled | 0.5 | not-allowed |

### Animation

- Press: Spring(0.4, 0.6) - scale 0.98 to 1
- Hover: Tween(150ms, easeOut)

---

## Card

### Variants

| Variant | Shadow | Border | Background | Use Case |
|---------|--------|--------|------------|----------|
| default | ComponentTokens.Card.ElevationResting | ComponentTokens.Card.Border | ComponentTokens.Card.Bg | Standard content |
| elevated | ComponentTokens.Card.ElevationRaised | ComponentTokens.Card.BorderHover | ComponentTokens.Card.Bg | Hovered/interactive |
| glass | ComponentTokens.Card.ElevationFloating | Glass.border | Glass.background | Premium surfaces |
| outline | none | ComponentTokens.Card.Border | Transparent | Subtle container |

### Anatomy

```
┌─────────────────────────────────────┐
│ Card Header                         │
│   Title                             │
│   Description                       │
├─────────────────────────────────────┤
│ Card Content                        │
│   Main content area                 │
├─────────────────────────────────────┤
│ Card Footer                         │
│   Actions                           │
└─────────────────────────────────────┘
```

### Spacing

| Area | Padding |
|------|---------|
| header | ComponentTokens.Card.Padding |
| content | ComponentTokens.Card.Padding |
| footer | ComponentTokens.Card.Padding |
| gap | ComponentTokens.Card.Gap |

### Animation

- Hover: Elevate + background change
- Press: Subtle scale 0.98
- Glass: Always active with backdrop blur

---

## Input / TextField

### Variants

| Variant | Description |
|---------|-------------|
| default | Standard text input |
| textarea | Multi-line text |
| select | Dropdown selection |

### Sizes

| Size | Height | Padding | Font Size |
|------|--------|---------|-----------|
| sm | ComponentTokens.Input.HeightSM | ComponentTokens.Input.PaddingHorizontal | ComponentTokens.Input.FontSize |
| default | ComponentTokens.Input.Height | ComponentTokens.Input.PaddingHorizontal | ComponentTokens.Input.FontSize |
| lg | ComponentTokens.Input.HeightLG | ComponentTokens.Input.PaddingHorizontal | ComponentTokens.Input.FontSize |

### States

| State | Border | Background | Ring |
|-------|--------|------------|------|
| default | Light.Outline | Light.Surface | none |
| focused | Light.Primary | Light.Surface | Light.Primary.copy(alpha = 0.24f) |
| error | Light.Error | Light.ErrorContainer | Light.Error |
| disabled | Light.OutlineVariant | Light.SurfaceContainerHighest.copy(alpha = 0.5f) | none |

### Animation

- Focus: Tween(150ms) for border/ring
- Error: Tween(200ms) for shake

---

## Chip

### Variants

| Variant | Background | Text | Selected Background | Use Case |
|---------|------------|------|---------------------|----------|
| default | Light.SurfaceVariant | Light.OnSurfaceVariant | Light.PrimaryContainer | Filter selection |
| input | Light.Surface | Light.OnSurface | Light.SecondaryContainer | Tag input |
| choice | Light.SurfaceVariant | Light.OnSurfaceVariant | Light.Primary | Single select |

### States

| State | Background | Text | Opacity |
|-------|------------|------|---------|
| default | ComponentTokens.Chip.BgDefault | ComponentTokens.Chip.TextDefault | 1 |
| selected | ComponentTokens.Chip.BgSelected | ComponentTokens.Chip.TextSelected | 1 |
| hover | ComponentTokens.Chip.BgHover | ComponentTokens.Chip.TextDefault | 1 |
| pressed | ComponentTokens.Chip.BgPressed | ComponentTokens.Chip.TextSelected | 1 |
| disabled | ComponentTokens.Chip.BgDisabled | ComponentTokens.Chip.TextDisabled | 0.38 |

### Sizing

| Property | Token |
|----------|-------|
| Radius | ComponentTokens.Chip.Radius (ComponentFull - pill) |
| Padding X | ComponentTokens.Chip.PaddingHorizontal |
| Padding Y | ComponentTokens.Chip.PaddingVertical |
| Gap | ComponentTokens.Chip.IconGap |

---

## FAB

### Variants

| Variant | Size | Radius |
|---------|------|--------|
| Normal | ComponentTokens.FAB.Size (56dp) | ComponentTokens.FAB.Radius (ComponentFull) |
| Mini | ComponentTokens.FAB.SizeMini (40dp) | ComponentTokens.FAB.Radius |

### States

| State | Background | Elevation | Scale |
|-------|------------|-----------|-------|
| default | Light.Primary | ComponentTokens.FAB.Elevation | 1 |
| pressed | Light.Primary | ComponentTokens.FAB.Elevation | 0.98 |
| disabled | Light.Surface.copy(alpha = 0.12f) | 0 | 1 |

### Animation

- Press: Spring(0.3, 0.9), scale 0.98 → 1
- Show/Hide: Tween(200ms, slide from/to bottom)

---

## Bottom Navigation

| Property | Token |
|----------|-------|
| Height | ComponentTokens.BottomNav.Height (72dp) |
| Active Icon | Light.Primary |
| Inactive Icon | Light.OnSurfaceVariant |
| Active Label | Light.Primary |
| Inactive Label | Light.OnSurfaceVariant |

### States

- Active: Bold label, filled container
- Inactive: Normal label, transparent container
- Badge: Red indicator on active icon

### Behavior

- Max 5 items (accessibility)
- Label alongside icon (not below)
- Scrollable overflow for extra items

---

## Progress

### Types

| Type | Indicator | Track | Use Case |
|------|-----------|-------|----------|
| Linear | ComponentTokens.Progress.Indicator | ComponentTokens.Progress.Track | Page progress |
| Circular | ComponentTokens.Progress.Indicator | none | Button indeterminates |

### States

| State | Color |
|-------|-------|
| default | Light.Primary |
| error | Light.Error |
| disabled | Light.OutlineVariant |

---

## Dialog / Modal

### Sizes

| Size | Max Width | Header Height | Use Case |
|------|-----------|---------------|----------|
| sm | 384dp | 48dp | Simple confirmations |
| default | 560dp | 64dp | Standard dialogs |
| lg | 640dp | 80dp | Forms |
| full | Screen - 32dp | 64dp | Mobile full-screen |

### Anatomy

```
┌───────────────────────────────────────┐
│ Dialog Header                      [✕]│
│   Title                               │
│   Description                         │
├───────────────────────────────────────┤
│ Dialog Content                        │
│   Scrollable if needed                │
├───────────────────────────────────────┤
│ Dialog Footer                         │
│                      [Cancel] [Confirm]│
└───────────────────────────────────────┘
```

### Animation

- Enter: Expanded + Fade in (ExpandTransition)
- Exit: Shrink + Fade out (StandardTransition)
- Backdrop: Fade in sync with dialog

---

## Empty State

### Properties

| Property | Token |
|----------|-------|
| Icon Size | 48dp |
| Icon Color | Primary |
| Title Size | ComponentTokens.Dialog.TitleFontSize |
| Message Size | ComponentTokens.Dialog.BodyFontSize |
| Button Gap | ComponentTokens.Card.Gap |

### Layout

- Centered
- Icon → Title → Message → Action
- Max width: 300dp (mobile)

---

## Toast / Snackbar

| Property | Token |
|----------|-------|
| Background | ComponentTokens.Toast.Bg |
| Text | ComponentTokens.Toast.Text |
| Radius | ComponentTokens.Toast.Radius |
| Position | Bottom, center horizontally |
| Duration | 4000ms (configurable) |

### States

- Default: Info
- Success: Success background
- Error: Error background
- Warning: Warning background