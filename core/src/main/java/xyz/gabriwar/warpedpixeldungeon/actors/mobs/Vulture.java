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
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.VultureFeather;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.VultureSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * A vulture of the desert: daytime fauna (OverworldFauna), alone or in a pair. It flies, high over
 * the sand, and is nobody's enemy until someone is close to dying:
 *
 * - it circles: RADIUS cells round its home (where it was set down, a world cell), or round a hero
 *   within SCENT cells who is below WARY of his health - the birds gathering over a wounded
 *   traveller are the warning;
 * - carrion within SCENT cells (raw meat lying on top of an open heap: a carcass, or meat the hero
 *   dropped) it flies down to and lands on, and eats a piece every EAT turns till none is left;
 *   a hero coming within SHY cells sends it up again;
 * - a hero within SCENT cells below WEAK of his health it dives on: it turns on him, strikes and
 *   climbs off a cell before it strikes again, and gives him up only once he is back above
 *   GIVE_UP of his health (or dead, or gone);
 * - struck by anyone who is not that weak, it flies off from him for FLEE turns, then circles again.
 *
 * Killed, it leaves one or two of its flight feathers (VultureFeather), whatever the hero's level: a
 * feather is no prize to outgrow.
 */
public class Vulture extends Mob {

	{
		spriteClass = VultureSprite.class;

		HP = HT = 45;
		defenseSkill = 24;

		EXP = 7;
		maxLvl = 28;

		baseSpeed = 1.2f;
		flying = true;

		alignment = Alignment.NEUTRAL;

		//the desert's own: its noon is nothing to it, a frost hurts it more
		thermal = Thermal.HEAT_DWELLER;

		WANDERING = new Circling();
		HUNTING = new Diving();
		FLEEING = new Climbing();
		state = WANDERING;

		//one or two flight feathers, dropped in die() whatever its alignment
		declareExtraLoot( VultureFeather.class, 1f );
	}

	/** How far it smells carrion and sees weakness, the ring it circles, the share of a hero's
	 *  health below which it circles him, dives on him, and gives him up again. */
	public static final int SCENT = 8, RADIUS = 3;
	public static final float WARY = 0.6f, WEAK = 0.3f, GIVE_UP = 0.4f;
	/** Turns to eat a piece of carrion, how near a hero sends it up off its carrion, and how many
	 *  turns it flies off for when struck. */
	public static final int EAT = 3, SHY = 2, FLEE = 5;

	//its home (a world cell; MIN_VALUE until it first acts), the hero it dives on and whoever it
	//flies off from (Actor ids, -1 for none), the turns it has eaten the piece in front of it, the
	//turns it still flies off for, a turn's climb after a strike, and which way it circles
	private int homeX = Integer.MIN_VALUE, homeY = Integer.MIN_VALUE;
	private int prey = -1, scared = -1, eating = 0, fleeing = 0, climb = 0;
	private boolean clockwise = false;

