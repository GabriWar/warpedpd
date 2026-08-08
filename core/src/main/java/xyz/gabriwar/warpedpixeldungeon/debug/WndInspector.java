package xyz.gabriwar.warpedpixeldungeon.debug;

import com.badlogic.gdx.Gdx;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Rect;
import com.watabou.utils.RectF;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Pick list (topmost first) of everything under the inspected point, plus a full
 * info dump of the selected entry. Hovering a row highlights it in yellow,
 * clicking selects it (cyan) and fills the info pane. Actions: copy the whole
 * dump to the clipboard, or open a {@link WndEditor} on the selected sprite or
 * on the mob/heap/plant/trap sitting on the inspected cell.
 * See {@link SpriteInspector}.
 */
public class WndInspector extends Window {

	private static final int ROW_HEIGHT = 11;

	private final ArrayList<Gizmo> targets;
	private final ArrayList<String> labels = new ArrayList<>();
	private final ArrayList<RedButton> rows = new ArrayList<>();

	private final RenderedTextBlock info;
	private final Component infoContent;
	private final ScrollPane listPane;
	private final ScrollPane infoPane;

	private int selected = -1;

	private final int screenX, screenY;

	public WndInspector(ArrayList<Gizmo> targets, int screenX, int screenY) {
		super();

		this.targets = targets;
		this.screenX = screenX;
		this.screenY = screenY;

		int ww = Math.min(220, PixelScene.uiCamera.width - 10);
		int hh = Math.min(180, PixelScene.uiCamera.height - 18);
		resize(ww, hh);

		//shift the window towards the half of the screen away from the inspected point, so the
		// inspected sprite stays visible. Must happen BEFORE the scroll panes are laid out, as
		// their content cameras are placed in screen space at layout time.
		int half = (PixelScene.uiCamera.height - camera.height) / 2;
		int maxDown = half - 1;
		int maxUp = half - 24; //never slide under the top overlay bar (HUD + inspect button)
		float uiY = (screenY - PixelScene.uiCamera.y) / PixelScene.uiCamera.zoom;
		if (uiY < PixelScene.uiCamera.height / 2f) {
			if (maxDown > 0) offset(0, maxDown);
		} else {
			if (maxUp > 0) offset(0, -maxUp);
		}

		RenderedTextBlock title = PixelScene.renderTextBlock(
				targets.size() + " under screen (" + screenX + ", " + screenY + "), top first:", 6);
		title.hardlight(TITLE_COLOR);
		title.maxWidth(ww);
		title.setPos(0, 0);
		add(title);

		float top = title.bottom() + 2;

		//IMPORTANT: the pane must exist before the row buttons are created - pointer events
		// are dispatched newest-listener-first, so buttons must register after the pane's
		// controller or it will swallow their clicks
		listPane = new ScrollPane(new Component());
		add(listPane);
		Component listContent = listPane.content();

		for (int i = 0; i < targets.size(); i++) {
			final int idx = i;
			String label = rowLabel(i, targets.get(i));
			labels.add(label);
			RedButton row = new RedButton(label, 6) {
				//don't swallow presses, so the scroll pane can still be dragged over the rows
				{ hotArea.blockLevel = PointerArea.NEVER_BLOCK; }
				@Override
				protected void onClick() {
					select(idx);
				}
			};
			row.setRect(0, i * (ROW_HEIGHT + 1), ww - 3, ROW_HEIGHT);
			listContent.add(row);
			rows.add(row);
		}
		listContent.setSize(ww - 3, targets.size() * (ROW_HEIGHT + 1));

		int listH = Math.min(4 * (ROW_HEIGHT + 1), targets.size() * (ROW_HEIGHT + 1));
		listPane.setRect(0, top, ww, listH);

		ColorBlock divider = new ColorBlock(ww, 1, 0xFF888888);
		divider.x = 0;
		divider.y = listPane.bottom() + 2;
		add(divider);

		float actionsY = divider.y + 2;
		float actionsBottom = buildActions(ww, actionsY);

		info = PixelScene.renderTextBlock(6);
		info.setHightlighting(false);
		infoPane = new ScrollPane(new Component());
		add(infoPane);
		infoContent = infoPane.content();
		infoContent.add(info);
		infoPane.setRect(0, actionsBottom + 2, ww, hh - (actionsBottom + 2));

		select(0);
	}

