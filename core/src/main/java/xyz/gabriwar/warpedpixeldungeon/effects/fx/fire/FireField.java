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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * What a burning blob does to the place it burns in, read off the blob's own state every SCAN s
 * (so a co-op guest, who has the blob, sees all of it): a glow on the floor of each burning cell
 * the hero sees (the FLOOR_GLOWS nearest him; a small round one at the waterline of a cell on
 * water), the light it throws - one glow and one pool for each of its LIGHTS busiest groups of
 * joined burning cells (one cut into pieces of at most TILE x TILE), sized to the group and
 * brighter the more of it burns, so a fire is lit as one blob and not as tiles; flickering as a
 * torch, shown a third as strong by day, thrown down any water below; all fires together hold at
 * most fireLightCap() lights (FIRE_LIGHTS, fewer on a struggling phone) and only ever take lights
 * the kit has room for, never another's - shimmer over its groups (a source for every four
 * cells), a warm wash on the water beside it, and the end of each cell it burns out of: smoke
 * curling off and embers glowing on the floor as they cool, or a pop of steam where frost put it
 * out. The trees an open fire or an inferno burns (the CROWNS nearest the hero, seen or not) have
 * their crowns alight (BurningTree) until they burn down.
 *
 * A group on the floor's layer holding the floor glows; made on the render thread the first time
 * its blob's factory emits (FireBlobFx), gone with its blob's emitter.
 */
public class FireField extends Group {

	/** How often it reads its blob, s; the floor glows at most; lights a field and all fields. */
	public static final float SCAN = 0.2f;
	public static final int FLOOR_GLOWS = 24, LIGHTS = 4, FIRE_LIGHTS = 12;
	/** Washes on the water at most, and how often they are laid, s. */
	public static final int WASHES = 8;
	public static final float WASH_EVERY = 1f;
	/** A ray of shimmer from each of a group's sources every this many s. */
	public static final float SHIMMER_EVERY = 0.3f;
	/** A cell gone out: its smoke, its embers and their life, its steam where frost put it out. */
	public static final int OUT_WISPS = 2, OUT_EMBERS = 3, FROST_WISPS = 6;
	public static final float OUT_EMBER_MIN = 1.5f, OUT_EMBER_MAX = 3f;
	/** Cooling embers' colours. */
	public static final Ramp COOLING = new Ramp( 0xFF8030, 0xC04418, 0x5A1808 );

	//the lights every field holds now (render thread)
	private static int fireLights;

	final Blob blob;
	final BlobEmitter emitter;
	final FireStyle style;

	private float scanIn, washIn;
	private boolean leaving;
	//when lights may be asked for again after the kit took some of this field's
	private float lightsRetryAt;
	/** How long a field waits to ask for lights again after the kit took one of its, s. */
	public static final float LIGHTS_RETRY = 2f;

	//the burning cells the hero sees, this scan and the last
	private int[] cells = new int[64], last = new int[64];
	private int count, lastCount;
	private float sumX, sumY;

	//the floor glows, a slot each
	private final FloorGlow[] glows = new FloorGlow[FLOOR_GLOWS];
	private final int[] glowCell = new int[FLOOR_GLOWS];
	private final int[] chosen = new int[FLOOR_GLOWS];
	private final boolean[] kept = new boolean[FLOOR_GLOWS];

	/** A group of joined burning cells wider or taller than this many is cut into pieces. */
	public static final int TILE = 4;
	/** A light follows its group as far as this, px, from scan to scan. */
	public static final float FOLLOW = 32f;
	/** A group's light by its size class (its extent: up to 2 cells, 3, 4): the glow and the
	 *  pool, px (past 63 a doubled frame). */
	public static final int[] GROUP_GLOW = { 31, 47, 94 }, GROUP_POOL = { 47, 63, 94 };

