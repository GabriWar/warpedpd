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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.ClericSpell;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundle;

/**
 * A beast out on a world event's hunt (levels.overworld.HuntEvent). The tag keeps it from the
 * hour's cull and from the parked store. For the wolves it is also the pack's mind: run the
 * quarry down, feed at the kill, and turn on the hero only when he crowds them or draws blood.
 * It lives in the mobs package because it steers a Mob through members only the package can
 * reach (target and the AI states).
 */
public class HuntPack extends Buff {

	/** How long the pack stays by its kill, in the wolves' turns. */
	public static final int FEED_TURNS = 150;
	/** A hero (any of the party) this close to a pack wolf is crowding it: it turns on him. */
	public static final int CROWDING = 2;

	{
		type = buffType.NEUTRAL;
	}

	//the hunt's event id and its night (WorldClock.night), and its hunting ground, world cells
	public long hunt = Long.MIN_VALUE;
	public int day = Integer.MIN_VALUE;
	public int ax, ay;
	//the hero crowded or hurt the pack: this wolf fights him now, like any wolf
	public boolean defending = false;
	//at the kill (world cells), and the turns left to feed there
	public boolean feeding = false;
	public int feedX, feedY, feedLeft;
	//steer()'s decision this turn (null: nobody), not bundled
	public Char choice;

	public static boolean isWolf( Object o ){
		return o instanceof GrayWolf || o instanceof BrownWolf;
	}

	//the world cell of a cell of the level being played, and back: the surface window's corner
	//is its origin, any other level's is its own (0, 0). -1 off the level's interior
	static int cellOf( int wx, int wy ){
		Level l = Dungeon.level;
		int x = wx - originX( l ), y = wy - originY( l );
		return x <= 0 || y <= 0 || x >= l.width() - 1 || y >= l.height() - 1 ? -1 : x + y * l.width();
	}

	public static int worldX( int cell ){
		return originX( Dungeon.level ) + cell % Dungeon.level.width();
	}

	public static int worldY( int cell ){
		return originY( Dungeon.level ) + cell / Dungeon.level.width();
	}

	private static int originX( Level l ){
		return l instanceof OverworldLevel ? ((OverworldLevel) l).worldX() : 0;
	}

	private static int originY( Level l ){
		return l instanceof OverworldLevel ? ((OverworldLevel) l).worldY() : 0;
	}

	/** Sets a pack wolf on its quarry as the hunt begins. */
	public static void loose( Mob wolf, Mob quarry ){
		wolf.aggro( quarry );
		wolf.target = quarry.pos;
	}

	/** The kill, world cell (wx, wy): this wolf stays by it for FEED_TURNS, at ease unless crowded or hurt. */
	public void feedAt( int wx, int wy ){
		feeding = true;
		feedX = wx;
		feedY = wy;
		feedLeft = FEED_TURNS;
	}

	/** A blow at a pack wolf from the hero's side (his weapon, a wand, a spell, an ally): the whole
	 *  pack turns on him. */
	public static boolean byHero( Object src ){
		return src instanceof Hero
				|| (src instanceof Char && ((Char) src).alignment == Char.Alignment.ALLY)
				|| src instanceof Wand || src instanceof ClericSpell || src instanceof ArmorAbility;
	}

	/** This wolf was hurt by the hero's side: it and every wolf of its hunt fight him now. */
	public void provoked(){
		defending = true;
		for (Mob m : Dungeon.level.mobs){
			if (!isWolf( m )) continue;
			HuntPack p = m.buff( HuntPack.class );
			if (p != null && p.hunt == hunt) p.defending = true;
		}
	}