	//action buttons: copy dump, edit selected sprite, edit entities on the inspected cell
	private float buildActions(int ww, float y) {
		RedButton btnCopy = new RedButton("copy all", 6) {
			@Override
			protected void onClick() {
				copyAll();
				text("copied!");
			}
		};
		btnCopy.setRect(0, y, 44, 10);
		add(btnCopy);

		RedButton btnEditSprite = new RedButton("edit sprite", 6) {
			@Override
			protected void onClick() {
				if (selected >= 0) {
					SpriteInspector.showWindow(new WndEditor(targets.get(selected)));
				}
			}
		};
		btnEditSprite.setRect(btnCopy.right() + 2, y, 54, 10);
		add(btnEditSprite);

		float x = btnEditSprite.right() + 2;
		float bottom = y + 10;

		//entities on the inspected cell (world position of the click)
		int cell = cellAt(screenX, screenY);
		if (cell != -1) {
			ArrayList<Object> entities = new ArrayList<>();
			try {
				Char ch = Actor.findChar(cell);
				if (ch != null) entities.add(ch);
				if (Dungeon.level.heaps.get(cell) != null) entities.add(Dungeon.level.heaps.get(cell));
				if (Dungeon.level.plants.get(cell) != null) entities.add(Dungeon.level.plants.get(cell));
				if (Dungeon.level.traps.get(cell) != null) entities.add(Dungeon.level.traps.get(cell));
			} catch (Throwable t) {
				//no level info, skip
			}

			for (Object e : entities) {
				final Object entity = e;
				String nm = simpleName(e.getClass());
				if (nm.length() > 9) nm = nm.substring(0, 9);
				RedButton btn = new RedButton(nm, 6) {
					@Override
					protected void onClick() {
						SpriteInspector.showWindow(new WndEditor(entity));
					}
				};
				if (x + 40 > ww) { //wrap to a second row
					x = 0;
					y += 11;
					bottom = y + 10;
				}
				btn.setRect(x, y, 40, 10);
				add(btn);
				x += 42;
			}
		}

		return bottom;
	}

	//world cell under a screen point, or -1
	private static int cellAt(int sx, int sy) {
		try {
			if (Dungeon.level == null || Camera.main == null
					|| !(com.watabou.noosa.Game.scene() instanceof GameScene)) return -1;
			PointF wp = Camera.main.screenToCamera(sx, sy);
			int col = (int) Math.floor(wp.x / DungeonTilemap.SIZE);
			int row = (int) Math.floor(wp.y / DungeonTilemap.SIZE);
			if (col < 0 || col >= Dungeon.level.width() || row < 0 || row >= Dungeon.level.height()) return -1;
			return col + row * Dungeon.level.width();
		} catch (Throwable t) {
			return -1;
		}
	}

	//hovering a list row highlights its target in yellow (mouse only)
	@Override
	public synchronized void update() {
		super.update();
		PointF hp = PointerEvent.currentHoverPos();
		Gizmo hovered = null;
		for (int j = 0; j < rows.size(); j++) {
			if (SpriteInspector.hitTest(rows.get(j), (int) hp.x, (int) hp.y)) {
				hovered = targets.get(j);
				break;
			}
		}
		SpriteInspector.hover(hovered);
	}

	private void select(int i) {
		if (i < 0 || i >= targets.size()) return;
		if (selected != i) {
			for (int j = 0; j < rows.size(); j++) {
				rows.get(j).text((j == i ? "> " : "") + labels.get(j));
			}
		}
		selected = i;

		Gizmo t = targets.get(i);
		SpriteInspector.select(t);

		info.text(describe(t, i), width - 6);
		info.setPos(2, 1);
		infoContent.setSize(width, info.bottom() + 2);
		infoPane.scrollTo(0, 0);
	}

