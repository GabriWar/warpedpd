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
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Hides;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GatorTooth;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.AlligatorSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * An alligator of the swamp: it lurks in the swamp's pools at any hour (OverworldFauna, on open
 * water), nobody's enemy while it lies there - only its eyes and its nostrils show above the water,
 * and it lets a hero walk by out of its reach. Whoever steps beside it - a hero of the party, or his
 * ally - it bursts out at: an ambush bite that never misses and lands half as hard again, and that,
 * from the water, drags him in after it (drag: it backs into the water behind it and pulls him onto
 * the cell it left). From then on it hunts him on land and in the water, slow on dry ground, until he
 * is more than CHASE cells off or out of its sight for LOST turns, or it is down to RETREAT of its
 * health; then it makes for the nearest water within REACH and lies in wait again. Struck while it
 * lurks, it turns on whoever struck it at once, without the ambush.
 *
 * A co-op guest's hero is his own machine's to move and is never dragged by the host: he takes the
 * bite alone. Killed, it leaves a piece of meat, one time in two its hide (homestead/gathering/Hides)
 * and one time in two a tooth (GatorTooth), whatever the hero's level.
 */
public class Alligator extends Mob {

	{
		spriteClass = AlligatorSprite.class;

		HP = HT = 130;
		defenseSkill = 14;

		EXP = 13;
		maxLvl = 30;

		alignment = Alignment.NEUTRAL;

		WANDERING = new Lurking();
		HUNTING = new Stalking();
		FLEEING = new Retreating();
		state = WANDERING;

		//one time in two its hide, for a homestead's leather (homestead/gathering/Hides), and one
		//time in two a tooth: both dropped in die(), whatever its alignment
		declareExtraLoot( Hide.class, Hides.GATOR );
		declareExtraLoot( GatorTooth.class, TOOTH );
	}

	/** How far off it gives up its prey, how many turns it may lose sight of him, the share of its
	 *  health it goes back to the water at, and how far it looks for water. */
	public static final int CHASE = 6, LOST = 3, REACH = 8;
	public static final float RETREAT = 0.3f;
	/** The chance it leaves a tooth; its pace on dry ground; one turn in DRIFT it drifts through its
	 *  water while it lurks. */
	public static final float TOOTH = 0.5f, LAND_PACE = 0.8f;
	private static final int DRIFT = 8;
	//an ambush bite lands this much harder
	private static final float AMBUSH_HIT = 1.5f;

	//the turns its prey has been out of its sight, and whether the bite under way is its ambush
	private int lost = 0;
	private boolean ambushing = false;

	/** Whether it lies in wait now (nobody's enemy). */
	public boolean lurking(){
		return state == WANDERING;
	}

