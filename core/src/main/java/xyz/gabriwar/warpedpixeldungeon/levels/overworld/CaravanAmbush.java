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


package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * A caravan under attack (RoadTraffic.ambushed): the outlaws break from the verge
 * as a hero comes in sight of the stall, and the siege settles from there - every
 * raider down while a hero is close by and the caravan is SAVED (a gift, and the
 * stall's prices marked down for the rest of the day); every hero gone off while
 * they live and it is ROBBED (half the goods carried into the trees); the raiders
 * cut down by someone else while the heroes kept their distance and it was merely
 * SPARED. Raiders carry the stall's mark (OverworldBandit.ambushOf), so the camps'
 * own outlaws never count. Works on any level: the surface hands it its parked store.
 */
public final class CaravanAmbush {

	private CaravanAmbush(){}

	public static final int SIGHT = 14;          //a hero this close, the stall in sight: it springs
	public static final int SAVED_REACH = 20;    //every raider down with a hero this close: saved
	public static final int ROBBED_REACH = 25;   //every hero further off while raiders live: robbed
	//the surface's sight (viewDistance 20) and a cell: a raider further off slips away unseen
	private static final int UNSEEN = 21;

	/** Is this stall's ambush due: some hero within SIGHT of it, and the stall in a hero's
	 *  sight? Whoever he is: one who would rather not fight can walk on, and past ROBBED_REACH
	 *  the outlaws take the goods instead and let him be. */
	public static boolean due( Level level, Caravaneer c ){
		if (c.ambush != Caravaneer.AMBUSH_PENDING || !NetManager.anyHeroSees( c.pos )) return false;
		for (Hero h : heroes( level )){
			if (level.distance( c.pos, h.pos ) <= SIGHT) return true;
		}
		return false;
	}

	/** The outlaws spring: two to four on open ground two to four cells off the stall, clear
	 *  of the goods and three from any hero, out of sight first. The first menaces the stall,
	 *  the rest go for the hero nearest it. Nowhere to stand and nothing happens today.
	 *  Returns how many sprang. */
	public static int spring( Level level, Caravaneer c ){
		if (c.ambush != Caravaneer.AMBUSH_PENDING) return 0;
		Hero hero = nearest( level, c.pos );
		if (hero == null) return 0;
		ArrayList<Integer> hidden = new ArrayList<>(), open = new ArrayList<>();
		int w = level.width(), cx = c.pos % w, cy = c.pos / w;
		for (int dy = -4; dy <= 4; dy++){
			for (int dx = -4; dx <= 4; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) < 2) continue;
				int x = cx + dx, y = cy + dy;
				if (x < 2 || y < 2 || x >= w - 2 || y >= level.height() - 2) continue;
				int cell = x + y * w;
				if (!level.passable[cell] || level.avoid[cell] || level.pit[cell] || level.heaps.get( cell ) != null
						|| occupied( level, cell ) || OverworldLevel.heroDistance( level, cell ) < 3) continue;
				(NetManager.anyHeroSees( cell ) ? open : hidden).add( cell );
			}
		}
		if (hidden.isEmpty() && open.isEmpty()){
			c.ambush = Caravaneer.AMBUSH_NONE;
			return 0;
		}
		Random.shuffle( hidden );
		Random.shuffle( open );
		hidden.addAll( open );
		c.ambush = Caravaneer.AMBUSH_BESIEGED;
		int n = Random.IntRange( 2, 4 );
		int placed = 0;
		for (int i = 0; i < hidden.size() && placed < n; i++){
			int cell = hidden.get( i );
			OverworldBandit b = new OverworldBandit();
			b.pos = cell;
			b.menace = placed == 0;
			b.sendAgainst( placed == 0 ? c : hero, c.stall );
			if (Dungeon.level == level) GameScene.add( b );
			else level.mobs.add( b );
			if (live( level ) && NetManager.anyHeroSees( cell )) CellEmitter.get( cell ).burst( Speck.factory( Speck.WOOL ), 6 );
			placed++;
		}
		//the birds go up off the verge with them
		OverworldCritters.noise( c.pos );
		c.yell( Messages.get( Caravaneer.class, "ambush_yell" ) );
		GLog.w( Messages.get( Caravaneer.class, "ambush_log" ) );
		if (live( level ) && Dungeon.hero.isAlive() && level.distance( Dungeon.hero.pos, c.pos ) <= ROBBED_REACH){
			Dungeon.hero.interrupt();
		}
		return placed;
	}

	/** Where a siege stands, once a turn: true when it has just ended (saved, robbed or spared). */
	public static boolean settle( Level level, Caravaneer c, HashMap<Long, Mob> parked, HashMap<Long, Float> parkedAt ){
		if (c.ambush != Caravaneer.AMBUSH_BESIEGED) return false;
		int d = OverworldLevel.heroDistance( level, c.pos );
		int live = count( level.mobs, c.stall ), held = count( parked.values(), c.stall );
		//every raider in the window cut down with a hero at hand: the ones a scare sent off past
		//the window's edge are not coming back
		if (live == 0 && held > 0 && d <= SAVED_REACH){
			forget( parked, parkedAt, c.stall );
			held = 0;
		}
		if (live + held > 0){
			if (d <= ROBBED_REACH) return false;
			c.ambush = Caravaneer.AMBUSH_ROBBED;
			release( level, c.stall, parked );
			rob( level, c );
			makeOff( level, c.stall, parked, parkedAt );
			GLog.n( Messages.get( Caravaneer.class, "robbed_log" ) );
			return true;
		}
		if (d <= SAVED_REACH){
			c.ambush = Caravaneer.AMBUSH_SAVED;
			release( level, c.stall, parked );
			c.yell( Messages.get( Caravaneer.class, "saved_thanks" ) );
			int at = -1;
			for (int ofs : PathFinder.NEIGHBOURS8){
				int cell = c.pos + ofs;
				if (cell >= 0 && cell < level.length() && level.passable[cell] && !level.pit[cell]
						&& level.heaps.get( cell ) == null){
					at = cell;
					break;
				}
			}
			if (at == -1){
				Hero h = nearest( level, c.pos );
				at = h != null ? h.pos : c.pos;
			}
			Heap gift = level.drop( c.gift(), at );
			if (live( level ) && gift.sprite != null) gift.sprite.drop();
			GLog.p( Messages.get( Caravaneer.class, "saved_log" ) );
			return true;
		}
		if (d > ROBBED_REACH){
			//somebody else saw them off while the heroes kept their distance
			c.ambush = Caravaneer.AMBUSH_SPARED;
			release( level, c.stall, parked );
			return true;
		}
		//none left, and a hero 21-25 cells off: his next steps decide saved or spared
		return false;
	}

	/** The day turned with the outlaws still at the stall: they had half the goods before it
	 *  was struck (OverworldLevel.placeCaravans), and a hero near enough hears of it. */
	public static void outlastedTheDay( Level level, Caravaneer c ){
		c.ambush = Caravaneer.AMBUSH_ROBBED;
		rob( level, c );
		if (OverworldLevel.heroDistance( level, c.pos ) <= ROBBED_REACH){
			GLog.n( Messages.get( Caravaneer.class, "robbed_log" ) );
		}
	}

	/** Sends off every raider whose stall no longer stands (OverworldLevel.placeCaravans): one a
	 *  hero could be watching stays on as an ordinary outlaw, the rest are simply gone, and so are
	 *  any parked. One already making off is left to it, and one the hero turned stays his. */
	public static void disbandOrphans( Level level, Set<Long> standing, HashMap<Long, Mob> parked, HashMap<Long, Float> parkedAt ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof OverworldBandit)) continue;
			OverworldBandit b = (OverworldBandit) m;
			if (b.ambushOf == OverworldBandit.NO_AMBUSH || b.retreating || standing.contains( b.ambushOf )) continue;
			if (b.alignment != Char.Alignment.ENEMY || watched( level, b.pos )){
				unmark( b );
			} else {
				OverworldLevel.vanish( level, b );
			}
		}
		Iterator<HashMap.Entry<Long, Mob>> it = parked.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof OverworldBandit)) continue;
			OverworldBandit b = (OverworldBandit) e.getValue();
			if (b.ambushOf == OverworldBandit.NO_AMBUSH || standing.contains( b.ambushOf )) continue;
			if (b.alignment != Char.Alignment.ENEMY){
				unmark( b );
				continue;
			}
			it.remove();
			parkedAt.remove( e.getKey() );
		}
	}

	//live raiders of a stall: one the hero has turned (charm wears off, corruption does not) is
	//beaten, so only enemies count
	private static int count( Collection<Mob> mobs, long stall ){
		int n = 0;
		for (Mob m : mobs){
			if (m instanceof OverworldBandit && ((OverworldBandit) m).ambushOf == stall && !((OverworldBandit) m).retreating
					&& m.isAlive() && m.alignment == Char.Alignment.ENEMY) n++;
		}
		return n;
	}

	//the parked raiders still against a stall dropped from the store (one the hero turned is his)
	private static void forget( HashMap<Long, Mob> parked, HashMap<Long, Float> parkedAt, long stall ){
		Iterator<HashMap.Entry<Long, Mob>> it = parked.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof OverworldBandit) || ((OverworldBandit) e.getValue()).ambushOf != stall
					|| e.getValue().alignment != Char.Alignment.ENEMY) continue;
			it.remove();
			parkedAt.remove( e.getKey() );
		}
	}

	//the siege is over: a raider of the stall the hero turned (corruption; charm wears off and
	//never counts) is an outlaw of no party now, live or parked - his to keep, never swept off
	private static void release( Level level, long stall, HashMap<Long, Mob> parked ){
		for (Mob m : level.mobs){
			if (m instanceof OverworldBandit && ((OverworldBandit) m).ambushOf == stall && m.alignment != Char.Alignment.ENEMY){
				unmark( (OverworldBandit) m );
			}
		}
		for (Mob m : parked.values()){
			if (m instanceof OverworldBandit && ((OverworldBandit) m).ambushOf == stall && m.alignment != Char.Alignment.ENEMY){
				unmark( (OverworldBandit) m );
			}
		}
	}

	private static void unmark( OverworldBandit b ){
		b.ambushOf = OverworldBandit.NO_AMBUSH;
		b.menace = false;
	}

	//the caravan robbed (its turned raiders released first): the rest make off into the trees -
	//the ones a hero could be watching on foot, still marked, the others at once - and any
	//parked are gone
	private static void makeOff( Level level, long stall, HashMap<Long, Mob> parked, HashMap<Long, Float> parkedAt ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof OverworldBandit) || ((OverworldBandit) m).ambushOf != stall) continue;
			OverworldBandit b = (OverworldBandit) m;
			if (watched( level, b.pos ) && b.isAlive()){
				b.retreating = true;
				b.clearEnemy();
				b.state = b.WANDERING;
			} else {
				OverworldLevel.vanish( level, b );
			}
		}
		forget( parked, parkedAt, stall );
	}

	//half the stall (rounded up) goes into the trees with them
	private static void rob( Level level, Caravaneer c ){
		ArrayList<Heap> goods = new ArrayList<>();
		for (int ofs : PathFinder.NEIGHBOURS9){
			int cell = c.pos + ofs;
			if (cell < 0 || cell >= level.length()) continue;
			Heap h = level.heaps.get( cell );
			if (h != null && h.type == Heap.Type.FOR_SALE) goods.add( h );
		}
		Random.shuffle( goods );
		for (int i = 0; i < (goods.size() + 1) / 2; i++) goods.get( i ).destroy();
	}

	//every hero in play on the level: the host's own, and on a host the other players' (as
	//OverworldLevel.heroDistance counts them)
	private static ArrayList<Hero> heroes( Level level ){
		ArrayList<Hero> out = new ArrayList<>();
		Hero h = Dungeon.hero;
		if (h != null && Dungeon.level == level && h.isAlive() && h.pos >= 0 && h.pos < level.length()) out.add( h );
		if (NetManager.isHost()){
			for (Hero nh : NetManager.getNetHeroes()){
				if (nh.isAlive() && !nh.atExit && nh.pos >= 0 && nh.pos < level.length()) out.add( nh );
			}
		}
		return out;
	}

	//the hero nearest a cell, or null
	private static Hero nearest( Level level, int cell ){
		Hero best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Hero h : heroes( level )){
			int d = level.distance( cell, h.pos );
			if (d < bestDist){
				best = h;
				bestDist = d;
			}
		}
		return best;
	}

	private static boolean occupied( Level level, int cell ){
		if (Actor.findChar( cell ) != null) return true;
		for (Mob m : level.mobs) if (m.pos == cell) return true;
		return false;
	}

	private static boolean live( Level level ){
		return Dungeon.level == level && Game.scene() instanceof GameScene;
	}

	//could a hero be watching this cell? by distance, not by sight: the orphan sweep runs inside
	//a rebase, before the heroes' sight is worked out for the shifted window
	private static boolean watched( Level level, int cell ){
		return OverworldLevel.heroDistance( level, cell ) <= UNSEEN;
	}
}
