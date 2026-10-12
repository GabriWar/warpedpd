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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * The hero up a watchtower (Watchtower.interact): he sees Watchtower.VIEW_BONUS cells further, and
 * the wolves and bandits coming for a home within Watchtower.SPOT_RANGE of the tower are shown to
 * him with a warning (ThreatSlice). He climbs down the moment he steps off, or the tower is gone.
 */
public class Lookout extends Buff {

	{
		type = buffType.POSITIVE;
	}

	//the world cell he climbed from: a window's move relabels his window cell, never this
	int standX, standY;

	/** Up the tower beside his cell. */
	public static Lookout climb( Char hero, OverworldLevel level ){
		Lookout l = Buff.affect( hero, Lookout.class );
		int w = level.width();
		l.standX = level.worldX + hero.pos % w;
		l.standY = level.worldY + hero.pos / w;
		l.see();
		return l;
	}

	/** What the hero sees with no tower under him: the level's sight, or his light's. */
	static int baseView( Char ch ){
		if (Dungeon.level == null) return ch.viewDistance;
		return ch.buff( Light.class ) != null ? Math.max( Light.DISTANCE, Dungeon.level.viewDistance ) : Dungeon.level.viewDistance;
	}

	//his sight from the top (a light lit or put out while he is up resets it: put back each turn)
	private void see(){
		int want = baseView( target ) + Watchtower.VIEW_BONUS;
		if (target.viewDistance != want){
			target.viewDistance = want;
			refresh( target );
		}
	}

	//what the hero sees, worked out again on the screen
	private static void refresh( Char ch ){
		if (ch == Dungeon.hero && Dungeon.level instanceof OverworldLevel && HomeThreats.live( (OverworldLevel) Dungeon.level )){
			Dungeon.observe();
		}
	}

	/** The tower he is up, or null when he has stepped off or it is gone. */
	public Watchtower tower(){
		if (!(Dungeon.level instanceof OverworldLevel) || target == null) return null;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ow.localCell( standX, standY ) != target.pos) return null;
		for (Mob m : ow.mobs){
			if (m instanceof Watchtower && ow.adjacent( m.pos, target.pos )) return (Watchtower) m;
		}
		return null;
	}

	@Override
	public boolean act(){
		if (tower() == null){
			detach();
		} else {
			see();
			spend( TICK );
		}
		return true;
	}

	@Override
	public void detach(){
		Char ch = target;
		super.detach();
		ch.viewDistance = baseView( ch );
		refresh( ch );
	}

	@Override
	public int icon(){
		return BuffIndicator.FORESIGHT;
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", Watchtower.VIEW_BONUS, Watchtower.SPOT_RANGE );
	}

	private static final String STAND_X = "stand_x", STAND_Y = "stand_y";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STAND_X, standX );
		bundle.put( STAND_Y, standY );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		standX = bundle.getInt( STAND_X );
		standY = bundle.getInt( STAND_Y );
	}
}
