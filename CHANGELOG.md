# DAYFLOW — CHANGELOG

This file records meaningful changes to DAYFLOW across development and releases.

It is intended to maintain a readable history of completed features, improvements, bug fixes, and technical changes.

---

## [0.6.0] — Initial GitHub Release

### Project

- Published the DAYFLOW source code on GitHub.
- Established the public GitHub repository.
- Updated the README with application information and installation instructions.

### Release

- Published GitHub Release v0.6.0.
- Attached the signed release APK.
- Established a permanent release signing identity for future updates.

### Documentation

- Added project installation and compatibility information to the README.

---

## [Unreleased]

Changes in development that have not yet been included in an official release.

This section should be updated as meaningful development work is completed.

### Added

- (Branch `feature/glass-redesign`) Glass design tokens and `GlassCard` / `Modifier.glass` components (built and installed by owner).
- (Branch `feature/glass-redesign`) Ambient glow backdrop and denser chrome glass variant (built and installed by owner).
- (Branch `feature/glass-redesign`) `GlassIconButton`; Home top section (overview, now/next, top icons, day-complete banner, review button) restyled with glass. Visual only (built and installed by owner).
- (Branch `feature/glass-redesign`, unverified) Liquid-glass refinement: cool-tinted translucent fills, diagonal edge highlight, internal sheen, lower-rim shade, outside-only soft shadow.

### Changed

- Light theme background shifted to a cooler neutral.



### Fixed

- (unverified) Sharp lighter inset rectangles and grey cast inside glass cards and round buttons, caused by the platform elevation shadow under translucent fills; replaced with a drawn outside-only shadow.

### Removed

- Nothing documented yet.

---

## Changelog Guidelines

- Record meaningful completed changes.
- Keep entries concise and understandable.
- Do not document unfinished work as completed.
- Update the Unreleased section during development.
- When preparing a new release, move completed changes into the corresponding version section.
- Keep version numbers consistent with the actual Android application version.
- Do not duplicate the entire development checkpoint here.
