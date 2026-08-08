package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dev notes shown by {@link SpriteInspector}.
 *
 * Two kinds:
 * - class notes: {@code note(SomeSprite.class, "anchoring facts...")} - applies to every
 *   instance, inherited notes from superclasses are shown too. Seed them in the static
 *   block below so they persist in source.
 * - instance tags: {@code InspectorNotes.tag(image, "the moon")} - names one specific
 *   object so it is identifiable in the inspector's pick list. Weakly held.
 */
public class InspectorNotes {

	private static final HashMap<Class<?>, String> NOTES = new HashMap<>();
	private static final Map<Object, String> TAGS = Collections.synchronizedMap(new WeakHashMap<Object, String>());

	private InspectorNotes() {}

	public static void note(Class<?> cls, String note) {
		NOTES.put(cls, note);
	}

	//returns the object so it can be tagged inline: add(InspectorNotes.tag(new Image(...), "moon"))
	public static <T> T tag(T obj, String tag) {
		if (obj != null) TAGS.put(obj, tag);
		return obj;
	}

	public static String tag(Object obj) {
		return obj == null ? null : TAGS.get(obj);
	}

	//all class notes that apply to this object, most specific class first
	public static ArrayList<String> notesFor(Object obj) {
		ArrayList<String> out = new ArrayList<>();
		if (obj == null) return out;
		Class<?> c = obj.getClass();
		while (c != null && c != Object.class) {
			String n = NOTES.get(c);
			if (n != null) out.add(c.getSimpleName() + ": " + n);
			c = c.getSuperclass();
		}
		return out;
	}

	static {
		note(Visual.class,
				"coords are ABSOLUTE in camera space, parents add no offset. " +
				"scale/rotation pivot at origin (default 0,0 = top-left). width()/height() include scale.");
		note(Component.class,
				"layout container. children are positioned absolutely in layout(). " +
				"move with setPos/setRect, never the fields.");
		note(Image.class,
				"width/height come from frame() in texture px, scale multiplies on top.");
		note(ColorBlock.class,
				"size lives in scale, the width/height fields stay 1 (1x1 solid texture).");
		note(CharSprite.class,
				"x,y is top-left. worldToCamera(cell) centers x on the tile and rests the sprite bottom " +
				"on the cell bottom, raised by tileSize*perspectiveRaise. place(cell) applies it.");
		note(ItemSprite.class,
				"heap items bob via place()/worldToCamera, centered on the tile.");
		note(RenderedTextBlock.class,
				"glyphs are pre-zoomed by 1/defaultZoom. PixelScene.align() after setPos or text blurs.");
		note(Window.class,
				"owns a centered camera. (0,0) is content top-left inside the chrome. offset() shifts the whole window.");
	}
}
