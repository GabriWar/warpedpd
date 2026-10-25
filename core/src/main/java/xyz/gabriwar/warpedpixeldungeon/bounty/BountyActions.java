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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Roaming;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * What the hero does about the villages' notices, on the host (a co-op guest reads the boards and
 * follows along, BountyShare): taking a notice down at a board (a delivery's parcel handed over with
 * it), collecting a hunt's or a gathering's pay there, handing a parcel to an elder, giving a notice
 * up; and what the world does about them - a kill counted (Mob.die), the turn's notices in the log
 * (BountySlice), the guards' friendlier words (OverworldGuard) and the villagers' (Settler), the
 * traders' better prices (OverworldShopkeeper).
 */
public final class BountyActions {

	private BountyActions(){}

	// ------------------------------------------------------------ at the board

	/** Why the hero cannot take this notice now, or null. */
	public static String refusal( Bounty offer ){
		BountyWorld w = BountyWorld.get();
		if (w == null || w.isClosed( offer.id )) return Messages.get( Bounties.class, "gone" );
		if (w.takenCount() >= Bounties.MAX_TAKEN) return Messages.get( Bounties.class, "full", Bounties.MAX_TAKEN );
		return null;
	}

	/** The hero takes a notice down: its reward is fixed now, and a delivery's parcel is his, due in
	 *  Bounties.deliverTime turns. False when it could not be taken (refusal). */
	public static boolean take( Hero hero, long seed, Bounty offer ){
		if (refusal( offer ) != null) return false;
		offer.reward = Bounties.reward( offer );
		if (offer.kind == Bounty.Kind.DELIVER){
			offer.takenTurn = WorldClock.turn();
			offer.deadline = offer.takenTurn + Bounties.deliverTime( offer.dist );
		}
		BountyWorld.get().take( offer, Bounties.periodEnd( seed, offer.village, offer.period ) );
		if (offer.kind == Bounty.Kind.DELIVER) give( hero, Parcel.of( seed, offer ) );
		GLog.i( Messages.get( Bounties.class, "taken", Bounties.title( seed, offer ) ) );
		refreshReady();
		return true;
	}

	/** How many of a good the hero carries, in his pack and every bag in it. */
	public static int carried( Hero hero, Bounties.Good g ){
		int n = 0;
		for (Item i : hero.belongings.backpack) if (handsIn( g, i )) n += i.quantity();
		return n;
	}

	//an item of the good asked for that may go to a board: never a unique one, a quest's own piece
	//(the rotberry seed the wandmaker waits for is no seed for a gathering)
	private static boolean handsIn( Bounties.Good g, Item i ){
		return g.takes( i ) && !i.unique;
	}

	/** Can the hero collect this notice's pay at its board now: a hunt's kills made, a gathering's goods
	 *  in his pack? (A parcel is paid by the elder it goes to, never at the board.) */
	public static boolean ready( Hero hero, Bounty b ){
		switch (b.kind){
			case HUNT:
				return b.progress >= b.need;
			case GATHER: {
				Bounties.Good g = Bounties.good( b.target );
				return hero != null && g != null && carried( hero, g ) >= b.need;
			}
			default:
				return false;
		}
	}

	/** The hero collects a ready hunt's or gathering's pay at its board: the goods taken from his pack,
	 *  the gold and anything besides paid, the village's standing up by one. */
	public static boolean turnIn( Hero hero, long seed, Bounty b ){
		if (!ready( hero, b )) return false;
		if (b.kind == Bounty.Kind.GATHER && !takeGoods( hero, Bounties.good( b.target ), b.need )) return false;
		pay( hero, seed, b );
		finish( seed, b, true );
		refreshReady();
		return true;
	}

	/** The hero gives a notice up: it goes back up on its board while its days last; a parcel he
	 *  carries for it is left behind. */
	public static void abandon( Hero hero, long seed, Bounty b ){
		if (b.kind == Bounty.Kind.DELIVER && hero != null){
			Parcel p = parcel( hero, b );
			if (p != null) p.detachAll( hero.belongings.backpack );
		}
		BountyWorld w = BountyWorld.get();
		if (w != null) w.abandon( b );
		GLog.i( Messages.get( Bounties.class, "abandoned", Bounties.title( seed, b ) ) );
		refreshReady();
	}

