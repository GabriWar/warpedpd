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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SectorMap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * A bandit come for a home's chests on a raid's night (HomeRaid): a raider of the village raids
 * (Raider), its fight, its armband and its purse, sent at a home instead of a village. The band
 * comes for ONE chest, the richest on the home's ground as it arrives (by Notice.valueOf, kept on
 * its raid: ThreatWorld.Event.chest): each raider makes for it, breaking in where something built
 * keeps it out (Breaker), takes up to GRAB of its things - the dearest first - and runs off with
 * them out of everyone's sight; once it is empty the rest make off too, never on to another chest.
 * Cut down, a raider drops all it carried. A hero within ENGAGE of it, or one who strikes it, is
 * fought; one more than CHASE off is let go for the chest. At dawn the band makes off, whatever it
 * holds.
 */
public class HomeRaider extends Raider {

	/** How many of a chest's things one raider carries off. */
	public static final int GRAB = 4;
	/** A hero this near (in its sight) is fought; one further than CHASE is left for the chests. */
	public static final int ENGAGE = 4, CHASE = 8;
	//the turns one making off has to get out of sight before it is simply gone, and how far is out of sight
	static final int FLEE_TURNS = 30, UNSEEN = 21;

	/** Its home's key (HomeThreats.key) and its raid's id (ThreatWorld.Event.id). */
	public String home;
	public long event;
	/** What it took. */
	public final ArrayList<Item> carried = new ArrayList<>();
	/** Making off, with its haul or with the dawn. */
	public boolean fleeing;
	//turns spent making off (not saved: a reload gives it the full while again)
	private int fled;

	{
		WANDERING = new Looting();
		state = WANDERING;
	}

	/**
	 * A raid begins (HomeThreats.raidNight): its band set down at the edge of the home's ground,
	 * spread round a third of it from one side, each making for the chests. The raiders put down.
	 */
	public static int band( OverworldLevel level, HomesteadWorld.Home home, ThreatWorld.Event e, int size ){
		int hearth = HomeThreats.hearth( level, home );
		if (hearth == -1) return 0;
		StorageChest target = richest( level, home, hearth );
		if (target != null){
			int w = level.width();
			e.chest = OverworldLevel.worldKey( level.worldX + target.pos % w, level.worldY + target.pos / w );
		}
		float bearing = Random.Float();
		int placed = 0, first = -1;
		for (int i = 0; i < size; i++){
			int c = HomeThreats.edgeCell( level, hearth, bearing + i / (3f * size) );
			if (c == -1) continue;
			HomeRaider r = new HomeRaider();
			r.home = HomeThreats.key( home );
			r.event = e.id;
			r.day = RaidEvent.day();
			HomeThreats.add( level, r, c );
			placed++;
			if (first == -1) first = c;
		}
		e.came += placed;
		if (placed > 0){
			int w = level.width();
			String from = HomeThreats.bearing( level.worldX + first % w - home.wx, level.worldY + first / w - home.wy );
			GLog.n( Messages.get( HomeRaider.class, "came", home.title(), from ) );
			if (HomeThreats.live( level )){
				SpatialSound.play( Assets.Sounds.CHALLENGE, first, 0.8f );
			}
		}
		return placed;
	}

	@Override
	protected boolean act(){
		//one charmed or turned fights as any raider turned
		if (alignment != Alignment.ENEMY) return super.act();
		//its raid is the night's: the village raids' day never sends it off
		day = RaidEvent.day();
		if (!fleeing && nightOver()) fleeing = true;
		if (fleeing) return flee();
		if (state == HUNTING && (enemy == null || !enemy.isAlive()
				|| Dungeon.level.distance( pos, enemy.pos ) > CHASE)){
			//let go of the fight: back to the chests
			state = WANDERING;
		}
		return super.act();
	}

	//dawn come, or its home or its raid gone
	private boolean nightOver(){
		DayNightCycle.Phase phase = DayNightCycle.phase();
		ThreatWorld world = ThreatWorld.get();
		return phase == DayNightCycle.Phase.DAWN || phase == DayNightCycle.Phase.DAY || HomeThreats.home( home ) == null
				|| world == null || world.event( home, event ) == null;
	}

	/** The richest chest on a home's ground with anything in it, the nearest the hearthstone of
	 *  equals: what a band comes for. Null for none. */
	public static StorageChest richest( OverworldLevel level, HomesteadWorld.Home home, int hearth ){
		StorageChest best = null;
		long bestValue = -1;
		for (Mob m : level.mobs){
			if (!(m instanceof StorageChest) || ((StorageChest) m).storage.items.isEmpty()
					|| !HomeThreats.inArea( level, home, m.pos )) continue;
			long v = Notice.valueOf( (StorageChest) m );
			if (v > bestValue || (v == bestValue && level.distance( hearth, m.pos ) < level.distance( hearth, best.pos ))){
				bestValue = v;
				best = (StorageChest) m;
			}
		}
		return best;
	}

