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
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.particles.Emitter;

/**
 * A burning blob's particles (Fire and its kin, the coal beds, the fire wind), every cell of it
 * built in layers from its own state - so a co-op guest, who has the blob, sees the same:
 * - a small tongue or a lick every 0.05 s (on water, licks only), leaning with 0.15 of the wind;
 * - rooted tongues by how much burns there, each taking one small flame's place: from 8 a tall or
 *   middling one every 0.14 s (30/70), from 5 a middling one every 0.16 s, from 2 every 0.3 s (the
 *   most fires ever hold: a grass fire's cells are 4 and less), none in a cell's last turn;
 * - an ember every 0.6 s (on water a wisp of steam every 0.5 s instead);
 * - smoke (for the fires that smoke) every 1.4 s off a cell at its top edge (one in two at 0.7 s),
 *   a fifth as often inside it, growing from a small puff to a middling one as it rises, among
 *   the gases;
 * About 9 live particles on a grass fire's cell (cur 4), 12 at cur 6, 13 at the thickest
 * (fx-previews/fire-heat: the numbers measured), a third of that in its last turn.
 * - motes for the clean fires (magic's sparkles, halomethane's cold motes);
 * - a camp fire's coals: a gentle smoke, a puff of the chimneys' grey every CAMP_SMOKE_EVERY s off
 *   each cell, leaning with the wind over the fog (CampSmoke).
 * Its FireField adds the floor's glow, the light, the shimmer, the washes and the cells' ends.
 *
 * A blob's use() makes one with pour() (one of the game's random numbers, as its old pour drew)
 * or, for a coal bed that drew none, coals() (none). Everything else is FxRandom.
 */
public class FireBlobFx extends Emitter.Factory implements FxBudget.Prioritized {

	/** How often each burning cell is visited, s (a coal bed's cells). */
	public static final float INTERVAL = 0.05f, COAL_INTERVAL = 0.12f;
	/** Where in its cell a flame takes root (BlobEmitter.bound): its lower two thirds. */
	public static final float[] BOUND = { 0.05f, 0.35f, 0.95f, 1f };
	/** The rooted tongues: the volumes they start at and one every this many s. */
	public static final int TALL_CUR = 8, MID_CUR = 5, LOW_CUR = 2;
	public static final float TALL_EVERY = 0.14f, MID_EVERY = 0.16f, LOW_EVERY = 0.3f, TALL_SHARE = 0.3f;
	/** Embers, smoke (and its shares on the top edge and inside the fire), steam on water, a coal's
	 *  spark, its ember. */
	public static final float EMBER_EVERY = 0.6f, SMOKE_EVERY = 0.7f, SMOKE_TOP = 0.5f, SMOKE_INNER = 0.2f, STEAM_EVERY = 0.5f,
			COAL_SPARK_EVERY = 1.5f, COAL_EMBER_EVERY = 1.2f;
	/** The smoke's peak alpha. */
	public static final float SMOKE_ALPHA = 0.45f;
	/** An inferno's embers circle its middle this fast, px/s. */
	public static final float VORTEX_MIN = 20f, VORTEX_MAX = 30f;
	/** A fire wind: its streaks, its ash, its embers, each one every this many s; its drift's least. */
	public static final float WIND_STREAK_EVERY = 0.12f, WIND_ASH_EVERY = 0.6f, WIND_EMBER_EVERY = 0.5f, WIND_DRIFT = 8f;
	/** A camp fire's smoke: a puff off each of its burning cells every this many s. */
	public static final float CAMP_SMOKE_EVERY = 0.5f;

	/** The column a camp fire's smoke rises in: the chimneys' (ChimneySmokeParticle.Column), a kind of
	 *  its own, never pouring by itself - its coals put each puff in while the hero sees them. */
	public static final class CampSmoke extends ChimneySmokeParticle.Column {}

	//a puff's sizes, one frame each (a matter frame grows by stepping to a bigger one)
	private static final int[][][] PUFF_M = { { FxFrames.PUFF_M[0] }, { FxFrames.PUFF_M[1] } };

