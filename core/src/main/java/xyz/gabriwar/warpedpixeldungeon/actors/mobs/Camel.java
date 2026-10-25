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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Animals;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leadable;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Hides;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CamelSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A camel of the desert: daytime fauna (OverworldFauna), alone or in a string of two or three.
 * Placid: it chews, plods a step now and then, and only ambles off when a hero comes within SHY
 * cells. Struck by someone, it spits at him once - from where it stands, up to SPIT_RANGE cells off
 * with a clear line to him - and the spit leaves him dazed for DAZE turns (half his aim and his
 * dodge, Daze); then it makes off, away from whoever is near, for FRIGHT turns. It never fights
 * otherwise. Cheap on purpose: no pathfinding, one greedy step a move, as the deer and the goat.
 *
 * Born to the heat (a heat dweller): the desert's noon is nothing to it, a frost hurts it more.
 * Roped, tied or penned it is the homestead's (homestead/animals), its butcher getting two pieces of
 * meat and a hide (Roping.meatOf). Killed in the wild it leaves two pieces of meat and, one time in
 * two, its hide. No experience: it is fauna, not a foe.
 */
public class Camel extends Mob implements Leadable {

	{
		spriteClass = CamelSprite.class;

		HP = HT = 80;
		defenseSkill = 12;

		maxLvl = -1;   //no xp: it is fauna, not a foe

		alignment = Alignment.NEUTRAL;
		state = WANDERING;

		thermal = Thermal.HEAT_DWELLER;

		//one time in two its hide, for a homestead's leather (homestead/gathering/Hides): dropped in die()
		declareExtraLoot( Hide.class, Hides.CAMEL );
	}

	/** How near a hero comes before it ambles off, how far it spits, how long the spit dazes, and
	 *  how many turns it makes off for after a blow. */
	public static final int SHY = 2, SPIT_RANGE = 3, DAZE = 3, FRIGHT = 8;

	//the Actor id of whoever just struck it (it spits at him on its next turn, or not at all), the
	//turns it still makes off for, and its last step (a NEIGHBOURS8 offset: it keeps its line)
	private int spitAt = -1, fright = 0, heading = 0;

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	//the spit: it may miss, and it barely hurts
	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 4 );
	}

	//a tap goes for it, as for a deer; one the hero has roped, tied or penned is his: a tap opens
	//its menu (homestead/animals/Roping)
	@Override
	public boolean heroShouldInteract() {
		return Animals.shouldInteract( this );
	}

	@Override
	public boolean interact( Char c ) {
		return Animals.interact( this, c ) || super.interact( c );
	}

	@Override
	public void damage( int dmg, Object src ) {
		Char by = src instanceof Char ? (Char) src : HuntPack.byHero( src ) ? Dungeon.hero : null;
		if (by != null && by != this && isAlive()){
			spitAt = by.id();
			fright = FRIGHT;
		}
		super.damage( dmg, src );
	}

	@Override
	protected boolean act() {
		OverworldFauna.sighted( this );
		if (paralysed > 0 || state == SLEEPING){
			spend( TICK );
			return true;
		}
		//roped, tied or penned: the homesteads' (homestead/animals/Animals)
		float held = Animals.act( this );
		if (held > 0){
			spitAt = -1;
			fright = 0;
			spend( held );
			return true;
		}

		//struck: one spit at whoever did it, while he is in reach and in a clear line
		Char foe = spitTarget();
		spitAt = -1;
		if (foe != null){
			enemy = foe;
			if (sprite != null){
				sprite.turnTo( pos, foe.pos );
				sprite.showAlert();
			}
			return doAttack( foe );
		}

		Level level = Dungeon.level;
		ArrayList<Integer> threats = new ArrayList<>();
		int shy = fright > 0 ? FRIGHT : SHY;
		for (Hero hero : OverworldLevel.heroesOn( level )){
			if (hero.invisible <= 0 && level.distance( pos, hero.pos ) <= shy) threats.add( hero.pos );
		}
		if (fright > 0) fright--;
		if (threats.isEmpty()){
			state = WANDERING;
			heading = 0;
			amble();
			spend( TICK );
			return true;
		}
		state = FLEEING;
		int step = away( threats );
		if (step != -1 && sprite != null){
			heading = step - pos;
			moveSprite( pos, step );
			move( step );
		}
		spend( 1f / speed() );
		return true;
	}

	//whoever struck it, while he lives, stands within SPIT_RANGE and a spit would reach him
	private Char spitTarget(){
		if (spitAt == -1) return null;
		Actor a = Actor.findById( spitAt );
		if (!(a instanceof Char) || !((Char) a).isAlive()) return null;
		Char c = (Char) a;
		Level level = Dungeon.level;
		if (c.pos < 0 || c.pos >= level.length() || level.distance( pos, c.pos ) > SPIT_RANGE) return null;
		return new Ballistica( pos, c.pos, Ballistica.PROJECTILE ).collisionPos == c.pos ? c : null;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		Buff.prolong( enemy, Daze.class, DAZE );
		if (enemy instanceof Hero) NetManager.heroLog( (Hero) enemy, GLog.WARNING + Messages.get( this, "spat" ) );
		return damage;
	}

	/** Its step away from the threats (cells): the open neighbour that leaves the nearest of them
	 *  furthest off, its own line kept on a tie; -1 when every way is closer to one of them, or shut. */
	int away( ArrayList<Integer> threats ){
		float here = nearest2( pos, threats );
		int best = -1;
		float bestScore = here;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (!canStep( c )) continue;
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

	//open ground it can step onto now: no water, pit or harm, nobody there
	private boolean canStep( int c ){
		Level level = Dungeon.level;
		return !rooted && c >= 0 && c < level.length() && level.insideMap( c )
				&& level.passable[c] && !level.avoid[c] && !level.water[c] && !level.pit[c]
				&& Actor.findChar( c ) == null;
	}

	//chewing, a plodding step now and then
	private void amble(){
		if (sprite == null || Random.Int( 4 ) != 0) return;
		int c = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		if (!canStep( c )) return;
		moveSprite( pos, c );
		move( c );
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		Bestiary.countEncounter( getClass() );
		//one gone over an edge leaves nothing here to carve
		if (cause == Chasm.class) return;
		int cell = RaidEvent.dropCell( Dungeon.level, pos );
		//one time in two its hide, for a homestead's leather (homestead/gathering/Hides), under the meat
		Hides.drop( this, cell );
		Heap carcass = Dungeon.level.drop( new MysteryMeat().quantity( 2 ), cell );
		if (carcass.sprite != null) carcass.sprite.drop();
	}

	private static final String SPIT_AT = "spit_at";
	private static final String FRIGHT_LEFT = "fright";
	private static final String HEADING = "heading";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SPIT_AT, spitAt );
		bundle.put( FRIGHT_LEFT, fright );
		bundle.put( HEADING, heading );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		spitAt  = bundle.contains( SPIT_AT ) ? bundle.getInt( SPIT_AT ) : -1;
		fright  = bundle.contains( FRIGHT_LEFT ) ? bundle.getInt( FRIGHT_LEFT ) : 0;
		heading = bundle.contains( HEADING ) ? bundle.getInt( HEADING ) : 0;
	}
}
