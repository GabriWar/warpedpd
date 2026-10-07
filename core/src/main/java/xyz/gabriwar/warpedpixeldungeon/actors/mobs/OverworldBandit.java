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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dread;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.VillagerSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** A highway bandit: a hostile thief in outlaw colours. */
public class OverworldBandit extends Bandit {

	public int tint = Random.Int( 8 );

	public static final long NO_AMBUSH = Long.MIN_VALUE;
	//the stall (Caravaneer.stall) this outlaw was sent against; NO_AMBUSH for an outlaw of the camps
	public long ambushOf = NO_AMBUSH;
	//the one of an ambush party who menaces the stall itself; the rest go for the hero
	public boolean menace = false;
	//the caravan robbed, an ambush party making off into the trees with the goods
	public boolean retreating = false;

	//turns spent making off (not bundled: a reload gives him the full while again)
	private int fled = 0;
	//how long one making off may take to get out of everyone's sight before he is simply gone
	private static final int MAKE_OFF = 30;
	//the surface's sight (viewDistance 20) and a cell
	private static final int UNSEEN = 21;

	@Override
	public CharSprite sprite() {
		return new VillagerSprite( VillagerSprite.THIEF, tint );
	}

	/** Sends this outlaw at someone in a stall's ambush (CaravanAmbush.spring). */
	public void sendAgainst( Char foe, long stall ){
		ambushOf = stall;
		aggro( foe );
		target = foe.pos;
	}

	/** Sent on a world event (a stall's ambush, a raid): never counted in a camp's census
	 *  (OverworldLevel.mobNear). */
	public boolean sentOnEvent(){
		return ambushOf != NO_AMBUSH;
	}

	/** Kept in the parked store while its event lives (OverworldLevel.keepsForever). */
	public boolean eventHeld(){
		return ambushOf != NO_AMBUSH;
	}

	//the stall this outlaw was sent against, standing in the window, or null
	private Caravaneer stall(){
		if (ambushOf == NO_AMBUSH || Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Caravaneer && ((Caravaneer) m).stall == ambushOf && m.isAlive()) return (Caravaneer) m;
		}
		return null;
	}

	@Override
	protected boolean act() {
		if (retreating) return makeOff();
		if (ambushOf != NO_AMBUSH && state == WANDERING && alignment == Alignment.ENEMY){
			//an ambush party never drifts off: having lost whoever it was after, the one menacing
			//the stall goes back to it, and the rest keep about it, watching for the hero
			Caravaneer stall = stall();
			if (stall != null){
				if (menace) aggro( stall );
				target = stall.pos;
			}
		}
		return super.act();
	}

	//the caravan robbed: away from the nearest hero into the trees, and gone the moment nobody
	//can see him (or once he has taken long enough about it)
	private boolean makeOff(){
		if (paralysed > 0){
			spend( TICK );
			return true;
		}
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );
		if (++fled > MAKE_OFF || sprite == null
				|| (OverworldLevel.heroDistance( Dungeon.level, pos ) > UNSEEN && !NetManager.anyHeroSees( pos ))){
			spend( TICK );
			OverworldLevel.vanish( Dungeon.level, this );
			return true;
		}
		int from = nearestHero();
		int old = pos;
		if (from != -1 && getFurther( from )){
			spend( 1 / speed() );
			return moveSprite( old, pos );
		}
		spend( TICK );
		return true;
	}

	//the cell of the nearest hero on the level, or -1
	private int nearestHero(){
		int best = -1, bestDist = Integer.MAX_VALUE;
		if (Dungeon.hero != null && Dungeon.hero.isAlive()){
			best = Dungeon.hero.pos;
			bestDist = Dungeon.level.distance( pos, best );
		}
		if (NetManager.isHost()){
			for (Hero h : NetManager.getNetHeroes()){
				if (!h.isAlive() || h.atExit || h.pos < 0 || h.pos >= Dungeon.level.length()) continue;
				int d = Dungeon.level.distance( pos, h.pos );
				if (d < bestDist){
					best = h.pos;
					bestDist = d;
				}
			}
		}
		return best;
	}

	@Override
	protected Char chooseEnemy() {
		Char e = super.chooseEnemy();
		//the road's own quarrels: an outlaw at large also turns on the town's watch, and an ambush
		//party on the stall it came for - none of it for one charmed, maddened, frightened,
		//fleeing or turned
		if (alignment != Alignment.ENEMY || state == PASSIVE || state == SLEEPING || state == FLEEING
				|| fieldOfView == null || Dungeon.level == null
				|| buff( Amok.class ) != null || buff( Charm.class ) != null
				|| buff( Terror.class ) != null || buff( Dread.class ) != null) return e;
		Caravaneer stall = stall();
		//an ambush party goes for the hero whenever he is there to be had
		if (stall != null && e != Dungeon.hero && heroAtHand( stall )) return Dungeon.hero;
		//locked on to someone besides the stall (a hero, the watch, an ally): kept
		if (e != null && e != stall && e.isAlive()) return e;
		Char best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof RoadPatrol) || !m.isAlive() || m.pos < 0 || m.pos >= fieldOfView.length
					|| !fieldOfView[m.pos]) continue;
			int d = Dungeon.level.distance( pos, m.pos );
			if (d < bestDist){
				best = m;
				bestDist = d;
			}
		}
		if (best != null) return best;
		//with nobody else to fight, the one menacing the stall falls on it; the rest are watching
		return menace && stall != null ? stall : (e == stall ? null : e);
	}

	//is the hero there to be had for an ambush party: in plain sight (to all but the one
	//menacing the stall), come right up to the stall, or striking at us
	private boolean heroAtHand( Caravaneer stall ){
		Hero h = Dungeon.hero;
		if (h == null || !h.isAlive() || h.pos < 0 || h.pos >= fieldOfView.length) return false;
		if (recentlyAttackedBy.contains( h ) || Dungeon.level.distance( h.pos, stall.pos ) <= 3) return true;
		return !menace && fieldOfView[h.pos] && h.invisible <= 0;
	}

	@Override
	protected boolean steal( Hero hero ) {
		//an ambush party is after the caravan's goods, not the hero's pack
		return ambushOf == NO_AMBUSH && super.steal( hero );
	}

	@Override
	public void die( Object cause ) {
		if (cause instanceof RoadPatrol) forfeit();
		super.die( cause );
	}

	/** Cut down by the town's watch: nobody's kill. Turned neutral so Mob.die and Mob.destroy pay
	 *  no experience, loot, tally or skill credit - but whatever it stole from the hero still
	 *  falls where it lies. */
	public void forfeit(){
		Thief self = this;   //Bandit.item shadows Thief.item: the stolen item is in Thief's field
		if (self.item != null){
			Heap h = Dungeon.level.drop( self.item, pos );
			if (h.sprite != null) h.sprite.drop();
			self.item = null;
		}
		alignment = Alignment.NEUTRAL;
	}

	private static final String TINT       = "tint";
	private static final String AMBUSH_OF  = "ambush_of";
	private static final String MENACE     = "menace";
	private static final String RETREATING = "retreating";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TINT, tint );
		bundle.put( AMBUSH_OF, ambushOf );
		bundle.put( MENACE, menace );
		bundle.put( RETREATING, retreating );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		tint = bundle.getInt( TINT );
		ambushOf = bundle.contains( AMBUSH_OF ) ? bundle.getLong( AMBUSH_OF ) : NO_AMBUSH;
		menace = bundle.contains( MENACE ) && bundle.getBoolean( MENACE );
		retreating = bundle.contains( RETREATING ) && bundle.getBoolean( RETREATING );
	}
}