	/** The chest it is after: its band's (ThreatWorld.Event.chest) while it stands with anything
	 *  in it; null when it is emptied or gone, and the band makes off. */
	public StorageChest goal(){
		ThreatWorld world = ThreatWorld.get();
		ThreatWorld.Event e = world == null ? null : world.event( home, event );
		if (e == null || e.chest == ThreatWorld.Event.NO_CHEST || !(Dungeon.level instanceof OverworldLevel)) return null;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int c = ow.localCell( SectorMap.wx( e.chest ), SectorMap.wy( e.chest ) );
		if (c == -1) return null;
		for (Mob m : ow.mobs){
			if (m.pos == c && m instanceof StorageChest) return ((StorageChest) m).storage.items.isEmpty() ? null : (StorageChest) m;
		}
		return null;
	}

	/** Takes up to GRAB of a chest's things, the dearest first, and makes off with them: how many. */
	public int grab( StorageChest chest ){
		ArrayList<Item> things = new ArrayList<>( chest.storage.items );
		things.sort( Comparator.comparingInt( (Item i) -> Notice.valueOf( i ) ).reversed() );
		int took = 0;
		for (Item i : things){
			if (took >= GRAB) break;
			i.detachAll( chest.storage );
			carried.add( i );
			took++;
		}
		if (took > 0){
			fleeing = true;
			if (Dungeon.level.heroFOV != null && (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[chest.pos])){
				GLog.n( Messages.get( HomeRaider.class, "grabbed", took ) );
			}
		}
		return took;
	}

	//off out of everyone's sight with what it took, away from the nearest hero; gone with it the
	//moment nobody sees it, or once it has taken long enough about it
	private boolean flee(){
		Level level = Dungeon.level;
		if (paralysed > 0){
			spend( TICK );
			return true;
		}
		if (fieldOfView == null || fieldOfView.length != level.length()) fieldOfView = new boolean[level.length()];
		level.updateFieldOfView( this, fieldOfView );
		if (++fled > FLEE_TURNS || (OverworldLevel.heroDistance( level, pos ) > UNSEEN && !NetManager.anyHeroSees( pos ))){
			escape();
			return true;
		}
		Hero hero = Dungeon.hero;
		int old = pos;
		if (hero != null && hero.isAlive() && getFurther( hero.pos )){
			spend( 1 / speed() );
			return sprite == null || moveSprite( old, pos );
		}
		spend( TICK );
		return true;
	}

	/** Got away: what it took is gone with it. */
	void escape(){
		ThreatWorld world = ThreatWorld.get();
		if (world != null && !carried.isEmpty()) world.stolen( home, event, carried.size() );
		carried.clear();
		spend( TICK );
		OverworldLevel.vanish( Dungeon.level, this );
	}

	@Override
	public void die( Object cause ){
		//all it carried falls where it lies
		for (Item i : carried){
			Heap h = Dungeon.level.drop( i, RaidEvent.dropCell( Dungeon.level, pos ) );
			if (h.sprite != null) h.sprite.drop();
		}
		carried.clear();
		ThreatWorld world = ThreatWorld.get();
		if (world != null) world.slain( home, event );
		super.die( cause );
	}

	//from the edge to the richest chest, and its things into the bag
	private class Looting extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			if (enemyInFOV && enemy != null && (justAlerted || Dungeon.level.distance( pos, enemy.pos ) <= ENGAGE)){
				return noticeEnemy();
			}
			StorageChest chest = goal();
			if (chest == null){
				//nothing left to take: off into the night
				fleeing = true;
				spend( TICK );
				return true;
			}
			if (Dungeon.level.adjacent( pos, chest.pos )){
				grab( chest );
				spend( TICK );
				return true;
			}
			int old = pos;
			if (getCloser( chest.pos )){
				spend( 1 / speed() );
				return sprite == null || moveSprite( old, pos );
			}
			//shut out: the weakest way in
			Breaker.Order way = Breaker.planFor( HomeRaider.this, Dungeon.level, chest.pos, Breaker.ruleOf( HomeRaider.this ) );
			if (way != null) return obeyBreaker( way );
			spend( TICK );
			return true;
		}
	}

	private static final String HOME = "home_raid_home", EVENT = "home_raid_event", CARRIED = "home_raid_carried",
			FLEEING = "home_raid_fleeing";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HOME, home == null ? "" : home );
		bundle.put( EVENT, event );
		bundle.put( CARRIED, carried );
		bundle.put( FLEEING, fleeing );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		home = bundle.getString( HOME );
		event = bundle.getLong( EVENT );
		carried.clear();
		if (bundle.contains( CARRIED )){
			Collection<Bundlable> things = bundle.getCollection( CARRIED );
			for (Bundlable b : things) carried.add( (Item) b );
		}
		fleeing = bundle.getBoolean( FLEEING );
	}
}
