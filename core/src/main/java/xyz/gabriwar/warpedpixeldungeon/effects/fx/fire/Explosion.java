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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * A blast, read off a burst of the old blast particles (BlastParticle.FACTORY, whose bursts every
 * blast already makes and every co-op guest replays) and sized by how many it asked for:
 * - under 10 (a gun's flash, a crumbling wall's puff): that many tongues and two sparks;
 * - 10 to 29: a flash core for two frames, a ring of heat 13 to 31 px, a pool of light, half as
 *   many tongues, debris, puffs, embers, a small scorch;
 * - 30 to 59 (a bomb): the core at full for 0.05 s then down by 0.12 s, a 47 px glow and a 63 px
 *   pool fading over 0.3 s, a ring 13 to 55 px easing out over 0.18 s, 16-20 tongues thrown
 *   through the upper 240 degrees, 8-12 bits of the level's floor, 10-16 puffs of warm smoke
 *   spreading and rising, 8-12 embers falling, a scorch that smoulders, the camera shaken
 *   (Fx.shake 2 within 8 cells, less the further);
 * - 60 and more (DM-300's): a bomb's with two great puffs, a large scorch, a second ring 0.08 s
 *   behind the first, a harder shake (3).
 * On water no scorch: a large ring, droplets and a hiss of steam. A third blast within 0.25 s
 * plays a class smaller. At most CAP particles; scaled up for a falling star's or a meteor's
 * (hint, meteor: a meteor's hint is sent on to co-op guests with the blast's burst).
 */
public class Explosion extends BurstWatch {

	public static final int GUNS = 0, SMALL = 1, BOMB = 2, BIG = 3;
	/** The burst sizes each class starts at. */
	public static final int SMALL_N = 10, BOMB_N = 30, BIG_N = 60;
	/** Particles of one blast at most. */
	public static final int CAP = 80;
	/** Blasts this close in time crowd each other: past CROWD, a class smaller. */
	public static final float CROWD_WINDOW = 0.25f;
	public static final int CROWD = 2;
	/** The heat ring's colour and the blast's light. */
	public static final int RING_COLOR = 0xFFD8A0, GLOW_COLOR = 0xFFE0A0;
	/** The shake within SHAKE_CELLS, by class (none for the small ones). */
	public static final float SHAKE_CELLS = 8f;
	/** The lights' levels: a small blast's pool, a bomb's glow and pool. */
	public static final float SMALL_POOL = 0.5f, BOMB_GLOW = 0.8f, BOMB_POOL = 0.7f;

	//the last blasts' times (game time), for the crowding
	private static final float[] recent = new float[4];
	private static int recentAt;
	/** A meteor's blast: its scale and its shake. */
	public static final float METEOR_SCALE = 1.2f, METEOR_SHAKE = 4f, METEOR_SHAKE_FOR = 0.6f;
	/** A hint is taken by a blast bursting at its point within this many s. */
	public static final float HINT_FOR = 1f;

	//a scale and a shake asked for the next blast burst at a point (a falling star's landing, a
	//meteor's), and when
	private static boolean hinted;
	private static float hintX, hintY, hintAt, hintScale = 1f, hintShake, hintShakeFor;

	private float scale = 1f, shake, shakeFor;

	/** The blast particle's burst, particle by particle (BlastParticle.FACTORY): the first opens an
	 *  explosion, the rest are counted. Render thread. */
	public static void burst( Emitter emitter, int index, float x, float y ){
		if (index == 0){
			Explosion ex = new Explosion();
			if (hinted && Math.abs( x - hintX ) < 1f && Math.abs( y - hintY ) < 1f && Game.timeTotal - hintAt <= HINT_FOR){
				ex.scale = hintScale;
				ex.shake = hintShake;
				ex.shakeFor = hintShakeFor;
				hinted = false;
			}
			open( ex, emitter, x, y );
		} else {
			count( Explosion.class, emitter, x, y );
		}
	}

	/** The next blast burst at (x, y) plays `scale` times as big (a falling star's 1.5, a meteor's
	 *  1.2; from 1.5 its scorch is a large one) and shakes the camera as told (Fx.shake, less the
	 *  further it is) instead of as its class does. Render thread, right before the burst. */
	public static void hint( float x, float y, float scale, float shake, float shakeFor ){
		hinted = true;
		hintX = x;
		hintY = y;
		hintAt = Game.timeTotal;
		hintScale = scale;
		hintShake = shake;
		hintShakeFor = shakeFor;
	}

	/** A meteor's blast about to burst on a cell's middle (CellEmitter.center): a fifth bigger,
	 *  shaking as the stone does; sent on to co-op guests ahead of the burst. Render thread. */
	public static void meteor( int cell ){
		FireNet.send( FireNet.METEOR, cell, 0, 0, null );
		meteorHint( cell );
	}

	static void meteorHint( int cell ){
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return;
		PointF p = DungeonTilemap.tileCenterToWorld( cell );
		hint( p.x, p.y, METEOR_SCALE, METEOR_SHAKE, METEOR_SHAKE_FOR );
	}

	/** The class a burst of n plays as. */
	public static int classOf( int n ){
		return n >= BIG_N ? BIG : n >= BOMB_N ? BOMB : n >= SMALL_N ? SMALL : GUNS;
	}

	/** A blast now (game time `now`) of class `cls`, crowded by the blasts just before it: a class
	 *  smaller once CROWD others went off within CROWD_WINDOW. Notes it down. */
	public static int crowd( int cls, float now ){
		int near = 0;
		for (float t : recent) if (t > 0 && now - t <= CROWD_WINDOW) near++;
		recent[recentAt] = now;
		recentAt = (recentAt + 1) % recent.length;
		return near >= CROWD && cls > GUNS ? cls - 1 : cls;
	}

	/** Whether a hint waits for its blast. */
	static boolean hinting(){
		return hinted;
	}

	/** Test only: no blast remembered. */
	public static void forget(){
		for (int i = 0; i < recent.length; i++) recent[i] = 0;
		hinted = false;
	}

	@Override
	protected void play( int n, float x, float y ){
		int cls = crowd( classOf( n ), Game.timeTotal + 1f );
		boolean water = WaterFX.onWater( x, y + 4 );
		switch (cls){
			case GUNS:
				guns( n, x, y, n >= 6 );
				break;
			case SMALL:
				small( n, x, y, water );
				break;
			default:
				bomb( x, y, water, cls == BIG );
		}
	}

	// ------------------------------------------------------------------ the classes

	//a gun's flash: its tongues and two sparks (the sparks gone when it is one of many)
	private void guns( int n, float x, float y, boolean bare ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		for (int i = 0; i < n; i++){
			tongue( glow, x, y, FxRandom.Float( (float)(-Math.PI * 5 / 6), (float)(-Math.PI / 6) ), FxRandom.Float( 10f, 30f ),
					Flames.M, FxBudget.P1 );
		}
		if (!bare) sparks( glow, x, y, 2 );
	}

	private void small( int n, float x, float y, boolean water ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		core( glow, x, y, 2 / 30f, 1f / 30f );
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y + 4, 13, 31, 0.18f, RING_COLOR, 0.6f, true );
		FxLight.pool( x, y + 4, 31, Element.FIRE.light, SMALL_POOL ).life( 0, 0.05f, 0.25f );
		int tongues = Math.max( 3, n / 2 );
		for (int i = 0; i < tongues; i++) tongue( glow, x, y, upper(), FxRandom.Float( 20f, 45f ), Flames.M, FxBudget.P1 );
		debris( x, y, 4 );
		puffs( x, y, 6, false );
		embers( glow, x, y, 4 );
		if (water) wet( x, y, WaterFX.M, 4, 2 );
		else FxDecal.at( x, y + 4, FxFrames.SCORCH_S, FxRecipes.SCORCH_COLOR, 0.6f, 3f, 3f, FxRandom.chance( 0.5f ) );
	}

	private void bomb( float x, float y, boolean water, boolean big ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		float k = scale;
		int tongues = Math.round( FxRandom.IntRange( 16, 20 ) * k ), debris = Math.round( FxRandom.IntRange( 8, 12 ) * k ),
				puffs = FxRandom.IntRange( 10, 16 ), embers = FxRandom.IntRange( 8, 12 ), smoulder = FxRandom.IntRange( 3, 5 );
		//never past the cap: the dressing gives way first
		int total = 1 + tongues + debris + puffs + embers + smoulder + (big ? 2 : 0);
		if (total > CAP){
			float cut = (CAP - 1 - tongues - (big ? 2 : 0)) / (float)(total - 1 - tongues - (big ? 2 : 0));
			debris = Math.round( debris * cut );
			puffs = Math.round( puffs * cut );
			embers = Math.round( embers * cut );
			smoulder = Math.round( smoulder * cut );
		}

		//the hit: the core at full for 0.05 s, gone by 0.12 s; the glow and the pool after it
		core( glow, x, y, 0.12f, 0.07f );
		FxLight.glow( x, y - 2, 47, GLOW_COLOR, BOMB_GLOW ).life( 0, 0.03f, 0.27f );
		FxLight.pool( x, y + 4, 63, Element.FIRE.light, BOMB_POOL ).life( 0, 0.05f, 0.25f );
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y + 4, 13, Math.round( 55 * Math.min( 1.3f, k ) ), 0.18f,
				RING_COLOR, 0.6f, true );
		if (big){
			FxRing r = FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y + 4, 9, 41, 0.3f, RING_COLOR, 0.45f, true );
			if (r != null) r.delay( 0.08f );
		}

		//the body: tongues thrown up and out, slowing and rising
		for (int i = 0; i < tongues; i++){
			tongue( glow, x, y, upper(), FxRandom.Float( 30f, 70f ), FxRandom.chance( 0.4f ) ? Flames.L : Flames.M, FxBudget.P1 );
		}
		debris( x, y, debris );
		puffs( x, y, puffs, big );
		embers( glow, x, y, embers );

		if (water){
			wet( x, y, WaterFX.L, 8, 4 );
		} else {
			FxDecal.at( x, y + 4, big || k >= 1.5f ? FxFrames.SCORCH_L : FxFrames.SCORCH_M, FxRecipes.SCORCH_COLOR, 0.6f, 4f, 4f,
					FxRandom.chance( 0.5f ) );
			FxEmitter sm = GameScene.lightEmitter();
			if (sm != null && smoulder > 0){
				sm.pos( x - 7, y, 14, 6 );
				sm.start( FxFactories.ember( Element.FIRE ), 0.6f, smoulder );
			}
		}
		if (shake > 0) shake( x, y, shake, shakeFor );
		else shake( x, y, big ? 3f : 2f, big ? 0.3f : 0.2f );
	}

	// ------------------------------------------------------------------ its parts

	//an angle in the upper 240 degrees (0 east, positive down)
	private static float upper(){
		return FxRandom.Float( (float)Math.toRadians( -210 ), (float)Math.toRadians( 30 ) );
	}

	private static void core( Emitter e, float x, float y, float life, float fade ){
		Flames.make( e, BlastParticle.class, x, y ).look( FxFrames.CORE_FLASH ).paint( 0xFFF6DC ).light( true )
				.life( life ).alpha( 1f, 0, fade ).snap().priority( FxBudget.P0 );
	}

	private static void tongue( Emitter e, float x, float y, float angle, float speed, int size, int priority ){
		if (!FxBudget.keep( priority, x, y )) return;
		Flames.tongue( Flames.make( e, BlastParticle.class, x, y ), Element.FIRE, size )
				.polar( angle, speed ).drag( 4f ).gravity( -30f ).life( FxRandom.Float( 0.35f, 0.6f ) ).priority( priority );
	}

	private static void sparks( Emitter e, float x, float y, int n ){
		for (int i = 0; i < n; i++) FxFactories.spark( Element.FIRE ).emit( e, i, x, y );
	}

	//bits of the floor thrown up, in the level's colours
	private static void debris( float x, float y, int n ){
		FxEmitter air = Fx.air();
		if (air == null || n <= 0) return;
		int c1 = Dungeon.level != null ? Dungeon.level.color1 : Element.EARTH.ramp.stop( 1 );
		int c2 = Dungeon.level != null ? Dungeon.level.color2 : Element.EARTH.ramp.stop( 3 );
		Emitter.Factory f = FxFactories.debris( c1, c2 );
		for (int i = 0; i < n; i++) f.emit( air, i, x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( -2f, 4f ) );
	}

	//warm smoke spreading out and rising, a puff of each size; two great ones over a big blast
	private static void puffs( float x, float y, int n, boolean big ){
		FxEmitter air = Fx.air();
		if (air == null) return;
		for (int i = 0; i < n; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) continue;
			SmokeParticle.puff( Flames.make( air, SmokeParticle.class, x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( -6f, 2f ) ),
					FxRandom.Float( -20f, 20f ), -FxRandom.Float( 10f, 16f ) );
		}
		if (big){
			for (int i = 0; i < 2; i++){
				float life = FxRandom.Float( 2f, 2.8f );
				Flames.make( air, SmokeParticle.class, x + FxRandom.Float( -4f, 4f ), y - 6 ).look( FxFrames.PUFF_XL[i % 2] )
						.ramp( SmokeParticle.RAMP, false ).smooth().life( life ).alpha( SmokeParticle.ALPHA, life * 0.15f, life * 0.5f )
						.vel( FxRandom.Float( -8f, 8f ), -FxRandom.Float( 6f, 10f ) ).drag( 0.8f ).gravity( -5f )
						.turbulence( 2f, 2.5f ).wind( 0.35f ).priority( FxBudget.P1 );
			}
		}
	}

	//embers thrown up that fall as they cool
	private static void embers( Emitter e, float x, float y, int n ){
		for (int i = 0; i < n; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) continue;
			Flames.make( e, BlastParticle.class, x, y ).look( FxRandom.chance( 0.7f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 )
					.ramp( Element.FIRE.ember, true ).life( FxRandom.Float( 0.2f, 1.2f ) ).alpha( 1f, 0, 0.3f )
					.polar( FxRandom.Float( (float)Math.toRadians( -150 ), (float)Math.toRadians( -30 ) ), FxRandom.Float( 30f, 60f ) )
					.gravity( 90f ).drag( 1f ).fate( FxFactories.EMBER_END ).priority( FxBudget.P2 );
		}
	}

	//on water: a ring, droplets thrown, a hiss of steam
	private static void wet( float x, float y, int ring, int droplets, int steam ){
		WaterFX.ring( x, y + 4, ring );
		WaterFX.droplets( x, y + 4, droplets, 40f, 80f, 120f, WaterFX.liquid().foam );
		FxRecipes.steam( x, y, steam );
	}

	//the camera shaken by a blast within SHAKE_CELLS of the hero, the less the further
	private static void shake( float x, float y, float magnitude, float duration ){
		if (Dungeon.hero == null || Dungeon.level == null || Dungeon.hero.pos < 0) return;
		int w = Dungeon.level.width();
		float hx = (Dungeon.hero.pos % w + 0.5f) * DungeonTilemap.SIZE, hy = (Dungeon.hero.pos / w + 0.5f) * DungeonTilemap.SIZE;
		float d = (float)Math.sqrt( (x - hx) * (x - hx) + (y - hy) * (y - hy) ) / DungeonTilemap.SIZE;
		float k = 1f - d / SHAKE_CELLS;
		if (k > 0) Fx.shake( magnitude * k, duration );
	}
}