	@Override
	public int attackSkill( Char target ) {
		return 34;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 8, 16 );
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 4 );
	}

	//a tap goes for it while it is nobody's enemy, as for any quarry: a neutral would trade places
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	/** Is this raw meat a vulture eats: a carcass's mystery meat, or monster meat. */
	public static boolean isCarrion( Item item ){
		return item instanceof MysteryMeat || item instanceof MonsterMeat;
	}

	/** ...told from its picture alone, as a co-op guest's copy of a heap shows only that
	 *  (VultureSprite: it settles down to feed on it). */
	public static boolean carrionImage( int image ){
		return image == ItemSpriteSheet.MEAT || image == ItemSpriteSheet.MONSTER_MEAT;
	}

	/** Carrion on a cell: raw meat on top of an open heap there. */
	public static boolean carrionAt( Level level, int cell ){
		Heap h = level.heaps.get( cell );
		return h != null && h.type == Heap.Type.HEAP && h.peek() != null && isCarrion( h.peek() );
	}

	//a hero of the party within SCENT in its sight, below this share of his health, nearest first
	private Hero hurtHero( float share ){
		Level level = Dungeon.level;
		Hero best = null;
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (h.invisible > 0 || h.HP >= h.HT * share || level.distance( pos, h.pos ) > SCENT) continue;
			if (fieldOfView != null && fieldOfView.length == level.length() && !fieldOfView[h.pos]) continue;
			if (best == null || level.distance( pos, h.pos ) < level.distance( pos, best.pos )) best = h;
		}
		return best;
	}

	//the hero it dives on while he is still weak enough to be worth it: alive, on the level, below
	//GIVE_UP of his health
	private Hero prey(){
		if (prey == -1) return null;
		Actor a = Actor.findById( prey );
		if (!(a instanceof Hero)) return null;
		Hero h = (Hero) a;
		if (!h.isAlive() || h.pos < 0 || h.pos >= Dungeon.level.length() || h.HP >= h.HT * GIVE_UP) return null;
		return OverworldLevel.heroesOn( Dungeon.level ).contains( h ) ? h : null;
	}

	@Override
	protected Char chooseEnemy() {
		if (state == HUNTING) return prey();
		if (state == FLEEING && scared != -1){
			Actor a = Actor.findById( scared );
			if (a instanceof Char && ((Char) a).isAlive()) return (Char) a;
		}
		return null;
	}

	//it turns on a weak hero: it is his enemy now
	private void dive( Hero h ){
		prey = h.id();
		scared = -1;
		eating = 0;
		alignment = Alignment.ENEMY;
		state = HUNTING;
		enemy = h;
		target = h.pos;
		if (sprite != null) sprite.showAlert();
		NetManager.heroLog( h, GLog.WARNING + Messages.get( Vulture.class, "dives" ) );
	}

	//nobody's enemy again: back up to circle
	private void settle(){
		prey = -1;
		climb = 0;
		alignment = Alignment.NEUTRAL;
		state = WANDERING;
		enemy = null;
		enemySeen = false;
		if (sprite != null) sprite.hideAlert();
	}

	@Override
	protected boolean act() {
		OverworldFauna.sighted( this );
		//its first turn: where it was set down is home, and it takes its way round
		if (homeX == Integer.MIN_VALUE){
			homeX = HuntPack.worldX( pos );
			homeY = HuntPack.worldY( pos );
			clockwise = Random.Int( 2 ) == 0;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage( dmg, src );
		if (!isAlive() || state == HUNTING && prey() != null) return;
		Char by = src instanceof Char ? (Char) src : HuntPack.byHero( src ) ? Dungeon.hero : null;
		if (by == null || by == this) return;
		if (by instanceof Hero && by.HP < by.HT * WEAK && OverworldLevel.heroesOn( Dungeon.level ).contains( by )){
			dive( (Hero) by );
			return;
		}
		//anyone else: it flies off from him a while
		if (prey != -1) settle();
		scared = by.id();
		fleeing = FLEE;
		eating = 0;
		state = FLEEING;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		//a strike, and a turn's climb off before the next
		climb = 1;
		return damage;
	}

	//a cell it can fly onto now: inside the map, open to the air, nobody there
	private static boolean canFly( Level level, int c ){
		return c >= 0 && c < level.length() && level.insideMap( c ) && !level.solid[c] && Actor.findChar( c ) == null;
	}

	//the open cell beside it nearest the one given, -1 when none is nearer
	private int flyToward( int cell ){
		Level level = Dungeon.level;
		int best = -1, bestD = level.distance( pos, cell );
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c != cell && !canFly( level, c )) continue;
			if (c == cell && Actor.findChar( c ) != null) continue;
			int d = level.distance( c, cell );
			if (d < bestD){
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	/** Its next step round the ring of RADIUS cells about `anchor`, turning the way it circles: the
	 *  open neighbour that best keeps it on the ring while it goes on round; -1 when it is shut in. */
	int circleStep( int anchor ){
		Level level = Dungeon.level;
		int w = level.width();
		float vx = pos % w - anchor % w, vy = pos / w - anchor / w;
		//the way round, at right angles to the anchor's direction
		float tx = clockwise ? vy : -vy, ty = clockwise ? -vx : vx;
		float tl = (float) Math.sqrt( tx * tx + ty * ty );
		int best = -1;
		float bestScore = -Float.MAX_VALUE;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (!canFly( level, c )) continue;
			float ox = c % w - pos % w, oy = c / w - pos / w;
			float dx = c % w - anchor % w, dy = c / w - anchor / w;
			float off = Math.abs( (float) Math.sqrt( dx * dx + dy * dy ) - RADIUS );
			float along = tl == 0 ? 0 : (ox * tx + oy * ty) / tl;
			float score = along - 1.5f * off;
			if (score > bestScore){
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	//its home in the level: its own cell when its home is out of the window (it keeps to where it is)
	private int homeCell(){
		int c = HuntPack.cellOf( homeX, homeY );
		return c == -1 ? pos : c;
	}

	//the carrion within SCENT nearest it, -1 for none
	private int nearestCarrion(){
		Level level = Dungeon.level;
		int best = -1, bestD = Integer.MAX_VALUE;
		for (Heap h : level.heaps.valueList()){
			int d = level.distance( pos, h.pos );
			if (d > SCENT || d >= bestD || !carrionAt( level, h.pos )) continue;
			if (Actor.findChar( h.pos ) != null && h.pos != pos) continue;
			best = h.pos;
			bestD = d;
		}
		return best;
	}

	//a hero of the party within SHY cells (startles it off its carrion)
	private boolean crowded(){
		Level level = Dungeon.level;
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (h.invisible <= 0 && level.distance( pos, h.pos ) <= SHY) return true;
		}
		return false;
	}

	//a piece of the carrion under it eaten: one off the top of the heap
	private void eatPiece(){
		Level level = Dungeon.level;
		Heap h = level.heaps.get( pos );
		if (h == null || !carrionAt( level, pos )) return;
		Item top = h.peek();
		if (top.quantity() > 1){
			top.quantity( top.quantity() - 1 );
			if (h.sprite != null) h.sprite.view( h ).place( h.pos );
		} else {
			h.remove( top );
		}
		if (level.heroFOV != null && level.heroFOV[pos]) CellEmitter.center( pos ).burst( BloodParticle.BURST, 4 );
	}

	//its turn as a wanderer: dive on the weak, feed on carrion, circle the hurt or its home
	protected class Circling extends Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			//sent here still diving (Mob's hunt gives up on prey it cannot see or reach, a fright
			//runs out): it is nobody's enemy again until it dives anew
			if (prey != -1 || alignment != Alignment.NEUTRAL) settle();
			Level level = Dungeon.level;
			Hero weak = hurtHero( WEAK );
			if (weak != null){
				dive( weak );
				return HUNTING.act( true, false );
			}

			if (carrionAt( level, pos ) && !crowded()){
				if (eating == 0 && level.heroFOV != null && level.heroFOV[pos]) GLog.i( Messages.get( Vulture.class, "feeds" ) );
				if (++eating % EAT == 0) eatPiece();
				spend( TICK );
				return true;
			}
			eating = 0;

			int carrion = crowded() ? -1 : nearestCarrion();
			int step;
			if (carrion != -1 && carrion != pos){
				step = flyToward( carrion );
			} else {
				Hero hurt = hurtHero( WARY );
				step = circleStep( hurt != null ? hurt.pos : homeCell() );
			}
			if (step == -1){
				spend( TICK );
				return true;
			}
			int from = pos;
			move( step );
			spend( 1 / speed() );
			return moveSprite( from, pos );
		}
	}

	//its turn diving on the weak: a strike, a turn's climb off, and again, while he stays weak
	protected class Diving extends Hunting {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			Hero h = prey();
			if (h == null){
				settle();
				spend( TICK );
				return true;
			}
			enemy = h;
			if (climb > 0){
				climb--;
				int from = pos;
				if (getFurther( h.pos )){
					spend( 1 / speed() );
					return moveSprite( from, pos );
				}
			}
			return super.act( enemyInFOV, justAlerted );
		}
	}

	//its turn flying off from whoever struck it, for FLEE turns
	protected class Climbing extends Fleeing {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			if (--fleeing < 0 || enemy == null){
				calmDown();
				spend( TICK );
				return true;
			}
			return super.act( enemyInFOV, justAlerted );
		}

		@Override
		protected void escaped(){
			calmDown();
		}

		//cornered, it never turns to fight a hero who is not weak: it waits for a way out
		@Override
		protected void nowhereToRun(){
		}
	}

	private void calmDown(){
		scared = -1;
		fleeing = 0;
		state = WANDERING;
	}

	@Override
	public void die( Object cause ) {
		//a foe at its death is counted by destroy() as any foe is; one nobody's enemy here
		boolean counted = alignment == Alignment.ENEMY;
		super.die( cause );
		if (!counted) Bestiary.countEncounter( getClass() );
		if (cause == Chasm.class) return;
		//its feathers, seen in the bestiary as its first extra loot (slot 3)
		Heap h = Dungeon.level.drop( new VultureFeather().quantity( Random.IntRange( 1, 2 ) ), RaidEvent.dropCell( Dungeon.level, pos ) );
		if (h.sprite != null) h.sprite.drop( pos );
		Bestiary.setLootSlotSeen( getClass(), 3 );
	}

	private static final String HOME_X    = "home_x";
	private static final String HOME_Y    = "home_y";
	private static final String PREY      = "prey";
	private static final String SCARED    = "scared";
	private static final String EATING    = "eating";
	private static final String FLEEING_T = "fleeing";
	private static final String CLIMB     = "climb";
	private static final String CLOCKWISE = "clockwise";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( HOME_X, homeX );
		bundle.put( HOME_Y, homeY );
		bundle.put( PREY, prey );
		bundle.put( SCARED, scared );
		bundle.put( EATING, eating );
		bundle.put( FLEEING_T, fleeing );
		bundle.put( CLIMB, climb );
		bundle.put( CLOCKWISE, clockwise );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		homeX     = bundle.contains( HOME_X ) ? bundle.getInt( HOME_X ) : Integer.MIN_VALUE;
		homeY     = bundle.contains( HOME_Y ) ? bundle.getInt( HOME_Y ) : Integer.MIN_VALUE;
		prey      = bundle.contains( PREY ) ? bundle.getInt( PREY ) : -1;
		scared    = bundle.contains( SCARED ) ? bundle.getInt( SCARED ) : -1;
		eating    = bundle.contains( EATING ) ? bundle.getInt( EATING ) : 0;
		fleeing   = bundle.contains( FLEEING_T ) ? bundle.getInt( FLEEING_T ) : 0;
		climb     = bundle.contains( CLIMB ) ? bundle.getInt( CLIMB ) : 0;
		clockwise = bundle.contains( CLOCKWISE ) && bundle.getBoolean( CLOCKWISE );
		if (prey != -1) alignment = Alignment.ENEMY;
	}
}
