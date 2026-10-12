---
name: StudySync Tactile Crisp
colors:
  surface: '#fbf9f8'
  surface-dim: '#dbd9d9'
  surface-bright: '#fbf9f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f3'
  surface-container: '#efeded'
  surface-container-high: '#eae8e7'
  surface-container-highest: '#e4e2e2'
  on-surface: '#1b1c1c'
  on-surface-variant: '#3f4a36'
  inverse-surface: '#303030'
  inverse-on-surface: '#f2f0f0'
  outline: '#6f7b64'
  outline-variant: '#becbb1'
  surface-tint: '#2b6c00'
  primary: '#2b6c00'
  on-primary: '#ffffff'
  primary-container: '#58cc02'
  on-primary-container: '#1e5000'
  inverse-primary: '#6be026'
  secondary: '#006590'
  on-secondary: '#ffffff'
  secondary-container: '#2fb8ff'
  on-secondary-container: '#004666'
  tertiary: '#545b8d'
  on-tertiary: '#ffffff'
  tertiary-container: '#aab0e8'
  on-tertiary-container: '#3b4272'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#87fe45'
  primary-fixed-dim: '#6be026'
  on-primary-fixed: '#082100'
  on-primary-fixed-variant: '#1f5100'
  secondary-fixed: '#c8e6ff'
  secondary-fixed-dim: '#88ceff'
  on-secondary-fixed: '#001e2e'
  on-secondary-fixed-variant: '#004c6e'
  tertiary-fixed: '#dfe0ff'
  tertiary-fixed-dim: '#bdc3fc'
  on-tertiary-fixed: '#0f1646'
  on-tertiary-fixed-variant: '#3c4374'
  background: '#fbf9f8'
  on-background: '#1b1c1c'
  surface-variant: '#e4e2e2'
  paper-white: '#ffffff'
  storybook-green: '#d7ffb8'
  fresh-leaf: '#a5ed6e'
  pencil-gray: '#777777'
  faded-gray: '#afafaf'
typography:
  display:
    fontFamily: Nunito Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Nunito Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: Nunito Sans
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Nunito Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  headline-sm:
    fontFamily: Nunito Sans
    fontSize: 19px
    fontWeight: '700'
    lineHeight: 26px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 17px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '500'
    lineHeight: 22px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  label-caps:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.06em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-lg: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style
This design system pairs the clarity of paper stationery with high-energy digital feedback, engineered specifically for focused mobile study workflows. Tailored for students, researchers, and lifelong learners, the aesthetic feels like opening a fresh notebook: deliberate, structured, and optimistic. 

The visual style blends **Tactile Modernism** with clean flat vector precision. It rejects blurry blurs, muddy shadows, and superficial gradients in favor of crisp 2px stroke boundaries, physical "press-down" sticker buttons, and generous white space. Saturated color is used as a functional signal—eager green marks achievement and progress milestones, spark blue invites intellectual exploration, while deep night ink provides unyielding structural contrast. Crisp line-drawn SVG icons replace pictorial noise or emojis, establishing an authoritative yet welcoming academic presence.

## Colors
The color architecture functions on high contrast and crisp legibility against pure white paper surfaces:

- **Primary (`#58cc02` - Eager Green)**: The anchor of momentum, positive streaks, and task completions. It is deployed on high-intent interactive buttons, completion badges, and key metric displays.
- **Secondary (`#1cb0f6` - Spark Blue)**: The exploratory tone. Used for interactive links, filter chips, secondary actions, and informational calls-to-action.
- **Tertiary (`#000437` - Night Ink)**: The deepest visual anchor. Used for high-impact structural typography, persistent tab bar accents, and dark high-contrast badges.
- **Neutral (`#4b4b4b` - Charcoal)**: The workhorse for readable body copy and titles, preserving visual comfort without the harsh glare of pure `#000000`.
- **Canvas & Neutrals**: Pure `#ffffff` (Paper White) forms the canvas ground. `#777777` (Pencil Gray) governs secondary metadata and timestamps, while `#afafaf` (Faded Gray) provides consistent 2px hairline structural borders and disabled UI states.
- **Tonal Tints**: `#d7ffb8` (Storybook Green) serves exclusively as a soft badge ground and subtle active container wash.

## Typography
The system employs **Nunito Sans** for expressive, friendly, high-weight display headings alongside **Plus Jakarta Sans** for neutral, geometrically balanced body copy and interface labels.

- **Headlines**: Set in heavy weights (700–800) with slight negative tracking to give milestones and section headers a confident, friendly sticker stamp.
- **Body**: Uses 500 (Medium) weights to ensure clean optical density on mobile retina screens without feeling frail or spindly.
- **Navigation & Labels**: Button labels and category markers utilize `label-caps`—strictly uppercase with deliberate letter spacing (`0.06em`) to establish visual punch and clear affordance.

## Layout & Spacing
The layout adheres strictly to an absolute **4px base unit**. On mobile displays, content conforms to a single-column fluid layout framed by 16px (`1rem`) outer margins. Element stacks, grids, and list rows maintain rigid multiples of 4px:

