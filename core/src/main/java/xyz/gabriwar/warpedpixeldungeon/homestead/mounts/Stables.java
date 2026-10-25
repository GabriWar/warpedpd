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

package xyz.gabriwar.warpedpixeldungeon.homestead.mounts;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Saddle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * A village's stable (OverworldShopkeeper.STABLE): every human settlement big enough for a trader
 * (a village or larger, VillageRoutine.vendorHouses) has a stablehand besides its usual traders,
 * on the first open spot of the ring STALL cells round its well, from a corner its key picks. He
 * keeps a saddled horse for sale beside him, HORSE_PRICE gold before the town's scaling
 * (TownLedger.scaled) and the keeper's mood (a raid, a rescue: Shopkeeper.priceFactor), and a
 * saddle and hay (thatch) on his rail (lay). A tap on the horse offers it (offer); bought, it is
 * the hero's (buy), and the next restock brings another. He trades by day, as every trader does.
 *
 * Set up once per village, kept in the run's record (StableWorld). Host only.
 */
public final class Stables {

	private Stables(){}

	/** A horse's price, saddle and all, before the town's scaling. */
	public static final int HORSE_PRICE = 200;
	/** How far from the well the stable stands, each way. */
	public static final int STALL = 4;

	/**
	 * Called as the window populates a settlement (OverworldLevel.populateSettlement): sets the
	 * stablehand up when this village should have one and never had it. Host only.
	 */
	public static void settle( OverworldLevel ow, long key ){
		if (ow.altitude() != 0 || ow.isMirror()) return;
		StableWorld world = HomesteadWorld.section( StableWorld.class );
		if (world == null || world.has( key )) return;
		VillageRoutine.Settlement s = VillageRoutine.settlement( ow.worldSeed(), key );
		if (s == null || s.gnoll || VillageRoutine.vendorHouses( s.houses() ) < 1) return;
		int at = stall( ow, s.cx, s.cy, key );
		if (at == -1) return;
		OverworldShopkeeper keeper = OverworldShopkeeper.of( OverworldShopkeeper.STABLE, key );
		if (ow.addMob( keeper, at )){
			keeper.openShop();
			world.set( key );
		}
	}

	/** The stable's cell for a village whose well is on a world cell: the first open spot of the
	 *  ring STALL cells round it, from a place on the ring its key picks; -1 for none in the window. */
	public static int stall( OverworldLevel ow, int cx, int cy, long key ){
		int ring = STALL * 8;
		int first = (int) Math.floorMod( (key * 0x9E3779B97F4A7C15L) >>> 33, (long) ring );
		for (int i = 0; i < ring; i++){
			int[] d = ringOffset( (first + i) % ring, STALL );
			int c = ow.localCell( cx + d[0], cy + d[1] );
			if (c != -1 && stallCell( ow, c )) return c;
		}
		return -1;
	}

	//the i-th cell of the square ring of radius r round a middle, clockwise from its north-west corner
	static int[] ringOffset( int i, int r ){
		int side = 2 * r;
		int edge = i / side, k = i % side;
		switch (edge){
			case 0:  return new int[]{ -r + k, -r };
			case 1:  return new int[]{ r, -r + k };
			case 2:  return new int[]{ r - k, r };
			default: return new int[]{ -r, r - k };
		}
	}

