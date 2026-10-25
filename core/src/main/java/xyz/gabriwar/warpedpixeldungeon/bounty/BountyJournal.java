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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollingGridPane;
import xyz.gabriwar.warpedpixeldungeon.windows.WndJournal;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;

import java.util.List;

/**
 * The notices the hero carries, in the journal's notes (WndJournal): a square each, with its kind's
 * picture - green where its pay waits at its board, red where a parcel is late - and a tap shows the
 * whole of it: what it asks, where, what it pays and how it stands, with the choice to give it up
 * (the host's hero alone; a co-op guest reads the host's, BountyShare).
 */
public final class BountyJournal {

	private BountyJournal(){}

	/** The notices' header and squares, after the journal's own notes; nothing when none is taken. */
	public static void addTo( ScrollingGridPane grid ){
		BountyWorld w = BountyShare.world();
		if (w == null) return;
		List<Bounty> taken = w.taken();
		if (taken.isEmpty()) return;
		//the world's seed as the level knows it (a guest's mirror carries the host's), else the run's
		final long seed = Dungeon.level instanceof OverworldLevel ? ((OverworldLevel) Dungeon.level).worldSeed
				: OverworldLevel.worldSeedOf( Dungeon.seed );
		grid.addHeader( "_" + Messages.get( BountyJournal.class, "header" ) + "_ (" + taken.size() + "/" + Bounties.MAX_TAKEN + ")" );
		for (final Bounty b : taken){
			ScrollingGridPane.GridItem item = new ScrollingGridPane.GridItem( new ItemSprite( Bounties.icon( b.kind ) ) ){
				@Override
				public boolean onClick( float x, float y ){
					if (!inside( x, y )) return false;
					show( b, seed );
					return true;
				}
			};
			grid.addItem( item );
			if (WndNoticeBoard.stateColor( b, NetManager.isNetClient() ? null : Dungeon.hero ) == WndNoticeBoard.READY_COLOR){
				item.hardLightBG( 0.6f, 1.6f, 0.6f );
			} else if (WndNoticeBoard.stateColor( b, null ) == WndNoticeBoard.LATE_COLOR){
				item.hardLightBG( 1.6f, 0.6f, 0.6f );
			}
		}
	}

	/** A notice's whole page: from which board, what it asks and where, its pay and how it stands. */
	static String page( Bounty b, long seed, Hero hero ){
		return Messages.get( BountyJournal.class, "from", Bounties.villageName( seed, b.village ) ) + "\n\n"
				+ Bounties.hint( seed, b ) + "\n\n"
				+ Messages.get( WndNoticeBoard.class, "pays", Bounties.pay( b.reward, b.bonus ) ) + "\n\n"
				+ WndNoticeBoard.state( b, hero );
	}

	private static void show( final Bounty b, final long seed ){
		ItemSprite icon = new ItemSprite( Bounties.icon( b.kind ) );
		String title = Bounties.title( seed, b );
		//a guest reads the host's notices; only the host gives one up
		if (NetManager.isNetClient() || Dungeon.hero == null){
			GameScene.show( new WndTitledMessage( icon, title, page( b, seed, null ) ) );
			return;
		}
		final Hero hero = Dungeon.hero;
		GameScene.show( new WndOptions( icon, title, page( b, seed, hero ),
				Messages.get( WndNoticeBoard.class, "keep" ), Messages.get( WndNoticeBoard.class, "give_up" ) ){
			@Override
			protected void onSelect( int index ){
				if (index != 1) return;
				BountyActions.abandon( hero, seed, b );
				//the journal shows the list as it is now (a new one closes the old, on the notes)
				WndJournal.last_index = WndJournal.NOTES;
				GameScene.show( new WndJournal() );
			}
		} );
	}
}