- **Micro-gaps (`space-xs` = 4px, `space-sm` = 8px)**: Used for icon-to-text spacing, status tags, and stacked inline labels.
- **Component rhythm (`space-md` = 12px, `space-lg` = 16px)**: Used for card internal padding, button gutters, and segmented pill groups.
- **Section breathing (`space-xl` = 24px to 32px)**: Separates task lists, study decks, and daily objective modules.
- **Form factors**: Mobile devices center content with 16px horizontal margins. Tablet screens scale horizontal margins to 24px (`1.5rem`) and cap content width at 640px to retain vertical scanning efficiency and touch reachability.

## Elevation & Depth
This design system avoids soft diffuse shadows or blurry gradients. Depth is created through **physical tactile stamping** and crisp 2px line boundaries:

- **Level 0 (Base Ground)**: Pure `#ffffff` canvas with no shadow.
- **Level 1 (Card & Module Surface)**: `#ffffff` paper cards defined by a solid `2px solid #afafaf` stroke, with an optional hard 2px or 4px solid drop-edge shadow (`box-shadow: 0 4px 0 #afafaf`) that visually sinks when tapped.
- **Level 2 (Tactile Primary Action)**: Buttons utilize a 3D physical edge created by a saturated solid bottom border (`box-shadow: 0 4px 0 #46a302` for green CTA buttons). On active/touch down, the button translates down 2px to 4px and drops the edge, producing an immediate tactile feedback mechanism.
- **Level 3 (Sticky Navigation / Modals)**: Floated bottom bars and bottom sheets feature a hard `2px solid #000437` top border or a dense, zero-blur hard edge offset (`0 -3px 0 rgba(0, 4, 55, 0.06)`).

## Shapes
A pronounced **pill-shaped geometry (Level 3)** governs interactive touchpoints, reinforcing an approachable, sticker-like tactile language.

- **Buttons & Pills**: All action buttons, chips, tags, and badge counters use fully rounded pill radii (`9999px` or standard `1rem` on compact tokens).
- **Cards & Surface Containers**: Standard task modules and content blocks use a consistent `16px` border-radius (`rounded-lg`), balancing rounded friendliness with interior layout density.
- **Inputs & Modals**: Text fields feature a `14px` border radius with uniform `2px` stroke borders.

## Components

### Buttons
- **Primary Action (Tactile Pill)**: Solid `#58cc02` fill, text in `#ffffff` (`label-caps`), zero soft shadow, anchored by a solid `box-shadow: 0 4px 0 #46a302`. Height is 48px to satisfy accessible mobile touch targets. On active press: translates down 3px with the shadow collapsing to 1px.
- **Secondary Outlined Pill**: Transparent or pure `#ffffff` ground, text in `#1cb0f6` (`label-caps`), framed with `2px solid #afafaf` and an offset `box-shadow: 0 3px 0 #afafaf`.
- **Ghost Action**: Transparent background with `#777777` or `#000437` text and no borders.

### Chips & Filters
- Compact 32px height, fully rounded pill radius, `2px solid #afafaf` perimeter.
- Inactive: `#ffffff` background with `#777777` text.
- Active: `#1cb0f6` border with `#1cb0f6` text and an optional 10% tint ground, or filled `#000437` with `#ffffff` text for high-contrast multi-selection.

### Cards & Study Modules
- White paper surface (`#ffffff`) bounded by a uniform `2px solid #afafaf` line, `16px` internal padding, and `16px` rounded corners.
- Interactive cards feature a tactile bottom edge (`box-shadow: 0 4px 0 #e5e5e5`).
- Streak or success modules replace the gray border with a `2px solid #58cc02` perimeter and a faint `#d7ffb8` header tint.

### Form Inputs
- Height 48px, 14px border radius, pure `#ffffff` background with `2px solid #afafaf`.
- Typography set in `body-md` (`#4b4b4b`). Floating helper labels in `label-sm` (`#777777`).
- Focus state switches border instantly to `2px solid #1cb0f6` with zero glow rings. Error states adopt a crisp `2px solid #ea2b2b`.

### Checkboxes & Radio Selectors
- Custom 24px circles (radios) and rounded squares (checkboxes, 6px radius) built with a `2px solid #afafaf` border.
- Selected state fills completely with `#58cc02` border and background, displaying a crisp `#ffffff` 2.5px line-weight SVG checkmark.

### Progress Bars
- 12px to 16px tall tracks with a full pill radius.
- Background track is `#e5e5e5`. Progress fill is `#58cc02` with an internal 2px highlight bar at the top edge to simulate physical enameled plastic.

### Iconography & Illustrations
- Strictly vector SVG line graphics with consistent 2px stroke weight matching the typographic charcoal and faded gray tones.
- No emojis are permitted anywhere in the UI; all status, emotion, and categorical metaphors are communicated via precision vector iconography.