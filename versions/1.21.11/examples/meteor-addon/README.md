# Build your own addon

This is a separate Fabric mod using the standard `MeteorAddon` entrypoint,
`Module`, `Category`, setting builders and Orbit events. It works in both GUI modes.

1. Build the client from the Zenith source root using JDK 25: `gradlew.bat build`.
2. Open Command Prompt in `examples\meteor-addon` and run `gradlew.bat build`.
3. Put `build\libs\zenith-example-addon-1.0.0.jar` beside the Zenith client JAR in
   your Minecraft 1.21.11 Fabric profile's `mods` folder.
4. Open Right Shift. **Example Addon → Example Counter** exposes a toggle and slider.

Linux: `bash gradlew build`. The launchers reuse the root Gradle wrapper. The
project uses the built client JAR as a compile-only API; it never bundles Zenith.
You can override its location with `-PzenithJar=C:\path\to\client.jar`.

To make another addon, rename the mod ID, package, category and entrypoint, update
`getPackage()`, and register your modules in `onInitialize()`. Put all your classes
under that package so Meteor can attribute modules and register event handlers.
Add your own authors and retain the applicable source licenses.

Existing addons must target Minecraft 1.21.11 and compatible Meteor APIs. Addons for
other Minecraft versions or ones that mix into changed internals need adaptation.
The visual theme does not rewrite addon code or bypass Fabric dependency checks.
Install Zenith instead of a separate Meteor Client JAR, since both use `meteor-client`.