	//the groups this scan: piece key, burning cells, the slot lighting it, extent in cells, sums
	//of their middles
	private static final int GROUPS = 48;
	private final int[] groupKey = new int[GROUPS], groupN = new int[GROUPS], groupSlot = new int[GROUPS];
	private final int[] groupX0 = new int[GROUPS], groupY0 = new int[GROUPS], groupX1 = new int[GROUPS], groupY1 = new int[GROUPS];
	private final float[] groupX = new float[GROUPS], groupY = new float[GROUPS];
	private int groups;
	//the walk over joined cells: a mark a level cell (this scan's burning, walked), its queue
	private int[] mark;
	private int markGen;
	private int[] queue = new int[64];

	//the lights, a slot each: its size class (-1 none), where it was put, a glow and a pool
	private final int[] slotSize = new int[LIGHTS], slotHeld = new int[LIGHTS];
	private final float[] slotX = new float[LIGHTS], slotY = new float[LIGHTS];
	private final LightRef[] slotGlow = new LightRef[LIGHTS], slotPool = new LightRef[LIGHTS];

	/** The burning trees whose crowns are alight at most (BurningTree): the nearest the hero. */
	public static final int CROWNS = 4;
	//the crowns alight, and this scan's burning trees (the wanted ones first)
	private final ArrayList<BurningTree> crowns = new ArrayList<>();
	private int[] trees = new int[16];

	FireField( Blob blob, BlobEmitter emitter, FireStyle style ){
		this.blob = blob;
		this.emitter = emitter;
		this.style = style;
		for (int i = 0; i < LIGHTS; i++){
			slotSize[i] = -1;
			slotGlow[i] = new LightRef();
			slotPool[i] = new LightRef();
		}
		for (int i = 0; i < FLOOR_GLOWS; i++) glowCell[i] = -1;
	}

	/** A field for a blob's emitter on the floor's layer, or null off the game's scene. Render
	 *  thread. */
	static FireField attach( Blob blob, BlobEmitter emitter, FireStyle style ){
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		if (floor == null) return null;
		FireField f = new FireField( blob, emitter, style );
		floor.add( f );
		return f;
	}

	/** A new scene: the last one's fields went with it, and their lights. */
	public static void sceneCreated(){
		fireLights = 0;
	}

	/** The lights all fires hold now. */
	public static int fireLights(){
		return fireLights;
	}

	/** The lights all fires may hold now: FIRE_LIGHTS, or half the kit's lights when a struggling
	 *  phone has fewer (FxBudget's tiers). */
	public static int fireLightCap(){
		return Math.min( FIRE_LIGHTS, FxBudget.lightCap() / 2 );
	}

	/** The lights this field holds now (two a group it lights). */
	public int lightsHeld(){
		int n = 0;
		for (int s = 0; s < LIGHTS; s++) if (slotSize[s] != -1) n += slotHeld[s];
		return n;
	}

	/** How many burning cells it saw at its last scan, and their middle. */
	public int seen(){
		return count;
	}

	public float middleX(){
		return count > 0 ? sumX / count : 0;
	}

	public float middleY(){
		return count > 0 ? sumY / count : 0;
	}

	@Override
	public void update(){
		if (!leaving && (!emitter.alive || emitter.parent == null || blob.emitter != emitter)) goAway();
		if (leaving){
			//its glows fade out, then it is let go with their buffers (the surface is one long scene)
			super.update();
			if (countLiving() == 0) Fx.dispose( this );
			return;
		}
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		float dt = Game.elapsed;
		if ((scanIn -= dt) <= 0){
			scanIn += SCAN;
			if (scanIn < 0) scanIn = SCAN;
			scan();
		}
		if ((washIn -= dt) <= 0){
			washIn = WASH_EVERY;
			washes();
		}
	}

