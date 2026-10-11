/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.PrismFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.RayFx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

//a ray: its core this image, drawn over the night's tint (carried to the light layer), and round
//it what effects/fx/arcane/RayFx builds on its first frame (a halo, its lights, its kind's own).
//The core strikes in over 0.04 s, holds shimmering, then thins in whole steps (8, 6, 4, 2 px)
//as it fades; its halo dies AFTERGLOW s after it. Nothing waits on a beam: its callers carry on.
public class Beam extends Image implements Lit {

	private static final double A = 180 / Math.PI;

	public float duration;

	public float timeLeft;

	//where it runs, how long it has, what it is
	private final float sx, sy, ex, ey;
	private final float fullScaleX;
	private final int kind;
	private boolean lite, started, done;
	private float age;
	private Gizmo carrier;
	private final RayFx fx = new RayFx();

	/** A beam's strike: its core 2 px, then 4, then whole, over this long. */
	public static final float ATTACK = 0.04f;
	/** The share of its life it holds whole before it thins. */
	public static final float HOLD = 0.5f;

	private Beam(PointF s, PointF e, Effects.Type asset, float duration, int kind) {
		super( Effects.get( asset ) );

		origin.set( 0, height / 2 );

		x = s.x - origin.x;
		y = s.y - origin.y;

		float dx = e.x - s.x;
		float dy = e.y - s.y;
		angle = (float)(Math.atan2( dy, dx ) * A);
		scale.x = (float)Math.sqrt( dx * dx + dy * dy ) / width;
		fullScaleX = scale.x;

		timeLeft = this.duration = duration;
		sx = s.x;
		sy = s.y;
		ex = e.x;
		ey = e.y;
		this.kind = kind;
	}

	/** Core and halo only (no lights, no bits): a sentry's, a ward's, an eye's, many at once. */
	public Beam lite(){
		lite = true;
		return this;
	}

	public static class DeathRay extends Beam{
		public DeathRay(PointF s, PointF e){
			super(s, e, Effects.Type.DEATH_RAY, 0.5f, RayFx.DEATH);
		}
	}

	public static class LightRay extends Beam{
		public LightRay(PointF s, PointF e){
			super(s, e, Effects.Type.LIGHT_RAY, 1f, RayFx.LIGHT);
		}
	}

	public static class SunRay extends Beam{
		public SunRay(PointF s, PointF e){
			super(s, e, Effects.Type.LIGHT_RAY, 1f, RayFx.SUN);
			tint(1f, 0xF0 / 255f, 0xA0 / 255f, 1);
		}
	}

	public static class HealthRay extends Beam{
		public HealthRay(PointF s, PointF e){
			super(s, e, Effects.Type.HEALTH_RAY, 0.75f, RayFx.HEALTH);
		}
	}

	//its colour for what it builds round it: its tint's (a crystal wisp's ray takes its blood),
	//else its kind's
	private int color(){
		if (ra + ga + ba > 0.01f){
			return (Math.round( Math.min( 1f, ra ) * 255 ) << 16) | (Math.round( Math.min( 1f, ga ) * 255 ) << 8)
					| Math.round( Math.min( 1f, ba ) * 255 );
		}
		return RayFx.colorOf( kind );
	}

	/** Its first frame, on the render thread: carried over the tint, its surroundings built. */
	protected void begin(){
		carrier = GameScene.carryLight( this );
		fx.build( kind, sx, sy, ex, ey, duration, color(), lite );
	}

	/** How much of its life is left, 1..0. */
	protected float left(){
		return Math.max( 0f, timeLeft / duration );
	}

	/** Its core's thickness (a share of its rows: 1, 0.75, 0.5, 0.25) at `age` s, `left` of its life to go. */
	public static float thickness( float age, float left ){
		if (age < ATTACK * 0.5f) return 0.25f;
		if (age < ATTACK) return 0.5f;
		if (left > HOLD) return 1f;
		float q = left / HOLD;
		return q > 0.75f ? 1f : (q > 0.5f ? 0.75f : (q > 0.25f ? 0.5f : 0.25f));
	}

	/** Its core's alpha, `left` of its life to go: whole while it holds, then down to 0.4 as it thins. */
	public static float coreAlpha( float left ){
		if (left > HOLD) return 1f;
		return 0.4f + 0.6f * (left / HOLD);
	}

	/** Seconds its halo outlives its core. */
	protected float afterglow(){
		return RayFx.AFTERGLOW;
	}

