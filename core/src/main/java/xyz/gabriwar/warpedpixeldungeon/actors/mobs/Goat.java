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
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.GoatSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A mountain goat of the high pastures and snowfields (OverworldFauna's cliff rows): daytime
 * fauna that keeps to the lips of the drops. Peaceful - it shies from the hero and from wolves
 * the way a deer does, with no field of view and no pathfinding - but it answers a blow: a goat
 * struck by someone standing beside it butts him back, and a butt throws the one who struck it
 * a cell. Whoever strikes a goat with a drop at his back goes over the edge (Chasm). Cornered
 * and never struck, it lowers its horns first and butts on its next turn only if the hero is
 * still at its side - and that butt throws no one.
 */
public class Goat extends Mob {

	{
		spriteClass = GoatSprite.class;

		HP = HT = 45;
		defenseSkill = 22;

		maxLvl = -1;   //no xp: it is fauna, not a foe

		alignment = Alignment.NEUTRAL;
		state = WANDERING;

		//the crags' own beast: the cold is home, a thaw is what hurts
		thermal = Thermal.COLD_DWELLER;
	}

	//how close a hero comes before it moves off, and how close any wolf
	private static final int SHY = 4, WOLF_WARY = 6;

	//the Actor id of whoever last struck it while standing at its side (only he is ever thrown),
	//its last step (a NEIGHBOURS8 offset: it keeps its line along an edge), and whether it has
	//lowered its horns at a hero who cornered it
	private int provoker = -1, heading = 0;
	private boolean bracing = false;

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	//a tap goes for it, as for a deer: an ordinary neutral would trade places
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	public int attackSkill( Char target ) {
		return 40;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 8, 16 );
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (src instanceof Char && src != this) provoker = ((Char) src).id();
		super.damage( dmg, src );
	}

	@Override
	protected boolean act() {
		Level level = Dungeon.level;
		if (level.heroFOV != null && level.heroFOV[pos] && !Bestiary.isSeen( getClass() )) Bestiary.setSeen( getClass() );
		if (paralysed > 0 || state == SLEEPING){
			spend( TICK );
			return true;
		}

		//every hero of the party it can see coming, and the wolves
		ArrayList<Integer> threats = new ArrayList<>();
		for (Hero hero : OverworldLevel.heroesOn( level )){
			if (hero.invisible <= 0 && level.distance( pos, hero.pos ) <= SHY) threats.add( hero.pos );
		}
		for (Mob m : level.mobs){
			if (HuntPack.isWolf( m ) && m.isAlive() && level.distance( pos, m.pos ) <= WOLF_WARY) threats.add( m.pos );
		}
		int step = threats.isEmpty() ? -1 : bolt( threats );

		//struck from its side: it butts back when it is cornered, or when the one who struck it
		//has a drop at his back - it knows the edge
		Char foe = striker();
		if (foe != null && (step == -1 || answers( level, pos, foe.pos ))){
			bracing = false;
			return butt( foe );
		}
		//cornered by a hero who never struck it: the horns go down first, the butt comes next turn
		if (foe == null && step == -1 && !threats.isEmpty()){
			Hero at = besideHero();
			if (at != null){
				if (bracing){
					bracing = false;
					return butt( at );
				}
				bracing = true;
				if (sprite instanceof GoatSprite){
					((GoatSprite) sprite).brace( at.pos );
					sprite.showAlert();
				}
				NetManager.heroLog( at, GLog.WARNING + Messages.get( this, "brace" ) );
				spend( TICK );
				return true;
			}
		}
		if (bracing){
			bracing = false;
			if (sprite != null) sprite.idle();
		}

		if (threats.isEmpty()){
			state = WANDERING;
			heading = 0;
			amble();
			spend( TICK );
			return true;
		}
		if (state != FLEEING && sprite != null) sprite.showAlert();
		state = FLEEING;
		if (step != -1 && sprite != null){
			heading = step - pos;
			moveSprite( pos, step );
			move( step );
		}
		spend( 1f / speed() );
		return true;
	}

	//the head down at someone beside it: Mob.doAttack spends the time now, or once the swing
	//has played (onAttackComplete)
	private boolean butt( Char foe ){
		enemy = foe;
		if (sprite != null) sprite.showAlert();
		return doAttack( foe );
	}

	//whoever struck it, while he stands beside it and lives; forgotten once he steps off
	private Char striker(){
		if (provoker == -1) return null;
		Actor a = Actor.findById( provoker );
		if (a instanceof Char && ((Char) a).isAlive() && Dungeon.level.adjacent( pos, ((Char) a).pos )) return (Char) a;
		provoker = -1;
		return null;
	}

	//a hero of the party at its side that it can see
	private Hero besideHero(){
		for (Hero hero : OverworldLevel.heroesOn( Dungeon.level )){
			if (hero.invisible <= 0 && Dungeon.level.adjacent( pos, hero.pos )) return hero;
		}
		return null;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		//only the one who struck it goes flying: a cornered goat's butt only hurts
		boolean struckIt = enemy.id() == provoker;
		provoker = -1;
		//a co-op guest's hero is his own machine's to move: only this machine's hero and the beasts are thrown
		boolean guest = enemy instanceof Hero && enemy != Dungeon.hero;
		int behind = shoveCell( Dungeon.level, pos, enemy.pos );
		if (struckIt && !guest && behind != -1 && enemy.isAlive()){
			if (enemy == Dungeon.hero) GLog.w( Messages.get( this, "butt" ) );
			WandOfBlastWave.throwChar( enemy, new Ballistica( enemy.pos, behind, Ballistica.MAGIC_BOLT ), 1, false, false, this );
		}
		return damage;
	}

	/** The cell a butt throws `target` onto: straight on, away from the goat at `from`; -1 when
	 *  rock or someone stands there. */
	public static int shoveCell( Level level, int from, int target ){
		int b = target + (target - from);
		return b >= 0 && b < level.length() && level.insideMap( b ) && !level.solid[b] && Actor.findChar( b ) == null ? b : -1;
	}

	/** Does it answer a blow from `target`? When he has a drop at his back. */
	public static boolean answers( Level level, int from, int target ){
		int b = shoveCell( level, from, target );
		return b != -1 && level.pit[b];
	}

	/** Its next step away from the threats: the open neighbour that leaves the nearest of them
	 *  furthest off, its own line kept on a tie; -1 when every way is closer to one of them, or
	 *  shut - cornered. */
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

	//firm ground it can step onto now: no water, drop or harm, nobody there - it never walks off
	//an edge. calm, it keeps off the tall grass as the grazers do
	private boolean canStep( int c, boolean fleeing ){
		Level level = Dungeon.level;
		return !rooted && c >= 0 && c < level.length() && level.insideMap( c )
				&& level.passable[c] && !level.avoid[c] && !level.water[c] && !level.pit[c]
				&& (fleeing || level.map[c] != Terrain.HIGH_GRASS)
				&& Actor.findChar( c ) == null;
	}

	//grazing: a step now and then
	private void amble(){
		if (sprite == null || Random.Int( 3 ) != 0) return;
		int c = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		if (!canStep( c, false )) return;
		moveSprite( pos, c );
		move( c );
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		Bestiary.countEncounter( getClass() );
		//one gone over the edge leaves nothing here to carve
		if (cause == Chasm.class) return;
		Heap carcass = Dungeon.level.drop( new MysteryMeat(), pos );
		if (carcass.sprite != null) carcass.sprite.drop();
	}

	private static final String PROVOKER = "provoker";
	private static final String HEADING  = "heading";
	private static final String BRACING  = "bracing";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( PROVOKER, provoker );
		bundle.put( HEADING, heading );
		bundle.put( BRACING, bracing );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		provoker = bundle.contains( PROVOKER ) ? bundle.getInt( PROVOKER ) : -1;
		heading  = bundle.contains( HEADING )  ? bundle.getInt( HEADING )  : 0;
		bracing  = bundle.contains( BRACING ) && bundle.getBoolean( BRACING );
	}
}