	//its blob's emitter gone: its lights put out, its glows fading, itself off the scene once
	//they are out
	private void goAway(){
		leaving = true;
		for (int i = 0; i < LIGHTS; i++) releaseSlot( i );
		groups = 0;
		for (int i = 0; i < FLOOR_GLOWS; i++){
			if (glows[i] != null) glows[i].out();
			glows[i] = null;
		}
		for (BurningTree t : crowns) t.out();
		crowns.clear();
	}

	/** Whether its blob's emitter is gone and it is fading away. */
	public boolean leaving(){
		return leaving;
	}

	// ------------------------------------------------------------------ the scan

	void scan(){
		Level level = Dungeon.level;
		if (level == null || level.heroFOV == null) return;
		int w = level.width();

		//this scan's cells become the last
		int[] t = last;
		last = cells;
		cells = t;
		lastCount = count;
		count = 0;
		sumX = sumY = 0;

		int[] cur = blob.cur;
		if (cur != null && blob.volume > 0 && !blob.area.isEmpty()){
			for (int j = blob.area.top; j < blob.area.bottom; j++){
				for (int i = blob.area.left; i < blob.area.right; i++){
					int c = i + j * w;
					if (c < 0 || c >= cur.length || c >= level.heroFOV.length) continue;
					if (!style.burning( cur[c] ) || !level.heroFOV[c]) continue;
					if (count == cells.length){
						int[] grown = new int[cells.length * 2];
						System.arraycopy( cells, 0, grown, 0, count );
						cells = grown;
					}
					cells[count++] = c;
					float x = (i + 0.5f) * DungeonTilemap.SIZE, y = (j + 0.5f) * DungeonTilemap.SIZE;
					sumX += x;
					sumY += y;
				}
			}
		}

		group( level );
		wentOut( level, cur );
		floorGlows( level );
		lights();
		shimmer();
		crowns( level, cur );
	}