	@Override
	public void update() {
		if (done) return;
		if (!started) {
			started = true;
			begin();
		}
		super.update();

		float dt = Game.elapsed;
		age += dt;
		timeLeft -= dt;
		float l = left();
		float core = 0;
		if (timeLeft > 0) {
			core = shape( age, l );
		} else {
			visible = false;
		}
		float after = timeLeft > 0 ? 0f : Math.min( 1f, -timeLeft / afterglow() );
		surround( age, dt, core, after );

		if (timeLeft <= -afterglow()) finish();
	}

	/** Its core this frame (`left` of its life to go); its alpha. */
	protected float shape( float age, float left ){
		float shimmer = 1f - 0.06f * (0.5f + 0.5f * (float)Math.sin( 2 * Math.PI * RayFx.SHIMMER_HZ * age ));
		float a = coreAlpha( left ) * shimmer;
		alpha( a );
		scale.y = thickness( age, left );
		return a;
	}

	/** What is round it, this frame. */
	protected void surround( float age, float dt, float core, float after ){
		float reach = fx.update( age, dt, core, after );
		scale.x = fullScaleX * reach;
	}

	/** Done: everything let go of, its own picture too. */
	protected void finish(){
		done = true;
		fx.dispose();
		if (carrier != null) carrier.killAndErase();
		Fx.dispose( this );
	}

	@Override
	public void draw() {
		if (carrier != null && carrier.exists && carrier.parent != null) return;
		drawLit();
	}

	@Override
	public void drawLit() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	public static class SuperNovaRay extends Beam {
		float multi;
		public SuperNovaRay(PointF s, PointF e, float multi){
			super(s, e, Effects.Type.SUPERNOVA_RAY, 2f, RayFx.SUPERNOVA);
			this.multi = multi;
		}

		@Override
		protected float left() {
			return Math.max( 0f, Math.min( 1f, timeLeft * multi / duration ) );
		}

		//as thick as its weight (x2, x3 for the heavier shots), thinning in whole rows
		@Override
		protected float shape( float age, float left ) {
			float a = super.shape( age, left );
			scale.y = rowsThick( thickness( age, left ), multi, height );
			return a;
		}
	}

	/** A core's scale.y at `thickness` of its rows times `multi`, kept to whole rows of `height`. */
	public static float rowsThick( float thickness, float multi, float height ){
		return Math.max( 1f, Math.round( thickness * multi * height ) ) / height;
	}

	/**
	 * The prismatic laser: a white core of the light sheet's bands, 1, 3 then 5 px wide and back,
	 * with the prism's fringes, packets, stars, ghosts, glints and pools round it (PrismFx).
	 */
	public static class PrismRay extends Beam {

		private final PrismFx prism = new PrismFx();
		private final RectF uv = new RectF();
		private int coreW = -1;
		private final float ps, pq, pe, pr;

		public PrismRay(PointF s, PointF e){
			super(s, e, Effects.Type.LIGHT_RAY, PrismFx.CORE_END, RayFx.LIGHT);
			texture( Assets.Effects.FX_LIGHT );
			ps = s.x;
			pq = s.y;
			pe = e.x;
			pr = e.y;
			band( 1 );
		}

		//the core at a width (1, 3, 5 px): its band of the light sheet, stretched from the wand
		private void band( int w ){
			if (w == coreW) return;
			coreW = w;
			int[] f = w >= 5 ? FxFrames.Light.CORE_BAND_5 : (w >= 3 ? FxFrames.Light.CORE_BAND_3 : FxFrames.Light.CORE_BAND_1);
			float tw = texture.width, th = texture.height;
			uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			frame( uv );
			origin.set( 0, height / 2f );
			x = ps;
			y = pq - height / 2f;
			float dx = pe - ps, dy = pr - pq;
			scale.x = (float)Math.sqrt( dx * dx + dy * dy ) / width;
			scale.y = 1;
		}

		@Override
		protected void begin() {
			carryAlone();
			prism.build( ps, pq, pe, pr );
		}

		@Override
		protected float afterglow() {
			return PrismFx.LIFE - PrismFx.CORE_END;
		}

		@Override
		protected float shape( float age, float left ) {
			band( PrismFx.coreWidth( age ) );
			float a = PrismFx.coreAlpha( age );
			alpha( a );
			return a;
		}

		@Override
		protected void surround( float age, float dt, float core, float after ) {
			prism.update( age, dt );
		}

		@Override
		protected void finish() {
			prism.dispose();
			super.finish();
		}
	}

	/** Carried over the night's tint, with nothing built round it (the prism builds its own). */
	protected void carryAlone(){
		carrier = GameScene.carryLight( this );
	}
}
