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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Animals;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leadable;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Mounts;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Stables;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Taming;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Saddle;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.HorseSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A horse of the open grass (OverworldFauna's plains and meadow rows: herds of two to four by
 * day). Wild, it grazes with its herd and bolts from anyone who comes within its shyness
 * (Taming.shy: three cells, one fewer for each meal it has had from the hero) and from wolves,
 * and a startled horse sets the herd round it running too. It never fights. Fed (Taming) until it
 * trusts the hero, it is TAME: his for good, kept as a homestead's animal (never culled, never
 * forgotten while parked), waiting where it was left, taking a rope like any animal of his
 * (homestead/animals/Roping). Saddled (items/misc/Saddle), it is ridden (homestead/mounts).
 *
 * A village stable's horse (Stables) is FOR SALE: gentle and saddled, it keeps by its stall
 * until the hero buys it and it is his.
 *
 * Its looks (a coat of three, saddled or not) are a sprite class of their own each
 * (HorseSprite.classFor), so a co-op guest, who builds a sprite from its class, draws the same horse.
 */
public class Horse extends Mob implements Leadable {

	{
		HP = HT = 40;
		defenseSkill = 10;

		maxLvl = -1;   //no xp: it is fauna, not a foe

		alignment = Alignment.NEUTRAL;
		state = WANDERING;

		//its hide, for a homestead's leather: dropped in die() with its meat
		declareExtraLoot( Hide.class, 1f );
	}

	/** How many coats a horse comes in (HorseSprite's rows): bay, chestnut, grey. */
	public static final int COATS = 3;

	//how close any wolf comes before a wild horse bolts; how far a startled horse's fright
	//reaches through the herd, and for how many turns; how near a herd keeps together
	private static final int WOLF_WARY = 8, ALARM_REACH = 4, ALARM_TURNS = 3, HERD = 3;
	//the gallop it bolts at, how long it keeps it up, and the calm turns before it settles
	private static final int GALLOP_TURNS = 6, CALM_TURNS = 6;
	private static final float GALLOP = 1.5f;
	/** A point of health back every REGEN turns, ridden or not. */
	public static final int REGEN = 10;

	public int coat = Random.Int( COATS );
	public boolean saddled;
	/** The hero's own: tamed, or bought from a stable. */
	public boolean tame;
	/** A wild horse's meals from the hero (Taming): it is tame at Taming.TAME_TRUST. */
	public int trust;
	/** A stable's, waiting for a buyer: the village it stands in (its sector key) and its stall
	 *  (world cell), which it keeps within a step of. */
	public boolean forSale;
	public long stable = Long.MIN_VALUE;
	public int stallX, stallY;

	//moves spent fleeing since it last calmed, turns without a threat, its last step (a
	//NEIGHBOURS8 offset), and a herd mate's fright it carries: for how long, where from, and
	//whether a hero gave it (a horse that has eaten from him minds that none)
	private int running = 0, calm = 0, heading = 0;
	private int alarm = 0, alarmCell = -1;
	private boolean alarmByHero;
	private float healing;

	{
		setLook();
	}

	/** Its sprite class from its coat and saddle: a co-op guest draws the same horse. The live
	 *  sprite changes its frames to match. */
	public void setLook(){
		spriteClass = HorseSprite.classFor( coat, saddled );
		if (sprite instanceof HorseSprite) ((HorseSprite) sprite).look( coat, saddled );
	}

	@Override
	public String name(){
		return Messages.get( this, tame || forSale ? "name_tame" : "name" );
	}

	//the hero's or a stable's knows him: no unaware dots over it
	@Override
	public boolean showsUnaware(){
		return !tame && !forSale && super.showsUnaware();
	}

	@Override
	protected Char chooseEnemy(){
		return null;
	}

	@Override
	public int attackSkill( Char target ){
		return 0;
	}

	//every tap talks to it: a wild one is fed (Taming), the hero's own has its menu or is ridden
	//(Mounts), a stable's is offered for sale (Stables)
	@Override
	public boolean heroShouldInteract(){
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (c != Dungeon.hero){
			if (c instanceof Hero) NetManager.heroLog( (Hero) c, Messages.get( Mounts.class, "host_only" ) );
			return true;
		}
		Hero hero = (Hero) c;
		if (forSale) Stables.offer( hero, this );
		else if (!tame) Taming.handFeed( hero, this );
		else Mounts.tapped( hero, this );
		return true;
	}

	/** May the hero put a rope on it: his own horse, never a wild one or a stable's. */
	@Override
	public String ropeRefusal(){
		return forSale ? "not_yours" : tame ? null : "too_wild";
	}

	//the hero's, a stable's, or held by a rope or a pen: kept for good, never culled with the wildlife
	@Override
	public boolean homesteadKept(){
		return tame || forSale || Roping.held( this );
	}

	@Override
	public float speed(){
		float s = super.speed();
		return running > 0 && running <= GALLOP_TURNS ? s * GALLOP : s;
	}

