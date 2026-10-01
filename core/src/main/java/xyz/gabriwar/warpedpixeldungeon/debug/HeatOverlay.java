package xyz.gabriwar.warpedpixeldungeon.debug;

import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.ColorMath;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;

import java.util.ArrayList;

/** Debug-only, viewport-sized pool: no objects or temperature reads while disabled. */
public class HeatOverlay extends Group {

	public static boolean enabled;
	private final ArrayList<Cell> cells = new ArrayList<>();
	private float refresh;

	@Override
	public void update() {
		visible = enabled;
		if (!enabled || Dungeon.level == null) {
			refresh = 0;
			return;
		}
		refresh -= Game.elapsed;
		if (refresh > 0) return;
		refresh = 0.1f;
		Level level = Dungeon.level;
		Camera view = Camera.main;
		int left = Math.max(0, (int)Math.floor(view.scroll.x / 16));
		int top = Math.max(0, (int)Math.floor(view.scroll.y / 16));
		int right = Math.min(level.width(), (int)Math.ceil((view.scroll.x + view.width) / 16));
		int bottom = Math.min(level.height(), (int)Math.ceil((view.scroll.y + view.height) / 16));
		int used = 0;
		for (int y = top; y < bottom; y++) {
			for (int x = left; x < right; x++) {
				if (used == cells.size()) {
					Cell cell = new Cell();
					cells.add(cell);
					add(cell);
				}
				int pos = x + y * level.width();
				cells.get(used++).show(x, y, TileTemperature.tileTemp(pos), level.waterPhaseProgress[pos]);
			}
		}
		for (int i = used; i < cells.size(); i++) cells.get(i).visible = false;
	}

	private static int color(float temperature) {
		if (temperature <= 0) return ColorMath.interpolate(0x203DB0, 0x42CFE0,
				Math.max(0, (temperature + 40) / 40));
		if (temperature <= 20) return ColorMath.interpolate(0x42CFE0, 0x70A060, temperature / 20);
		if (temperature <= 60) return ColorMath.interpolate(0x70A060, 0xFF8020, (temperature - 20) / 40);
		return ColorMath.interpolate(0xFF8020, 0xE02040, Math.min(1, (temperature - 60) / 140));
	}

	private static class Cell extends Group {
		private final ColorBlock tint = new ColorBlock(15, 15, 0xFFFFFFFF);
		private final BitmapText text = new BitmapText(PixelScene.pixelFont);
		private final ColorBlock progress = new ColorBlock(1, 1, 0xFFFFFFFF);

		Cell() {
			add(tint);
			tint.alpha(0.55f);
			add(text);
			add(progress);
		}

		void show(int x, int y, float temperature, float phase) {
			visible = true;
			tint.x = x * 16;
			tint.y = y * 16;
			tint.hardlight(color(temperature));
			String value = Integer.toString(Math.round(temperature));
			if (!value.equals(text.text())) {
				text.text(value);
				text.measure();
			}
			text.scale.set(Math.min(0.6f, 14f / Math.max(1, text.width)));
			text.x = x * 16 + (16 - text.width()) / 2;
			text.y = y * 16 + (16 - text.height()) / 2;
			progress.visible = phase != 0;
			progress.x = x * 16 + 1;
			progress.y = y * 16 + 13;
			progress.size(14 * Math.abs(phase), 1);
			progress.hardlight(phase < 0 ? 0x80E8FF : 0xFFD080);
		}
	}
}