	final Blob blob;
	final BlobEmitter emitter;
	final FireStyle style;
	private final float interval;
	private FireField field;
	//a camp fire's smoke column over the fog, made with its first puff
	private CampSmoke campSmoke;

	FireBlobFx( Blob blob, BlobEmitter emitter, FireStyle style, float interval ){
		this.blob = blob;
		this.emitter = emitter;
		this.style = style;
		this.interval = interval;
	}

	/** A burning blob's look poured on its emitter (one of the game's random numbers, as ever). */
	public static FireBlobFx pour( Blob blob, BlobEmitter emitter, FireStyle style ){
		FireBlobFx f = new FireBlobFx( blob, emitter, style, INTERVAL );
		emitter.bound.set( BOUND[0], BOUND[1], BOUND[2], BOUND[3] );
		emitter.pour( f, INTERVAL );
		return f;
	}

	/** A coal bed's look started on its emitter without a random delay: it draws none of the
	 *  game's random numbers, as the bed never did. */
	public static FireBlobFx coals( Blob blob, BlobEmitter emitter, FireStyle style ){
		FireBlobFx f = new FireBlobFx( blob, emitter, style, COAL_INTERVAL );
		emitter.bound.set( 0.15f, 0.4f, 0.85f, 0.95f );
		emitter.startDelayed( f, COAL_INTERVAL, 0, 0f );
		return f;
	}