	//scroll pane content cameras are placed in screen space at layout time, so any
	// offset change after construction (e.g. GameScene.show inheriting offsets) must re-layout them
	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (listPane != null) {
			listPane.setRect(listPane.left(), listPane.top(), listPane.width(), listPane.height());
		}
		if (infoPane != null) {
			infoPane.setRect(infoPane.left(), infoPane.top(), infoPane.width(), infoPane.height());
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		SpriteInspector.windowClosed(this);
	}

	//full dump of every candidate to the system clipboard
	private void copyAll() {
		StringBuilder sb = new StringBuilder();
		sb.append(targets.size()).append(" under screen (").append(screenX).append(", ").append(screenY).append("), top first\n");
		for (int i = 0; i < targets.size(); i++) {
			sb.append("\n==== ").append(labels.get(i)).append(" ====\n");
			try {
				sb.append(describe(targets.get(i), i));
			} catch (Throwable t) {
				sb.append("(describe failed: ").append(t).append(")\n");
			}
		}
		try {
			Gdx.app.getClipboard().setContents(sb.toString());
		} catch (Throwable t) {
			//clipboard can be unavailable on some platforms, don't crash a debug tool over it
		}
	}

	//=== labels & info dump ===

	private static String rowLabel(int i, Gizmo g) {
		StringBuilder sb = new StringBuilder();
		sb.append("#").append(i + 1).append(" ").append(simpleName(g.getClass()));
		String tag = InspectorNotes.tag(g);
		if (tag != null) sb.append(" \"").append(tag).append("\"");
		RectF b = SpriteInspector.camBounds(g);
		if (b != null) sb.append(" ").append(Math.round(b.width())).append("x").append(Math.round(b.height()));
		if (g instanceof Visual && ((Visual) g).alpha() <= 0) sb.append(" [a=0]");
		return sb.toString();
	}

	private static String describe(Gizmo g, int idx) {
		StringBuilder sb = new StringBuilder();

		sb.append("#").append(idx + 1).append(" ").append(simpleName(g.getClass()))
				.append(" @").append(Integer.toHexString(System.identityHashCode(g))).append("\n");

		String tag = InspectorNotes.tag(g);
		if (tag != null) sb.append("tag: \"").append(tag).append("\"\n");

		try { classChain(sb, g); } catch (Throwable t) { err(sb, t); }
		try { path(sb, g); } catch (Throwable t) { err(sb, t); }
		try { geometry(sb, g); } catch (Throwable t) { err(sb, t); }
		try { align(sb, g); } catch (Throwable t) { err(sb, t); }
		try { state(sb, g); } catch (Throwable t) { err(sb, t); }
		try { image(sb, g); } catch (Throwable t) { err(sb, t); }
		try { charInfo(sb, g); } catch (Throwable t) { err(sb, t); }
		try { textInfo(sb, g); } catch (Throwable t) { err(sb, t); }
		try { children(sb, g); } catch (Throwable t) { err(sb, t); }
		try { siblings(sb, g); } catch (Throwable t) { err(sb, t); }
		try { notes(sb, g); } catch (Throwable t) { err(sb, t); }
		try { fields(sb, g); } catch (Throwable t) { err(sb, t); }

		return sb.toString();
	}

	private static void err(StringBuilder sb, Throwable t) {
		sb.append("(section failed: ").append(t.getClass().getSimpleName()).append(")\n");
	}

	private static void classChain(StringBuilder sb, Gizmo g) {
		sb.append("class: ");
		Class<?> c = g.getClass();
		while (c != null && c != Object.class) {
			sb.append(simpleName(c));
			c = c.getSuperclass();
			if (c != null && c != Object.class) sb.append(" < ");
		}
		sb.append("\n");
	}

	private static void path(StringBuilder sb, Gizmo g) {
		StringBuilder p = new StringBuilder();
		Gizmo cur = g;
		while (cur != null) {
			String nm = simpleName(cur.getClass());
			if (cur.parent != null) nm += "[" + cur.parent.indexOf(cur) + "]";
			if (p.length() == 0) p.append(nm);
			else p.insert(0, nm + " > ");
			cur = cur.parent;
		}
		sb.append("path: ").append(p).append("\n");
	}

