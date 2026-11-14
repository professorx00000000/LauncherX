# Development checklist  (status: WRITTEN = code exists, VERIFIED = tested on a device)

| Feature | Status |
|---|---|
| Launcher activity + HOME intent filters | WRITTEN |
| Red Phantom + 5 other themes | WRITTEN |
| Glass components (Card, Button, SearchBar, Dock, IconContainer, Widget, Folder) | WRITTEN (translucent approximation, no true backdrop blur) |
| Workspace: drag, resize (3 handles), snap, lock, duplicate, remove w/ confirm, bounds clamp | WRITTEN |
| Multiple pages + indicators | WRITTEN |
| Persistence (DataStore + JSON, schema version, corrupt-config backup) | WRITTEN |
| Backup / restore (JSON export/import) | WRITTEN (photos not included) |
| Clock (digital/analog, 12/24h) | WRITTEN |
| Calendar (month view, no permission) | WRITTEN |
| System monitor (RAM, storage, battery, temp, model, cores) | WRITTEN |
| Screen Time (UsageStatsManager, 7-day chart, top apps, permission flow) | WRITTEN |
| Photos (Photo Picker, multiple, size presets, aspect, rotation, radius, border, opacity) | WRITTEN |
| Folders (resizable, dynamic icon grid, rename, add/remove apps) | WRITTEN |
| App drawer (search, favorites, recents, hide, columns) | WRITTEN |
| Dock (up to 5, add/remove, show/hide) | WRITTEN |
| Icon shapes (circle, rounded, square, squircle), labels on/off | WRITTEN |
| Wallpaper (custom image, dim, default red-glow) | WRITTEN |
| Settings sheet with live preview | WRITTEN |

## Not implemented yet (deliberate, see CHANGELOG)
Weather (needs provider/API decision), Music widget (needs notification-listener media session),
AppWidgetHost third-party widgets, Room database (DataStore+JSON used instead), calendar events,
icon packs, custom user themes, photo groups, parallax/blur wallpaper, animation-speed settings,
configurable gestures, custom grid sizes, dock size customization, font selection.

Nothing above is VERIFIED until built and tested on a device.
