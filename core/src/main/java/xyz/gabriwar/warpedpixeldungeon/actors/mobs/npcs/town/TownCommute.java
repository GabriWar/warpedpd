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


package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer4;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInnLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Reflection;

//the town's day: by day every townsperson stands at their post, at night they walk
//to the inn and sleep in their own bed. Levels are never loaded together, so nobody
//walks between buildings; instead each level, while the hero is in it, keeps its
//own books straight once a turn - whoever should be here and is not comes in
//through the door and walks to their spot, whoever should not be here walks out
//and is gone (NPC.commute). Arriving hero finds everyone already in place.
public class TownCommute {

	//the bed by the door in the first room: never handed to a townsperson, so the
	//hero always has somewhere to lie down
	public static final int PLAYER_BED = 77;

	//who sleeps where: the foot of each bed, room by room, from the first room's
	//two spare beds down. One bed in the last room stays empty
	static final Object[][] BEDS = {
			{ Bard.class,              78 },
			{ InnServant.class,        79 },
			{ Drunkard.class,         202 },
			{ Mercenary.class,        203 },
			{ Bishop.class,           204 },
			{ Librarian.class,        327 },
			{ FortuneTellerFolk.class, 328 },
			{ Employee.class,         329 },
			{ TownGuardFolk.class,    452 },
			{ TownGuardFolk.class,    453 },
			{ TownGuardFolk.class,    454 },
			{ TownGuardFolk.class,    577 },
			{ TownsfolkMovie.class,   578 },
			{ Townsfolk.class,        579 },
			{ TownsfolkSilent.class,  702 },
			{ Tinkerer1.class,        703 },
			{ Tinkerer2.class,        704 },
			{ Tinkerer4.class,        827 },
			{ Tinkerer5.class,        828 },
			{ Blacksmith2.class,      829 },
			{ TownGuard.class,        952 },
			{ OtilukeNPC.class,       953 },
	};

	private static Level lastLevel;

	//once a hero turn: the level the hero is in settles who should be present
	public static void onHeroTurn() {
		Level level = Dungeon.level;
		boolean arrived = level != lastLevel;
		lastLevel = level;

		boolean night = DayNightCycle.isNight();
		if (level instanceof TownInnLevel) {
			//a hero walking in by day should not meet last night's guests filing out
			if (arrived && !night) clearOut( level, true );
			settle( level, night ? BEDS : ((TownInteriorLevel) level).folk(), night, arrived );
		} else if (level instanceof TownInteriorLevel || level instanceof OverworldLevel) {
			//at night away from the inn nobody is wanted: whoever is still about walks
			//out (NPC.commute), or is simply not there when the hero arrives
			if (night) {
				if (arrived) clearOut( level, false );
			} else if (level instanceof OverworldLevel) {
				settle( level, ((OverworldLevel) level).sleeperHomes(), false, arrived );
			} else {
				settle( level, ((TownInteriorLevel) level).folk(), false, arrived );
			}
		}
	}

	//everyone who has somewhere else to be, gone at once: the inn's guests by day
	//(guestsOnly), or every sleeper of a home level whose bedtime has passed
	private static void clearOut( Level level, boolean guestsOnly ) {
		for (Mob m : level.mobs.toArray( new Mob[0] )) {
			if (!(m instanceof NPC) || !((NPC) m).sleepsAtInn()) continue;
			//a guest is someone with a bed here and a home elsewhere; the inn's own
			//folk fresh from the build have neither yet, and stay
			if (guestsOnly && (((NPC) m).home != -1 || ((NPC) m).bed == -1)) continue;
			if (!guestsOnly && !((NPC) m).bedtimeNow()) continue;   //not their bedtime yet
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
	}

	//the cell a wanted townsperson enters and leaves by
	public static int door( Level level ) {
		if (level instanceof TownInteriorLevel) return ((TownInteriorLevel) level).doorCell();
		if (level instanceof OverworldLevel) return ((OverworldLevel) level).innDoorCell();
		return -1;
	}

	//every slot in the table gets exactly one townsperson of its class: one already
	//holding the slot, else an unassigned one of that class adopts it, else a new
	//one comes in through the door - one per turn, so they file in rather than
	//appear in a heap. On the turn the hero arrives they are placed directly
	private static void settle( Level level, Object[][] slots, boolean beds, boolean arrived ) {
		int door = door( level );
		for (Object[] slot : slots) {
			Class<?> cls = (Class<?>) slot[0];
			int key = (Integer) slot[1];
			int cell = cellOf( level, key );
			if (cell == -1) continue;   //off the overworld's window: not this level's concern right now

			if (holder( level, cls, key, beds ) != null) continue;

			NPC spare = holder( level, cls, -1, beds );
			if (spare != null) {
				if (beds) spare.bed = key; else spare.home = key;
				continue;
			}

			if (level instanceof OverworldLevel && ((OverworldLevel) level).parkedFolk( cls, key )) continue;

			NPC npc = (NPC) Reflection.newInstance( cls );
			if (beds && !npc.bedtimeNow()) continue;   //still out on the town
			if (beds) npc.bed = key; else npc.home = key;
			int at = arrived || door == -1 || Actor.findChar( door ) != null ? cell : door;
			if (Actor.findChar( at ) != null) continue;   //somebody on the spot: next turn
			npc.pos = at;
			if (beds && at == cell) npc.state = npc.SLEEPING;
			GameScene.add( npc );
			if (at == door) return;   //one through the door per turn
		}
	}

	private static NPC holder( Level level, Class<?> cls, int key, boolean beds ) {
		for (Mob m : level.mobs) {
			if (m.getClass() != cls || !(m instanceof NPC)) continue;
			NPC f = (NPC) m;
			if ((beds ? f.bed : f.home) == key) return f;
		}
		return null;
	}

	//a slot key as a cell of this level: interiors store the cell itself, the
	//overworld stores the town's layout cell because its window moves under it
	public static int cellOf( Level level, int key ) {
		if (key == -1) return -1;
		return level instanceof OverworldLevel ? ((OverworldLevel) level).townLocal( key ) : key;
	}
}