	private static void geometry(StringBuilder sb, Gizmo g) {
		sb.append("\n- geometry (camera units) -\n");

		Camera c = g.camera();

		if (g instanceof Visual) {
			Visual v = (Visual) g;
			sb.append("pos: ").append(pt(v.x, v.y)).append("\n");
			sb.append("size: ").append(f(v.width)).append(" x ").append(f(v.height))
					.append("  scale ").append(pt(v.scale.x, v.scale.y))
					.append(" -> ").append(f(v.width())).append(" x ").append(f(v.height())).append("\n");
			sb.append("center: ").append(pt(v.center().x, v.center().y)).append("\n");
			sb.append("origin: ").append(pt(v.origin.x, v.origin.y))
					.append("  angle: ").append(f(v.angle)).append("\n");
			if (v.speed.x != 0 || v.speed.y != 0 || v.acc.x != 0 || v.acc.y != 0 || v.angularSpeed != 0) {
				sb.append("speed: ").append(pt(v.speed.x, v.speed.y))
						.append("  acc: ").append(pt(v.acc.x, v.acc.y))
						.append("  angularSpeed: ").append(f(v.angularSpeed)).append("\n");
			}
		} else if (g instanceof Component) {
			Component comp = (Component) g;
			sb.append("pos: ").append(pt(comp.left(), comp.top())).append("\n");
			sb.append("size: ").append(f(comp.width())).append(" x ").append(f(comp.height())).append("\n");
			sb.append("center: ").append(pt(comp.centerX(), comp.centerY())).append("\n");
		}

		RectF b = SpriteInspector.camBounds(g);
		if (b != null) {
			sb.append("cam bounds: [").append(f(b.left)).append(", ").append(f(b.top))
					.append("] to [").append(f(b.right)).append(", ").append(f(b.bottom)).append("]\n");
		}
		RectF s = SpriteInspector.screenRectOf(g);
		if (s != null) {
			sb.append("screen px: [").append(Math.round(s.left)).append(", ").append(Math.round(s.top))
					.append("] to [").append(Math.round(s.right)).append(", ").append(Math.round(s.bottom)).append("]\n");
		}

		if (c != null && b != null) {
			sb.append("pixel align: x ").append(alignInfo(b.left, c.zoom))
					.append(", y ").append(alignInfo(b.top, c.zoom)).append("\n");
			sb.append("camera: ").append(simpleName(c.getClass()));
			if (c == PixelScene.uiCamera) sb.append(" (uiCamera)");
			else if (c == Camera.main) sb.append(" (main)");
			sb.append(" ").append(c.width).append("x").append(c.height)
					.append(" @(").append(c.x).append(", ").append(c.y).append(")")
					.append(" zoom ").append(f(c.zoom))
					.append(" scroll ").append(pt(c.scroll.x, c.scroll.y)).append("\n");
		} else if (c == null) {
			sb.append("camera: null\n");
		}
	}

	//whether pos lands on a whole device pixel under this camera zoom
	private static String alignInfo(float pos, float zoom) {
		float px = pos * zoom;
		float off = px - Math.round(px);
		if (Math.abs(off) < 0.01f) return "OK";
		return "OFF by " + f(off) + "px";
	}

