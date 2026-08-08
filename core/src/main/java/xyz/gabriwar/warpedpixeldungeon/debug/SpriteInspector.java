package xyz.gabriwar.warpedpixeldungeon.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.WndMessage;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Scene;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Dev-only overlay inspector. Active only when DeviceCompat.isDebug() is true
 * (version contains INDEV, e.g. running via gradlew desktop:debug).
 *
 * Top of the screen shows a live cursor readout plus an [inspect] button:
 * click it, then click/tap anything on screen - that click is consumed (the game
 * never sees it) and the inspector opens. On desktop, hovering + F12 also works.
 *
 * Every visible Visual/Component under the point is listed, topmost first, so
 * overlapping sprites are all individually selectable. The current selection
 * stays outlined (cyan + center cross) and is shown as a chip with its icon next
 * to the inspect button; clicking the chip reopens its info, [x] clears it.
 *
 * From the inspector window you can edit properties (reflection based, see
 * {@link WndEditor}) of the selected sprite, or of the mob/heap/plant/trap on
 * the inspected cell.
 *
 * Dev notes: seed class notes in {@link InspectorNotes} or tag single instances
 * with {@code InspectorNotes.tag(obj, "name")}.
 *
 * Hooked from PixelScene.update().
 */
public class SpriteInspector {

	public static final int HOTKEY = Input.Keys.F12;

	private static final int COLOR_SELECTED = 0xFF00E0FF;
	private static final int COLOR_HOVER    = 0xFFFFEE33;

	private static Scene lastScene;

	private static Highlight selectedHL;
	private static Highlight hoverHL;

	private static BitmapText cursorHUD;
	private static String cursorHUDText = "";

	private static RedButton inspectBtn;
	private static boolean lastArmed = false;
	private static RedButton selChip;
	private static RedButton clearBtn;
	private static Gizmo chipTarget;
	private static PointerArea armedBlocker;

	private static WndInspector window;

	private static Field membersField;
	static {
		try {
			membersField = Group.class.getDeclaredField("members");
			membersField.setAccessible(true);
		} catch (Exception e) {
			membersField = null;
		}
	}

	private SpriteInspector() {}

	//called every frame from PixelScene.update() when in debug mode.
	//only actually shows anything while the "inspector" toggle (settings > UI, debug builds
	// only) is enabled
	public static void update() {
		Scene scene = Game.scene();
		if (scene == null || PixelScene.uiCamera == null) return;

		if (!WPDSettings.inspector()) {
			teardown();
			return;
		}

		if (scene != lastScene) {
			//old scene destroyed our gizmos with it, just drop the references
			lastScene = scene;
			selectedHL = hoverHL = null;
			cursorHUD = null;
			cursorHUDText = "";
			inspectBtn = selChip = clearBtn = null;
			chipTarget = null;
			armedBlocker = null;
			lastArmed = false;
			window = null;
		}

		if (window != null && window.parent == null) window = null;
		if (armedBlocker != null && armedBlocker.parent == null) armedBlocker = null;

		//drop dead selections
		Gizmo sel = selected();
		if (sel != null && (!sel.exists || sel.parent == null)) select(null);

		updateCursorHUD(scene);
		updateTopButtons(scene);

		if (selectedHL != null) selectedHL.updatePos();
		if (hoverHL != null) hoverHL.updatePos();

		if (Gdx.input.isKeyJustPressed(HOTKEY)) {
			disarm();
			inspect(Gdx.input.getX(), Gdx.input.getY());
		}
	}

	static Gizmo selected() {
		return selectedHL == null ? null : selectedHL.target;
	}

	//remove every overlay artifact, used when the inspector setting is turned off
	private static void teardown() {
		disarm();
		if (window != null && window.parent != null) window.hide();
		window = null;
		remove(selectedHL);
		remove(hoverHL);
		remove(cursorHUD);
		remove(inspectBtn);
		remove(selChip);
		remove(clearBtn);
		selectedHL = hoverHL = null;
		cursorHUD = null;
		cursorHUDText = "";
		inspectBtn = selChip = clearBtn = null;
		chipTarget = null;
		lastArmed = false;
		lastScene = null;
	}

	private static void remove(Gizmo g) {
		if (g != null && g.parent != null) {
			g.killAndErase();
			g.destroy();
		}
	}