	/**
	 * Decides a pack wolf's turn: true when the pack's choice stands (in `choice`, null meaning
	 * nobody), false when the wolf should choose as any wolf does - turned, charmed, maddened,
	 * frightened, asleep or fleeing, provoked, or with nothing left to chase.
	 */
	public boolean steer( Mob wolf ){
		choice = null;
		if (wolf.alignment != Char.Alignment.ENEMY || defending) return false;
		if ((wolf.state != wolf.WANDERING && wolf.state != wolf.HUNTING)
				|| wolf.buff( Terror.class ) != null || wolf.buff( Dread.class ) != null
				|| wolf.buff( Amok.class ) != null || wolf.buff( Charm.class ) != null
				|| wolf.buff( Sleep.class ) != null || wolf.buff( MagicalSleep.class ) != null) return false;

		//any hero of the party crowding it: the nearest
		Hero hero = null;
		int near = CROWDING + 1;
		for (Hero h : OverworldLevel.heroesOn( Dungeon.level )){
			int d = Dungeon.level.distance( wolf.pos, h.pos );
			if (h.invisible <= 0 && d < near){
				near = d;
				hero = h;
			}
		}
		if (hero != null){
			defending = true;
			choice = hero;
			wolf.state = wolf.HUNTING;
			if (sees( wolf, hero.pos )) wolf.target = hero.pos;
			return true;
		}

		if (feeding){
			int kill = cellOf( feedX, feedY );
			//fed, or the kill slid out of the world: a wolf like any other
			if (--feedLeft <= 0 || kill == -1){
				detach();
				wolf.state = wolf.WANDERING;
				return false;
			}
			//at ease by the kill: wandering with the target pinned to it (or to where it lies)
			wolf.state = wolf.WANDERING;
			wolf.target = Dungeon.level.distance( wolf.pos, kill ) <= 1 ? wolf.pos : kill;
			return true;
		}

		Mob quarry = null;
		int best = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof Deer) || !m.isAlive()) continue;
			HuntPack p = m.buff( HuntPack.class );
			if (p == null || p.hunt != hunt) continue;
			int d = Dungeon.level.distance( wolf.pos, m.pos );
			if (d < best){
				best = d;
				quarry = m;
			}
		}
		//nothing left to chase: a wolf like any other
		if (quarry == null) return false;
		choice = quarry;
		wolf.state = wolf.HUNTING;
		//the pack runs by scent: it knows where the deer is even out of sight
		if (!sees( wolf, quarry.pos )) wolf.target = quarry.pos;
		return true;
	}

	private static boolean sees( Mob wolf, int cell ){
		return wolf.fieldOfView != null && wolf.fieldOfView.length == Dungeon.level.length()
				&& cell >= 0 && cell < wolf.fieldOfView.length && wolf.fieldOfView[cell];
	}

	private static final String HUNT      = "hunt";
	private static final String DAY       = "day";
	private static final String AX        = "ax";
	private static final String AY        = "ay";
	private static final String DEFENDING = "defending";
	private static final String FEEDING   = "feeding";
	private static final String FEED_X    = "feed_x";
	private static final String FEED_Y    = "feed_y";
	private static final String FEED_LEFT = "feed_left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( HUNT, hunt );
		bundle.put( DAY, day );
		bundle.put( AX, ax );
		bundle.put( AY, ay );
		bundle.put( DEFENDING, defending );
		bundle.put( FEEDING, feeding );
		bundle.put( FEED_X, feedX );
		bundle.put( FEED_Y, feedY );
		bundle.put( FEED_LEFT, feedLeft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		hunt      = bundle.contains( HUNT )      ? bundle.getLong( HUNT )         : Long.MIN_VALUE;
		day       = bundle.contains( DAY )       ? bundle.getInt( DAY )           : Integer.MIN_VALUE;
		ax        = bundle.contains( AX )        ? bundle.getInt( AX )            : 0;
		ay        = bundle.contains( AY )        ? bundle.getInt( AY )            : 0;
		defending = bundle.contains( DEFENDING ) && bundle.getBoolean( DEFENDING );
		feeding   = bundle.contains( FEEDING )   && bundle.getBoolean( FEEDING );
		feedX     = bundle.contains( FEED_X )    ? bundle.getInt( FEED_X )        : 0;
		feedY     = bundle.contains( FEED_Y )    ? bundle.getInt( FEED_Y )        : 0;
		feedLeft  = bundle.contains( FEED_LEFT ) ? bundle.getInt( FEED_LEFT )     : 0;
	}
}