	//alignment relations: distances to camera viewport edges and to the nearest bounded ancestor
	private static void align(StringBuilder sb, Gizmo g) {
		RectF b = SpriteInspector.camBounds(g);
		if (b == null) return;
		sb.append("\n- align -\n");

		Camera c = g.camera();
		if (c != null) {
			float viewL = c.scroll.x, viewT = c.scroll.y;
			sb.append("camera edges: L ").append(f(b.left - viewL))
					.append("  R ").append(f((viewL + c.width) - b.right))
					.append("  T ").append(f(b.top - viewT))
					.append("  B ").append(f((viewT + c.height) - b.bottom)).append("\n");
			float dx = (b.left + b.right) / 2f - (viewL + c.width / 2f);
			float dy = (b.top + b.bottom) / 2f - (viewT + c.height / 2f);
			sb.append("camera center off: x ").append(delta(dx)).append("  y ").append(delta(dy)).append("\n");
		}

		//nearest ancestor that has bounds (skips plain Groups)
		Gizmo anc = g.parent;
		RectF pb = null;
		while (anc != null && (pb = SpriteInspector.camBounds(anc)) == null) anc = anc.parent;
		if (anc != null && pb != null) {
			sb.append("in ").append(simpleName(anc.getClass()))
					.append(" ").append(f(pb.width())).append("x").append(f(pb.height()))
					.append(" @").append(pt(pb.left, pb.top)).append(":\n");
			sb.append("  offset: ").append(pt(b.left - pb.left, b.top - pb.top)).append("\n");
			sb.append("  margins: L ").append(f(b.left - pb.left))
					.append("  R ").append(f(pb.right - b.right))
					.append("  T ").append(f(b.top - pb.top))
					.append("  B ").append(f(pb.bottom - b.bottom)).append("\n");
			float dx = (b.left + b.right) / 2f - (pb.left + pb.right) / 2f;
			float dy = (b.top + b.bottom) / 2f - (pb.top + pb.bottom) / 2f;
			sb.append("  center off: x ").append(delta(dx)).append("  y ").append(delta(dy)).append("\n");
		}
	}

	//nested members of the selected group, with position relative to the container
	private static void children(StringBuilder sb, Gizmo g) {
		if (!(g instanceof Group)) return;
		ArrayList<Gizmo> members = SpriteInspector.membersOf((Group) g);
		if (members == null) return;

		RectF base = SpriteInspector.camBounds(g);
		StringBuilder body = new StringBuilder();
		int total = 0, shown = 0;
		for (int i = 0; i < members.size(); i++) {
			Gizmo m = members.get(i);
			if (m == null) continue;
			total++;
			if (shown < 40) {
				body.append(gizmoLine(i, m, base));
				shown++;
			}
		}
		if (total == 0) return;
		sb.append("\n- children (").append(total).append(", rel = offset inside me) -\n").append(body);
		if (total > shown) sb.append("...+").append(total - shown).append(" more\n");
	}

	//members of the parent, with offsets relative to the selected gizmo
	private static void siblings(StringBuilder sb, Gizmo g) {
		if (g.parent == null) return;
		ArrayList<Gizmo> members = SpriteInspector.membersOf(g.parent);
		if (members == null) return;

		RectF base = SpriteInspector.camBounds(g);
		StringBuilder body = new StringBuilder();
		int total = 0, shown = 0;
		for (int i = 0; i < members.size(); i++) {
			Gizmo m = members.get(i);
			if (m == null || m == g) continue;
			total++;
			if (shown < 20) {
				body.append(gizmoLine(i, m, base));
				shown++;
			}
		}
		if (total == 0) return;
		sb.append("\n- siblings (").append(total).append(", in ").append(simpleName(g.parent.getClass()))
				.append(", rel = offset from me) -\n").append(body);
		if (total > shown) sb.append("...+").append(total - shown).append(" more\n");
	}

	//one-line summary of a gizmo: [idx] Class "tag" WxH @(x, y) rel(dx, dy) [flags]
	private static String gizmoLine(int idx, Gizmo m, RectF base) {
		StringBuilder s = new StringBuilder();
		s.append("[").append(idx).append("] ").append(simpleName(m.getClass()));
		String tag = InspectorNotes.tag(m);
		if (tag != null) s.append(" \"").append(tag).append("\"");

		RectF mb = SpriteInspector.camBounds(m);
		if (mb != null) {
			s.append(" ").append(f(mb.width())).append("x").append(f(mb.height()))
					.append(" @").append(pt(mb.left, mb.top));
			if (base != null) s.append(" rel").append(pt(mb.left - base.left, mb.top - base.top));
		} else if (m instanceof Group) {
			ArrayList<Gizmo> kids = SpriteInspector.membersOf((Group) m);
			int n = 0;
			if (kids != null) {
				for (Gizmo k : kids) if (k != null) n++;
			}
			s.append(" (group, ").append(n).append(" kids)");
		}

		if (!m.exists) s.append(" [!exists]");
		if (!m.visible) s.append(" [!vis]");
		if (m instanceof Visual && ((Visual) m).alpha() <= 0) s.append(" [a=0]");
		s.append("\n");
		return s.toString();
	}

