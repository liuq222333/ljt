# Front Admin Redesign Design

## Summary

Redesign `D:\code\aaaaljt\front-admin` into a desktop-first admin console that reads as an internal operations product rather than an AI-generated showcase. The redesign keeps the existing business modules and route map, but replaces the current gradient-heavy, oversized-card presentation with a consistent "steady operations console" system: left primary navigation, top utility bar, tighter information density, restrained neutral palette, compact radii, and page templates that balance overview cards with list- and form-driven management workflows.

## Why This Work Is Needed

The current admin UI communicates the wrong product identity for a management console:

- Global and page-level gradients create a landing-page feel rather than an operations tool feel.
- Many panels use large radii, heavy shadows, and decorative hover motion that read as marketing polish instead of admin precision.
- Page templates are inconsistent: some pages behave like dashboards, some like demos, and some like forms without a unified shell.
- Information hierarchy is loose, so overview data, recent records, filters, and actions do not consistently look like parts of the same system.
- The user explicitly wants the redesign to feel aligned with an admin console, remain simple, remove gradients, reduce the "AI" aesthetic, and stay desktop-focused.

## Approved Direction

This design is based on explicit user approval for the following constraints:

- Visual direction: **A / steady operations console**
- Layout: **left primary navigation + top utility bar**
- Information organization: **balanced overview + list/form workflows**
- Density: **compact rather than spacious**
- Device priority: **desktop only for now**
- Styling constraints:
  - no gradients
  - no glassmorphism / frosted panels
  - card corners must not feel overly rounded
  - reduced decorative motion and reduced floating-card behavior

## Goals

1. Make the app read immediately as an internal admin product.
2. Unify all pages under one shell and one visual system.
3. Preserve existing route coverage and business capability.
4. Improve scanability for governance, launch, review, notification, market, API, and user-management tasks.
5. Keep overview surfaces where they are useful, but make inner pages primarily list-, table-, and form-oriented.
6. Limit the redesign to the frontend presentation layer so implementation risk stays low.

## Non-Goals

- No backend API redesign.
- No route re-architecture that changes module ownership.
- No mobile-first or phone-specific redesign.
- No rebranding exercise or expressive visual identity system.
- No chart-heavy executive dashboard aesthetic.
- No animation system beyond minimal state feedback.

## Existing Surface Area

The current route map in `D:\code\aaaaljt\front-admin\src\Router\index.ts` covers these module families:

- Governance dashboard, replay, eval, release, diagnostics
- Launch center
- Notifications publish
- Local activity review and schedule review
- User management
- Neighbor task review
- API management
- Second-hand market management

The shell and overall aesthetic currently come primarily from:

- `D:\code\aaaaljt\front-admin\src\App.vue`
- `D:\code\aaaaljt\front-admin\src\components\Home.vue`
- `D:\code\aaaaljt\front-admin\src\style.css`

Representative page templates that currently influence the rest of the product are:

- `D:\code\aaaaljt\front-admin\src\components\governance\GovernanceDashboard.vue`
- `D:\code\aaaaljt\front-admin\src\components\launch\LaunchCenter.vue`

## Design Principles

### 1. Operational, Not Promotional

Every page should feel like a tool used to complete work. Visual treatments that imply storytelling, product marketing, or hero-section composition should be removed.

### 2. Structure Before Decoration

Hierarchy comes from layout, spacing, borders, alignment, and typography. Decoration is secondary and should stay minimal.

### 3. Compact but Readable

The UI should be denser than the current version, but not cramped. Target fast scanning and desktop efficiency rather than roomy presentation.

### 4. Consistency Across Modules

Governance, launch, review, market, user, and notification pages should feel like variants of one system, not independent microsites.

### 5. Action Clarity

Users should be able to distinguish at a glance between:

- page title/context
- status summary
- filters/search
- primary actions
- data list/form body
- side context or helper content

## Visual System

### Color

Use a restrained admin palette built around neutral greys and a single low-saturation emphasis color.

- App background: cool light grey, not white and not tinted by gradients
- Surface background: white panels
- Border color: visible light grey to define structure cleanly
- Primary text: dark slate / charcoal
- Secondary text: medium neutral grey
- Accent color: one dark blue-grey or muted blue for active nav, primary actions, and focus treatment
- Semantic colors: quiet green/orange/red for states only, never as decorative accents

Rules:

- No linear gradients
- No radial gradients
- No tinted glowing backgrounds
- No translucent glass cards

### Typography

Typography should support dense management screens:

- Clear, neutral sans-serif stack suited to Chinese and English UI copy
- Smaller, more functional headings than the current dashboard style
- Tightened spacing between title, description, and toolbars
- Strong weight reserved for titles, section headers, active items, and key numbers
- Secondary copy should support context, not dominate the page

### Radius

Reduce roundness across the system.

- Major panels/cards: `8px`
- Inputs, selects, buttons, filter chips: `6px`
- Badges/status pills: modest rounding only; avoid oversized capsules where not required

### Shadow and Depth

Depth should be subtle and rare.

- Use borders first
- Use background contrast second
- Use very light shadow only on major panels or overlays
- Remove hover-lift transforms and floating-card motion

### Motion

Motion should be minimal and functional.

Allowed:

- hover background transitions
- border-color transitions
- active-row highlighting
- loading indicators

Avoid:

- translate hover motion
- dramatic panel lifts
- decorative card animation
- animated gradient or glow treatments

## Global Layout Architecture

The app uses a standard admin shell with four layers.

### Layer 1: Application Frame

A full-height desktop frame with:

- fixed or visually stable left navigation column
- top utility bar spanning the main content column
- main workspace under the utility bar

### Layer 2: Left Primary Navigation

The left column is the main module switcher.

Requirements:

- grouped module sections remain, because the current information architecture already maps well to domain responsibilities
- active state must be obvious through background, text weight, and a restrained accent treatment
- expanded/collapsed groups remain supported
- the visual form changes from card-like blocks to a disciplined navigation rail/list
- the nav header should identify the console clearly, without promotional taglines

### Layer 3: Top Utility Bar

The top bar is a utility surface, not a hero banner.

It should contain page-global controls such as:

- page title context or breadcrumb support when needed
- primary and secondary actions for the current page
- optional status chip, environment indicator, or quick context

It should not use dark-to-light gradients or glowing effects. It should feel like a tool chrome.

### Layer 4: Page Workspace

Inside the main content area, every page uses a consistent composition pattern:

1. title/context block
2. page action group
3. optional filters/search/segmented controls
4. main body
5. optional side context panel

## Canonical Page Templates

### Template A: Overview Dashboard

Use for:

- governance dashboard
- any future module landing page that summarizes health, recent records, and quick entry points

Structure:

1. compact title and description
2. page actions on the same row
3. 3-4 summary KPI cards with restrained styling
4. main two-column body:
   - left: recent records, trends, active queues, pending items
   - right: status panels, checklist summaries, alerts, quick access modules
5. cards should read as information containers, not clickable promo tiles

This template keeps overview value without turning the screen into a showcase dashboard.

### Template B: Management Workbench

Use for:

- replay center
- eval center
- release center
- diagnostics center
- launch center
- user management
- activity review
- task review
- API management
- market products management
- notification publishing where applicable

Structure:

1. title/context row with primary actions
2. filter and toolbar row under the title
3. main split area:
   - primary column: table/list/form body
   - secondary column: details, summary, side notes, status, or helper information
4. empty/loading/error states embedded into the work area rather than detached showcase blocks

### Template C: Form-Centered Editor

Use for pages that are mostly configuration entry or publishing.

Structure:

1. title row + status/meta
2. action bar
3. left main form column
4. right side summary/preview/checklist column

The form shell still inherits the same panel treatment as Template B.

## Module-Specific Behavior

### Governance Dashboard

The current page should move away from large glossy cards and quick-link tiles. It should become the system's cleanest overview screen.

Expected composition:

- compact KPI row
- recent trend / recent records on the left
- health summaries, integration readiness, and alerts on the right
- quick entry points presented as concise operational shortcuts, not feature showcase tiles

### Launch Center

This page should stop reading like a demo lab and instead read like a launch workbench.

Expected composition:

- summary status row near the top
- grouped action panels for smoke, drill, checklist, and signoff
- records/timeline in a denser workbench area
- exported artifacts and runbook references handled as admin panels, not display blocks

### Remaining Management Pages

All remaining pages should converge on the same workbench language:

- filter bar first
- list/table/form second
- supporting metadata in side panels or sub-panels
- one shared set of buttons, badges, section headers, and empty states

## Component Model

The redesign should consolidate the UI around a small number of reusable admin building blocks.

Required shared building blocks:

- admin shell frame
- navigation group and navigation item styles
- top utility bar
- page header
- toolbar/filter row
- summary metric card
- standard panel container
- standard list/table row container
- form section container
- semantic badge/status chip
- loading, empty, and error state blocks