	//the burning cells seen, gathered into groups of joined cells (8 ways); a group wider or taller
	//than TILE cut evenly into pieces of at most TILE x TILE
	private void group( Level level ){
		groups = 0;
		if (count == 0) return;
		int w = level.width(), h = level.height();
		if (mark == null || mark.length != level.length()) mark = new int[level.length()];
		if (queue.length < count) queue = new int[cells.length];
		int burning = ++markGen, walked = ++markGen;
		for (int k = 0; k < count; k++) mark[cells[k]] = burning;
		for (int k = 0; k < count; k++){
			int start = cells[k];
			if (mark[start] != burning) continue;
			int head = 0, tail = 0;
			queue[tail++] = start;
			mark[start] = walked;
			int x0 = start % w, x1 = x0, y0 = start / w, y1 = y0;
			while (head < tail){
				int c = queue[head++];
				int cx = c % w, cy = c / w;
				x0 = Math.min( x0, cx );
				x1 = Math.max( x1, cx );
				y0 = Math.min( y0, cy );
				y1 = Math.max( y1, cy );
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int nx = cx + dx, ny = cy + dy;
						if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
						int n = nx + ny * w;
						if (mark[n] == burning){
							mark[n] = walked;
							queue[tail++] = n;
						}
					}
				}
			}
			//its pieces: as many across and down as TILE needs, as even as they come
			int wide = x1 - x0 + 1, high = y1 - y0 + 1;
			int across = (wide + TILE - 1) / TILE, down = (high + TILE - 1) / TILE;
			int first = groups;
			for (int q = 0; q < tail; q++){
				int c = queue[q];
				int cx = c % w, cy = c / w;
				addTo( first, (cx - x0) * across / wide + (cy - y0) * down / high * across, cx, cy );
			}
		}
	}

	//a cell into its piece (key) among the pieces from `first` on
	private void addTo( int first, int key, int cx, int cy ){
		float x = (cx + 0.5f) * DungeonTilemap.SIZE, y = (cy + 0.5f) * DungeonTilemap.SIZE;
		for (int g = first; g < groups; g++){
			if (groupKey[g] == key){
				groupN[g]++;
				groupX[g] += x;
				groupY[g] += y;
				groupX0[g] = Math.min( groupX0[g], cx );
				groupX1[g] = Math.max( groupX1[g], cx );
				groupY0[g] = Math.min( groupY0[g], cy );
				groupY1[g] = Math.max( groupY1[g], cy );
				return;
			}
		}
		if (groups == GROUPS) return;
		groupKey[groups] = key;
		groupN[groups] = 1;
		groupX[groups] = x;
		groupY[groups] = y;
		groupX0[groups] = groupX1[groups] = cx;
		groupY0[groups] = groupY1[groups] = cy;
		groups++;
	}

	/** The groups of burning cells found at the last scan. */
	public int groups(){
		return groups;
	}

	//a group's size class by its extent in cells: up to 2, 3, 4
	private int sizeOf( int g ){
		int extent = Math.max( groupX1[g] - groupX0[g], groupY1[g] - groupY0[g] ) + 1;
		return extent <= 2 ? 0 : extent == 3 ? 1 : 2;
	}

	//every cell burning last scan and not now, that the hero sees: its end, once
	private void wentOut( Level level, int[] cur ){
		for (int k = 0; k < lastCount; k++){
			int c = last[k];
			if (cur != null && c < cur.length && style.burning( cur[c] )) continue;
			if (c >= level.heroFOV.length || !level.heroFOV[c]) continue;
			goingOut( level, c );
		}
	}

	/** A cell gone out: frost put it out (a pop of steam), or it burnt down (smoke curling off,
	 *  embers glowing on the floor as they cool). */
	void goingOut( Level level, int c ){
		int w = level.width();
		float x = (c % w + 0.5f) * DungeonTilemap.SIZE, y = (c / w + 0.5f) * DungeonTilemap.SIZE;
		Emitter matter = emitter.matter();
		Freezing frost = level.blobs == null ? null : (Freezing) level.blobs.get( Freezing.class );
		if (frost != null && frost.cur != null && c < frost.cur.length && (frost.cur[c] > 0 || frost.volume > 0 && near( frost.cur, c, w ))){
			for (int i = 0; i < FROST_WISPS; i++){
				FxFactories.steam().emit( matter, i, x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -2f, 4f ) );
			}
			return;
		}
		if (style.smoke != null){
			for (int i = 0; i < OUT_WISPS; i++){
				if (!FxBudget.keep( FxBudget.P2, x, y )) continue;
				FireBlobFx.smoke( Flames.make( matter, FxParticle.class, x + FxRandom.Float( -4f, 4f ), y - 2 ), style.smoke, 0.35f );
			}
		}
		for (int i = 0; i < OUT_EMBERS; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) continue;
			float life = FxRandom.Float( OUT_EMBER_MIN, OUT_EMBER_MAX );
			Flames.make( emitter, FxParticle.class, x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( 0f, 6f ) )
					.look( FxFrames.EMBER_1 ).ramp( COOLING, true ).life( life ).alpha( 1f, 0.05f, life * 0.4f )
					.snap().priority( FxBudget.P2 );
		}
	}

	//frost beside the cell (it went out as the cold reached it)
	private static boolean near( int[] frost, int c, int w ){
		return (c - 1 >= 0 && frost[c - 1] > 0) || (c + 1 < frost.length && frost[c + 1] > 0)
				|| (c - w >= 0 && frost[c - w] > 0) || (c + w < frost.length && frost[c + w] > 0);
	}

	// ------------------------------------------------------------------ the floor's glows

	private void floorGlows( Level level ){
		if (style.floor == 0) return;
		int n = pickNearest( level );
		for (int s = 0; s < FLOOR_GLOWS; s++) kept[s] = false;
		//the glows kept: their cells still chosen
		for (int s = 0; s < FLOOR_GLOWS; s++){
			FloorGlow g = glows[s];
			if (g == null) continue;
			if (!g.alive || g.isOut()){
				glows[s] = null;
				continue;
			}
			boolean still = false;
			for (int k = 0; k < n; k++){
				if (chosen[k] == glowCell[s]){
					still = true;
					chosen[k] = -1;
					break;
				}
			}
			if (still) kept[s] = true;
			else {
				g.out();
				glows[s] = null;
			}
		}
		//new ones for the cells left
		int w = level.width();
		for (int k = 0; k < n; k++){
			int c = chosen[k];
			if (c < 0) continue;
			int s = 0;
			while (s < FLOOR_GLOWS && glows[s] != null) s++;
			if (s == FLOOR_GLOWS) break;
			float x = (c % w + 0.5f) * DungeonTilemap.SIZE, y = (c / w + 0.5f) * DungeonTilemap.SIZE;
			FloorGlow g = (FloorGlow) recycle( FloorGlow.class );
			if (level.water[c]){
				g.reset( x, y + 5, FxFrames.Light.GLOW_7, style.floor, 0.25f, style.floorFlicker() );
			} else {
				g.reset( x, y + 3, FxFrames.Light.POOL_15, style.floor, style.floorLevel, style.floorFlicker() );
			}
			glows[s] = g;
			glowCell[s] = c;
		}
	}

	//the FLOOR_GLOWS burning cells nearest the hero into chosen; how many
	private int pickNearest( Level level ){
		int n = Math.min( count, FLOOR_GLOWS );
		if (count <= FLOOR_GLOWS){
			System.arraycopy( cells, 0, chosen, 0, n );
			return n;
		}
		int hero = Dungeon.hero != null ? Dungeon.hero.pos : -1;
		int w = level.width();
		//a selection, best first, without allocating: the cells' order is shuffled in place
		for (int k = 0; k < n; k++){
			int best = k;
			int bd = Integer.MAX_VALUE;
			for (int m = k; m < count; m++){
				int d = hero < 0 ? m : Math.max( Math.abs( cells[m] % w - hero % w ), Math.abs( cells[m] / w - hero / w ) );
				if (d < bd){
					bd = d;
					best = m;
				}
			}
			int t = cells[k];
			cells[k] = cells[best];
			cells[best] = t;
			chosen[k] = cells[k];
		}
		return n;
	}

	// ------------------------------------------------------------------ the lights

	private void lights(){
		//the busiest groups first (insertion sort: few of them)
		for (int a = 1; a < groups; a++){
			for (int b = a; b > 0 && groupN[b] > groupN[b - 1]; b--) swapGroups( b, b - 1 );
		}
		int wanted = Math.min( groups, LIGHTS );
		for (int g = 0; g < groups; g++) groupSlot[g] = -1;
		//slots keep their lights on the wanted group nearest where they were, while it is as big
		for (int s = 0; s < LIGHTS; s++){
			if (slotSize[s] == -1) continue;
			//a light the kit took for a newer one is no longer counted
			int held = (slotGlow[s].held() ? 1 : 0) + (slotPool[s].held() ? 1 : 0);
			fireLights = Math.max( 0, fireLights - (slotHeld[s] - held) );
			slotHeld[s] = held;
			//the kit took this group's lights for newer ones: none asked for again for a while
			if (held < 2) lightsRetryAt = Game.timeTotal + LIGHTS_RETRY;
			int g = nearestUnlit( slotX[s], slotY[s], wanted );
			if (held == 0 || g < 0 || sizeOf( g ) != slotSize[s]){
				releaseSlot( s );
				continue;
			}
			groupSlot[g] = s;
			place( s, g );
		}
		//a struggling phone holds fewer: the least busy groups' lights go first
		for (int g = wanted - 1; g >= 0 && fireLights > fireLightCap(); g--){
			if (groupSlot[g] == -1) continue;
			releaseSlot( groupSlot[g] );
			groupSlot[g] = -1;
		}
		//new lights for the wanted groups that have none, while the fires' lights allow and the kit
		//has room for them (no other light is put out for one of these)
		for (int g = 0; g < wanted; g++){
			if (groupSlot[g] != -1) continue;
			int s = 0;
			while (s < LIGHTS && slotSize[s] != -1) s++;
			if (s == LIGHTS || fireLights + 2 > fireLightCap() || Game.timeTotal < lightsRetryAt || !LightRef.room( 2, 0 )) break;
			float x = groupX[g] / groupN[g], y = groupY[g] / groupN[g];
			float lv = level( groupN[g] );
			int size = sizeOf( g );
			FxLight pool = FxLight.pool( x, y + 4, GROUP_POOL[size], style.light, lv ).flicker( FxLight.TORCH ).day( Fx.DAY_PERSISTENT ).reflect();
			FxLight glow = FxLight.glow( x, y - 4, GROUP_GLOW[size], style.light, lv ).flicker( FxLight.TORCH ).day( Fx.DAY_PERSISTENT );
			slotPool[s].set( pool );
			slotGlow[s].set( glow );
			slotSize[s] = size;
			slotX[s] = x;
			slotY[s] = y;
			slotHeld[s] = 2;
			groupSlot[g] = s;
			fireLights += 2;
		}
	}

	/** A group's light: the style's base and a tenth more for each of its cells burning. */
	float level( int cells ){
		return Math.min( style.lightLevel + 0.1f * cells, style.lightLevel + 0.4f );
	}

	//the wanted group without a light whose middle is nearest (x, y), within FOLLOW; -1 for none
	private int nearestUnlit( float x, float y, int wanted ){
		int best = -1;
		float bd = FOLLOW * FOLLOW;
		for (int g = 0; g < wanted; g++){
			if (groupSlot[g] != -1) continue;
			float dx = groupX[g] / groupN[g] - x, dy = groupY[g] / groupN[g] - y;
			float d = dx * dx + dy * dy;
			if (d <= bd){
				bd = d;
				best = g;
			}
		}
		return best;
	}

	private void place( int s, int g ){
		float x = groupX[g] / groupN[g], y = groupY[g] / groupN[g];
		float lv = level( groupN[g] );
		slotPool[s].place( x, y + 4, lv );
		slotGlow[s].place( x, y - 4, lv );
		slotX[s] = x;
		slotY[s] = y;
	}

	private void releaseSlot( int s ){
		if (slotSize[s] == -1) return;
		slotPool[s].release( 0.3f );
		slotGlow[s].release( 0.3f );
		slotSize[s] = -1;
		fireLights = Math.max( 0, fireLights - slotHeld[s] );
		slotHeld[s] = 0;
	}

	private void swapGroups( int a, int b ){
		int k = groupKey[a]; groupKey[a] = groupKey[b]; groupKey[b] = k;
		int n = groupN[a]; groupN[a] = groupN[b]; groupN[b] = n;
		float x = groupX[a]; groupX[a] = groupX[b]; groupX[b] = x;
		float y = groupY[a]; groupY[a] = groupY[b]; groupY[b] = y;
		int t = groupX0[a]; groupX0[a] = groupX0[b]; groupX0[b] = t;
		t = groupX1[a]; groupX1[a] = groupX1[b]; groupX1[b] = t;
		t = groupY0[a]; groupY0[a] = groupY0[b]; groupY0[b] = t;
		t = groupY1[a]; groupY1[a] = groupY1[b]; groupY1[b] = t;
	}

	// ------------------------------------------------------------------ shimmer and washes

	//over each group, a source for every four of its cells (at least one, for what always
	//shimmers), each a ray a SHIMMER_EVERY s somewhere over the group
	private void shimmer(){
		float p = SCAN / SHIMMER_EVERY;
		for (int g = 0; g < groups; g++){
			int sources = groupN[g] / 4;
			if (style.shimmers()) sources = Math.max( 1, sources );
			for (int k = 0; k < sources; k++){
				if (FxRandom.Float() >= p) continue;
				float x = (groupX0[g] + FxRandom.Float( groupX1[g] - groupX0[g] + 1 )) * DungeonTilemap.SIZE;
				float y = (groupY0[g] + 0.5f + FxRandom.Float( groupY1[g] - groupY0[g] )) * DungeonTilemap.SIZE + FxRandom.Float( -2f, 4f );
				HeatRay.rise( emitter, x, y );
			}
		}
	}

	//the fire's light washing over the water beside it, WASHES cells at most
	private void washes(){
		Level level = Dungeon.level;
		if (level == null || count == 0) return;
		int w = level.width(), laid = 0;
		for (int k = 0; k < count && laid < WASHES; k++){
			int c = cells[k];
			for (int d = 0; d < 4 && laid < WASHES; d++){
				int n = c + (d == 0 ? -1 : d == 1 ? 1 : d == 2 ? -w : w);
				if (n < 0 || n >= level.length() || !level.water[n]) continue;
				if (blob.cur != null && n < blob.cur.length && style.burning( blob.cur[n] )) continue;
				WaterFX.wash( n, style.light, 0.12f, WASH_EVERY * 1.2f );
				laid++;
			}
		}
	}

	// ------------------------------------------------------------------ the trees

	//the trees burning in it, for an open fire or an inferno (the clean and the cursed fires take
	//no tree as wood): the CROWNS nearest the hero, seen or not, have their crowns alight; one whose
	//tree is gone, no longer burns or is no longer among them goes out
	private void crowns( Level level, int[] cur ){
		int n = 0;
		if (style.flame == Element.FIRE && (style.kind == FireStyle.Kind.OPEN || style.kind == FireStyle.Kind.INFERNO)
				&& cur != null && blob.volume > 0 && !blob.area.isEmpty()){
			int w = level.width();
			for (int j = blob.area.top; j < blob.area.bottom; j++){
				for (int i = blob.area.left; i < blob.area.right; i++){
					int c = i + j * w;
					if (c < 0 || c >= cur.length || !style.burning( cur[c] ) || !BurningTree.tree( level.map[c] )) continue;
					if (n == trees.length) trees = Arrays.copyOf( trees, n * 2 );
					trees[n++] = c;
				}
			}
			n = nearest( level, trees, n, CROWNS );
		}
		//the crowns kept: their trees still among the wanted (each taken off the list)
		for (Iterator<BurningTree> it = crowns.iterator(); it.hasNext(); ){
			BurningTree t = it.next();
			int c = t.cell(), k = 0;
			while (k < n && trees[k] != c) k++;
			if (c < 0 || k == n){
				t.out();
				it.remove();
				continue;
			}
			trees[k] = -1;
			t.burn( cur[c], level.heroFOV[c] );
		}
		//and the rest set alight
		for (int k = 0; k < n; k++){
			int c = trees[k];
			if (c < 0) continue;
			BurningTree t = BurningTree.light( level, c, cur[c], level.heroFOV[c] );
			if (t != null) crowns.add( t );
		}
	}

	//the `most` of the first n cells nearest the hero moved to the front (a selection, nothing
	//allocated); how many are wanted
	private static int nearest( Level level, int[] cells, int n, int most ){
		if (n <= most) return n;
		int hero = Dungeon.hero != null ? Dungeon.hero.pos : -1;
		if (hero < 0) return most;
		int w = level.width(), hx = hero % w, hy = hero / w;
		for (int k = 0; k < most; k++){
			int best = k, bd = Integer.MAX_VALUE;
			for (int m = k; m < n; m++){
				int d = Math.max( Math.abs( cells[m] % w - hx ), Math.abs( cells[m] / w - hy ) );
				if (d < bd){
					bd = d;
					best = m;
				}
			}
			int t = cells[k];
			cells[k] = cells[best];
			cells[best] = t;
		}
		return most;
	}
}