	//signed delta, flagging perfect centering
	private static String delta(float v) {
		if (Math.abs(v) < 0.005f) return "0 (centered)";
		return (v > 0 ? "+" : "") + f(v);
	}

	private static void state(StringBuilder sb, Gizmo g) {
		sb.append("\n- state -\n");
		sb.append("exists ").append(tf(g.exists))
				.append("  alive ").append(tf(g.alive))
				.append("  active ").append(tf(g.active)).append("(chain ").append(tf(g.isActive())).append(")")
				.append("  visible ").append(tf(g.visible)).append("(chain ").append(tf(g.isVisible())).append(")\n");
		if (g instanceof Visual) {
			Visual v = (Visual) g;
			sb.append("alpha: ").append(f(v.alpha())).append("\n");
			if (v.rm != 1 || v.gm != 1 || v.bm != 1 || v.am != 1
					|| v.ra != 0 || v.ga != 0 || v.ba != 0 || v.aa != 0) {
				sb.append("color mul: (").append(f(v.rm)).append(", ").append(f(v.gm)).append(", ")
						.append(f(v.bm)).append(", ").append(f(v.am)).append(")")
						.append(" add: (").append(f(v.ra)).append(", ").append(f(v.ga)).append(", ")
						.append(f(v.ba)).append(", ").append(f(v.aa)).append(")\n");
			}
		}
	}

	private static void image(StringBuilder sb, Gizmo g) {
		if (g instanceof Image) {
			Image img = (Image) g;
			sb.append("\n- image -\n");
			if (img.texture != null) {
				sb.append("texture: ").append(img.texture.width).append("x").append(img.texture.height)
						.append(" @").append(Integer.toHexString(System.identityHashCode(img.texture))).append("\n");
				RectF fr = img.frame();
				if (fr != null) {
					sb.append("frame uv: [").append(f(fr.left)).append(", ").append(f(fr.top))
							.append(", ").append(f(fr.right)).append(", ").append(f(fr.bottom)).append("]\n");
					sb.append("frame px: [").append(Math.round(fr.left * img.texture.width))
							.append(", ").append(Math.round(fr.top * img.texture.height))
							.append(", ").append(Math.round(fr.right * img.texture.width))
							.append(", ").append(Math.round(fr.bottom * img.texture.height)).append("]\n");
				}
			} else {
				sb.append("texture: null\n");
			}
			sb.append("flip h/v: ").append(tf(img.flipHorizontal)).append("/").append(tf(img.flipVertical)).append("\n");
		}
		if (g instanceof MovieClip) {
			MovieClip mc = (MovieClip) g;
			sb.append("anim: paused ").append(tf(mc.paused)).append("  looping ").append(tf(mc.looping()))
					.append("  (curAnim/curFrame in fields)\n");
		}
	}

	private static void charInfo(StringBuilder sb, Gizmo g) {
		if (!(g instanceof CharSprite)) return;
		CharSprite cs = (CharSprite) g;
		Char ch = cs.ch;
		sb.append("\n- char -\n");
		if (ch == null) {
			sb.append("ch: null (unlinked)\n");
			return;
		}
		sb.append("ch: ").append(ch.name()).append("  HP ").append(ch.HP).append("/").append(ch.HT)
				.append("  pos ").append(ch.pos);
		if (Dungeon.level != null) {
			int lw = Dungeon.level.width();
			sb.append(" (col ").append(ch.pos % lw).append(", row ").append(ch.pos / lw).append(")");
		}
		sb.append("\n");
		if (Dungeon.level != null) {
			PointF ideal = cs.worldToCamera(ch.pos);
			sb.append("worldToCamera(pos): ").append(pt(ideal.x, ideal.y))
					.append("  delta from sprite: ").append(pt(cs.x - ideal.x, cs.y - ideal.y)).append("\n");
		}
	}

