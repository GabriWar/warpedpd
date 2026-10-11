/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WindBottle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

/**
 * fx-combat (and fx-combat-night): a fight to try every kind of blow on. The blank room round the
 * hero (blank-room), a weapon of every class in the pack (a sword's slash, a spear's stab, a mace's
 * crush, darts' arrows, the wind bottle's magic, the triangle's parry) and a wand of blast wave;
 * a skeleton (bone dust), a slime (its blood) and a golem (sparks off its body) standing still and
 * taking whatever comes (9999 health), the skeleton in line with a wall stub to blast it into. By
 * day, or held at night.
 */
public final class CombatScene implements DebugScenes.Scene {

	/** The dummies' health. */
	static final int HARDY = 9999;

	final boolean night;

	public CombatScene( boolean night ){
		this.night = night;
	}

	@Override
	public String id(){
		return night ? "fx-combat-night" : "fx-combat";
	}

	@Override
	public String title(){
		return night ? "Effects: a fight to try every blow on, at night" : "Effects: a fight to try every blow on";
	}

	@Override
	public void apply( Hero hero ){
		DebugScenes.Scene room = null;
		for (DebugScenes.Scene s : DebugScenes.SCENES) if (s.id().equals( "blank-room" )) room = s;
		if (room == null) return;
		room.apply( hero );
		if (Dungeon.level instanceof OverworldLevel){
			GLog.w( "Scene: run " + id() + " again once below the surface (the blank room needs a dungeon floor)" );
			return;
		}
		Level level = Dungeon.level;
		//a wall stub east, the skeleton in line with it: blast it into the wall
		for (int dy = -1; dy <= 1; dy++) set( level, at( hero, 4, dy ), Terrain.WALL );
		GameScene.updateMap();
		level.cleanWalls();
		spawn( Skeleton.class, at( hero, 2, 0 ) );
		spawn( Slime.class, at( hero, -2, 0 ) );
		spawn( Golem.class, at( hero, 0, 2 ) );

		give( hero, new Sword() );
		give( hero, new Spear() );
		give( hero, new Mace() );
		give( hero, new WindBottle() );
		give( hero, new Triangolo() );
		give( hero, new Dart().quantity( 20 ) );
		WandOfBlastWave wand = new WandOfBlastWave();
		wand.identify();
		wand.curCharges = wand.maxCharges;
		wand.collect( hero.belongings.backpack );

		DayNightCycle.debugPhaseOverride = night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY;
		ClimateManager.onHeroTurn();
		GameScene.updateDayNightTint();
		GLog.i( "Scene: a skeleton east (a wall behind it for the blast wave), a slime west, a golem south, all standing"
				+ " still. In the pack: sword (slash), spear (stab), mace (crush), darts (arrow), wind bottle (magic),"
				+ " triangle (parry), a wand of blast wave. " + (night ? "Held at night." : "Held by day.") );
	}

	private static int at( Hero hero, int dx, int dy ){
		int w = Dungeon.level.width();
		return hero.pos + dx + dy * w;
	}

	private static void set( Level level, int cell, int terrain ){
		if (cell < 0 || cell >= level.length() || Actor.findChar( cell ) != null) return;
		Level.set( cell, terrain );
		level.visited[cell] = true;
		level.mapped[cell] = true;
	}

	private static void spawn( Class<? extends Mob> kind, int cell ){
		if (cell < 0 || cell >= Dungeon.level.length() || Actor.findChar( cell ) != null || Dungeon.level.solid[cell]) return;
		Mob mob = Reflection.newInstance( kind );
		if (mob == null) return;
		mob.pos = cell;
		mob.HP = mob.HT = HARDY;
		mob.state = mob.PASSIVE;
		GameScene.add( mob );
	}

	private static void give( Hero hero, Item item ){
		item.identify();
		item.collect( hero.belongings.backpack );
	}
}
