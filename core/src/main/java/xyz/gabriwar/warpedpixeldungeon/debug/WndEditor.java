package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic reflection-based property editor (dev tool, see {@link SpriteInspector}).
 *
 * Lists every instance field of the target, one button per field:
 * - boolean: click toggles
 * - enum: click cycles through the constants
 * - number/char/String: click opens a text input, parsed by field type
 * - object/collection: click drills down into a new editor window
 *
 * Editing a Collection lists its elements; elements of a List with
 * primitive-ish values can be edited in place, others drill down.
 */
public class WndEditor extends Window {

	private static final int ROW_HEIGHT = 10;

	private final Object target;

	public WndEditor(Object target) {
		super();

		this.target = target;

		int ww = Math.min(220, PixelScene.uiCamera.width - 10);
		int hh = Math.min(180, PixelScene.uiCamera.height - 18);
		resize(ww, hh);

		String tag = InspectorNotes.tag(target);
		RenderedTextBlock title = PixelScene.renderTextBlock(
				"edit: " + WndInspector.simpleName(target.getClass())
						+ " @" + Integer.toHexString(System.identityHashCode(target))
						+ (tag != null ? " \"" + tag + "\"" : ""), 6);
		title.hardlight(TITLE_COLOR);
		title.maxWidth(ww);
		title.setPos(0, 0);
		add(title);

		//pane before rows: pointer events go newest-listener-first, rows must register after it
		ScrollPane pane = new ScrollPane(new Component());
		add(pane);
		Component content = pane.content();

		float y = 0;
		if (target instanceof Collection) {
			y = buildElementRows(content, (Collection<?>) target, ww);
		} else {
			y = buildFieldRows(content, ww);
		}

		content.setSize(ww - 3, y);
		pane.setRect(0, title.bottom() + 2, ww, hh - (title.bottom() + 2));
	}

	//=== rows for a plain object: one per instance field ===

	private float buildFieldRows(Component content, int ww) {
		float y = 0;
		Class<?> c = target.getClass();
		while (c != null && c != Object.class) {
			for (Field fld : c.getDeclaredFields()) {
				if (Modifier.isStatic(fld.getModifiers()) || fld.isSynthetic()) continue;
				fld.setAccessible(true);

				final Field f = fld;
				RedButton row = new RedButton(fieldLabel(f), 6) {
					//don't swallow presses, so the scroll pane can still be dragged over the rows
					{ hotArea.blockLevel = PointerArea.NEVER_BLOCK; }
					@Override
					protected void onClick() {
						editField(f, this);
					}
				};
				row.setRect(0, y, ww - 3, ROW_HEIGHT);
				content.add(row);
				y += ROW_HEIGHT + 1;
			}
			c = c.getSuperclass();
		}
		return y;
	}

	private String fieldLabel(Field f) {
		String value;
		try {
			value = WndInspector.fmtValue(f.get(target));
		} catch (Throwable t) {
			value = "(inaccessible)";
		}
		String label = f.getName() + " = " + value;
		if (Modifier.isFinal(f.getModifiers())) label = "(f) " + label;
		return WndInspector.trunc(label, 42);
	}

	private void editField(final Field f, final RedButton row) {
		try {
			Class<?> t = f.getType();
			Object cur = f.get(target);

			if (t == boolean.class || t == Boolean.class) {
				f.set(target, !(Boolean) (cur == null ? Boolean.FALSE : cur));
				row.text(fieldLabel(f));

			} else if (t.isEnum()) {
				Object[] consts = t.getEnumConstants();
				int idx = 0;
				for (int i = 0; i < consts.length; i++) {
					if (consts[i] == cur) { idx = i; break; }
				}
				f.set(target, consts[(idx + 1) % consts.length]);
				row.text(fieldLabel(f));

			} else if (isTextEditable(t)) {
				String initial = cur == null ? "" : String.valueOf(cur);
				SpriteInspector.showWindow(new WndTextInput(
						f.getName() + " (" + t.getSimpleName() + ")", null, initial, 64, false, "set", "cancel") {
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive) {
							try {
								f.set(target, parse(f.getType(), text));
							} catch (Throwable t) {
								//bad input, leave the field as it was
							}
							row.text(fieldLabel(f));
						}
					}
				});

			} else if (cur != null) {
				//objects, collections, maps: drill down
				SpriteInspector.showWindow(new WndEditor(cur));
			}
		} catch (Throwable t) {
			//a debug tool must never crash the game
		}
	}

	//=== rows for a Collection: one per element ===

	private float buildElementRows(Component content, Collection<?> col, int ww) {
		final List<?> asList = col instanceof List ? (List<?>) col : new ArrayList<>(col);
		float y = 0;
		int cap = Math.min(asList.size(), 100);
		for (int i = 0; i < cap; i++) {
			final int idx = i;
			RedButton row = new RedButton(elementLabel(asList, idx), 6) {
				//don't swallow presses, so the scroll pane can still be dragged over the rows
				{ hotArea.blockLevel = PointerArea.NEVER_BLOCK; }
				@Override
				protected void onClick() {
					editElement(asList, idx, this);
				}
			};
			row.setRect(0, y, ww - 3, ROW_HEIGHT);
			content.add(row);
			y += ROW_HEIGHT + 1;
		}
		if (asList.size() > cap) y += 1;
		return y;
	}

	private String elementLabel(List<?> list, int idx) {
		return WndInspector.trunc("[" + idx + "] " + WndInspector.fmtValue(list.get(idx)), 42);
	}

	@SuppressWarnings("unchecked")
	private void editElement(final List<?> list, final int idx, final RedButton row) {
		try {
			final Object cur = list.get(idx);
			//in-place editing only works on the actual backing List
			if (target instanceof List && cur != null && isTextEditable(cur.getClass())) {
				SpriteInspector.showWindow(new WndTextInput(
						"[" + idx + "] (" + cur.getClass().getSimpleName() + ")", null,
						String.valueOf(cur), 64, false, "set", "cancel") {
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive) {
							try {
								((List<Object>) target).set(idx, parse(cur.getClass(), text));
							} catch (Throwable t) {
								//bad input, leave the element as it was
							}
							row.text(elementLabel(list, idx));
						}
					}
				});
			} else if (cur != null) {
				SpriteInspector.showWindow(new WndEditor(cur));
			}
		} catch (Throwable t) {
			//never crash
		}
	}

	//=== parsing ===

	private static boolean isTextEditable(Class<?> t) {
		return t == int.class || t == Integer.class
				|| t == long.class || t == Long.class
				|| t == short.class || t == Short.class
				|| t == byte.class || t == Byte.class
				|| t == float.class || t == Float.class
				|| t == double.class || t == Double.class
				|| t == char.class || t == Character.class
				|| t == String.class;
	}

	private static Object parse(Class<?> t, String s) {
		String v = s.trim();
		if (t == int.class || t == Integer.class) return Integer.parseInt(v);
		if (t == long.class || t == Long.class) return Long.parseLong(v);
		if (t == short.class || t == Short.class) return Short.parseShort(v);
		if (t == byte.class || t == Byte.class) return Byte.parseByte(v);
		if (t == float.class || t == Float.class) return Float.parseFloat(v);
		if (t == double.class || t == Double.class) return Double.parseDouble(v);
		if (t == char.class || t == Character.class) return v.isEmpty() ? ' ' : v.charAt(0);
		return s; //String
	}
}
