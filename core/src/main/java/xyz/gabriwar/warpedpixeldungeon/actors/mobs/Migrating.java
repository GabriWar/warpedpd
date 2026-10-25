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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dread;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MigrationEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundle;

/**
 * A beast of a migrating herd (levels/overworld/MigrationEvent): one of its deer, grazing its way
 * along with the herd (Deer.act makes for its place in it), or one of the wolves on its trail,
 * keeping behind its stragglers until, after dark, the pack runs one down (a HuntPack of the
 * herd's own for the night). The tag carries the herd's road and the beast's place in the herd,
 * so it knows where that place is at any turn without the world's help; it keeps the beast from
 * the hour's cull and from the parked store (the herd puts its beasts back where they should be).
 * It lives in the mobs package because it steers a wolf through members only the package can
 * reach (target and the AI states).
 */
public class Migrating extends Buff {

	{
		type = buffType.NEUTRAL;
	}

	//the migration (WorldEvents id) and the beast's place in its herd (MigrationEvent.WOLVES on: a wolf's)
	public long herd = Long.MIN_VALUE;
	public int slot = 0;
	//the herd's road, world cells, and its times (WorldClock turns)
	public int x0, y0, x1, y1;
	public int startTurn, endTurn;
	//its place: cells behind the herd's head, and to the right of its line
	public int offset, lateral;
	//a wolf's: the night its pack last ran a straggler down (WorldClock.night), and whether a hero
	//crowded or hurt it - then it fights him like any wolf
	public int huntedNight = Integer.MIN_VALUE;
	public boolean defending = false;
	//steer()'s decision this turn (null: nobody), not bundled
	public Char choice;

	public boolean wolf(){
		return slot >= MigrationEvent.WOLVES;
	}

	/** The window cell its place in the herd is on now, or -1 when that is off the window or off
	 *  the herd's road (not on it yet, or past its end). */
	public int cellNow(){
		if (!(Dungeon.level instanceof OverworldLevel)) return -1;
		int turn = ((OverworldLevel) Dungeon.level).eventTurn();
		int[] at = MigrationEvent.placeAt( x0, y0, x1, y1, startTurn, endTurn, offset, lateral, turn );
		return at == null ? -1 : HuntPack.cellOf( at[0], at[1] );
	}

	/**
	 * Decides a trailing wolf's turn (GrayWolf, BrownWolf.chooseEnemy): true when the herd's choice
	 * stands (in `choice`, null meaning nobody), false when the wolf should choose as any wolf does -
	 * out on the night's hunt (its HuntPack decides), turned, charmed, maddened, frightened, asleep
	 * or fleeing, or provoked. A hero who comes within HuntPack.CROWDING of it is its enemy from then
	 * on; otherwise it wanders at ease toward its place behind the herd.
	 */
	public boolean steer( Mob wolf ){
		choice = null;
		if (wolf.buff( HuntPack.class ) != null || wolf.alignment != Char.Alignment.ENEMY || defending) return false;
		if ((wolf.state != wolf.WANDERING && wolf.state != wolf.HUNTING)
				|| wolf.buff( Terror.class ) != null || wolf.buff( Dread.class ) != null
				|| wolf.buff( Amok.class ) != null || wolf.buff( Charm.class ) != null
				|| wolf.buff( Sleep.class ) != null || wolf.buff( MagicalSleep.class ) != null) return false;
		for (Hero h : OverworldLevel.heroesOn( Dungeon.level )){
			if (h.invisible <= 0 && Dungeon.level.distance( wolf.pos, h.pos ) <= HuntPack.CROWDING){
				defending = true;
				return false;
			}
		}
		int cell = cellNow();
		wolf.state = wolf.WANDERING;
		wolf.target = cell != -1 ? cell : wolf.pos;
		return true;
	}

	/** A blow at a trailing wolf from the hero's side (HuntPack.byHero): every wolf of its herd
	 *  fights him now. */
	public void provoked(){
		defending = true;
		for (Mob m : Dungeon.level.mobs){
			Migrating o = m.buff( Migrating.class );
			if (o != null && o.herd == herd && o.wolf()) o.defending = true;
		}
	}

	private static final String HERD       = "herd";
	private static final String SLOT       = "slot";
	private static final String X0         = "x0";
	private static final String Y0         = "y0";
	private static final String X1         = "x1";
	private static final String Y1         = "y1";
	private static final String START      = "start_turn";
	private static final String END        = "end_turn";
	private static final String OFFSET     = "offset";
	private static final String LATERAL    = "lateral";
	private static final String HUNTED     = "hunted_night";
	private static final String DEFENDING  = "defending";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HERD, herd );
		bundle.put( SLOT, slot );
		bundle.put( X0, x0 );
		bundle.put( Y0, y0 );
		bundle.put( X1, x1 );
		bundle.put( Y1, y1 );
		bundle.put( START, startTurn );
		bundle.put( END, endTurn );
		bundle.put( OFFSET, offset );
		bundle.put( LATERAL, lateral );
		bundle.put( HUNTED, huntedNight );
		bundle.put( DEFENDING, defending );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		herd        = bundle.contains( HERD )      ? bundle.getLong( HERD )    : Long.MIN_VALUE;
		slot        = bundle.contains( SLOT )      ? bundle.getInt( SLOT )     : 0;
		x0          = bundle.contains( X0 )        ? bundle.getInt( X0 )       : 0;
		y0          = bundle.contains( Y0 )        ? bundle.getInt( Y0 )       : 0;
		x1          = bundle.contains( X1 )        ? bundle.getInt( X1 )       : 0;
		y1          = bundle.contains( Y1 )        ? bundle.getInt( Y1 )       : 0;
		startTurn   = bundle.contains( START )     ? bundle.getInt( START )    : 0;
		endTurn     = bundle.contains( END )       ? bundle.getInt( END )      : 0;
		offset      = bundle.contains( OFFSET )    ? bundle.getInt( OFFSET )   : 0;
		lateral     = bundle.contains( LATERAL )   ? bundle.getInt( LATERAL )  : 0;
		huntedNight = bundle.contains( HUNTED )    ? bundle.getInt( HUNTED )   : Integer.MIN_VALUE;
		defending   = bundle.contains( DEFENDING ) && bundle.getBoolean( DEFENDING );
	}
}