	public static void inspect(int screenX, int screenY) {
		Scene scene = Game.scene();
		if (scene == null) return;

		if (window != null && window.parent != null) {
			window.hide();
		}
		window = null;

		ArrayList<Gizmo> hits = new ArrayList<>();
		collect(scene, screenX, screenY, hits);
		Collections.reverse(hits); //draw order -> topmost first

		if (hits.isEmpty()) {
			showWindow(new WndMessage("Inspector: nothing visible under screen (" + screenX + ", " + screenY + ")."));
			return;
		}

		ensureHighlights();
		window = new WndInspector(hits, screenX, screenY);
		showWindow(window);
	}

	//reopen the info window for a single known target (used by the selection chip)
	static void inspectTarget(Gizmo g) {
		Scene scene = Game.scene();
		if (scene == null || g == null || g.parent == null) return;

		if (window != null && window.parent != null) {
			window.hide();
		}
		window = null;

		ensureHighlights();
		select(g);

		int cx = 0, cy = 0;
		RectF r = screenRectOf(g);
		if (r != null) {
			cx = Math.round((r.left + r.right) / 2f);
			cy = Math.round((r.top + r.bottom) / 2f);
		}

		ArrayList<Gizmo> single = new ArrayList<>();
		single.add(g);
		window = new WndInspector(single, cx, cy);
		showWindow(window);
	}

	static void showWindow(Window w) {
		if (Game.scene() instanceof GameScene) {
			GameScene.show(w);
		} else if (Game.scene() != null) {
			Game.scene().add(w);
		}
	}

	//=== inspect button / armed click ===

	private static boolean armed() {
		return armedBlocker != null && armedBlocker.parent != null;
	}

	private static void arm() {
		Scene scene = Game.scene();
		if (scene == null || armed()) return;

		armedBlocker = new PointerArea(0, 0, PixelScene.uiCamera.width, PixelScene.uiCamera.height) {
			@Override
			protected void onClick(PointerEvent event) {
				int x = (int) event.current.x;
				int y = (int) event.current.y;
				disarm();
				//clicking the inspect button itself just cancels
				if (inspectBtn == null || !hitTest(inspectBtn, x, y)) {
					inspect(x, y);
				}
			}
		};
		armedBlocker.camera = PixelScene.uiCamera;
		armedBlocker.blockLevel = PointerArea.ALWAYS_BLOCK;
		scene.add(armedBlocker);
	}

	private static void disarm() {
		if (armedBlocker != null) {
			armedBlocker.killAndErase();
			armedBlocker.destroy();
			armedBlocker = null;
		}
	}

	private static void updateTopButtons(Scene scene) {
		if (inspectBtn == null || inspectBtn.parent == null) {
			inspectBtn = new RedButton("inspect", 6) {
				@Override
				protected void onClick() {
					if (armed()) disarm();
					else arm();
				}
			};
			inspectBtn.camera = PixelScene.uiCamera;
			scene.add(inspectBtn);
			lastArmed = false;
			layoutTopButtons();
		}

		if (lastArmed != armed()) {
			lastArmed = armed();
			inspectBtn.text(lastArmed ? "click it!" : "inspect");
		}

		//selection chip with the target's own image, plus a clear button.
		//not rebuilt while an inspector window is open: recreating it would register its
		// pointer listener above the window's, stealing clicks wherever they overlap
		Gizmo sel = selected();
		if (window != null && window.parent != null) return;
		if (sel != chipTarget || (sel != null && (selChip == null || selChip.parent == null))) {
			chipTarget = sel;
			if (selChip != null) {
				selChip.killAndErase();
				selChip.destroy();
				selChip = null;
			}
			if (clearBtn != null) {
				clearBtn.killAndErase();
				clearBtn.destroy();
				clearBtn = null;
			}
			if (sel != null) {
				String name = InspectorNotes.tag(sel);
				if (name == null) name = WndInspector.simpleName(sel.getClass());
				if (name.length() > 10) name = name.substring(0, 10);

				selChip = new RedButton(name, 6) {
					@Override
					protected void onClick() {
						inspectTarget(chipTarget);
					}
				};
				if (sel instanceof Image && ((Image) sel).texture != null) {
					Image ic = new Image((Image) sel);
					float s = Math.min(1f, 9f / Math.max(1f, Math.max(ic.width, ic.height)));
					ic.scale.set(s);
					selChip.icon(ic);
				}
				selChip.camera = PixelScene.uiCamera;
				scene.add(selChip);

				clearBtn = new RedButton("x", 6) {
					@Override
					protected void onClick() {
						select(null);
					}
				};
				clearBtn.camera = PixelScene.uiCamera;
				scene.add(clearBtn);
			}
			layoutTopButtons();
		}

		scene.bringToFront(inspectBtn);
		if (selChip != null) scene.bringToFront(selChip);
		if (clearBtn != null) scene.bringToFront(clearBtn);
		if (cursorHUD != null) scene.bringToFront(cursorHUD);
	}

