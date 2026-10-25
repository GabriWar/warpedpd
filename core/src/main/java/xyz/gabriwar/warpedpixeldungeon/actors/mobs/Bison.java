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
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Hides;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.BisonSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * A bison of the plains and the meadows: daytime fauna (OverworldFauna), out in herds of two to
 * five. Calm, it grazes - a step now and then, back toward the rest of the herd when it has strayed
 * more than KEEP cells from them - and it minds no one: it is too big to be afraid of a hero.
 *
 * Struck by anyone - a hero, his ally, a wolf - it and every wild bison within HERD cells of it turn
 * on whoever did it: the herd charges. A charging bison runs half as fast again, and a blow it
 * strikes at the end of a run (it moved on its turn before) lands half as hard again and throws its
 * target a cell straight back (Goat.shoveCell; a co-op guest's hero is his own machine's to move and
 * is never thrown by the host). The herd calms down once the one it charges is dead, gone from the
 * level, or out of its sight for CALM turns running; then it grazes again, peaceful.
 *
 * Roped, tied or penned (homestead/animals) it is the homestead's: it never charges, a blow at it
 * stirs no herd, and a butcher gets two pieces of meat and a hide (Roping.meatOf). Killed in the
 * wild it leaves its hide and two pieces of meat, wherever its anger stood: no experience either
 * way - it is fauna, not a foe.
 */
public class Bison extends Mob implements Leadable {

	{
		spriteClass = BisonSprite.class;

		HP = HT = 140;
		defenseSkill = 14;

		maxLvl = -1;   //no xp: it is fauna, not a foe

		alignment = Alignment.NEUTRAL;
		state = WANDERING;

		//always its hide, for a homestead's leather (homestead/gathering/Hides): dropped in die()
		declareExtraLoot( Hide.class, Hides.BISON );
	}

	/** How far a blow's anger spreads through the herd, how far a grazing bison strays from the
	 *  rest of it, and how many turns running the one it charges may be out of its sight before
	 *  it calms down. */
	public static final int HERD = 8, KEEP = 3, CALM = 8;
	//a charge's pace, and how much harder a blow at the end of a run lands
	private static final float CHARGE_SPEED = 1.5f, CHARGE_HIT = 1.5f;

	//the Actor id of the one it charges (-1: calm), the turns he has been out of its sight, whether
	//it moved on its last turn and whether the blow it is striking now ends a run
	private int quarry = -1, unseen = 0;
	private boolean moved = false, charging = false;

	/** Whether it is charging someone now. */
	public boolean charging(){
		return quarry != -1;
	}

	/** Who it charges, or null. */
	public Char quarry(){
		if (quarry == -1) return null;
		Actor a = Actor.findById( quarry );
		return a instanceof Char && ((Char) a).isAlive() ? (Char) a : null;
	}