The visual language for these blocks must be defined once and reused everywhere.

## Data Flow and State Boundaries

This redesign does not change the business data flow. The intended architecture remains:

- route selects page component
- page component loads data through the existing API layer
- page maps backend data to presentational sub-sections
- local UI state handles filter state, expanded sections, and transient feedback

Design implication:

- API modules such as `adminGovernance.ts` and `adminLaunch.ts` remain in place
- data contracts remain unchanged unless implementation uncovers a necessary frontend-only mapping improvement
- the redesign focuses on view composition and style architecture, not data ownership changes

## Error, Empty, and Loading Behavior

All pages should use one coherent operational pattern.

### Loading

- inline loading states near the data region they affect
- buttons may show local busy states
- no dramatic skeleton theatrics required, but loading feedback must be obvious

### Empty

- empty states should be concise and task-oriented
- they should explain what is missing and what the user can do next
- they should not look like marketing placeholders

### Error

- errors should appear in a standard alert/panel style
- error copy should describe the failed area in admin language
- retry actions should stay close to the failing section

## Copy and Tone

UI copy should move toward a neutral operations tone.

Use language that feels like:

- status
- record
- review
- configuration
- pending action
- recent activity
- sync state
- publish status

Avoid language that feels like:

- feature showcase
- product marketing
- visionary system branding
- decorative slogans

If implementation encounters inconsistent or garbled labels, normalize them into clear business-admin wording while preserving domain meaning.

## Accessibility and Interaction

Even though the app is currently desktop-first, the redesign must preserve baseline accessibility quality.

Requirements:

- visible keyboard focus states
- strong text contrast on light surfaces
- action buttons remain clearly distinguishable by hierarchy
- active nav and selected states cannot rely on color alone
- tables/lists/forms maintain readable row spacing at compact density

## Technical Design Scope

The redesign should be implemented entirely inside the frontend application.

Expected affected areas:

- global shell and root frame
- global design tokens / shared CSS
- shared admin panels, cards, headers, and action styles
- page-level layout restructuring in representative overview and workbench pages
- remaining pages migrated onto the same design language

The redesign should avoid introducing a full external UI framework at this stage unless a later planning step proves it is necessary. The existing Vue + Vite stack is sufficient for this work.

## Testing Strategy

The implementation plan should validate the redesign at three levels.

### Visual/System Validation

- no gradients remain in the admin shell or page surfaces
- radius values are visibly tighter and consistent
- hover motion is functional and restrained
- all pages share the same panel, toolbar, and state treatment

### Functional Regression Validation

- existing routes still render
- current page actions continue to work
- API-driven content still loads through existing modules
- page-local refresh, copy, export, submit, and toggle operations remain intact

### Layout Validation

Desktop widths to verify at minimum:

- 1280px
- 1440px
- 1600px+

Success criteria:

- navigation remains stable
- toolbars do not wrap awkwardly under normal desktop widths
- dense pages remain readable
- list/form + side-panel layouts stay balanced

## Risks and Mitigations

### Risk: Inconsistent Page Migration

If pages are restyled one by one without a shared system first, the result will still feel fragmented.

Mitigation:

- establish shell, tokens, and shared containers first
- migrate representative templates before the long tail

### Risk: Overcorrecting Into a Harsh Ops Console

A severe monitor-room style would fix the "AI" problem but overshoot the user's request for simplicity.

Mitigation:

- keep the palette light and neutral
- keep spacing compact but not cramped
- preserve enough whitespace for legibility

### Risk: Overview Pages Becoming Too Sparse or Too Busy

If overview cards dominate, the app still feels showcase-driven. If they disappear entirely, the console loses orientation value.

Mitigation:

- keep a small KPI row only
- move core screen real estate to records, work queues, and operational summaries

## Acceptance Criteria

The redesign is complete when:

1. The app reads visually as a coherent internal admin console.
2. No gradients remain in the main shell or page panels.
3. Card and panel radii are restrained and consistent.
4. The shell is standardized around left navigation and top utility bar.
5. Overview pages balance metrics with recent records and action-oriented summaries.
6. Inner pages prioritize filters, lists, tables, and forms.
7. Governance, launch, and secondary management pages feel like one product.
8. Existing business capabilities remain available through the same route coverage.

## Implementation Readiness

This redesign is scoped as one frontend presentation project, not multiple unrelated sub-projects. The next step should be an implementation plan that:

- establishes shared shell/tokens/components first
- migrates the overview template and workbench template next
- then applies the unified system across the remaining pages in a controlled sequence
