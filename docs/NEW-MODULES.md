# Zenith 2.1 modules and animations

The **Zenith Addons** category now contains ten optional visual and utility modules.
They start disabled and each has an editable accent color.

| Module | Behavior and controls |
| --- | --- |
| Zenith Jump Rings | Expanding takeoff and landing rings; adjustable radius, duration and landing toggle. Keeps at most eight rings. |
| Zenith Orbit Crown | Two rotating crescents and four stars above your head; radius, speed and first-person visibility controls. |
| Zenith Target Focus | Rotating segmented rings and a vertical sweep around your last attacked target. |
| Zenith Damage Numbers | Floating confirmed health-loss numbers on your attacked target; adjustable lifetime, at most 24 labels. |
| Zenith Viewmodel Motion | Breathing, walking sway and roll for held items; strength/roll controls and optional pause while using an item. |
| Zenith Crosshair Pulse | Vanilla attack-cooldown ring and a fading attack-input ripple; adjustable radius and cooldown toggle. |
| Zenith Low Health Glow | Smooth pulsing screen-edge warning with health threshold and opacity controls. |
| Zenith Armor Warning | Animated warning for equipped armor below the selected durability percentage. |
| Zenith Module Toasts | Sliding module enable/disable notifications; adjustable duration, at most four notifications. |
| Zenith Waypoint Beacon | Animated local position marker; captures your position on enable or uses manual coordinates. |

The overlays share one rendering pass. World markers use the existing depth-tested
geometry batch, distance/view checks and bounded segments. State clears when modules
are disabled or you leave the world. Viewmodel motion changes presentation only.

Under **GUI → Theme → Zenith 2.0 → Zenith**, use **Motion speed**, **Click pulses**,
**Header motion** and **Reduced motion** to control module-row and header animations.
Reduced motion makes switches immediate and suppresses the new click pulses.