	//a cell a stablehand can keep his stable on: open ground out of doors, no house's floor, nobody
	//there, with room beside it for his horse and his rail. By the window's own width: on the
	//window's first build the path finder may still be sized for the level left
	private static boolean stallCell( OverworldLevel ow, int c ){
		if (!ow.insideMap( c ) || !ow.passable[c] || ow.pit[c] || ow.water[c] || Mounts.indoors( ow, c )
				|| ow.map[c] == Terrain.EMPTY_SP || taken( ow, c )) return false;
		int room = 0, w = ow.width();
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int nc = c + dx + dy * w;
				if ((dx != 0 || dy != 0) && nc >= 0 && nc < ow.length() && Mounts.standable( ow, nc ) && !ow.water[nc]
						&& ow.heaps.get( nc ) == null && !taken( ow, nc ) && ow.map[nc] != Terrain.EMPTY_SP) room++;
			}
		}
		return room >= 3;
	}

	//someone stands there, live or only listed (on the window's first build nobody is on the scheduler yet)
	private static boolean taken( Level level, int c ){
		if (Actor.findChar( c ) != null) return true;
		for (Mob m : level.mobs) if (m.pos == c) return true;
		return false;
	}

	/** A fresh rail of the stablehand's goods: a saddle and hay. */
	public static ArrayList<Item> stock(){
		ArrayList<Item> stock = new ArrayList<>();
		stock.add( new Saddle() );
		stock.add( new Thatch().quantity( Random.IntRange( 6, 10 ) ) );
		return stock;
	}

	/**
	 * The stablehand restocks (OverworldShopkeeper.restock): each of his goods not still on his
	 * rail (a ware for sale within two cells of him) is laid out beside him, and a saddled horse
	 * for sale stands beside him again when the last one was sold. How many things were laid.
	 */
	public static int lay( Mob keeper ){
		Level level = Dungeon.level;
		int w = level.width(), kx = keeper.pos % w, ky = keeper.pos / w;
		HashSet<Class<?>> shown = new HashSet<>();
		ArrayList<Integer> near = new ArrayList<>(), far = new ArrayList<>();
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int x = kx + dx, y = ky + dy;
				if ((dx == 0 && dy == 0) || x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				int c = x + y * w;
				Heap h = level.heaps.get( c );
				if (h != null && h.type == Heap.Type.FOR_SALE && !h.isEmpty()) shown.add( h.peek().getClass() );
				if (railCell( level, c )) (Math.max( Math.abs( dx ), Math.abs( dy ) ) == 1 ? near : far).add( c );
			}
		}
		int placed = 0;
		//the horse first, beside him, so the rail never takes its place
		long key = keeper instanceof OverworldShopkeeper ? ((OverworldShopkeeper) keeper).homeSector : Long.MIN_VALUE;
		if (horseFor( level, key ) == null && level instanceof OverworldLevel){
			int at = -1;
			for (int c : near) if (Mounts.standable( level, c ) && !level.water[c]){ at = c; break; }
			if (at != -1){
				near.remove( (Integer) at );
				OverworldLevel ow = (OverworldLevel) level;
				Horse h = new Horse();
				h.forSale = true;
				h.saddled = true;
				h.stable = key;
				h.stallX = ow.worldX + at % w;
				h.stallY = ow.worldY + at / w;
				h.setLook();
				Mounts.setDown( level, h, at, -1 );
				placed++;
			}
		}
		near.addAll( far );
		for (Item it : stock()){
			if (near.isEmpty()) break;
			if (shown.contains( it.getClass() )) continue;
			level.drop( it, near.remove( 0 ) ).type = Heap.Type.FOR_SALE;
			placed++;
		}
		return placed;
	}

	//a cell a ware may be laid on: open floor with nothing lying there and nobody on it
	private static boolean railCell( Level level, int c ){
		return c >= 0 && c < level.length() && level.insideMap( c ) && level.passable[c] && !level.pit[c]
				&& level.heaps.get( c ) == null && !taken( level, c );
	}

	/** The horse this stable has for sale on the level, wherever it stands (an unparked keeper can be
	 *  set down a few cells off his stall); null for none. */
	public static Horse horseFor( Level level, long key ){
		for (Mob m : level.mobs){
			if (m instanceof Horse && ((Horse) m).forSale && ((Horse) m).stable == key && m.isAlive()) return (Horse) m;
		}
		return null;
	}

	/** A stable's horse's turn (Horse.act): it keeps within a step of its stall, a step now and
	 *  then. */
	public static void keep( Horse h ){
		Level level = Dungeon.level;
		int stall = level instanceof OverworldLevel ? ((OverworldLevel) level).localCell( h.stallX, h.stallY ) : -1;
		if (stall == -1) return;
		if (level.distance( h.pos, stall ) > 1){
			int step = Roping.stepToward( h, stall );
			if (step != -1) Roping.moveTo( h, step );
			return;
		}
		if (Random.Int( 4 ) != 0) return;
		int c = h.pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		if (Mounts.standable( level, c ) && !level.water[c] && level.distance( c, stall ) <= 1) Roping.moveTo( h, c );
	}

	//the stablehand who keeps a horse: the keeper of its stable on the level, null for none
	static Shopkeeper keeperOf( Horse h ){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).speciality == OverworldShopkeeper.STABLE
					&& ((OverworldShopkeeper) m).homeSector == h.stable) return (Shopkeeper) m;
		}
		return null;
	}

	/** What the hero pays for a stable's horse: HORSE_PRICE scaled as the town's services are,
	 *  at its keeper's mood. */
	public static int price( Shopkeeper keeper ){
		int base = TownLedger.scaled( HORSE_PRICE );
		float f = keeper == null ? 1f : keeper.priceFactor();
		return f == 1f ? base : Shopkeeper.discounted( base, f );
	}

	/** A tap on a stable's horse (Horse.interact): it is offered to the hero, by day, while its
	 *  keeper trades. */
	public static void offer( final Hero hero, final Horse h ){
		if (hero != Dungeon.hero){
			Mounts.tell( hero, "host_only" );
			return;
		}
		Shopkeeper keeper = keeperOf( h );
		if (Shopkeeper.closedForNight()){
			GLog.w( Messages.get( Stables.class, "closed" ) );
			return;
		}
		String block = keeper == null ? null : keeper.tradeBlock();
		if (block != null){
			GLog.w( block );
			return;
		}
		final int price = price( keeper );
		final boolean afford = Dungeon.gold >= price;
		String body = Messages.get( Stables.class, "offer", price, Dungeon.gold );
		String buy = afford ? Messages.get( Stables.class, "do_buy", price ) : Messages.get( Stables.class, "too_poor", price );
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( h.sprite(), Messages.titleCase( h.name() ), body,
				buy, Messages.get( Mounts.class, "do_leave" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0 && afford) buy( hero, h, price );
			}

			@Override
			protected boolean enabled( int index ){
				return index != 0 || afford;
			}
		} ) );
	}

	/** The hero buys a stable's horse: its price paid, it is his, saddle and all. */
	public static void buy( Hero hero, Horse h, int price ){
		if (hero != Dungeon.hero || !h.forSale || !h.isAlive() || Dungeon.gold < price) return;
		Dungeon.gold -= price;
		h.forSale = false;
		h.tame = true;
		h.trust = Taming.TAME_TRUST;
		h.stable = Long.MIN_VALUE;
		h.settle();
		Taming.sound( Assets.Sounds.GOLD, h.pos );
		GLog.p( Messages.get( Stables.class, "bought" ) );
		Mounts.seen();
	}
}