	private static void textInfo(StringBuilder sb, Gizmo g) {
		String txt = null;
		if (g instanceof RenderedTextBlock) txt = ((RenderedTextBlock) g).text();
		else if (g instanceof BitmapText) txt = ((BitmapText) g).text();
		if (txt != null) {
			sb.append("\n- text -\n\"").append(trunc(txt, 80)).append("\"\n");
		}
	}

	private static void notes(StringBuilder sb, Gizmo g) {
		sb.append("\n- dev notes -\n");
		ArrayList<String> ns = InspectorNotes.notesFor(g);
		if (ns.isEmpty()) {
			sb.append("(none - add via InspectorNotes.note/tag)\n");
		} else {
			for (String n : ns) {
				sb.append(n).append("\n");
			}
		}
	}

	private static void fields(StringBuilder sb, Gizmo g) {
		sb.append("\n- fields -\n");
		Class<?> c = g.getClass();
		while (c != null && c != Object.class) {
			Field[] fs = c.getDeclaredFields();
			boolean printedHeader = false;
			for (Field fld : fs) {
				if (Modifier.isStatic(fld.getModifiers()) || fld.isSynthetic()) continue;
				if (!printedHeader) {
					sb.append("[").append(simpleName(c)).append("]\n");
					printedHeader = true;
				}
				sb.append(fld.getName()).append(" = ");
				try {
					fld.setAccessible(true);
					sb.append(fmtValue(fld.get(g)));
				} catch (Throwable t) {
					sb.append("(inaccessible)");
				}
				sb.append("\n");
			}
			c = c.getSuperclass();
		}
	}

	static String fmtValue(Object v) {
		if (v == null) return "null";
		if (v instanceof PointF) return pt(((PointF) v).x, ((PointF) v).y);
		if (v instanceof Point) return "(" + ((Point) v).x + ", " + ((Point) v).y + ")";
		if (v instanceof RectF) {
			RectF r = (RectF) v;
			return "[" + f(r.left) + ", " + f(r.top) + ", " + f(r.right) + ", " + f(r.bottom) + "]";
		}
		if (v instanceof Rect) {
			Rect r = (Rect) v;
			return "[" + r.left + ", " + r.top + ", " + r.right + ", " + r.bottom + "]";
		}
		if (v instanceof Float) return f((Float) v);
		if (v instanceof Double) return f(((Double) v).floatValue());
		if (v instanceof Number || v instanceof Boolean || v instanceof Character) return String.valueOf(v);
		if (v instanceof String) return "\"" + trunc((String) v, 48) + "\"";
		if (v instanceof Enum) return ((Enum<?>) v).name();
		if (v.getClass().isArray()) {
			return simpleName(v.getClass().getComponentType()) + "[" + java.lang.reflect.Array.getLength(v) + "]";
		}
		if (v instanceof Collection) return simpleName(v.getClass()) + "(" + ((Collection<?>) v).size() + ")";
		if (v instanceof Map) return simpleName(v.getClass()) + "(" + ((Map<?, ?>) v).size() + ")";
		return simpleName(v.getClass()) + "@" + Integer.toHexString(System.identityHashCode(v));
	}

	//=== small format helpers ===

	static String simpleName(Class<?> c) {
		String n = c.getSimpleName();
		if (n.isEmpty()) {
			n = c.getName();
			n = n.substring(n.lastIndexOf('.') + 1);
		}
		return n;
	}

	static String f(float v) {
		if (v == (long) v) return String.valueOf((long) v);
		return String.format(Locale.ROOT, "%.2f", v);
	}

	private static String pt(float x, float y) {
		return "(" + f(x) + ", " + f(y) + ")";
	}

	private static String tf(boolean b) {
		return b ? "T" : "F";
	}

	static String trunc(String s, int max) {
		s = s.replace("\n", "\\n");
		if (s.length() > max) s = s.substring(0, max) + "...";
		return s;
	}
}
