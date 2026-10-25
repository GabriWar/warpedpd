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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * What the net, the rod and their catches share: who may use them, a line for their user alone, the
 * bait an item is (baitOf), and the Descent Guide's pages they write into it when first carried.
 *
 * The net and the rod are the host's own hero's (as the shovel's and the sickle's work is,
 * homestead/gathering/Gathering.mayWork): the critters are pictures each machine draws for itself,
 * and the line's bobber is drawn on the host's screen alone. A co-op guest is told so.
 */
public final class Catching {

	private Catching(){}

	/** The guide's pages: the net's, the rod's (GuideGraph). */
	public static final String PAGE_NET = "hs_catching", PAGE_ROD = "hs_fishing";

	/** May this hero use a net or a rod here? The host's own hero (or a lone one's); a guest is told. */
	public static boolean mayUse( Hero hero ){
		boolean mirror = Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).isMirror();
		if (hero == Dungeon.hero && !mirror && !NetManager.isNetClient()) return true;
		tell( hero, Messages.get( Catching.class, "guest" ) );
		return false;
	}

	/** A line for this hero alone (a co-op guest's on his own screen). */
	public static void tell( Hero hero, String text ){
		NetManager.heroLog( hero, GLog.WARNING + text );
	}

	/** The bait an item is on a hook, null for none. */
	public static Bait baitOf( Item item ){
		if (item instanceof Earthworm) return Bait.WORM;
		if (item instanceof CritterJar) return ((CritterJar) item).critter.bait;
		return null;
	}

	/** An item that came into a bag: the hero's first net or jar writes the net's page, his first
	 *  rod or bait the rod's (GuideGraph.TAG_NET, TAG_ROD). */
	public static void carried( Item item, Bag container ){
		if (Dungeon.hero == null || container == null || container.owner != Dungeon.hero) return;
		if (item instanceof BugNet || item instanceof CritterJar){
			if (!GuideProgress.pageFound( PAGE_NET )) GuideGraph.reveal( GuideGraph.TAG_NET );
		} else if (item instanceof FishingRod || item instanceof Earthworm){
			if (!GuideProgress.pageFound( PAGE_ROD )) GuideGraph.reveal( GuideGraph.TAG_ROD );
		}
	}
}