	private static void layoutTopButtons() {
		Camera ui = PixelScene.uiCamera;
		float total = 38 + (selChip != null ? 2 + 60 + 2 + 12 : 0);
		float x = PixelScene.align((ui.width - total) / 2f);
		inspectBtn.setRect(x, 8, 38, 12);
		if (selChip != null) {
			selChip.setRect(inspectBtn.right() + 2, 8, 60, 12);
			clearBtn.setRect(selChip.right() + 2, 8, 12, 12);
		}
	}

	//=== hit testing ===

	private static void collect(Gizmo g, int sx, int sy, ArrayList<Gizmo> out) {
		if (g == null || !g.exists || !g.visible) return;
		//never inspect the inspector's own artifacts
		if (g == selectedHL || g == hoverHL || g == cursorHUD || g == armedBlocker
				|| g == inspectBtn || g == selChip || g == clearBtn
				|| g instanceof WndInspector || g instanceof WndEditor) return;

		if (g instanceof Group) {
			if (g instanceof Component && hitTest(g, sx, sy)) out.add(g);
			ArrayList<Gizmo> members = membersOf((Group) g);
			if (members != null) {
				for (int i = 0; i < members.size(); i++) {
					collect(members.get(i), sx, sy, out);
				}
			}
		} else if (g instanceof Visual) {
			if (hitTest(g, sx, sy)) out.add(g);
		}
	}

	static boolean hitTest(Gizmo g, int sx, int sy) {
		Camera c = g.camera();
		if (c == null || !c.hitTest(sx, sy)) return false;
		PointF p = c.screenToCamera(sx, sy);
		if (g instanceof Visual) {
			RectF b = visualBounds((Visual) g);
			return p.x >= b.left && p.x < b.right && p.y >= b.top && p.y < b.bottom;
		} else {
			return ((Component) g).inside(p.x, p.y);
		}
	}

	//camera-space AABB, replicating Visual.updateMatrix (translate, then rotate+scale about origin)
	static RectF visualBounds(Visual v) {
		float w = v.width, h = v.height;
		float ox = v.origin.x, oy = v.origin.y;
		float cos = (float) Math.cos(Math.toRadians(v.angle));
		float sin = (float) Math.sin(Math.toRadians(v.angle));

		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

		float[] cxs = {0, w, 0, w};
		float[] cys = {0, 0, h, h};
		for (int i = 0; i < 4; i++) {
			float lx = v.scale.x * (cxs[i] - ox);
			float ly = v.scale.y * (cys[i] - oy);
			float px = v.x + ox + cos * lx - sin * ly;
			float py = v.y + oy + sin * lx + cos * ly;
			if (px < minX) minX = px;
			if (px > maxX) maxX = px;
			if (py < minY) minY = py;
			if (py > maxY) maxY = py;
		}
		return new RectF(minX, minY, maxX, maxY);
	}

	//camera-space AABB of any inspectable gizmo, or null
	static RectF camBounds(Gizmo g) {
		if (g instanceof Visual) {
			return visualBounds((Visual) g);
		} else if (g instanceof Component) {
			Component c = (Component) g;
			return new RectF(c.left(), c.top(), c.right(), c.bottom());
		}
		return null;
	}

	//screen-space (real input px) rect of any inspectable gizmo, or null
	static RectF screenRectOf(Gizmo g) {
		Camera c = g.camera();
		RectF b = camBounds(g);
		if (c == null || b == null) return null;
		return new RectF(
				c.x + (b.left   - c.scroll.x) * c.zoom,
				c.y + (b.top    - c.scroll.y) * c.zoom,
				c.x + (b.right  - c.scroll.x) * c.zoom,
				c.y + (b.bottom - c.scroll.y) * c.zoom);
	}

	@SuppressWarnings("unchecked")
	static ArrayList<Gizmo> membersOf(Group g) {
		if (membersField == null) return null;
		try {
			return (ArrayList<Gizmo>) membersField.get(g);
		} catch (Exception e) {
			return null;
		}
	}

	//=== highlights ===

