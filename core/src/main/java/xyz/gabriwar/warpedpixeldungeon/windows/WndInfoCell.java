/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTerrainTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

public class WndInfoCell extends Window {
	
	private static final float GAP	= 2;
	
	private static final int WIDTH = 120;

	public static Image cellImage( int cell ){
		int tile = Dungeon.level.map[cell];
		if (Dungeon.level.water[cell]) {
			tile = Terrain.WATER;
		} else if (Dungeon.level.pit[cell]) {
			tile = Terrain.CHASM;
		}

		Image customImage = null;
		int x = cell % Dungeon.level.width();
		int y = cell / Dungeon.level.width();
		for (CustomTilemap i : Dungeon.level.customTiles){
			if ((x >= i.tileX && x < i.tileX+i.tileW) &&
					(y >= i.tileY && y < i.tileY+i.tileH)){
				if ((customImage = i.image(x - i.tileX, y - i.tileY)) != null) {
					break;
				}
			}
		}

		if (customImage != null){
			return customImage;
		} else {

			if (tile == Terrain.WATER) {
				Image water = new Image(Dungeon.level.waterTex());
				water.frame(0, 0, DungeonTilemap.SIZE, DungeonTilemap.SIZE);
				return water;
			} else {
				return DungeonTerrainTilemap.tile(cell, tile);
			}
		}
	}

	public static String cellName( int cell ){

		CustomTilemap customTile = null;
		int x = cell % Dungeon.level.width();
		int y = cell / Dungeon.level.width();
		for (CustomTilemap i : Dungeon.level.customTiles){
			if ((x >= i.tileX && x < i.tileX+i.tileW) &&
					(y >= i.tileY && y < i.tileY+i.tileH)){
				if (i.image(x - i.tileX, y - i.tileY) != null) {
					x -= i.tileX;
					y -= i.tileY;
					customTile = i;
					break;
				}
			}
		}

		if (customTile != null && customTile.name(x, y) != null){
			return customTile.name(x, y);
		} else {
			return Dungeon.level.tileName(Dungeon.level.map[cell]);
		}
	}
	