	//takes `n` of a good from the pack, the cheapest stacks first (seeds of any kind: never the
	//rarest of them while a common one would do)
	private static boolean takeGoods( Hero hero, Bounties.Good g, int n ){
		if (g == null || carried( hero, g ) < n) return false;
		ArrayList<Item> stacks = new ArrayList<>();
		for (Item i : hero.belongings.backpack) if (handsIn( g, i )) stacks.add( i );
		stacks.sort( Comparator.comparingInt( (Item i) -> i.value() / Math.max( 1, i.quantity() ) )
				.thenComparing( i -> i.getClass().getName() ) );
		for (Item i : stacks){
			if (n <= 0) break;
			int take = Math.min( n, i.quantity() );
			if (take == i.quantity()){
				i.detachAll( hero.belongings.backpack );
			} else {
				i.quantity( i.quantity() - take );
				i.updateQuickslot();
			}
			n -= take;
		}
		return true;
	}

	//into the purse, as a sale is (Shopkeeper.credit), and anything besides into the pack
	private static void pay( Hero hero, long seed, Bounty b ){
		Dungeon.gold += b.reward;
		Statistics.goldCollected += b.reward;
		Badges.validateGoldCollected();
		if (hero.sprite != null){
			hero.sprite.showStatusWithIcon( CharSprite.NEUTRAL, Integer.toString( b.reward ), FloatingText.GOLD );
		}
		Sample.INSTANCE.play( Assets.Sounds.GOLD );
		String name = Bounties.villageName( seed, b.village );
		Item extra = bonusItem( b.bonus );
		if (extra == null){
			GLog.p( Messages.get( Bounties.class, "paid", name, b.reward ) );
		} else {
			GLog.p( Messages.get( Bounties.class, "paid_bonus", name, b.reward, extra.name() ) );
			give( hero, extra );
		}
	}

	/** What a notice's bonus is when it is paid: a potion, a scroll or a runestone, as the villages'
	 *  traders sell them - no curse, its make known. */
	static Item bonusItem( Bounty.Bonus bonus ){
		Generator.Category cat;
		switch (bonus){
			case POTION: cat = Generator.Category.POTION; break;
			case SCROLL: cat = Generator.Category.SCROLL; break;
			case STONE:  cat = Generator.Category.STONE; break;
			default:     return null;
		}
		Item it = Generator.random( cat );
		if (it == null) return null;
		it.cursed = false;
		it.identify( false );
		return it;
	}

	//into the pack, or at his feet when it is full
	private static void give( Hero hero, Item item ){
		if (item.collect( hero.belongings.backpack )) return;
		if (Dungeon.level == null) return;
		Heap h = Dungeon.level.drop( item, hero.pos );
		if (h.sprite != null) h.sprite.drop();
	}

	//done with: off the hero's list, the village's standing up when it was done well, and a new rank told
	private static void finish( long seed, Bounty b, boolean credit ){
		BountyWorld w = BountyWorld.get();
		if (w == null) return;
		int before = Bounties.rank( w.points( b.village ) );
		w.finish( b, credit );
		int after = Bounties.rank( w.points( b.village ) );
		if (after > before){
			GLog.p( Messages.get( Bounties.class, "rank_up_" + after, Bounties.villageName( seed, b.village ),
					Math.round( Bounties.RANK_DISCOUNT * after * 100 ) ) );
		}
	}

	// ------------------------------------------------------------ the parcels

	/** The parcel for a delivery the hero carries, or null. */
	public static Parcel parcel( Hero hero, Bounty b ){
		if (hero == null) return null;
		for (Item i : hero.belongings.backpack){
			if (i instanceof Parcel && ((Parcel) i).bounty == b.id) return (Parcel) i;
		}
		return null;
	}

	//the taken delivery to a village whose parcel the hero carries, or null
	private static Bounty deliveryTo( BountyWorld w, Hero hero, long village ){
		for (Bounty b : w.taken()){
			if (b.kind == Bounty.Kind.DELIVER && b.dest == village && parcel( hero, b ) != null) return b;
		}
		return null;
	}

	//an elder of a village standing in the window, or null
	private static Villager elderIn( OverworldLevel ow, long village ){
		for (Mob m : ow.mobs){
			if (m instanceof Villager && ((Villager) m).settlementKey() == village
					&& ((Villager) m).role == VillageRoutine.Role.ELDER.ordinal()) return (Villager) m;
		}
		return null;
	}

