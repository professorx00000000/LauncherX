# Changelog
## 1.0.0 (initial source drop)
- Project foundation, manifest (HOME + LAUNCHER), Gradle version catalog, GitHub Actions debug build.
- Design system: 6 themes, glass components.
- Workspace engine, widgets (clock, calendar, system monitor, screen time), photos, folders, drawer, dock, settings, backup.
- Decision: DataStore + versioned JSON instead of Room (fewer moving parts to build on a phone; Room can replace it later behind StateStore).
- Not compiled in the authoring environment (no Android SDK/network). Expect first-build fixes.