	private static void ensureHighlights() {
		Scene scene = Game.scene();
		if (scene == null) return;
		if (selectedHL == null || selectedHL.parent == null) {
			selectedHL = new Highlight(COLOR_SELECTED);
			scene.add(selectedHL);
		}
		if (hoverHL == null || hoverHL.parent == null) {
			hoverHL = new Highlight(COLOR_HOVER);
			scene.add(hoverHL);
		}
	}

	static void select(Gizmo g) {
		if (g != null) ensureHighlights();
		if (selectedHL != null) selectedHL.target = g;
	}

	static void hover(Gizmo g) {
		if (hoverHL != null) hoverHL.target = g;
	}

	static void windowClosed(WndInspector w) {
		if (window == w) window = null;
		//selection (and its outline) intentionally survives closing the window
		hover(null);
	}

	//outline + center cross drawn in ui camera space over the target's screen rect
	private static class Highlight extends Group {

		Gizmo target;

		private final ColorBlock top, bottom, left, right, crossH, crossV;

		Highlight(int color) {
			camera = PixelScene.uiCamera;
			top    = block(color);
			bottom = block(color);
			left   = block(color);
			right  = block(color);
			crossH = block(color);
			crossV = block(color);
			crossH.am = crossV.am = 0.75f;
			visible = false;
		}

		private ColorBlock block(int color) {
			ColorBlock b = new ColorBlock(1, 1, color);
			add(b);
			return b;
		}

		void updatePos() {
			if (target == null || !target.exists || target.parent == null) {
				visible = false;
				return;
			}
			RectF r = screenRectOf(target);
			Camera ui = PixelScene.uiCamera;
			if (r == null || ui == null) {
				visible = false;
				return;
			}

			//screen px -> ui camera units
			float l = (r.left  - ui.x) / ui.zoom + ui.scroll.x;
			float t = (r.top   - ui.y) / ui.zoom + ui.scroll.y;
			float w = Math.max(r.width()  / ui.zoom, 0.5f);
			float h = Math.max(r.height() / ui.zoom, 0.5f);

			float th = 1f / ui.zoom; //1 device px

			place(top,    l, t, w, th);
			place(bottom, l, t + h - th, w, th);
			place(left,   l, t + th, th, Math.max(h - 2 * th, 0));
			place(right,  l + w - th, t + th, th, Math.max(h - 2 * th, 0));

			float cx = l + w / 2f, cy = t + h / 2f;
			float arm = 3 * th;
			place(crossH, cx - arm, cy - th / 2f, 2 * arm, th);
			place(crossV, cx - th / 2f, cy - arm, th, 2 * arm);

			visible = true;
		}

		private void place(ColorBlock b, float x, float y, float w, float h) {
			b.x = x;
			b.y = y;
			b.size(w, h);
		}
	}

	//=== cursor position HUD (top center of the screen) ===

	private static void updateCursorHUD(Scene scene) {
		if (PixelScene.pixelFont == null) return;

		if (cursorHUD == null || cursorHUD.parent == null) {
			cursorHUD = new BitmapText(PixelScene.pixelFont);
			cursorHUD.camera = PixelScene.uiCamera;
			cursorHUD.hardlight(0x88CCFF);
			cursorHUD.alpha(0.85f);
			cursorHUDText = "";
			scene.add(cursorHUD);
		}

		int sx = Gdx.input.getX();
		int sy = Gdx.input.getY();

		Camera ui = PixelScene.uiCamera;
		PointF up = ui.screenToCamera(sx, sy);
		StringBuilder s = new StringBuilder();
		s.append(sx).append(",").append(sy).append("scr ")
				.append(f1(up.x)).append(",").append(f1(up.y)).append("ui");

		if (Camera.main != null && Camera.main != ui) {
			PointF wp = Camera.main.screenToCamera(sx, sy);
			s.append(" ").append(f1(wp.x)).append(",").append(f1(wp.y)).append("cam");
			if (Dungeon.level != null && scene instanceof GameScene) {
				int col = (int) Math.floor(wp.x / DungeonTilemap.SIZE);
				int row = (int) Math.floor(wp.y / DungeonTilemap.SIZE);
				s.append(" tile:").append(col).append(",").append(row)
						.append("#").append(col + row * Dungeon.level.width());
			}
		}

		String str = s.toString();
		if (!str.equals(cursorHUDText)) {
			cursorHUDText = str;
			cursorHUD.text(str);
			cursorHUD.measure();
			cursorHUD.x = PixelScene.align((ui.width - cursorHUD.width()) / 2f);
			cursorHUD.y = 1;
		}
	}

	private static String f1(float v) {
		return String.valueOf(Math.round(v * 10) / 10f);
	}
}