	public WndInfoCell( int cell ) {
		
		super();

		CustomTilemap customTile = null;
		int x = cell % Dungeon.level.width();
		int y = cell / Dungeon.level.width();
		for (CustomTilemap i : Dungeon.level.customTiles){
			if ((x >= i.tileX && x < i.tileX+i.tileW) &&
					(y >= i.tileY && y < i.tileY+i.tileH)){
				if (i.image(x - i.tileX, y - i.tileY) != null) {
					x -= i.tileX;
					y -= i.tileY;
					customTile = i;
					break;
				}
			}
		}


		String desc = "";

		IconTitle titlebar = new IconTitle();
		titlebar.icon(cellImage(cell));
		titlebar.label(cellName(cell));

		if (customTile != null){
			String customDesc = customTile.desc(x, y);
			if (customDesc != null) {
				desc += customDesc;
			} else {
				desc += Dungeon.level.tileDesc(Dungeon.level.map[cell]);
			}

		} else {

			desc += Dungeon.level.tileDesc(Dungeon.level.map[cell]);
		}

		String extra = Dungeon.level.cellDescExtra(cell);
		if (extra != null){
			desc += (desc.length() > 0 ? "\n\n" : "") + extra;
		}
		titlebar.setRect(0, 0, WIDTH, 0);
		add(titlebar);

		RenderedTextBlock info = PixelScene.renderTextBlock(6);
		add(info);

		if (Dungeon.level.heroFOV[cell]) {
			for (Blob blob : Dungeon.level.blobs.values()) {
				if (blob.volume > 0 && blob.cur[cell] > 0 && blob.tileDesc() != null) {
					if (desc.length() > 0) {
						desc += "\n\n";
					}
					desc += blob.tileDesc();
				}
			}

			// --- Ambient info ---
			float tileT    = TileTemperature.tileTemp(cell);
			float feelsLike = TileTemperature.feelsLikeAt(cell);
			float wind     = ClimateManager.localWindSpeed();

			if (desc.length() > 0) desc += "\n\n";

			// Temperature
			String tempLine = "_Temperature:_ " + Messages.decimalFormat("#.#", tileT) + "°C";
			if (Math.abs(feelsLike - tileT) >= 1f) {
				tempLine += " (feels like " + Messages.decimalFormat("#.#", feelsLike) + "°C)";
			}
			desc += tempLine;

			// Wind (only if noticeable)
			if (wind >= 3f) {
				float dir = ClimateManager.surfaceWindDir();
				String[] cardinals = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
				String cardinal = cardinals[Math.round(dir / 45f) % 8];
				desc += "\n_Wind:_ " + Messages.decimalFormat("#.#", wind) + " m/s " + cardinal;
			}

			// Light level
			float light = ClimateManager.localAmbientLight();
			String lightDesc;
			if      (light > 0.75f) lightDesc = "bright";
			else if (light > 0.45f) lightDesc = "dim";
			else if (light > 0.15f) lightDesc = "dark";
			else                    lightDesc = "very dark";
			desc += "\n_Light:_ " + lightDesc;
		}
		
		//debug aid: the cell index and x,y of what was tapped
		if (com.watabou.utils.DeviceCompat.isDebug()
				|| xyz.gabriwar.warpedpixeldungeon.WPDSettings.debugUnlocked()){
			desc += (desc.length() > 0 ? "\n\n" : "") + "_Cell:_ " + cell
					+ " (" + cell % Dungeon.level.width() + ", " + cell / Dungeon.level.width() + ")";
		}

		info.text( desc.length() == 0 ? Messages.get(this, "nothing") : desc );
		info.maxWidth(WIDTH);
		info.setPos(titlebar.left(), titlebar.bottom() + 2*GAP);

		float btnY = info.bottom() + GAP;
		boolean anyButton = false;

		boolean adjacent = cell == Dungeon.hero.pos || Dungeon.level.adjacent(cell, Dungeon.hero.pos);

		if (Dungeon.level.water[cell]) {
			RedButton btnWet = new RedButton("Get Wet") {
				@Override
				protected void onClick() {
					super.onClick();
					if (!adjacent) {
						GLog.w("You are too far away to wet yourself.");
						return;
					}
					float currentDiff = TileTemperature.tileTemp(cell) - Dungeon.hero.bodyTemp;
					float before = Dungeon.hero.bodyTemp;
					Buff.affect(Dungeon.hero, Drenched.class, Drenched.DURATION);
					Dungeon.hero.bodyTemp += currentDiff * 0.20f;
					float after = Dungeon.hero.bodyTemp;
					GLog.i("You soak yourself in the water. You are now drenched. Body temp: "
							+ Messages.decimalFormat("#.#", before) + "°C → "
							+ Messages.decimalFormat("#.#", after) + "°C");
					Dungeon.hero.spendAndNext(5f);
					hide();
				}
			};
			btnWet.setRect(0, btnY, WIDTH, 16);
			add(btnWet);
			btnY += 16 + GAP;
			anyButton = true;
		}

		if (Dungeon.level.map[cell] == Terrain.EMBERS) {
			RedButton btnLie = new RedButton("Warm Up") {
				@Override
				protected void onClick() {
					super.onClick();
					if (!adjacent) {
						GLog.w("You are too far away to warm up on these embers.");
						return;
					}
					float emberTemp = TileTemperature.tileTemp(cell);
					float before = Dungeon.hero.bodyTemp;
					float diff = emberTemp - before;

					// Diminishing returns: the closer you are to ember temp, the less you gain.
					// gain = diff * random(0.10..0.25), so each warm up is slightly different
					float gain = diff * (0.10f + com.watabou.utils.Random.Float(0.15f));
					Dungeon.hero.bodyTemp += gain;
					float after = Dungeon.hero.bodyTemp;

					if (after >= 45f) {
						// Too hot! Catch fire
						GLog.w("The embers scorch you! Body temp: "
								+ Messages.decimalFormat("#.#", before) + "°C → "
								+ Messages.decimalFormat("#.#", after) + "°C");
						Buff.affect(Dungeon.hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning.class).reignite(Dungeon.hero);
					} else if (gain > 0.5f) {
						GLog.i("You warm up on the embers. Body temp: "
								+ Messages.decimalFormat("#.#", before) + "°C → "
								+ Messages.decimalFormat("#.#", after) + "°C");
					} else {
						GLog.i("The embers barely warm you anymore. ("
								+ Messages.decimalFormat("#.#", after) + "°C)");
					}
					Dungeon.hero.spendAndNext(3f);
					hide();
				}
			};
			btnLie.setRect(0, btnY, WIDTH, 16);
			add(btnLie);
			btnY += 16 + GAP;
			anyButton = true;
		}

		resize( WIDTH, (int)(anyButton ? btnY : info.bottom()+2) );
	}
}