	//which way from a village's well its elder's house lies
	private static String elderWay( OverworldLevel ow, long village ){
		long seed = ow.worldSeed;
		int cx = Bounties.wellX( seed, village ), cy = Bounties.wellY( seed, village );
		Villager elder = elderIn( ow, village );
		if (elder != null && elder.homeX != Integer.MIN_VALUE) return Bounties.direction( elder.homeX - cx, elder.homeY - cy );
		VillageRoutine.Settlement s = VillageRoutine.settlement( seed, village );
		int h = Bounties.elderHouse( seed, village );
		if (s == null || h < 0) return Bounties.direction( 0, 0 );
		return Bounties.direction( s.houseX( h ) - cx, s.houseY( h ) - cy );
	}

	/**
	 * A villager spoken to (Settler.interact) by the hero carrying a parcel for its village: an elder
	 * takes it - paying for it when it is on time, and nothing when it is late - anyone else awake
	 * points the way to the elder's house, or takes it for the elder when none is about. The villager
	 * says so; false when this is no business of the bounties'. The host's own hero only.
	 */
	public static boolean settlerTalk( Settler s ){
		Hero hero = Dungeon.hero;
		if (!(s instanceof Villager) || hero == null || !(Dungeon.level instanceof OverworldLevel)) return false;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		BountyWorld w = BountyWorld.get();
		if (w == null || ow.isMirror()) return false;
		long village = s.settlementKey();
		Bounty b = deliveryTo( w, hero, village );
		if (b == null) return false;
		long seed = ow.worldSeed;
		boolean elder = s.role == VillageRoutine.Role.ELDER.ordinal();
		if (!elder){
			if (s.state == s.SLEEPING) return false;
			if (elderIn( ow, village ) != null){
				s.yell( Messages.get( Bounties.class, "villager_elder_way", elderWay( ow, village ) ) );
				return true;
			}
		}
		Parcel p = parcel( hero, b );
		if (p != null) p.detachAll( hero.belongings.backpack );
		String from = Bounties.villageName( seed, b.village );
		//past its time, whether or not the turn's word has said so yet
		if (b.late || WorldClock.turn() >= b.deadline){
			s.yell( Messages.get( Bounties.class, elder ? "elder_late" : "villager_late", from ) );
			finish( seed, b, false );
			GLog.n( Messages.get( Bounties.class, "delivered_late", Bounties.villageName( seed, village ) ) );
			return true;
		}
		s.yell( Messages.get( Bounties.class, elder ? "elder_thanks" : "villager_thanks", from ) );
		pay( hero, seed, b );
		finish( seed, b, true );
		refreshReady();
		return true;
	}

	/**
	 * A guard of a village spoken to (OverworldGuard.interact) by the hero: the way to the elder's
	 * house when he carries a parcel for it; else, once the village knows him, a friendlier greeting
	 * - and once it trusts him, where the beasts of his hunt for it were last seen. Null: the guard's
	 * own lines.
	 */
	public static String guardLine( OverworldGuard g ){
		Hero hero = Dungeon.hero;
		if (hero == null || !(Dungeon.level instanceof OverworldLevel)) return null;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		BountyWorld w = BountyWorld.get();
		if (w == null || ow.isMirror() || g.homeSector == Long.MIN_VALUE) return null;
		long village = g.homeSector;
		if (deliveryTo( w, hero, village ) != null) return Messages.get( Bounties.class, "guard_parcel", elderWay( ow, village ) );
		int rank = Bounties.rank( w.points( village ) );
		if (rank == 0) return null;
		if (rank >= 2){
			for (Bounty b : w.taken()){
				if (b.kind == Bounty.Kind.HUNT && b.village == village && b.progress < b.need) return preyTip( ow, g, b );
			}
		}
		return Messages.get( Bounties.class, "guard_rank" + rank + "_" + Random.Int( 3 ),
				Bounties.villageName( ow.worldSeed, village ) );
	}

