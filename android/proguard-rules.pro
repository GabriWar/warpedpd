# retain these to support class references for the bundling and translation systems
-keepnames class xyz.gabriwar.** { *; }
-keepnames class com.watabou.** { *; }

# keep classes that are instantiated via reflection
-keep class * extends com.watabou.noosa.Gizmo { *; }
-keep class * extends com.watabou.glscripts.Script { *; }
-keep class * implements com.watabou.utils.Bundlable { *; }
# the homesteads' registries load their providers and sections by class name
# (homestead/BuildCatalog, HomesteadStore, HomesteadWorld): nothing else references them
-keep class xyz.gabriwar.warpedpixeldungeon.homestead.** { *; }
# and the catching's, from outside that package: its run-wide record (HomesteadWorld.SECTIONS) and
# the world map's marks on the bottles' caches (MapMarks.PROVIDERS)
-keep class xyz.gabriwar.warpedpixeldungeon.items.catching.CatchWorld { *; }
-keep class xyz.gabriwar.warpedpixeldungeon.items.catching.BottleMarks { *; }
# and the bounties', also from outside it: the villages' boards laid on a slice (HomesteadStore.SECTIONS)
# and the run-wide record of notices and standing (HomesteadWorld.SECTIONS)
-keep class xyz.gabriwar.warpedpixeldungeon.bounty.BountySlice { *; }
-keep class xyz.gabriwar.warpedpixeldungeon.bounty.BountyWorld { *; }

# retained to support meaningful stack traces
# note that the mapping file must be referenced in order to make sense of line numbers
# mapping file can be found in core/build/outputs/mapping after running a release build
-keepattributes SourceFile,LineNumberTable

# libGDX stuff
-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*

# needed for libGDX skin reflection used in text fields. Perhaps just don't use skin?
-keep class com.badlogic.gdx.graphics.Color { *; }
-keep class com.badlogic.gdx.scenes.scene2d.ui.TextField$TextFieldStyle { *; }
-keepnames class com.badlogic.gdx.scenes.scene2d.ui.TextField { *; }

# needed for libGDX controllers
-keep class com.badlogic.gdx.controllers.android.AndroidControllers { *; }

-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
    <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}