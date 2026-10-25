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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.NoticeBoardSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * A village's NOTICE BOARD: a little roofed board on two posts beside the well of every village of
 * people (BountySlice puts it down), the village's notices pinned to it (Bounties). The hero reads it
 * by stepping up to it: its window (WndNoticeBoard) lists what is up, what he took from it and his
 * standing there; he takes notices down, collects his pay and gives notices up there. Its picture
 * shows how many notices are still up and, with a gold coin pinned on, that a reward waits for him.
 *
 * It is the village's, not the hero's: never hurt, never moved, never touched by a buff; an NPC, so
 * the surface keeps it for good, parked with the window and put back like the villagers - and stepping
 * back onto its own cell when that put it a cell off. A co-op guest reads it (the host's notices, shipped
 * as an info dialog), but only the host takes them.
 */
public class NoticeBoard extends NPC {

	{
		spriteClass = NoticeBoardSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	/** Its village (a sector key). */
	public long key = Long.MIN_VALUE;
	/** The world cell it was put down on. */
	public int wx, wy;
	/** Its village's name. */
	public String place = "";

	@Override
	public String description(){
		return Messages.get( this, "desc", place );
	}

	@Override
	protected boolean act(){
		//put back a cell off its own (someone stood there as the window came back): home again once
		//nobody is near enough to see it go
		if (Dungeon.level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			int home = ow.localCell( wx, wy );
			//(a move reads the sprite: none before it has one)
			if (sprite != null && home != -1 && home != pos && Actor.findChar( home ) == null && ow.passable[home]
					&& OverworldLevel.heroDistance( ow, pos ) > 8 && OverworldLevel.heroDistance( ow, home ) > 8){
				move( home, false );
				sprite.place( home );
			}
		}
		spend( TICK );
		return true;
	}

	@Override
	public void damage( int dmg, Object src ){
		//nails and planks the village keeps mended
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public void beckon( int cell ){
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero) || !(Dungeon.level instanceof OverworldLevel)) return true;
		Hero hero = (Hero) c;
		final long seed = ((OverworldLevel) Dungeon.level).worldSeed;
		if (hero.isRemote){
			//a guest reads the board; the notices are the host's to take
			NetDialogs.handleNetHero( hero, BountyActions.boardText( seed, key ) );
			return true;
		}
		if (hero != Dungeon.hero) return true;
		if (!GuideProgress.pageFound( "surface_bounties" )) GuideGraph.reveal( GuideGraph.TAG_BOUNTY );
		Game.runOnRenderThread( () -> GameScene.show( new WndNoticeBoard( this, seed ) ) );
		return true;
	}

	private static final String KEY = "key", WX = "wx", WY = "wy", PLACE = "place";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( KEY, key );
		bundle.put( WX, wx );
		bundle.put( WY, wy );
		bundle.put( PLACE, place == null ? "" : place );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		key = bundle.getLong( KEY );
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
		place = bundle.getString( PLACE );
	}
}