	//where the nearest beast of a hunt stands in the window, as the guard sees it (a great beast's
	//hunt: that one beast)
	private static String preyTip( OverworldLevel ow, Mob guard, Bounty b ){
		Bounties.Prey p = Bounties.prey( b.target );
		boolean great = Bounties.beastKind( b ) != null;
		String name = great ? Bounties.beastName( b ) : Messages.get( Bounties.class, "prey_" + b.target );
		Mob best = null;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : ow.mobs){
			if (!m.isAlive() || m.pos < 0) continue;
			if (great ? !isBeast( m, b.beast ) : (p == null || !p.cls.isInstance( m ))) continue;
			int d = ow.distance( guard.pos, m.pos );
			if (d < bestD){
				bestD = d;
				best = m;
			}
		}
		if (best == null) return Messages.get( Bounties.class, great ? "guard_beast_none" : "guard_prey_none", name );
		int w = ow.width();
		return Messages.get( Bounties.class, great ? "guard_beast" : "guard_prey", name, bestD,
				Bounties.direction( best.pos % w - guard.pos % w, best.pos / w - guard.pos / w ) );
	}

	// ------------------------------------------------------------ the world's side

	/** What a village's traders charge the hero against the shelf price, by his standing there. */
	public static float priceFactor( long village ){
		BountyWorld w = BountyWorld.get();
		if (w == null || village == Long.MIN_VALUE) return 1f;
		return Bounties.priceAt( Bounties.rank( w.points( village ) ) );
	}

	/**
	 * A beast died (Mob.die): a kill by a hero of the party - a blow, a thrown or fired weapon, its
	 * enchantment, a wand, a skill - of a kind a taken hunt asks for, within Bounties.HUNT_REACH of that
	 * village's well on the surface, counts for it, and the log says so. The host's world only.
	 */
	public static void onKill( Mob mob, Object cause ){
		if (NetManager.isNetClient() || !(Dungeon.level instanceof OverworldLevel)) return;
		BountyWorld w = BountyWorld.get();
		if (w == null || !w.hunting() || !byHero( cause )) return;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ow.altitude() != 0 || ow.isMirror() || mob.pos < 0 || mob.pos >= ow.length()) return;
		long seed = ow.worldSeed;
		int wx = ow.worldX + mob.pos % ow.width(), wy = ow.worldY + mob.pos / ow.width();
		for (Bounty b : w.taken()){
			if (b.kind != Bounty.Kind.HUNT) continue;
			if (Bounties.beastKind( b ) != null){
				//a great beast's hunt: that one beast, wherever it falls
				if (!isBeast( mob, b.beast )) continue;
			} else {
				Bounties.Prey p = Bounties.prey( b.target );
				if (p == null || !p.cls.isInstance( mob )) continue;
				if (Math.max( Math.abs( wx - Bounties.wellX( seed, b.village ) ), Math.abs( wy - Bounties.wellY( seed, b.village ) ) )
						> Bounties.HUNT_REACH) continue;
			}
			if (!w.count( b )) continue;
			String name = Bounties.villageName( seed, b.village );
			if (b.progress >= b.need) GLog.p( Messages.get( Bounties.class, "hunt_done", name ) );
			else GLog.i( Messages.get( Bounties.class, "hunt_count", Messages.get( Bounties.class, "prey_" + b.target ),
					name, b.progress, b.need ) );
			refreshReady();
		}
	}

	//is this the roaming beast (RoamerEvent) of an event?
	private static boolean isBeast( Mob m, long event ){
		Roaming r = m.buff( Roaming.class );
		return r != null && r.event == event;
	}

	//a hero's own doing: his blow or his throw, his weapon's edge, his wand, his skill
	private static boolean byHero( Object cause ){
		return cause instanceof Hero || cause instanceof Weapon || cause instanceof Weapon.Enchantment
				|| cause instanceof Wand || SkillInteractions.heroSkillSource( cause );
	}

	//the last day closed notices were pruned on
	private static int prunedDay = Integer.MIN_VALUE;

	/**
	 * The host's turn on a world slice (BountySlice.worldTurn): closed notices of a period gone
	 * forgotten, and the log's word on the notices taken - a gathering's goods all in the pack, a
	 * parcel's road: its village reached, its time running short, its time run out.
	 */
	public static void tick( OverworldLevel ow ){
		Hero hero = Dungeon.hero;
		BountyWorld w = BountyWorld.get();
		if (hero == null || w == null || ow.isMirror()) return;
		int day = WorldClock.day();
		if (day != prunedDay){
			prunedDay = day;
			w.prune( day );
		}
		refreshReady();
		if (w.takenCount() == 0) return;
		int turn = WorldClock.turn();
		long seed = ow.worldSeed;
		for (Bounty b : w.taken()){
			if (Bounties.beastKind( b ) != null){
				//a great beast's hunt not made: void once its days are over, or once it fell to another
				if (b.progress < b.need && (turn >= b.deadline || (ow.altitude() == 0 && RoamerEvent.slain( ow, b.beast )))){
					w.finish( b, false );
					GLog.w( Messages.get( Bounties.class, "beast_gone", Bounties.beastName( b ) ) );
				}
			} else if (b.kind == Bounty.Kind.GATHER){
				boolean have = ready( hero, b );
				if (have != b.told( Bounty.TOLD_READY )){
					w.tell( b, Bounty.TOLD_READY, have );
					if (have) GLog.p( Messages.get( Bounties.class, "gather_ready", Bounties.what( seed, b ),
							Bounties.villageName( seed, b.village ) ) );
				}
			} else if (b.kind == Bounty.Kind.DELIVER){
				String to = Bounties.villageName( seed, b.dest );
				if (!b.late && turn >= b.deadline){
					w.lateNow( b );
					GLog.n( Messages.get( Bounties.class, "parcel_late", to ) );
				} else if (!b.late && !b.told( Bounty.TOLD_DUE )
						&& b.deadline - turn <= (b.deadline - b.takenTurn) / Bounties.DUE_SHARE){
					w.tell( b, Bounty.TOLD_DUE, true );
					GLog.w( Messages.get( Bounties.class, "parcel_due", to, Bounties.hours( b.deadline - turn ) ) );
				}
				if (!b.told( Bounty.TOLD_ARRIVED ) && ow.altitude() == 0 && within( ow, hero, seed, b.dest )){
					w.tell( b, Bounty.TOLD_ARRIVED, true );
					GLog.i( Messages.get( Bounties.class, "parcel_arrived", to, elderWay( ow, b.dest ) ) );
				}
			}
		}
	}

	//is the hero inside a village's fence?
	private static boolean within( OverworldLevel ow, Hero hero, long seed, long village ){
		VillageRoutine.Settlement s = VillageRoutine.settlement( seed, village );
		if (s == null || hero.pos < 0) return false;
		int wx = ow.worldX + hero.pos % ow.width(), wy = ow.worldY + hero.pos / ow.width();
		return Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) <= s.radius + 2;
	}

	//the villages where the host's hero could collect a reward, as last worked out on the actor thread
	//(or by the window that changed it): the boards' looks and the guests' copy read it from any
	//thread, and never walk the pack themselves
	private static volatile long[] readyCache = new long[0];

	/** Works out again where the hero could collect a hunt's or a gathering's pay now. */
	public static void refreshReady(){
		ArrayList<Long> out = new ArrayList<>();
		BountyWorld w = BountyWorld.get();
		if (w != null && Dungeon.hero != null){
			for (Bounty b : w.taken()){
				if (!out.contains( b.village ) && ready( Dungeon.hero, b )) out.add( b.village );
			}
		}
		long[] keys = new long[out.size()];
		for (int i = 0; i < keys.length; i++) keys[i] = out.get( i );
		readyCache = keys;
	}

	/** The villages where the host's hero could collect a hunt's or a gathering's pay now (as of his
	 *  last turn on a world slice): their boards show it (NoticeBoardSprite), on a guest's screen too
	 *  (BountyShare). */
	public static ArrayList<Long> readyVillages(){
		ArrayList<Long> out = new ArrayList<>();
		for (long k : readyCache) out.add( k );
		return out;
	}

	/** What a board says to a co-op guest, who reads it but leaves the notices to the host: its open
	 *  notices with their pay. */
	public static String boardText( long seed, long village ){
		BountyWorld w = BountyWorld.get();
		StringBuilder sb = new StringBuilder( Messages.get( Bounties.class, "board_head", Bounties.villageName( seed, village ) ) );
		ArrayList<Bounty> open = Dungeon.level instanceof OverworldLevel
				? Bounties.openOffers( (OverworldLevel) Dungeon.level, village, WorldClock.day(), w )
				: Bounties.openOffers( seed, village, WorldClock.day(), w );
		if (open.isEmpty()) sb.append( "\n\n" ).append( Messages.get( Bounties.class, "board_empty" ) );
		for (Bounty b : open){
			sb.append( "\n\n" ).append( Bounties.title( seed, b ) ).append( "\n" )
					.append( Messages.get( Bounties.class, "pays", Bounties.pay( Bounties.reward( b ), b.bonus ) ) );
		}
		sb.append( "\n\n" ).append( Messages.get( Bounties.class, "board_guest" ) );
		return sb.toString();
	}
}
