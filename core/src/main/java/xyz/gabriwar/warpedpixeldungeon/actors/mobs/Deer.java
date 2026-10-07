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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.HuntEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.DeerSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DoeSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A deer of the open woods and meadows: daytime fauna (OverworldFauna) and the quarry of the
 * wolf packs' night hunts (levels.overworld.HuntEvent). Peaceful but fair game: it bolts from
 * the hero and from wolves - a sprint, then it flags - and never fights. Cheap on purpose: no
 * field of view and no pathfinding, one greedy step a move.
 *
 * With wolves at a deer's heels (speed 1, the deer three to five cells ahead) it gains about a
 * cell over its four sprinting moves and then loses three tenths of one a move: the pack has it
 * some fourteen turns in, a dozen cells from where the chase began - in front of the hero.
 */
public class Deer extends Mob {

	{
		spriteClass = DeerSprite.class;

		HP = HT = 14;
		defenseSkill = 10;

		maxLvl = -1;   //no xp: it is fauna, not a foe

		alignment = Alignment.NEUTRAL;
		state = WANDERING;
	}

	//moves at full tilt before it flags, and calm turns before it settles again
	private static final int SPRINT_TURNS = 4, CALM_TURNS = 8;
	private static final float SPRINT = 1.25f, TIRED = 0.7f;
	//how close a hero (any of the party) comes before an ordinary deer bolts; how close any wolf
	private static final int SHY = 5, WOLF_WARY = 8;
	//a hunted deer runs from the pack it can hear far off, and lets the hero nearer
	private static final int SHY_HUNTED = 4, PACK_RANGE = 16;
	//cells from the window's edge at which a deer making good its escape is gone
	private static final int EDGE = 2;

	public boolean doe = Random.Int( 2 ) == 0;
	//moves spent fleeing since it last calmed, turns without a threat, and its last step (a
	//NEIGHBOURS8 offset) - it keeps its line when it can
	private int running = 0, calm = 0, heading = 0;

	{
		setDoe( doe );
	}

	/** A doe or a stag: the sprite follows, so a co-op guest draws the same deer. */
	public void setDoe( boolean doe ){
		this.doe = doe;
		spriteClass = doe ? DoeSprite.class : DeerSprite.class;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public int attackSkill( Char target ) {
		return 0;
	}

	//a tap goes for it, as for any quarry: an ordinary neutral would trade places
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	public float speed() {
		float s = super.speed();
		return running == 0 ? s : running <= SPRINT_TURNS ? s * SPRINT : s * TIRED;
	}

	@Override
	protected boolean act() {
		Level level = Dungeon.level;
		if (level.heroFOV != null && level.heroFOV[pos] && !Bestiary.isSeen( getClass() )) Bestiary.setSeen( getClass() );
		if (paralysed > 0 || state == SLEEPING){
			spend( TICK );
			return true;
		}
		HuntPack pack = buff( HuntPack.class );
		//the hunt's quarry with no pack left behind it: it gets away
		boolean escaping = pack != null && !HuntEvent.packHunting( pack.hunt );
		if (escaping){
			HuntEvent.packBroken( pack.hunt, this );
			if (leaves()){
				spend( TICK );
				OverworldLevel.vanish( level, this );
				return true;
			}
		}

		//every hero of the party it can see coming
		ArrayList<Integer> threats = new ArrayList<>();
		int shy = escaping ? Integer.MAX_VALUE : pack != null ? SHY_HUNTED : SHY;
		for (Hero hero : OverworldLevel.heroesOn( level )){
			if (hero.invisible <= 0 && level.distance( pos, hero.pos ) <= shy) threats.add( hero.pos );
		}
		if (!escaping){
			int range = pack != null ? PACK_RANGE : WOLF_WARY;
			for (Mob m : level.mobs){
				if (HuntPack.isWolf( m ) && m.isAlive() && level.distance( pos, m.pos ) <= range) threats.add( m.pos );
			}
		}

		if (threats.isEmpty()){
			if (++calm >= CALM_TURNS && running > 0){
				running = 0;
				heading = 0;
				if (sprite != null) sprite.hideAlert();
			}
			state = WANDERING;
			amble();
			spend( TICK );
			return true;
		}
		calm = 0;
		if (sprite != null){
			if (running == 0) sprite.showAlert();
			else if (running == 2) sprite.hideAlert();
		}
		running++;
		state = FLEEING;
		int step = bolt( threats );
		if (step != -1 && sprite != null){
			heading = step - pos;
			moveSprite( pos, step );
			move( step );
		}
		spend( 1f / speed() );
		return true;
	}

	/** Its next step away from the threats (cells): the open neighbour that leaves the nearest
	 *  of them furthest off, its own line kept on a tie; -1 when every way is closer to one of
	 *  them, or shut - cornered, it freezes. */
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

	//open ground it can step onto now: no water, pit or harm, nobody there. calm, it keeps off
	//the tall grass - trampling it rolls for seeds, and a meadow of grazers would carpet itself
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

	//a deer making good its escape is gone: off the edge of the world, out of everyone's sight,
	//or into the trees well away from the hero (where he sees it go)
	private boolean leaves(){
		Level level = Dungeon.level;
		int w = level.width(), x = pos % w, y = pos / w;
		if (x <= EDGE || y <= EDGE || x >= w - 1 - EDGE || y >= level.height() - 1 - EDGE) return true;
		int dist = OverworldLevel.heroDistance( level, pos );
		boolean seen = NetManager.anyHeroSees( pos );
		if (!seen && dist >= 6) return true;
		if (dist < 8) return false;
		int trees = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int t = level.map[pos + n];
			if (t == Terrain.TREE_PINE || t == Terrain.TREE_OAK) trees++;
		}
		if (trees < 3) return false;
		//one-off leaves from the trees it slips into (GENERAL: the level's own palette is not for bursts)
		if (seen) CellEmitter.get( pos ).burst( LeafParticle.GENERAL, 6 );
		return true;
	}

	@Override
	public void die( Object cause ) {
		//read before death takes the buffs
		HuntPack pack = buff( HuntPack.class );
		long hunt = pack != null ? pack.hunt : Long.MIN_VALUE;
		boolean byPack = HuntPack.isWolf( cause );
		super.die( cause );
		Bestiary.countEncounter( getClass() );
		//what is left of it: more where the pack brought it down and was driven off it
		Heap carcass = Dungeon.level.drop( new MysteryMeat().quantity( byPack ? 2 : 1 ), RaidEvent.dropCell( Dungeon.level, pos ) );
		if (carcass.sprite != null) carcass.sprite.drop();
		HuntEvent.deerDown( this, hunt, byPack );
	}

	private static final String DOE     = "doe";
	private static final String RUNNING = "running";
	private static final String CALM    = "calm";
	private static final String HEADING = "heading";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( DOE, doe );
		bundle.put( RUNNING, running );
		bundle.put( CALM, calm );
		bundle.put( HEADING, heading );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		setDoe( bundle.contains( DOE ) && bundle.getBoolean( DOE ) );
		running = bundle.contains( RUNNING ) ? bundle.getInt( RUNNING ) : 0;
		calm    = bundle.contains( CALM )    ? bundle.getInt( CALM )    : 0;
		heading = bundle.contains( HEADING ) ? bundle.getInt( HEADING ) : 0;
	}
}
