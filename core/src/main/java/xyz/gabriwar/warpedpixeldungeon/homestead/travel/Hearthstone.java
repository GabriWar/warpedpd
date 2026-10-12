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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeAnchors;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HearthstoneSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * A HEARTHSTONE: the low carved stone that makes a place a HOME (TravelPieces builds it; its
 * listener founds the home in HomesteadWorld and lets it go when the stone comes down). It keeps
 * wild animals away (HomeAnchors: every home is an anchor), it is a waystone of the hero's own
 * (Travel), and its menu (WndHome) travels, renames the home and offers what the homesteads' other
 * parts do at a home (HomeActions). Its rune lies dormant by day and glows after dark.
 *
 * A built piece: its health is the piece's (Homestead.damage), so blows on the mob itself do
 * nothing, and no buff takes. Kept for good by the slice (HomesteadActor).
 */
public class Hearthstone extends Mob implements HomesteadActor {

	{
		spriteClass = HearthstoneSprite.class;

		HP = HT = 1;
		defenseSkill = 0;
		EXP = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	/** The world cell of its home (set as it goes up: TravelPieces). */
	public int wx, wy;

	/** Its home, or null (on no slice, or a home already gone). */
	public HomesteadWorld.Home home(){
		if (!(Dungeon.level instanceof OverworldLevel)) return null;
		return HomesteadWorld.homeAt( WorldLayers.depthOf( ((OverworldLevel) Dungeon.level).altitude() ), wx, wy );
	}

	@Override
	public String name(){
		HomesteadWorld.Home h = home();
		return h == null ? Messages.get( this, "name" ) : Messages.get( this, "name_of", h.title() );
	}

	@Override
	public String description(){
		return Messages.get( this, "desc", HomeAnchors.RADIUS );
	}

	@Override
	protected boolean act(){
		spend( TICK );
		return true;
	}

	@Override
	public void damage( int dmg, Object src ){
		//a built piece: hurt only through Homestead.damage, which keeps its health
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
		if (!(c instanceof Hero)) return true;
		final HomesteadWorld.Home home = home();
		if (home == null) return true;
		Hero hero = (Hero) c;
		if (hero.isRemote){
			NetDialogs.handleNetHero( hero, Messages.get( this, "guest", home.title() ) );
			return true;
		}
		if (hero != Dungeon.hero) return true;
		Game.runOnRenderThread( () -> GameScene.show( new WndHome( home ) ) );
		return true;
	}

	/**
	 * The hearthstone piece's listener (TravelPieces): going up, the stone founds its home - on its
	 * slice and world cell, named after where it stands (HomeNames), visited now - and coming down
	 * (taken down, or wrecked), the home goes with it.
	 */
	public static class Founding implements BuildPiece.Listener {

		@Override
		public void placed( OverworldLevel level, int cell, Hero builder ){
			int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
			for (Mob m : level.mobs){
				if (m.pos == cell && m instanceof Hearthstone){
					((Hearthstone) m).wx = wx;
					((Hearthstone) m).wy = wy;
				}
			}
			HomesteadWorld.Home home = new HomesteadWorld.Home( WorldLayers.depthOf( level.altitude() ), wx, wy,
					HomeNames.forNewHome( level.worldSeed, level.altitude(), wx, wy ), WorldClock.day() );
			home.visited();
			HomesteadWorld.addHome( home );
			if (builder != null && builder == Dungeon.hero && Dungeon.level == level){
				GLog.p( Messages.get( Hearthstone.class, "founded", home.title() ) );
				xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_STONES );
			}
		}

		@Override
		public void removed( OverworldLevel level, int cell ){
			int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
			int depth = WorldLayers.depthOf( level.altitude() );
			HomesteadWorld.Home home = HomesteadWorld.homeAt( depth, wx, wy );
			if (home == null) return;
			HomesteadWorld.removeHome( depth, wx, wy );
			if (Dungeon.level == level) GLog.w( Messages.get( Hearthstone.class, "lost", home.title() ) );
		}
	}

	private static final String WX = "wx", WY = "wy";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( WX, wx );
		bundle.put( WY, wy );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
	}
}
