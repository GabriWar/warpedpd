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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The two-ended pull (docs/fx/skills.md, LinkFX): a bond drawn OUT OF BOTH BODIES. Motes peel off
 * each end and run along the sag (SkillMarkFX.sag / catenary) to the middle, where they feed a knot
 * of light that swells and snaps on the bite.
 *
 * - bind: a bond thrown between two points that bites once (an ethereal chain's replacement):
 *   added to its parent on the caller's thread, as the chain was; its bite is called from its
 *   update on the frame `spent > duration` (duration = DreadFX.tetherTime: distance / 320 + 0.2 s,
 *   counted as Chains counts it); `bite` may be null. From each end motesASide(len) motes
 *   (clamp(len / MOTE_PX, 3, 12): a link between neighbours, ~16 px, gets 3 a side, a 70 px one 12),
 *   born within 2 px of the anchor evenly over the gather, each flying to the knot along the
 *   catenary's chord in flight(duration) s, growing MOTE_1, MOTE_2, SQ_3, deep to the body (never
 *   the ramp's flash); matter peeling off each body every PEEL_EVERY s past PEEL_FROM px; the knot
 *   (STAR4_5, STAR4_7 past half fed) over a GLOW_11 rising KNOT_LOW to KNOT_HIGH as it is fed. The
 *   bite: one frame of IMPACT_STAR (P0), two motes racing home to both ends in RACE s (P1), at each
 *   end END_MOTES motes flicked 4 to 6 px (P2) where it takes hold, a thread of threadBits(len)
 *   bits along the sag gone over THREAD_LIFE; gone AFTER s after the bite. Crisp ends: no flare.
 * - held: a bond that lasts (Shadow Link's linked pair): HELD_LINKS SPARK_3 links on the catenary
 *   following both sprites, swaying a pixel and breathing, and every HELD_EVERY s a mote peeled off
 *   each body pulled to the middle in HELD_FLIGHT s, a glint of the accent where a pair meets
 *   (SHADOW: a SPARK_3 cut across the thread, never the heal's plus; STEEL: a PLUS_3 glint). Made
 *   while CAP bonds live, it waits and tries again every HELD_RETRY s, so a pair linked in a
 *   crowded moment is bound once there is room. The caller holds it and lets it go with Fx.dispose.
 * - drain: one way, a mote every DRAIN_EVERY s for `seconds` from one body to the other in
 *   DRAIN_FLIGHT s, lerping the look's start colour to its end; the look's landing on the last.
 *
 * Looks: SHADOW (the game's ShadowParticle violet-black: light motes 3A2A55 to 7A48B0 to A070E0 one
 * in two, ink motes 2A1F3A (matter) the other; knot A070E0 over 7A48B0; ink peeling and a thread of
 * ink sinking; the accent FF5050 of a shared cut; never paler than A070E0 but the bite's one frame),
 * STEEL (7FA9C9 to B9DEEE, a pale steel knot over B9DEEE, sparks scraping off and a thread of sparks),
 * SOUL_HEAL (a drain: the soul's B4B4EC turning the heal's 58E08C, landing in red crosses and greens).
 *
 * Caps: CAP binds, helds and drains live at once (counted; past it a bind still bites on its frame
 * and draws its thread only, a drain shows nothing, a held waits for room); a bind at most 2 x MOTES_MAX motes +
 * 2 knot quads + 1 light + THREAD_MAX thread bits + 2 x END_MOTES end motes; a held HELD_LINKS links +
 * 4 motes + 1 glint; a drain DRAIN_MOTES motes. Lights at most KNOT_HIGH. Draws none of the game's
 * Random; no GL until its first update (render thread); no allocation per frame.
 */
public final class LinkFX {

	private LinkFX(){}

	public static final int SHADOW = 0, STEEL = 1, SOUL_HEAL = 2;
	public static final int LOOKS = 3;

	/** Bonds (binds, helds, drains) live at once. */
	public static final int CAP = 6;
	/** Motes a side: one per MOTE_PX px of length, MOTES_MIN to MOTES_MAX. */
	public static final int MOTES_MIN = 3, MOTES_MAX = 12, MOTE_PX = 6;
	/** A mote's flight to the knot at most (s), and its share of a short bond's time. */
	public static final float FLIGHT_MAX = 0.3f, FLIGHT_SHARE = 0.6f;
	/** Matter peeling off the bodies: how often (s), from how long a bond (px). */
	public static final float PEEL_EVERY = 0.06f, PEEL_FROM = 32f;
	/** The knot's light as it is fed. */
	public static final float KNOT_LOW = 0.3f, KNOT_HIGH = 0.6f;
	/** The bite: the motes racing home (s), those flicked at each end, the thread's bits at most and
	 *  their life (s), how long the bond lasts after it. */
	public static final float RACE = 0.08f, END_LIFE = 0.2f, THREAD_LIFE = 0.4f, AFTER = 0.5f;
	public static final int END_MOTES = 3, THREAD_MAX = 12;
	public static final float STAR_LIFE = 0.04f;
	/** held: its links, its sway (px, Hz), its alpha and breath, a mote every HELD_EVERY s taking
	 *  HELD_FLIGHT s, the glint where a pair meets (s). */
	public static final int HELD_LINKS = 12;
	public static final float HELD_SWAY = 1f, HELD_HZ = 0.5f, HELD_ALPHA = 0.7f, HELD_BREATH = 0.12f,
			HELD_EVERY = 0.25f, HELD_FLIGHT = 0.35f, HELD_GLINT = 0.25f, HELD_RETRY = 0.5f;
	/** drain: a mote every DRAIN_EVERY s, each flying DRAIN_FLIGHT s; at most DRAIN_MOTES alive. */
	public static final float DRAIN_EVERY = 0.03f, DRAIN_FLIGHT = 0.3f;
	public static final int DRAIN_MOTES = 10;

	//by look: the motes' ramp (deep to body; a drain's start to end), the knot's colour, its light,
	//the accent, the bite's flash
	static final Ramp[] MOTES = {
			new Ramp( 0x3A2A55, 0x7A48B0, 0xA070E0 ),
			new Ramp( 0x7FA9C9, 0xB9DEEE ),
			new Ramp( SkillPalette.SOUL_LIGHT, SkillPalette.HEAL_LIGHT ) };
	static final int[] KNOT = { 0xA070E0, 0xE8F0FF, SkillPalette.HEAL_LIGHT };
	static final int[] GLOW = { SkillPalette.SHADOW_LIGHT, SkillPalette.STEEL_LIGHT, SkillPalette.HEAL_LIGHT };
	static final int[] ACCENT = { SkillPalette.BLOOD_LIGHT, 0xE8F0FF, SkillPalette.HEAL_CROSS };
	static final int[] FLASH = { SkillPalette.SHADOW_FLASH, 0xE8F0FF, SkillPalette.HEAL.stop( 0 ) };
	//a mote grows as it arrives
	static final int[][] GROW = { FxFrames.MOTE_1, FxFrames.MOTE_2, FxFrames.SQ_3 };
	static final int[][] GROW_2 = { FxFrames.MOTE_1, FxFrames.MOTE_2 };

	private static int live;

	/** Bonds alive now. */
	public static synchronized int live(){
		return live;
	}

	/** A new scene: none of the last one's counted. */
	public static synchronized void reset(){
		live = 0;
	}

	private static synchronized boolean enter(){
		if (live >= CAP) return false;
		live++;
		return true;
	}

	private static synchronized void leave(){
		live = Math.max( 0, live - 1 );
	}

	/** Motes a side for a bond this long, px. */
	public static int motesASide( float len ){
		return Math.max( MOTES_MIN, Math.min( MOTES_MAX, Math.round( len / MOTE_PX ) ) );
	}

	/** Bits of the thread laid at the bite for a bond this long. */
	public static int threadBits( float len ){
		return Math.max( 1, Math.min( THREAD_MAX, (int) (len / MOTE_PX) ) );
	}

	/** A mote's flight to the knot for a bond biting after `duration` s. */
	public static float flight( float duration ){
		return Math.min( FLIGHT_MAX, FLIGHT_SHARE * duration );
	}

	// ------------------------------------------------------------------ bind

	/** A bond from a to b in `look`, added to `parent` here (the group the chain it replaces was
	 *  added to), biting after DreadFX.tetherTime(a, b) s (`bite` may be null). */
	public static Gizmo bind( Group parent, PointF a, PointF b, int look, Callback bite ){
		Bind g = new Bind( a, b, Math.max( 0, Math.min( LOOKS - 1, look ) ), bite );
		parent.add( g );
		return g;
	}

	static final class Bind extends Gizmo {
		private final float ax, ay, bx, by, len, sag, duration, flight;
		private final int look, n, threadN;
		private final Callback bite;
		private final boolean shown;
		private boolean counted, bitten, begun, seen;
		private float spent, after, peelIn;
		private int laid;
		private FxPic knot;
		private FxLight glow;
		private int glowSetups;

		Bind( PointF a, PointF b, int look, Callback bite ){
			ax = a.x;
			ay = a.y;
			bx = b.x;
			by = b.y;
			this.look = look;
			this.bite = bite;
			duration = DreadFX.tetherTime( a, b );
			len = (float) Math.hypot( bx - ax, by - ay );
			sag = SkillMarkFX.sag( len );
			flight = flight( duration );
			n = motesASide( len );
			threadN = threadBits( len );
			shown = enter();
			counted = shown;
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			//the bite first, on the chain's frame and its count (Chains.update)
			if (!bitten && (spent += dt) > duration){
				bitten = true;
				if (bite != null) bite.call();
				if (seen) bit();
				let();
			}
			if (!begun){
				begun = true;
				seen = Fx.seen( ax, ay ) || Fx.seen( bx, by );
			}
			if (bitten){
				if ((after += dt) >= AFTER) finish();
				return;
			}
			if (!seen || !shown) return;
			//the gather: a mote from each end at its time
			float gather = Math.max( 0f, duration - flight );
			while (laid < n && spent >= gather * laid / Math.max( 1, n - 1 )){
				mote( ax, ay, laid );
				mote( bx, by, laid );
				laid++;
			}
			//matter off the bodies (a long bond's)
			if (len > PEEL_FROM && (peelIn -= dt) <= 0){
				peelIn = PEEL_EVERY;
				peel( ax, ay );
				peel( bx, by );
			}
			knot();
		}

		private float knotX(){
			return (ax + bx) / 2f;
		}

		private float knotY(){
			return (ay + by) / 2f + sag;
		}

		//one mote from an end to the knot, along the chord of the sag
		private void mote( float x0, float y0, int i ){
			float x = x0 + FxRandom.Float( -2f, 2f ), y = y0 + FxRandom.Float( -2f, 2f );
			float vx = (knotX() - x) / flight, vy = (knotY() - y) / flight;
			if (look == SHADOW && (i & 1) == 1){
				//ink: matter, under the night's tint
				FxParticle p = SkillVfx.matter( x, y, FxBudget.P2 );
				if (p != null) p.look( GROW_2, 0 ).paint( SkillPalette.SHADOW_INK ).vel( vx, vy ).life( flight ).alpha( 0.9f, 0.03f, 0.03f );
				return;
			}
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
			if (p != null) p.look( GROW, 0 ).ramp( MOTES[look], true ).vel( vx, vy ).life( flight ).alpha( 0.9f, 0.03f, 0.03f );
		}

		//matter peeling off a body: ink sinking, or steel scraping
		private void peel( float x, float y ){
			if (look == STEEL){
				FxParticle p = SkillVfx.glow( x + FxRandom.Float( -3f, 3f ), y + FxRandom.Float( -2f, 2f ), FxBudget.P2 );
				if (p != null) p.look( FxFrames.SPARK_2 ).orient().ramp( SkillPalette.STEEL, true )
						.polar( FxRandom.Float( 0.3f, (float) Math.PI - 0.3f ), FxRandom.Float( 20f, 35f ) ).gravity( 120f )
						.life( 0.2f ).alpha( 1f, 0, 0.1f );
				return;
			}
			FxParticle p = SkillVfx.matter( x + FxRandom.Float( -3f, 3f ), y + FxRandom.Float( -2f, 2f ), FxBudget.P2 );
			if (p != null) p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( FxRandom.Float( 0.2f, 0.45f ) ) )
					.vel( FxRandom.Float( -2f, 2f ), FxRandom.Float( 3f, 6f ) ).turbulence( 1f, 4f ).life( 0.4f )
					.alpha( 0.7f, 0.03f, 0.25f ).smooth();
		}

		//the knot: shown from the first mote's arrival, its light rising as it is fed
		private void knot(){
			if (spent < flight) return;
			float gather = Math.max( 0f, duration - flight );
			int fed = 0;
			for (int i = 0; i < laid; i++) if (spent >= gather * i / Math.max( 1, n - 1 ) + flight) fed++;
			float k = Math.min( 1f, fed / (float) n );
			if (knot == null){
				if (GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
				knot = new FxPic( SkillVfx.SHEET );
				knot.additive = true;
				knot.lit( KNOT[look] );
				GameScene.light( knot );
				glow = FxLight.glow( knotX(), knotY(), 11, GLOW[look], KNOT_LOW );
				glowSetups = glow.setups();
			}
			knot.show( k > 0.5f ? FxFrames.STAR4_7 : FxFrames.STAR4_5 ).place( knotX(), knotY() );
			knot.am = 0.7f + 0.3f * k;
			if (glow != null && glow.setups() == glowSetups) glow.level( KNOT_LOW + (KNOT_HIGH - KNOT_LOW) * k );
		}

		//the bite: a frame of a star, two motes racing home, motes flicked where it takes hold, the
		//thread laid along the sag and gone
		private void bit(){
			float kx = knotX(), ky = knotY();
			if (shown){
				FxParticle s = SkillVfx.glow( kx, ky, FxBudget.P0 );
				if (s != null) s.look( FxFrames.IMPACT_STAR[0] ).paint( FLASH[look] ).life( STAR_LIFE ).alpha( 1f, 0, 0.01f ).snap();
				race( kx, ky, ax, ay );
				race( kx, ky, bx, by );
				ends( ax, ay );
				ends( bx, by );
			}
			for (int i = 0; i < threadN; i++){
				float u = (i + 0.5f) / threadN;
				float x = ax + (bx - ax) * u, y = ay + (by - ay) * u + SkillMarkFX.catenary( u ) * sag;
				if (look == STEEL){
					FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
					if (p != null) p.look( FxFrames.SPARK_2 ).ramp( SkillPalette.STEEL, true ).vel( 0, FxRandom.Float( 2f, 6f ) )
							.life( THREAD_LIFE * FxRandom.Float( 0.7f, 1f ) ).alpha( 0.9f, 0, 0.25f ).snap();
				} else {
					FxParticle p = SkillVfx.matter( x, y, FxBudget.P2 );
					if (p != null) p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( FxRandom.Float( 0.2f, 0.4f ) ) )
							.vel( FxRandom.Float( -1.5f, 1.5f ), FxRandom.Float( 5f, 8f ) ).turbulence( 1f, 4f )
							.life( THREAD_LIFE ).alpha( 0.8f, 0.02f, 0.3f ).smooth();
				}
			}
		}

		private void race( float x, float y, float tx, float ty ){
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P1 );
			if (p != null) p.look( FxFrames.MOTE_2 ).paint( KNOT[look] ).vel( (tx - x) / RACE, (ty - y) / RACE ).life( RACE )
					.alpha( 1f, 0, 0.02f );
		}

		//where it takes hold: a few motes flicked out 4 to 6 px
		private void ends( float x, float y ){
			for (int i = 0; i < END_MOTES; i++){
				FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
				if (p == null) continue;
				float a = FxRandom.angle(), d = FxRandom.Float( 4f, 6f );
				p.look( FxFrames.MOTE_1 ).paint( KNOT[look] ).polar( a, d * DRAG_REACH ).drag( 4f ).life( END_LIFE )
						.alpha( 1f, 0, 0.1f ).snap();
			}
		}

		private void let(){
			if (knot != null){
				Fx.dispose( knot );
				knot = null;
			}
			if (glow != null){
				if (glow.setups() == glowSetups) glow.putOut( 0.08f );
				glow = null;
			}
		}

		private void finish(){
			let();
			killAndErase();
			uncount();
		}

		private synchronized void uncount(){
			if (counted){
				counted = false;
				leave();
			}
		}

		@Override
		public void kill(){
			super.kill();
			let();
			uncount();
		}

		@Override
		public void destroy(){
			super.destroy();
			let();
			uncount();
		}
	}

	//a speed that carries a mote d px against a drag of 4 a second over END_LIFE: d x DRAG_REACH px/s
	static final float DRAG_REACH = (float) (4 / (1 - Math.exp( -4 * END_LIFE )));

	// ------------------------------------------------------------------ held

	/** A lasting bond between two sprites in `look`, added to the light layer (render thread);
	 *  the caller holds it and lets it go with Fx.dispose. Any thread. */
	public static Gizmo held( CharSprite a, CharSprite b, int look ){
		final Held h = new Held( a, b, Math.max( 0, Math.min( LOOKS - 1, look ) ) );
		Fx.post( () -> {
			if (GameScene.layer( GameScene.Layer.LIGHTS ) == null || h.gone){
				h.uncount();
				return;
			}
			GameScene.light( h );
		} );
		return h;
	}

	static final class Held extends Group {
		private final CharSprite a, b;
		private final int look;
		//made past CAP: not shown until a later try finds room (render thread)
		boolean shown;
		private boolean counted;
		volatile boolean gone;
		private final FxPic[] links = new FxPic[HELD_LINKS];
		private float age, moteIn, retryIn;
		private final float[] arrive = new float[4];
		private int pairs;

		Held( CharSprite a, CharSprite b, int look ){
			this.a = a;
			this.b = b;
			this.look = look;
			shown = enter();
			counted = shown;
		}

		@Override
		public void update(){
			super.update();
			if (gone) return;
			float dt = Game.elapsed;
			if (!shown){
				if ((retryIn -= dt) > 0) return;
				retryIn = HELD_RETRY;
				if (!(shown = enterCounted())) return;
			}
			age += dt;
			if (links[0] == null){
				for (int i = 0; i < HELD_LINKS; i++){
					FxPic p = new FxPic( SkillVfx.SHEET );
					p.additive = true;
					p.show( FxFrames.SPARK_3 );
					p.origin.set( 0.5f, 1.5f );
					p.lit( (i & 1) == 0 ? MOTES[look].stop( 1 ) : MOTES[look].stop( MOTES[look].size() - 1 ) );
					links[i] = p;
					add( p );
				}
			}
			boolean on = a != null && b != null && a.parent != null && b.parent != null && a.visible && b.visible;
			float ax = on ? SkillVfx.midX( a ) : 0, ay = on ? SkillVfx.midY( a ) : 0;
			float bx = on ? SkillVfx.midX( b ) : 0, by = on ? SkillVfx.midY( b ) : 0;
			float len = (float) Math.hypot( bx - ax, by - ay );
			float sag = SkillMarkFX.sag( len );
			float sway = HELD_SWAY * (float) Math.sin( 2 * Math.PI * HELD_HZ * age );
			for (int i = 0; i < HELD_LINKS; i++){
				FxPic p = links[i];
				if (!on || !Fx.seen( (ax + bx) / 2f, (ay + by) / 2f )){
					p.visible = false;
					continue;
				}
				float u = (i + 0.5f) / HELD_LINKS;
				float x = ax + (bx - ax) * u, y = ay + (by - ay) * u + SkillMarkFX.catenary( u ) * (sag + sway);
				float tx = bx - ax, ty = (by - ay) + sag * (4f - 8f * u);
				p.angle = (float) Math.toDegrees( Math.atan2( ty, tx ) ) - 90f;
				p.x = Math.round( x ) - 0.5f;
				p.y = Math.round( y ) - 1.5f;
				p.am = HELD_ALPHA * (1f + HELD_BREATH * (float) Math.sin( 2 * Math.PI * HELD_HZ * age + i * 0.5f ));
				p.visible = true;
			}
			if (!on) return;
			//a mote off each body pulled to the middle; a glint as each pair meets
			if ((moteIn -= dt) <= 0){
				moteIn = HELD_EVERY;
				float mx = (ax + bx) / 2f, my = (ay + by) / 2f + sag;
				pull( ax, ay, mx, my );
				pull( bx, by, mx, my );
				arrive[pairs++ % arrive.length] = age + HELD_FLIGHT;
			}
			for (int i = 0; i < arrive.length; i++){
				if (arrive[i] > 0 && age >= arrive[i]){
					arrive[i] = 0;
					FxParticle g = SkillVfx.glow( (ax + bx) / 2f, (ay + by) / 2f + sag, FxBudget.P1 );
					if (g == null) continue;
					//a shared cut is a cut across the thread (a red plus is the heal's cross)
					if (look == SHADOW) g.look( FxFrames.SPARK_3 ).orient().polar( (float) Math.atan2( by - ay, bx - ax ) + (float) Math.PI / 2f, 2f );
					else g.look( FxFrames.PLUS_3 );
					g.paint( ACCENT[look] ).life( HELD_GLINT ).alpha( 1f, 0, 0.12f ).snap();
				}
			}
		}

		private void pull( float x, float y, float tx, float ty ){
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
			if (p != null) p.look( GROW_2, 0 ).ramp( MOTES[look], true ).vel( (tx - x) / HELD_FLIGHT, (ty - y) / HELD_FLIGHT )
					.life( HELD_FLIGHT ).alpha( 0.9f, 0.04f, 0.03f );
		}

		synchronized void uncount(){
			if (counted){
				counted = false;
				leave();
			}
		}

		private synchronized boolean enterCounted(){
			if (gone || !enter()) return false;
			counted = true;
			return true;
		}

		@Override
		public void kill(){
			super.kill();
			gone = true;
			uncount();
		}

		@Override
		public void destroy(){
			super.destroy();
			gone = true;
			uncount();
		}
	}

	// ------------------------------------------------------------------ drain

	/** One way, from `from` to `to`: a mote every DRAIN_EVERY s for `seconds`, each flying
	 *  DRAIN_FLIGHT s to where `to` is at its birth, lerping the look's colours; the look's landing
	 *  on the last. Added to the light layer on the render thread. Any thread. */
	public static Gizmo drain( Visual from, Visual to, int look, float seconds ){
		final Drain d = new Drain( from, to, Math.max( 0, Math.min( LOOKS - 1, look ) ), seconds );
		if (d.shown) Fx.post( () -> {
			if (GameScene.layer( GameScene.Layer.LIGHTS ) == null || from == null || to == null){
				d.uncount();
				return;
			}
			GameScene.light( d );
		} );
		return d;
	}

	static final class Drain extends Gizmo {
		private final Visual from, to;
		private final int look;
		private final float seconds;
		final boolean shown;
		private boolean counted, landed;
		private float age, nextAt;

		Drain( Visual from, Visual to, int look, float seconds ){
			this.from = from;
			this.to = to;
			this.look = look;
			this.seconds = Math.max( DRAIN_EVERY, seconds );
			shown = enter();
			counted = shown;
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			age += dt;
			boolean on = from.parent != null && to.parent != null;
			while (on && age >= nextAt && nextAt < seconds){
				nextAt += DRAIN_EVERY;
				float fx = from.x + from.width() / 2f + FxRandom.Float( -3f, 3f ), fy = from.y + from.height() / 2f + FxRandom.Float( -3f, 3f );
				float tx = to.x + to.width() / 2f, ty = to.y + to.height() / 2f;
				if (!Fx.seen( fx, fy ) && !Fx.seen( tx, ty )) continue;
				FxParticle p = SkillVfx.glow( fx, fy, FxBudget.P2 );
				if (p != null) p.look( GROW_2, 0 ).ramp( MOTES[look], true ).vel( (tx - fx) / DRAIN_FLIGHT, (ty - fy) / DRAIN_FLIGHT )
						.life( DRAIN_FLIGHT ).alpha( 0.95f, 0.03f, 0.03f );
			}
			if (!landed && age >= seconds + DRAIN_FLIGHT){
				landed = true;
				if (on) land();
				killAndErase();
				uncount();
			}
		}

		private void land(){
			if (look == SOUL_HEAL && to instanceof CharSprite){
				MoteFX.crosses( (CharSprite) to, 2 );
				MoteFX.greens( (CharSprite) to, 3 );
			} else {
				MoteFX.flick( to.x + to.width() / 2f, to.y + to.height() / 2f, 4, FxFrames.MOTE_1, KNOT[look], 25f, 40f, 4f, 0f,
						0.25f, 12f, FxBudget.P2 );
			}
		}

		synchronized void uncount(){
			if (counted){
				counted = false;
				leave();
			}
		}

		@Override
		public void kill(){
			super.kill();
			uncount();
		}
	}
}