	/**
	 * A blob that pours a particle factory of its own (the sacrificial fire's) given a fire's
	 * field all the same: its floor's glow and its light, made the first time it emits. The
	 * factory is the one poured; this only watches.
	 */
	public static Emitter.Factory withField( final Blob blob, final BlobEmitter emitter, final FireStyle style, final Emitter.Factory particles ){
		return new Emitter.Factory(){
			private FireField field;

			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if ((field == null || !field.alive) && e == emitter) field = FireField.attach( blob, emitter, style );
				particles.emit( e, index, x, y );
			}

			@Override
			public boolean lightMode(){
				return particles.lightMode();
			}
		};
	}

	/** Its field, once its blob burnt where the hero sees (null before, or off the scene). */
	public FireField field(){
		return field;
	}

	@Override
	public int priority(){
		//every cell is visited; each layer below asks the budget for itself
		return FxBudget.P0;
	}

	@Override
	public float density( int cur ){
		return 1f;
	}

	@Override
	public boolean lightMode(){
		return true;
	}

	@Override
	public void emit( Emitter e, int index, float x, float y ){
		BlobEmitter.Cell c = BlobEmitter.cell();
		if (!style.burning( c.cur )) return;
		if ((field == null || !field.alive) && e == emitter) field = FireField.attach( blob, emitter, style );
		Level level = Dungeon.level;
		boolean water = level != null && c.cell >= 0 && c.cell < level.length() && level.water[c.cell];
		switch (style.kind){
			case COAL:
				coal( e, x, y );
				break;
			case WIND:
				wind( e, x, y );
				break;
			default:
				open( e, index, x, y, c, water, level );
		}
	}

	private void open( Emitter e, int index, float x, float y, BlobEmitter.Cell c, boolean water, Level level ){
		float k = style.rate;
		int n = (int)k + (FxRandom.Float() < k - (int)k ? 1 : 0);
		//the rooted tongue, by how much burns there: it takes one small flame's place
		if (!water){
			float every = c.cur >= TALL_CUR ? TALL_EVERY : (c.cur >= MID_CUR ? MID_EVERY : (c.cur >= LOW_CUR ? LOW_EVERY : 0));
			if (every > 0 && FxRandom.Float() < interval / every * k){
				if (FxBudget.keep( FxBudget.P1, x, y )){
					int size = c.cur >= TALL_CUR && FxRandom.chance( TALL_SHARE ) ? Flames.L : Flames.M;
					Flames.tongue( make( e, x, y ), style.flame, size );
				}
				n--;
			}
		}
		//the small flames, a lick or a small tongue each visit (more than one for a faster fire)
		for (int i = 0; i < n; i++){
			if (water || ((index + i) & 1) == 1){
				if (FxBudget.keep( FxBudget.P2, x, y )) Flames.lick( make( e, x, y ), style.flame );
			} else if (FxBudget.keep( FxBudget.P1, x, y )){
				Flames.tongue( make( e, x, y ), style.flame, Flames.S );
			}
		}
		Emitter matter = emitter.matter();
		//embers, or on water the steam they become
		if (water){
			if (FxRandom.Float() < interval / STEAM_EVERY && FxBudget.keep( FxBudget.P2, x, y )){
				FxFactories.steam().emit( matter, index, x, y - 4 );
			}
		} else if (FxRandom.Float() < interval / EMBER_EVERY * k && FxBudget.keep( FxBudget.P2, x, y )){
			FxParticle p = Flames.ember( make( e, x, y - 3 ), style.ember );
			if (style.kind == FireStyle.Kind.INFERNO && field != null && field.seen() > 1) vortex( p, x, y );
		}
		//smoke off its top edge, thinner inside
		if (style.smoke != null && !water && level != null){
			int w = level.width();
			boolean top = c.cell - w < 0 || blob.cur == null || !style.burning( blob.cur[c.cell - w] );
			float p = interval / SMOKE_EVERY * (top ? SMOKE_TOP : SMOKE_INNER) * k;
			if (FxRandom.Float() < p && FxBudget.keep( FxBudget.P2, x, y )){
				smoke( make( matter, x, y - 6 ), style.smoke, SMOKE_ALPHA );
			}
		}
		//the clean fires' motes
		if (style.motes != -1 && FxRandom.Float() < interval / style.moteEvery && FxBudget.keep( FxBudget.P2, x, y )){
			FxParticle m = make( e, x + FxRandom.Float( -3f, 3f ), y - FxRandom.Float( 2f, 8f ) );
			if (style == FireStyle.MAGIC){
				m.look( FxFrames.PLUS_3 ).life( FxRandom.Float( 0.5f, 0.9f ) ).vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 10f, 18f ) );
			} else {
				m.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).life( FxRandom.Float( 0.8f, 1.4f ) )
						.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 6f, 10f ) );
			}
			m.paint( style.motes ).light( true ).alpha( 0.9f, 0.1f, 0.3f ).drag( 1f ).snap().priority( FxBudget.P2 );
		}
	}

	//an inferno's ember: thrown round the fire's middle on the ground's 2:1 ellipse, still rising
	private void vortex( FxParticle p, float x, float y ){
		float dx = x - field.middleX(), dy = (y - field.middleY()) * 2f;
		float len = Math.max( 4f, (float)Math.sqrt( dx * dx + dy * dy ) );
		float speed = FxRandom.Float( VORTEX_MIN, VORTEX_MAX );
		p.vel( -dy / len * speed, dx / len * speed * 0.5f - 6f ).drag( 0.4f );
	}

	//a bed of coals: a lick or a small tongue each visit, a spark popping now and then, an ember
	private void coal( Emitter e, float x, float y ){
		if (FxRandom.chance( 0.5f )){
			if (FxBudget.keep( FxBudget.P1, x, y )) Flames.tongue( make( e, x, y ), style.flame, Flames.S );
		} else if (FxBudget.keep( FxBudget.P2, x, y )){
			Flames.lick( make( e, x, y ), style.flame );
		}
		if (FxRandom.Float() < interval / COAL_SPARK_EVERY) FxFactories.spark( Element.FIRE ).emit( e, 0, x, y - 2 );
		if (FxRandom.Float() < interval / COAL_EMBER_EVERY && FxBudget.keep( FxBudget.P2, x, y )){
			Flames.ember( make( e, x, y - 2 ), style.ember );
		}
		//a camp fire smokes a little
		if (style == FireStyle.CAMP && FxRandom.Float() < interval / CAMP_SMOKE_EVERY && FxBudget.keep( FxBudget.P2, x, y )){
			Emitter smoke = campSmoke();
			if (smoke != null) ChimneySmokeParticle.FACTORY.emit( smoke, 0, x, y - 4 );
		}
	}

	//its smoke column: held over the fog, ungated (a puff is only made where the hero sees its cell)
	//and never pouring nor dying by itself
	private Emitter campSmoke(){
		if (campSmoke == null || !campSmoke.exists || campSmoke.parent == null){
			campSmoke = GameScene.overFogEmitter( CampSmoke.class );
			if (campSmoke == null) return null;
			campSmoke.gated = campSmoke.soot = campSmoke.steam = false;
			campSmoke.on = false;
			campSmoke.autoKill = false;
		}
		return campSmoke;
	}

	//a wind of fire: streaks of flame drifting with the wind (or on, where there is none), ash
	//tumbling, embers
	private void wind( Emitter e, float x, float y ){
		float wx = 1.2f * Fx.windX();
		float drift = Math.abs( wx ) < WIND_DRIFT ? (wx < 0 ? -WIND_DRIFT : WIND_DRIFT) : wx;
		if (FxRandom.Float() < interval / WIND_STREAK_EVERY && FxBudget.keep( FxBudget.P1, x, y )){
			float life = FxRandom.Float( 0.3f, 0.5f );
			make( e, x, y ).look( FxFrames.STREAK_FLAME, 12 + FxRandom.Float( -2f, 2f ) ).ramp( Flames.ramp( style.flame ), true )
					.life( life ).alpha( 1f, 0, life * 0.4f ).vel( drift + FxRandom.Float( -4f, 4f ), -FxRandom.Float( 4f, 10f ) )
					.gravity( -12f ).priority( FxBudget.P1 );
		}
		if (FxRandom.Float() < interval / WIND_ASH_EVERY && FxBudget.keep( FxBudget.P3, x, y )){
			float life = FxRandom.Float( 1.5f, 3f );
			make( emitter.matter(), x, y ).look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_2 : FxFrames.PEBBLE_2 ).paint( 0x5A5258 )
					.life( life ).alpha( 0.6f, 0.1f, 0.5f ).vel( drift * 0.6f + FxRandom.Float( -3f, 3f ), -FxRandom.Float( 0f, 6f ) )
					.gravity( 3f ).sway( 5f, 3f ).spin( 0.3f ).priority( FxBudget.P3 );
		}
		if (FxRandom.Float() < interval / WIND_EMBER_EVERY && FxBudget.keep( FxBudget.P2, x, y )){
			Flames.ember( make( e, x, y ), style.ember ).vel( drift * 0.8f, -FxRandom.Float( 6f, 12f ) );
		}
	}

	static FxParticle make( Emitter e, float x, float y ){
		return Flames.make( e, FxParticle.class, x, y );
	}

	/** Sets `p` up as a puff of a fire's smoke: small, growing to middling halfway, stepping down
	 *  its ramp, fading in and out to `alpha` at most, rising 9-12 px/s and slowing 6% a second,
	 *  wobbling, 0.35 of the wind, 1.6-2.4 s. Matter, P2. */
	public static FxParticle smoke( FxParticle p, Ramp ramp, float alpha ){
		float life = FxRandom.Float( 1.6f, 2.4f );
		int v = FxRandom.Int( 2 );
		return p.look( FxFrames.PUFF_S[v] ).then( 0.5f, PUFF_M[v] ).ramp( ramp, false ).smooth().life( life )
				.alpha( alpha, life * 0.3f, life * 0.45f ).vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 9f, 12f ) ).drag( 0.06f )
				.turbulence( FxRandom.Float( 1f, 2f ), FxRandom.Float( 2f, 4f ) ).wind( 0.35f ).priority( FxBudget.P2 );
	}
}
