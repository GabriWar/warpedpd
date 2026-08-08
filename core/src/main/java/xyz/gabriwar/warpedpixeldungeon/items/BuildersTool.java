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

package xyz.gabriwar.warpedpixeldungeon.items;

import com.watabou.noosa.audio.Sample;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTileEdit;

import java.util.ArrayList;

/**
 * A special tool that only works in the safe level. Point it at any tile to
 * edit it: remove walls, place walls, add water, add grass, or install a door.
 */
public class BuildersTool extends Item {

	public static final String AC_BUILD = "BUILD";

	{
		image = ItemSpriteSheet.DWARF_HAMMER;
		defaultAction = AC_BUILD;
		stackable = false;
		unique = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (Dungeon.level instanceof SafeLevel) {
			actions.add(AC_BUILD);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_BUILD)) {
			if (!(Dungeon.level instanceof SafeLevel)) {
				GLog.w(Messages.get(this, "not_safe"));
				return;
			}
			GameScene.selectCell(builder);
		}
	}

	private final CellSelector.Listener builder = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			if (!(Dungeon.level instanceof SafeLevel)) return;
			if (cell == Dungeon.hero.pos) {
				GLog.w(Messages.get(BuildersTool.class, "cant_edit_self"));
				return;
			}
			int col = cell % Dungeon.level.width();
			int row = cell / Dungeon.level.width();
			if (col == 0 || col == Dungeon.level.width() - 1
					|| row == 0 || row == Dungeon.level.height() - 1) {
				GLog.w(Messages.get(BuildersTool.class, "cant_edit"));
				return;
			}

			//one big palette: turn (almost) any tile into (almost) any other
			GameScene.show(new WndTileEdit(cell,
					Messages.get(BuildersTool.class, "title"),
					PALETTE,
					paletteLabels(),
					terrain -> buildTile(cell, terrain)));
		}

		@Override
		public String prompt() {
			return Messages.get(BuildersTool.class, "prompt");
		}
	};

	//the two unlockable amenities cost gold; everything else is free terrain editing
	public static final int ALCHEMY_COST  = 800;
	public static final int PEDESTAL_COST = 1500;

	//the full build palette, in menu order. removal of an amenity is just picking
	//a plain tile over it (no gold refunded)
	private static final int[] PALETTE = {
			Terrain.EMPTY, Terrain.EMPTY_SP, Terrain.EMPTY_DECO,
			Terrain.WALL, Terrain.WALL_DECO,
			Terrain.DOOR, Terrain.OPEN_DOOR, Terrain.WATER, Terrain.FROZEN_WATER,
			Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS,
			Terrain.SHRUB, Terrain.WOOL_RUG, Terrain.EMBERS, Terrain.BOOKSHELF,
			Terrain.STATUE, Terrain.STATUE_SP, Terrain.WELL, Terrain.BARRICADE,
			Terrain.REGION_DECO, Terrain.REGION_DECO_ALT, Terrain.MINE_CRYSTAL,
			Terrain.INACTIVE_TRAP, Terrain.CHASM,
			Terrain.ALCHEMY, Terrain.PEDESTAL
	};

	private static String[] paletteLabels() {
		return new String[]{
				Messages.get(BuildersTool.class, "opt_floor"),
				Messages.get(BuildersTool.class, "opt_wood_floor"),
				Messages.get(BuildersTool.class, "opt_floor_deco"),
				Messages.get(BuildersTool.class, "opt_wall"),
				Messages.get(BuildersTool.class, "opt_wall_deco"),
				Messages.get(BuildersTool.class, "opt_door"),
				Messages.get(BuildersTool.class, "opt_open_door"),
				Messages.get(BuildersTool.class, "opt_water"),
				Messages.get(BuildersTool.class, "opt_ice"),
				Messages.get(BuildersTool.class, "opt_grass"),
				Messages.get(BuildersTool.class, "opt_high_grass"),
				Messages.get(BuildersTool.class, "opt_till"),
				Messages.get(BuildersTool.class, "opt_shrub"),
				Messages.get(BuildersTool.class, "opt_rug"),
				Messages.get(BuildersTool.class, "opt_embers"),
				Messages.get(BuildersTool.class, "opt_bookshelf"),
				Messages.get(BuildersTool.class, "opt_statue"),
				Messages.get(BuildersTool.class, "opt_statue_sp"),
				Messages.get(BuildersTool.class, "opt_well"),
				Messages.get(BuildersTool.class, "opt_barricade"),
				Messages.get(BuildersTool.class, "opt_region_deco"),
				Messages.get(BuildersTool.class, "opt_region_deco_alt"),
				Messages.get(BuildersTool.class, "opt_crystal"),
				Messages.get(BuildersTool.class, "opt_disarmed_trap"),
				Messages.get(BuildersTool.class, "opt_chasm"),
				Messages.get(BuildersTool.class, "opt_alchemy", ALCHEMY_COST),
				Messages.get(BuildersTool.class, "opt_pedestal", PEDESTAL_COST)
		};
	}

	private void buildTile(int cell, int terrain) {

		int oldTile = Dungeon.level.map[cell];

		//placing an amenity or any solid/hazard tile must not bury a mob
		boolean solidTarget = terrain == Terrain.ALCHEMY || terrain == Terrain.PEDESTAL
				|| terrain == Terrain.WALL || terrain == Terrain.WALL_DECO
				|| terrain == Terrain.STATUE || terrain == Terrain.STATUE_SP
				|| terrain == Terrain.BOOKSHELF || terrain == Terrain.BARRICADE
				|| terrain == Terrain.WELL || terrain == Terrain.CHASM
				|| terrain == Terrain.SHRUB || terrain == Terrain.MINE_CRYSTAL
				;
		if (solidTarget && xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar(cell) != null) {
			GLog.w(Messages.get(this, "occupied"));
			return;
		}

		//removing an enchanting pedestal despawns its station (no gold back)
		if (oldTile == Terrain.PEDESTAL && terrain != Terrain.PEDESTAL) {
			for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m
					: Dungeon.level.mobs.toArray(new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0])) {
				if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation
						&& m.pos == cell) {
					m.destroy();
					if (m.sprite != null) m.sprite.killAndErase();
				}
			}
		}

		//a disarmed trap tile gets a real (inactive, visible) trap object so it
		//renders with the standard grey trap art; building anything else over a
		//trapped cell clears the trap
		if (terrain == Terrain.INACTIVE_TRAP) {
			if (Dungeon.level.traps.get(cell) == null) {
				xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap t
						= new xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap().reveal();
				t.active = false;
				Dungeon.level.setTrap(t, cell);
			}
		} else if (Dungeon.level.traps.get(cell) != null) {
			Dungeon.level.traps.remove(cell);
		}

		if (terrain == Terrain.ALCHEMY && oldTile != Terrain.ALCHEMY) {
			if (Dungeon.gold < ALCHEMY_COST) {
				GLog.w(Messages.get(this, "no_gold"));
				return;
			}
			Dungeon.gold -= ALCHEMY_COST;
			applyTerrain(cell, terrain);
			GLog.p(Messages.get(this, "built_alchemy"));

		} else if (terrain == Terrain.PEDESTAL && oldTile != Terrain.PEDESTAL) {
			if (Dungeon.gold < PEDESTAL_COST) {
				GLog.w(Messages.get(this, "no_gold"));
				return;
			}
			Dungeon.gold -= PEDESTAL_COST;
			applyTerrain(cell, terrain);
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation station
					= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation();
			station.pos = cell;
			GameScene.add(station);
			GLog.p(Messages.get(this, "built_pedestal"));

		} else {
			applyTerrain(cell, terrain);
		}
	}

	public static void applyTerrain(int cell, int terrain) {
		Level.set(cell, terrain);
		Dungeon.level.cleanWalls();
		Dungeon.level.visited[cell] = true;
		Dungeon.level.mapped[cell] = true;
		GameScene.updateMap();
		Dungeon.observe();

		switch (terrain) {
			case Terrain.WALL:
				Sample.INSTANCE.play(Assets.Sounds.ROCKS_LIGHT);
				break;
			case Terrain.EMPTY:
				Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
				break;
			case Terrain.WATER:
				Sample.INSTANCE.play(Assets.Sounds.WATER);
				break;
			case Terrain.GRASS:
				Sample.INSTANCE.play(Assets.Sounds.GRASS);
				break;
			case Terrain.DOOR:
				Sample.INSTANCE.play(Assets.Sounds.OPEN);
				break;
		}
	}

	private static final ItemSprite.Glowing GOLDEN = new ItemSprite.Glowing( 0xFFAA00 );

	@Override
	public ItemSprite.Glowing glowing() { return GOLDEN; }

	@Override
	public boolean isUpgradable() { return false; }

	@Override
	public boolean isIdentified() { return true; }

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}
}