	@Override
	protected boolean act(){
		Level level = Dungeon.level;
		if (level.heroFOV != null && pos >= 0 && pos < level.heroFOV.length && level.heroFOV[pos]){
			if (!Bestiary.isSeen( getClass() )) Bestiary.setSeen( getClass() );
			Mounts.seen();
		}
		if (paralysed > 0 || state == SLEEPING){
			spend( TICK );
			return true;
		}
		//roped, tied or penned: the homesteads' (homestead/animals/Animals)
		float held = Animals.act( this );
		if (held > 0){
			heal( held );
			spend( held );
			return true;
		}
		heal( TICK );
		if (forSale){
			Stables.keep( this );
			spend( TICK );
			return true;
		}
		if (tame){
			//it waits where it was left
			if (sprite != null) sprite.hideAlert();
			state = WANDERING;
			spend( TICK );
			return true;
		}
		wild( level );
		return true;
	}

	/** It calms at once: no flight, no fright from the herd (a horse just tamed, bought or set down). */
	public void settle(){
		running = calm = heading = 0;
		alarm = 0;
		alarmCell = -1;
		state = WANDERING;
		if (sprite != null) sprite.hideAlert();
	}

	//a point of health back every REGEN turns
	private void heal( float time ){
		healing += time;
		while (healing >= REGEN){
			healing -= REGEN;
			if (HP < HT) HP++;
		}
	}

	//a wild horse's turn: away from whoever it fears, to the food it was thrown, or grazing
	//with its herd
	private void wild( Level level ){
		ArrayList<Integer> threats = new ArrayList<>();
		int shy = Taming.shy( trust );
		boolean byHero = false;
		for (Hero hero : OverworldLevel.heroesOn( level )){
			if (hero.invisible <= 0 && level.distance( pos, hero.pos ) <= shy){
				threats.add( hero.pos );
				byHero = true;
			}
		}
		for (Mob m : level.mobs){
			if (HuntPack.isWolf( m ) && m.isAlive() && level.distance( pos, m.pos ) <= WOLF_WARY) threats.add( m.pos );
		}
		boolean direct = !threats.isEmpty();
		if (alarm > 0){
			alarm--;
			//a horse that has eaten from the hero no longer runs from him with the others
			if (alarmCell >= 0 && alarmCell < level.length() && !(alarmByHero && trust > 0)) threats.add( alarmCell );
		}

		if (threats.isEmpty()){
			if (++calm >= CALM_TURNS && running > 0){
				running = 0;
				heading = 0;
				if (sprite != null) sprite.hideAlert();
			}
			state = WANDERING;
			int food = Taming.foodNear( this );
			if (food != -1){
				if (food == pos || level.adjacent( pos, food )){
					if (sprite != null) sprite.turnTo( pos, food );
					Taming.eat( this, food );
					spend( TICK );
					return;
				}
				int step = Roping.stepToward( this, food );
				if (step != -1){
					Roping.moveTo( this, step );
					spend( 1f / speed() );
					return;
				}
			}
			graze( level );
			spend( TICK );
			return;
		}

		calm = 0;
		if (sprite != null){
			if (running == 0) sprite.showAlert();
			else if (running == 2) sprite.hideAlert();
		}
		//a hero it bolts from is told, now and then, how a wild horse is won
		if (running == 0 && byHero) Taming.shied( this );
		running++;
		state = FLEEING;
		//its fright runs through the herd round it
		if (direct) alarmHerd( level, nearest( pos, threats, level ), byHero );
		int step = bolt( threats );
		if (step != -1){
			heading = step - pos;
			Roping.moveTo( this, step );
		}
		spend( 1f / speed() );
	}

	//the herd mates within ALARM_REACH take fright from the same place for ALARM_TURNS turns
	private void alarmHerd( Level level, int from, boolean byHero ){
		for (Mob m : level.mobs){
			if (!(m instanceof Horse) || m == this || !m.isAlive()) continue;
			Horse h = (Horse) m;
			if (h.tame || h.forSale || h.alarm > 0 || level.distance( pos, h.pos ) > ALARM_REACH) continue;
			h.alarm = ALARM_TURNS;
			h.alarmCell = from;
			h.alarmByHero = byHero;
		}
	}

	//the threat nearest a cell
	private static int nearest( int cell, ArrayList<Integer> threats, Level level ){
		int best = threats.get( 0 );
		for (int t : threats) if (level.distance( cell, t ) < level.distance( cell, best )) best = t;
		return best;
	}

