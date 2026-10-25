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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.windows.WndFisher;
import com.watabou.noosa.Game;

/**
 * The villages' fishers (a settler of the trade VillageRoutine.Role.FISHER, with water to fish):
 * spoken to, one tells the hero what is on his mind and trades (WndFisher) - he buys every raw fish
 * the hero carries at BUYS_AT its worth, and sells a fishing rod and earthworms by the handful, at
 * prices the town's scale steps up with the deepest floor reached (TownLedger.scaled). The host's own
 * hero's: a co-op guest's settlers only chat, as they always have.
 */
public final class FisherTrade {

	private FisherTrade(){}

	/** A rod's price and a handful of worms', before the town's scale; how many worms a handful is. */
	public static final int ROD = 80, WORMS = 15, HANDFUL = 5;
	/** What a fisher pays for a raw fish over its worth. */
	public static final float BUYS_AT = 1.5f;

	public static int rodPrice(){
		return TownLedger.scaled( ROD );
	}

	public static int wormsPrice(){
		return TownLedger.scaled( WORMS );
	}

	/** What a fisher pays for one stack of raw fish. */
	public static int offerFor( Item fish ){
		return Math.round( fish.value() * BUYS_AT );
	}

	/** How many raw fish the hero carries, all told. */
	public static int fishCount( Hero hero ){
		int n = 0;
		if (hero != null) for (CaughtFish f : hero.belongings.getAllItems( CaughtFish.class )) n += f.quantity();
		return n;
	}

	/** What a fisher pays for every raw fish the hero carries. */
	public static int fishOffer( Hero hero ){
		int gold = 0;
		if (hero != null) for (CaughtFish f : hero.belongings.getAllItems( CaughtFish.class )) gold += offerFor( f );
		return gold;
	}

	/** Settler.interact: a fisher at his trade talks (his line) and opens his trade. Any thread. */
	public static void offer( final Settler fisher, final String line ){
		Game.runOnRenderThread( () -> GameScene.show( new WndFisher( fisher, line ) ) );
	}

	/** Every raw fish the hero carries sold to the fisher: the gold paid. Render thread (his window). */
	public static int sellFish( Hero hero ){
		int gold = 0;
		for (CaughtFish f : hero.belongings.getAllItems( CaughtFish.class )){
			gold += offerFor( f );
			f.detachAll( hero.belongings.backpack );
		}
		if (gold > 0) Shopkeeper.paySale( hero, gold );
		return gold;
	}

	/** One of the fisher's goods bought: false when the hero cannot pay. Into the pack, or at his feet. */
	public static boolean buy( Hero hero, Item item, int price ){
		if (Dungeon.gold < price) return false;
		Dungeon.gold -= price;
		SpatialSound.play( Assets.Sounds.GOLD, hero, 0.8f, 1f );
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
		return true;
	}
}