	@Override
	public int attackSkill( Char target ) {
		return 36;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 16, 30 );
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 12 );
	}

	@Override
	public float speed() {
		float s = super.speed();
		return quarry != -1 && enemy != null && Dungeon.level != null
				&& Dungeon.level.distance( pos, enemy.pos ) > 1 ? s * CHARGE_SPEED : s;
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
	protected Char chooseEnemy() {
		return quarry();
	}

	@Override
	public void damage( int dmg, Object src ) {
		Char by = src instanceof Char ? (Char) src : HuntPack.byHero( src ) ? Dungeon.hero : null;
		if (by != null && by != this && !(by instanceof Bison) && isAlive()) provoke( by );
		super.damage( dmg, src );
	}

	/** Struck by `by`: this bison and every wild one of its herd within HERD cells charge him. A
	 *  bison the homestead holds is no herd's: a blow at it stirs none. */
	public void provoke( Char by ){
		Level level = Dungeon.level;
		if (level == null || Roping.held( this ) || !by.isAlive()) return;
		boolean turned = false;
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof Bison) || !m.isAlive() || Roping.held( m )) continue;
			if (m != this && level.distance( pos, m.pos ) > HERD) continue;
			turned |= ((Bison) m).charge( by );
		}
		if (turned && by instanceof Hero) NetManager.heroLog( (Hero) by, GLog.WARNING + Messages.get( Bison.class, "stampede" ) );
	}

	//sets it on `by`; false when it was already charging him
	private boolean charge( Char by ){
		if (quarry == by.id()) return false;
		quarry = by.id();
		unseen = 0;
		alignment = Alignment.ENEMY;
		state = HUNTING;
		enemy = by;
		target = by.pos;
		if (sprite != null) sprite.showAlert();
		return true;
	}

	//the one it charges is dead, gone or long out of its sight: it calms down and grazes again
	private void calm(){
		quarry = -1;
		unseen = 0;
		moved = charging = false;
		alignment = Alignment.NEUTRAL;
		state = WANDERING;
		enemy = null;
		enemySeen = false;
		if (sprite != null) sprite.hideAlert();
	}

	//whether the herd's anger at its quarry holds this turn: he lives, is on the level and was in
	//its sight within CALM turns
	private boolean stillAngry(){
		Char q = quarry();
		if (q == null || q.pos < 0 || q.pos >= Dungeon.level.length()) return false;
		boolean seen = fieldOfView != null && fieldOfView.length == Dungeon.level.length()
				&& fieldOfView[q.pos] && q.invisible <= 0;
		unseen = seen ? 0 : unseen + 1;
		return unseen <= CALM;
	}

	@Override
	protected boolean act() {
		OverworldFauna.sighted( this );
		//roped, tied or penned: the homesteads' (homestead/animals/Animals)
		float held = Animals.act( this );
		if (held > 0){
			if (quarry != -1) calm();
			spend( held );
			return true;
		}
		if (quarry != -1 && !stillAngry()) calm();
		if (quarry == -1){
			if (paralysed > 0 || state == SLEEPING){
				spend( TICK );
				return true;
			}
			state = WANDERING;
			graze();
			spend( TICK );
			return true;
		}
		//the charge: Mob's hunt at its quarry, a blow struck at the end of a run landing harder
		charging = moved;
		int from = pos;
		boolean done = super.act();
		moved = pos != from;
		return done;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (charging){
			charging = false;
			moved = false;
			damage = Math.round( damage * CHARGE_HIT );
			//a co-op guest's hero is his own machine's to move: only this machine's hero and the beasts are thrown
			boolean guest = enemy instanceof Hero && enemy != Dungeon.hero;
			int behind = Goat.shoveCell( Dungeon.level, pos, enemy.pos );
			if (!guest && behind != -1 && enemy.isAlive() && !Char.hasProp( enemy, Property.IMMOVABLE )){
				if (enemy == Dungeon.hero) GLog.w( Messages.get( this, "trampled" ) );
				WandOfBlastWave.throwChar( enemy, new Ballistica( enemy.pos, behind, Ballistica.MAGIC_BOLT ), 1, false, false, this );
			}
		}
		return damage;
	}

	//grazing: a step now and then, back toward the herd when it has strayed from it
	private void graze(){
		if (sprite == null || rooted || Random.Int( 3 ) != 0) return;
		Level level = Dungeon.level;
		int step = -1;
		int herd = herdCentre( level );
		if (herd != -1 && level.distance( pos, herd ) > KEEP) step = stepToward( level, herd );
		if (step == -1){
			int c = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			if (canGraze( level, c )) step = c;
		}
		if (step == -1) return;
		moveSprite( pos, step );
		move( step );
	}

	/** The cell at the middle of the rest of its herd (the wild bison within HERD cells), -1 when it
	 *  grazes alone. */
	int herdCentre( Level level ){
		int w = level.width(), sx = 0, sy = 0, n = 0;
		for (Mob m : level.mobs){
			if (m == this || !(m instanceof Bison) || !m.isAlive() || Roping.held( m )) continue;
			if (level.distance( pos, m.pos ) > HERD) continue;
			sx += m.pos % w;
			sy += m.pos / w;
			n++;
		}
		return n == 0 ? -1 : Math.round( sx / (float) n ) + Math.round( sy / (float) n ) * w;
	}

	//the open neighbour that brings it nearest the cell, -1 when none brings it nearer
	private int stepToward( Level level, int cell ){
		int best = -1, bestD = level.distance( pos, cell );
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (!canGraze( level, c )) continue;
			int d = level.distance( c, cell );
			if (d < bestD){
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	//open ground it grazes onto: no water, pit or harm, nobody there, and off the tall grass - a
	//herd trampling it would carpet the plain in seeds
	private static boolean canGraze( Level level, int c ){
		return c >= 0 && c < level.length() && level.insideMap( c )
				&& level.passable[c] && !level.avoid[c] && !level.water[c] && !level.pit[c]
				&& level.map[c] != Terrain.HIGH_GRASS && Actor.findChar( c ) == null;
	}

	@Override
	public void die( Object cause ) {
		//a foe at its death is counted by destroy() as any foe is; a calm one here
		boolean counted = alignment == Alignment.ENEMY;
		super.die( cause );
		if (!counted) Bestiary.countEncounter( getClass() );
		//one gone over an edge leaves nothing here to carve
		if (cause == Chasm.class) return;
		int cell = RaidEvent.dropCell( Dungeon.level, pos );
		//its hide, for a homestead's leather (homestead/gathering/Hides), under the meat
		Hides.drop( this, cell );
		Heap carcass = Dungeon.level.drop( new MysteryMeat().quantity( 2 ), cell );
		if (carcass.sprite != null) carcass.sprite.drop();
	}

	private static final String QUARRY   = "quarry";
	private static final String UNSEEN   = "unseen";
	private static final String MOVED    = "moved";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( QUARRY, quarry );
		bundle.put( UNSEEN, unseen );
		bundle.put( MOVED, moved );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		quarry = bundle.contains( QUARRY ) ? bundle.getInt( QUARRY ) : -1;
		unseen = bundle.contains( UNSEEN ) ? bundle.getInt( UNSEEN ) : 0;
		moved  = bundle.contains( MOVED ) && bundle.getBoolean( MOVED );
		if (quarry != -1) alignment = Alignment.ENEMY;
	}
}