	@Override
	public int attackSkill( Char target ) {
		return ambushing ? INFINITE_ACCURACY : 40;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 20, 36 );
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 14 );
	}

	@Override
	public float speed() {
		Level level = Dungeon.level;
		float s = super.speed();
		return level != null && pos >= 0 && pos < level.length() && !level.water[pos] ? s * LAND_PACE : s;
	}

	//a tap goes for it while it lurks: a neutral would trade places
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	protected Char chooseEnemy() {
		if (state != HUNTING) return null;
		if (enemy != null && enemy.isAlive()) return enemy;
		return super.chooseEnemy();
	}

	@Override
	protected boolean act() {
		OverworldFauna.sighted( this );
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		Char by = src instanceof Char ? (Char) src : HuntPack.byHero( src ) ? Dungeon.hero : null;
		if (by != null && by != this && isAlive() && state != HUNTING){
			//struck while it lurks or makes for the water: it turns on whoever did it
			hunt( by );
		}
		super.damage( dmg, src );
	}

	private void hunt( Char prey ){
		alignment = Alignment.ENEMY;
		state = HUNTING;
		enemy = prey;
		target = prey.pos;
		lost = 0;
		if (sprite != null) sprite.showAlert();
	}

	//nobody's enemy again, lying in wait
	private void submerge(){
		alignment = Alignment.NEUTRAL;
		state = WANDERING;
		enemy = null;
		enemySeen = false;
		lost = 0;
		ambushing = false;
		if (sprite != null) sprite.hideAlert();
	}

	//a hero of the party, or an ally of theirs, beside it and not hidden
	private Char preyBeside(){
		Level level = Dungeon.level;
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (h.invisible <= 0 && level.adjacent( pos, h.pos )) return h;
		}
		for (Mob m : level.mobs){
			if (m.alignment == Alignment.ALLY && m.isAlive() && m.invisible <= 0 && level.adjacent( pos, m.pos )) return m;
		}
		return null;
	}

	//it bursts out at whoever stepped beside it
	private boolean ambush( Char prey ){
		hunt( prey );
		ambushing = true;
		if (prey instanceof Hero) NetManager.heroLog( (Hero) prey, GLog.WARNING + Messages.get( Alligator.class, "ambush" ) );
		return doAttack( prey );
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		boolean hit = super.attack( enemy, dmgMulti, dmgBonus, accMulti );
		ambushing = false;
		return hit;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (ambushing){
			damage = Math.round( damage * AMBUSH_HIT );
			drag( enemy );
		}
		return damage;
	}

	/** Its ambush from the water drags whoever it caught in after it: it backs into the water behind
	 *  it and he is pulled onto the cell it left. Never a co-op guest's hero (his own machine moves
	 *  him), nor the large, the rooted, the fliers and the immovable; nothing when no water behind it
	 *  is free. */
	void drag( Char victim ){
		Level level = Dungeon.level;
		if (!level.water[pos] || !victim.isAlive() || victim.flying || victim.rooted
				|| Char.hasProp( victim, Property.IMMOVABLE ) || Char.hasProp( victim, Property.LARGE )) return;
		if (victim instanceof Hero && victim != Dungeon.hero) return;
		int back = backInto( level, pos, victim.pos );
		if (back == -1) return;
		int left = pos;
		if (sprite != null) moveSprite( pos, back );
		move( back );
		if (victim == Dungeon.hero) GLog.w( Messages.get( this, "dragged" ) );
		WandOfBlastWave.throwChar( victim, new Ballistica( victim.pos, left, Ballistica.MAGIC_BOLT ), 1, false, false, this );
	}

	/** The water it backs into from `at`, away from the one it caught at `prey`: the free open-water
	 *  cell beside it furthest from him (the nearest to it on a tie, first found), -1 for none. */
	public static int backInto( Level level, int at, int prey ){
		int w = level.width(), best = -1;
		float bestD = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = at + n;
			if (c < 0 || c >= level.length() || c == prey || !level.insideMap( c )) continue;
			if (!level.water[c] || !level.passable[c] || Actor.findChar( c ) != null) continue;
			float dx = c % w - prey % w, dy = c / w - prey / w;
			float d = dx * dx + dy * dy;
			if (d > bestD){
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	/** The open water nearest `from` within `reach` cells (a square), free or `from` itself; -1 for none. */
	public static int nearestWater( Level level, int from, int reach ){
		int w = level.width(), fx = from % w, fy = from / w;
		for (int r = 0; r <= reach; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = fx + dx, y = fy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int c = x + y * w;
					if (level.water[c] && level.passable[c] && (c == from || Actor.findChar( c ) == null)) return c;
				}
			}
		}
		return -1;
	}

	//a free open-water cell beside it, picked at random; -1 for none
	private int drift( Level level ){
		ArrayList<Integer> ways = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c >= 0 && c < level.length() && level.insideMap( c ) && level.water[c] && level.passable[c]
					&& Actor.findChar( c ) == null) ways.add( c );
		}
		return ways.isEmpty() ? -1 : Random.element( ways );
	}

	//its turn lying in wait: it bursts out at whoever stepped beside it, or keeps to its water
	protected class Lurking extends Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			//sent here still his enemy (Mob's hunt gives up on prey it cannot see or reach): it lies
			//in wait as nobody's enemy, as it does once back in the water
			if (alignment != Alignment.NEUTRAL) submerge();
			Char prey = preyBeside();
			if (prey != null) return ambush( prey );
			Level level = Dungeon.level;
			int from = pos;
			if (!level.water[pos]){
				//out of the water (set down on the bank, or left there): back to the nearest pool
				int pool = nearestWater( level, pos, REACH );
				if (pool != -1 && getCloser( pool )){
					spend( 1 / speed() );
					return moveSprite( from, pos );
				}
			} else if (Random.Int( DRIFT ) == 0){
				int c = drift( level );
				if (c != -1){
					move( c );
					spend( 1 / speed() );
					return moveSprite( from, pos );
				}
			}
			spend( TICK );
			return true;
		}
	}

	//its turn hunting: Mob's, until its prey gets away from it or it is badly hurt
	protected class Stalking extends Hunting {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			if (enemy == null || !enemy.isAlive()){
				state = FLEEING;
				return FLEEING.act( enemyInFOV, justAlerted );
			}
			lost = enemyInFOV ? 0 : lost + 1;
			if (lost > LOST || Dungeon.level.distance( pos, enemy.pos ) > CHASE || HP < HT * RETREAT){
				state = FLEEING;
				return FLEEING.act( enemyInFOV, justAlerted );
			}
			return super.act( enemyInFOV, justAlerted );
		}
	}

	//its turn making for the water, where it lies in wait again
	protected class Retreating extends Fleeing {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			Level level = Dungeon.level;
			if (!level.water[pos]){
				int pool = nearestWater( level, pos, REACH );
				int from = pos;
				if (pool != -1 && getCloser( pool )){
					spend( 1 / speed() );
					return moveSprite( from, pos );
				}
			}
			//in the water, or with none in reach: it lies in wait where it is
			submerge();
			spend( TICK );
			return true;
		}
	}

	@Override
	public void die( Object cause ) {
		//a foe at its death is counted by destroy() as any foe is; one lying in wait here
		boolean counted = alignment == Alignment.ENEMY;
		super.die( cause );
		if (!counted) Bestiary.countEncounter( getClass() );
		if (cause == Chasm.class) return;
		int cell = RaidEvent.dropCell( Dungeon.level, pos );
		Hides.drop( this, cell );
		//a tooth, seen in the bestiary as its second extra loot (slot 4, after the hide)
		if (Random.Float() < TOOTH){
			Heap h = Dungeon.level.drop( new GatorTooth(), cell );
			if (h.sprite != null) h.sprite.drop( pos );
			Bestiary.setLootSlotSeen( getClass(), 4 );
		}
		Heap carcass = Dungeon.level.drop( new MysteryMeat(), cell );
		if (carcass.sprite != null) carcass.sprite.drop();
	}

	private static final String LOST_T = "lost";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LOST_T, lost );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		lost = bundle.contains( LOST_T ) ? bundle.getInt( LOST_T ) : 0;
		//lying in wait it is nobody's enemy; hunting, or making for the water after a fight, it is
		alignment = state == WANDERING ? Alignment.NEUTRAL : Alignment.ENEMY;
	}
}