	/** Its next step away from the threats (cells): the open neighbour that leaves the nearest
	 *  of them furthest off, its own line kept on a tie; -1 when every way is closer to one of
	 *  them, or shut. */
	int bolt( ArrayList<Integer> threats ){
		float here = nearest2( pos, threats );
		int best = -1;
		float bestScore = here;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (!canStep( c, true )) continue;
			float score = nearest2( c, threats ) + (n == heading ? 0.5f : 0f);
			if (score > bestScore){
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	//the squared distance from a cell to the nearest threat
	private float nearest2( int cell, ArrayList<Integer> threats ){
		int w = Dungeon.level.width(), x = cell % w, y = cell / w;
		float best = Float.MAX_VALUE;
		for (int t : threats){
			float dx = t % w - x, dy = t / w - y;
			best = Math.min( best, dx * dx + dy * dy );
		}
		return best;
	}

	//open ground it can step onto now: no water, pit or harm, nobody there, never indoors or
	//through a door. Calm, it keeps off the tall grass (trampling it rolls for seeds)
	private boolean canStep( int c, boolean fleeing ){
		Level level = Dungeon.level;
		return !rooted && c >= 0 && c < level.length() && level.insideMap( c )
				&& level.passable[c] && !level.avoid[c] && !level.water[c] && !level.pit[c]
				&& (fleeing || level.map[c] != Terrain.HIGH_GRASS)
				&& !Mounts.indoors( level, c )
				&& Actor.findChar( c ) == null;
	}

	//grazing: back toward the herd when it has strayed from it, else a step now and then
	private void graze( Level level ){
		Horse mate = null;
		for (Mob m : level.mobs){
			if (!(m instanceof Horse) || m == this || !m.isAlive() || ((Horse) m).tame || ((Horse) m).forSale) continue;
			if (mate == null || level.distance( pos, m.pos ) < level.distance( pos, mate.pos )) mate = (Horse) m;
		}
		if (mate != null && level.distance( pos, mate.pos ) > HERD && level.distance( pos, mate.pos ) < 12){
			int best = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (canStep( c, false ) && level.distance( c, mate.pos ) < level.distance( pos, mate.pos )
						&& (best == -1 || level.distance( c, mate.pos ) < level.distance( best, mate.pos ))) best = c;
			}
			if (best != -1){
				Roping.moveTo( this, best );
				return;
			}
		}
		if (Random.Int( 3 ) != 0) return;
		int c = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		if (canStep( c, false )) Roping.moveTo( this, c );
	}

	//a wild horse struck forgets the hero's kindness and runs from whoever struck it
	@Override
	public void damage( int dmg, Object src ){
		if (!tame && !forSale){
			trust = 0;
			if (src instanceof Char && ((Char) src).pos >= 0){
				alarm = ALARM_TURNS;
				alarmCell = ((Char) src).pos;
				alarmByHero = false;
			}
		}
		super.damage( dmg, src );
	}

	@Override
	public void die( Object cause ){
		boolean mine = tame;
		boolean seen = Dungeon.level != null && Dungeon.level.heroFOV != null && pos >= 0
				&& pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos];
		super.die( cause );
		Bestiary.countEncounter( getClass() );
		remains( Dungeon.level, pos );
		if (mine && seen) GLog.n( Messages.get( this, "died" ) );
	}

	/** What a dead horse leaves on a level near a cell: its meat, its hide and its saddle. */
	public void remains( Level level, int near ){
		int cell = RaidEvent.dropCell( level, near );
		if (saddled){
			saddled = false;
			Heap s = level.drop( new Saddle(), cell );
			if (s.sprite != null) s.sprite.drop();
		}
		Heap h = level.drop( new Hide(), cell );
		if (h.sprite != null) h.sprite.drop();
		Bestiary.setLootSlotSeen( getClass(), xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Hides.SLOT );
		h = level.drop( new MysteryMeat().quantity( 2 ), cell );
		if (h.sprite != null) h.sprite.drop();
	}

	@Override
	public String description(){
		String d = Messages.get( this, "desc" );
		if (forSale){
			d += "\n\n" + Messages.get( this, "desc_sale" );
		} else if (tame){
			d += "\n\n" + Messages.get( this, saddled ? "desc_saddled" : "desc_tame" );
		} else {
			d += "\n\n" + Messages.get( this, "desc_trust_" + Math.min( trust, Taming.TAME_TRUST - 1 ) );
		}
		return d;
	}

	private static final String COAT = "coat", SADDLED = "saddled", TAME = "tame", TRUST = "trust",
			FOR_SALE = "for_sale", STABLE = "stable", STALL_X = "stall_x", STALL_Y = "stall_y",
			RUNNING = "running", CALM = "calm", HEADING = "heading", HEALING = "healing";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( COAT, coat );
		bundle.put( SADDLED, saddled );
		bundle.put( TAME, tame );
		bundle.put( TRUST, trust );
		bundle.put( FOR_SALE, forSale );
		bundle.put( STABLE, stable );
		bundle.put( STALL_X, stallX );
		bundle.put( STALL_Y, stallY );
		bundle.put( RUNNING, running );
		bundle.put( CALM, calm );
		bundle.put( HEADING, heading );
		bundle.put( HEALING, healing );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		coat = Math.floorMod( bundle.getInt( COAT ), COATS );
		saddled = bundle.getBoolean( SADDLED );
		tame = bundle.getBoolean( TAME );
		trust = bundle.getInt( TRUST );
		forSale = bundle.getBoolean( FOR_SALE );
		stable = bundle.contains( STABLE ) ? bundle.getLong( STABLE ) : Long.MIN_VALUE;
		stallX = bundle.getInt( STALL_X );
		stallY = bundle.getInt( STALL_Y );
		running = bundle.getInt( RUNNING );
		calm = bundle.getInt( CALM );
		heading = bundle.getInt( HEADING );
		healing = bundle.getFloat( HEALING );
		setLook();
	}
}
