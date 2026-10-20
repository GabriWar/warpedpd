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
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Fear, shadow, the dead and the rogue's tricks: the pictures those skills share beside the
 * kit's (ShoutFX's dread and howl, SkillBolt, LanceFX.stab, SkillBursts), each the skill's own and
 * none a thing of the game's (no curse landing, no grim trap's column, no item on the floor):
 *
 * - nerve: a blow that cracks an enemy's nerve: a small violet star at its head and one wisp of ink
 *   sinking off it (Demoralize, every hit: quiet).
 * - madness: fear turned to madness (Dread Howl's Amok): a crimson ring closing on its feet, ink
 *   crawling in, a crimson shudder, an ink pool, and two crimson runes circling over its head
 *   (crimson, BLOOD's FF5050 to D01818: never the old pink).
 * - sink / pool: a body sinking into ink (Silent Death, Vanish's shape left behind): a pool of ink
 *   spreading from its middle under the feet, a violet star.
 * - tether: Shadow Link's bond, drawn out of BOTH enemies (LinkFX.bind, SHADOW): ink peeling off
 *   both bodies, violet-black motes pulled from each end along the sag into a knot that swells and
 *   snaps on the bite (one frame of a star, two motes racing home, motes flicked where it takes
 *   hold at both ends, a thread of ink sinking), and between the kit's motes a dense stream of
 *   shadow out of each body (PULL_EVERY s apart, two of light to one of ink, growing 1 to 2 px and
 *   brightening as they near the knot), so the two ends read as two currents meeting; it calls back
 *   on the frame the ethereal chain it replaces bit (distance / 320 + 0.2 s, counted as the chain
 *   counted it). Never rising, never white, never a chain item or the Ethereal Chains' teal.
 * - gather: things drawn into a body (Dark Pact's blood, Summon Skeleton's bones knitting), then
 *   what the caller does when they arrive. Dark Pact's (pact) ends in a bloom of mana (motes of
 *   mana drawn in, a mana glow) while a ring of blood's runes and a ring of mana's circle his feet
 *   the two ways round, each rune meeting its twin, one pair at his front in a one-frame star: the
 *   pact sealed.
 * - stake: Ambush's shadow stake: a short lance of dread light driven down into the shadow at the
 *   feet, a one-frame star where it bites, an ink pool.
 * - pin: Aimed Shot's arrow of light standing in the floor, driven in, fading, a crack.
 * - star: a one-frame strike star (Master Thief's coin in the eyes).
 * - coat: Venom on the blade: a glint of the poison's light running down it, three drops of
 *   venom dripping off it one after another, each gone at the feet (no puddle: a green pool on the
 *   floor reads as a toxic hazard).
 *
 * Any thread: each is put on the scene on the render thread (Fx.post), but tether, which is added
 * where the caller is (as the Chains it replaces was) so its callback keeps the chain's frame.
 * Draws only FxRandom. Caps: particles through FxBudget (P0 strike stars, P1 heads, P2 motes and
 * wisps, P3 dressing), flares PixelFlare's 24, afterimages the kit's 12, pools FxDecal's cap;
 * madness 1 ring + 4 wisps + 2 afterimages + 2 runes + 1 decal, runes over at most RUNES_CAP bodies
 * at once; tether LinkFX's (a bind at most 24 motes + 2 knot quads + 1 light + 12 thread + 6 end
 * motes, LinkFX.CAP bonds at once) + its stream (PULL_MOST a side); gather at most 8; pact 6 drops + 4 mana motes + 1 light + 2
 * rings of PACT_RUNES runes (RuneFX's caps) + 1 star; pin 1 arrow + 1 glow + 1 decal; coat 1 glint + COAT_DROPS drops.
 */
public final class DreadFX {

	private DreadFX(){}

	/** The mage's soul, lightest first (soul skills): SkillPalette.SOUL's cold lavender-grey, apart
	 *  from the rogue's shadow lilac; [2] its body (B4B4EC), what its rings and stars are lit. */
	public static final int[] SOUL = { 0xDCDCFA, 0xC8C8F3, SkillPalette.SOUL_LIGHT, 0x7C7AC4 };
	/** A shout's flinch in the wolf's and the dread's colours, a step darker than ShoutFX's ramps
	 *  (their second stop is near white, and a body's double image in near white reads as a body
	 *  hit): ShoutFX.flinch draws its double images in [1] and [2], its flare in [1]. */
	public static final int[] WOLF_FLINCH = { 0xFFFFFF, 0x9EA4B0, 0x5B6170, 0x3A3F4A };
	public static final int[] DREAD_FLINCH = { 0xFFFFFF, 0xB89CE4, 0x6B4F94, 0x4A3570 };
	/** A nerve cracking (Demoralize), madness (Amok, Demoralize's Terror: crimson, SkillPalette.BLOOD's
	 *  D01818, never pink), a bone's colour, the huntress's pin. */
	public static final int NERVE = 0x8C6BA8, MADNESS = 0xD01818, BONE = 0xE8E0CC, PIN = SkillPalette.NATURE_LIGHT;
	/** Ambush's stake: its shaft, px (its head 8 more), and its ramp: a step deeper than the dread's,
	 *  so its head is violet and never a white beam of light coming down (a smite). */
	public static final int STAKE = 7;
	public static final int[] STAKE_RAMP = { 0xFFFFFF, 0xC9B3EC, 0x8C6BA8, 0x4A3570 };
	/** Madness's runes: how long they circle, s; how many bodies' pairs circle at once (a howl
	 *  maddening a crowd: the rest go without). */
	public static final float RUNES_LIFE = 0.5f;
	public static final int RUNES_CAP = 4;
	/** The pin: how long it stands, s, and how fast it is driven in. */
	public static final float PIN_LIFE = 0.6f, PIN_IN = 0.05f;
	/** Dark Pact's seal: two rings of PACT_RUNES runes (a rune alone is a dot: runes read in numbers),
	 *  blood's and mana's, circle his feet on a PACT_RX x PACT_RY ellipse at PACT_TURN rad/s the two
	 *  ways round from opposite sides, every rune meeting its twin after PACT_MEET s (one pair at his
	 *  front) while they are whole, fading after it, gone by PACT_LIFE; the mana's glow. */
	public static final int PACT_RUNES = 3;
	public static final float PACT_RX = 9f, PACT_RY = 3.5f, PACT_TURN = (float) Math.PI, PACT_MEET = 0.5f, PACT_LIFE = 0.75f,
			PACT_GLOW = 0.5f;
	/** Venom's coat: its drops, this far apart (s), each dripping from 12 px/s under COAT_FALL px/s^2;
	 *  the glint running down the blade at COAT_RUN px/s for COAT_GLINT s. */
	public static final int COAT_DROPS = 3;
	public static final float COAT_EVERY = 0.08f, COAT_FALL = 160f, COAT_RUN = 12f, COAT_GLINT = 0.25f;
	/** The venom's drops (matter): PoisonParticle's own greens. */
	static final Ramp VENOM = new Ramp( 0xB8F060, 0x6CCC3C );

	// ------------------------------------------------------------------ nerve

	/** A blow cracking an enemy's nerve: a small violet star at its head and a wisp of ink sinking
	 *  off it. Any thread. */
	public static void nerve( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float mx = SkillVfx.midX( s ), hy = s.y + 3;
			PixelFlare.show( mx, hy, NERVE, PixelFlare.S, 0.3f );
			FxParticle p = SkillVfx.matter( mx + FxRandom.Float( -2f, 2f ), hy + 1, FxBudget.P2 );
			if (p != null) p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( 0.3f ) )
					.vel( FxRandom.Float( -2f, 2f ), FxRandom.Float( 8f, 11f ) ).turbulence( 1f, 4f ).life( 0.5f )
					.alpha( 0.75f, 0.05f, 0.25f ).smooth();
		} );
	}

	// ------------------------------------------------------------------ madness

	/** Fear turned to madness on a body: a crimson ring closing on its feet, ink crawling in, a
	 *  crimson shudder, an ink pool, two crimson runes circling over its head. Any thread. */
	public static void madness( final CharSprite s ){
		if (s == null) return;
		ClosingRing.on( s, 31, 7, 0.36f, MADNESS );
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float mx = SkillVfx.midX( s ), feet = s.y + s.height() - 3;
			crawl( mx, feet, 4 );
			Afterimage.of( s, MADNESS, 0.45f, 0.1f, -2, 0 );
			FxTimeline.start().at( 0.06f, () -> {
				if (s.parent != null && s.visible) Afterimage.of( s, MADNESS, 0.45f, 0.1f, 2, 0 );
			} );
			pool( mx, feet + 1, 0.4f, 0.35f, 0.4f );
			int slot = Runes.free();
			if (slot >= 0 && GameScene.layer( GameScene.Layer.LIGHTS ) != null){
				Runes r = new Runes( s );
				Runes.SLOTS[slot] = r;
				GameScene.light( r );
			}
		} );
	}

	//ink crawling in over the floor from the rim of an 11 x 5 ellipse at the feet, sinking (as dread)
	private static void crawl( float mx, float feet, int n ){
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			float a = a0 + i * (float) (2 * Math.PI / n) + FxRandom.Float( -0.3f, 0.3f );
			float c = (float) Math.cos( a ), sn = (float) Math.sin( a );
			float x = mx + c * 5.5f, y = feet + sn * 2.5f;
			FxParticle p = SkillVfx.onFloor( x, y, FxBudget.P2 );
			if (p == null) continue;
			float v = FxRandom.Float( 18f, 24f );
			p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( 0.3f ) )
					.vel( -c * v, -sn * v * 0.5f + 3f ).life( 0.45f ).alpha( 0.7f, 0.05f, 0.25f ).smooth();
		}
	}

	//two crimson runes circling over a maddened head, flickering through their frames, for RUNES_LIFE s;
	//at most RUNES_CAP of them, held in slots that free themselves when one is gone (render thread)
	private static final class Runes extends Group {
		private static final Ramp TONE = new Ramp( SkillPalette.BLOOD_LIGHT, MADNESS );
		static final Runes[] SLOTS = new Runes[RUNES_CAP];

		static int free(){
			for (int i = 0; i < SLOTS.length; i++){
				Runes r = SLOTS[i];
				if (r == null || !r.exists || r.parent == null) return i;
			}
			return -1;
		}

		private final CharSprite s;
		private final FxPic[] runes = new FxPic[2];
		private final float a0 = FxRandom.angle();
		private float time;

		Runes( CharSprite s ){
			this.s = s;
			for (int i = 0; i < runes.length; i++){
				FxPic p = new FxPic( SkillVfx.SHEET );
				p.additive = true;
				p.show( FxFrames.RUNE_3[0] );
				p.visible = false;
				runes[i] = p;
				add( p );
			}
		}

		@Override
		public void update(){
			super.update();
			time += Game.elapsed;
			if (time >= RUNES_LIFE || s.parent == null){
				Fx.dispose( this );
				return;
			}
			float p = time / RUNES_LIFE;
			float a = p < 0.15f ? p / 0.15f : p > 0.6f ? (1f - p) / 0.4f : 1f;
			float mx = SkillVfx.midX( s ), y = s.y - 2;
			for (int i = 0; i < runes.length; i++){
				//a turn and a half over its life, the two opposite each other
				float t = a0 + i * (float) Math.PI + p * 3f * (float) Math.PI;
				FxPic r = runes[i];
				r.show( FxFrames.RUNE_3[((int) (time * 12) + 2 * i) % FxFrames.RUNE_3.length] );
				r.lit( TONE.at( p ) );
				r.place( mx + (float) Math.cos( t ) * 5f, y + (float) Math.sin( t ) * 2f );
				r.am = a;
				r.visible = s.visible;
			}
		}
	}

	// ------------------------------------------------------------------ ink

	/** A pool of ink spreading from its middle at (x, y), at `alpha`, held `hold` s and faded over
	 *  `fade`. Any thread. */
	public static void pool( final float x, final float y, final float alpha, final float hold, final float fade ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> pool( x, y, alpha, hold, fade ) );
			return;
		}
		if (!Fx.seen( x, y )) return;
		FxDecal d = FxDecal.at( x, y, FxFrames.SPLAT[2], ShoutFX.INK_POOL, alpha, hold, fade, FxRandom.chance( 0.5f ) );
		if (d != null) d.revealCrop( FxDecal.FROM_MIDDLE, 0.12f );
	}

	/** A body sinking into ink (Silent Death's sleeper): a pool spreading under its feet and a small
	 *  dark violet star. Any thread. */
	public static void sink( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float mx = SkillVfx.midX( s ), feet = s.y + s.height() - 3;
			pool( mx, feet + 1, 0.5f, 0.4f, 0.5f );
			PixelFlare.show( mx, SkillVfx.midY( s ), ShoutFX.DREAD[3], PixelFlare.S, 0.2f );
		} );
	}

	/** The shape left behind (Vanish): the body as a dark violet afterimage where it stood, a pool
	 *  of ink under it. Any thread. */
	public static void vanish( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null) return;
			Afterimage.of( s, ShoutFX.DREAD[3], 0.5f, 0.4f, 0, 0 );
			pool( SkillVfx.midX( s ), s.y + s.height() - 2, 0.5f, 0.3f, 0.5f );
		} );
	}

	// ------------------------------------------------------------------ the tether

	/**
	 * Shadow Link's bond from `from` to `to` (an ethereal chain's points), added to `parent` here,
	 * on the caller's thread, as that chain was: the bond drawn out of both enemies (LinkFX.bind,
	 * SHADOW): ink peeling off both, violet-black motes pulled from each end along the sag into a
	 * knot that swells, snapping on the bite; `bite` is called on the frame the chain called its own
	 * (its distance / 320 + 0.2 s, counted as Chains counts it), and only by the parent's update.
	 */
	public static Gizmo tether( Group parent, PointF from, PointF to, Callback bite ){
		//past LinkFX's cap the bind draws only its thread: no stream gathers where no knot will be
		if (LinkFX.live() < LinkFX.CAP){
			final Pull pull = new Pull( from, to );
			Fx.post( () -> {
				if (GameScene.layer( GameScene.Layer.LIGHTS ) != null) GameScene.light( pull );
			} );
		}
		return LinkFX.bind( parent, from, to, LinkFX.SHADOW, bite );
	}

	/** Shadow Link's stream: a mote out of each end every PULL_EVERY s while the bond gathers, at
	 *  most PULL_MOST alive a side; every PULL_INK-th one ink (matter). Each end's current runs in
	 *  its own lane PULL_LANE px beside the bind's chord (the first's on one side, the second's on
	 *  the other), into the knot's flank: its light is added, so a current riding the bind's own
	 *  motes would add up to white. On a short bond, whose motes fly slower, they come at least
	 *  PULL_GAP px apart for the same reason. */
	public static final float PULL_EVERY = 0.025f, PULL_LANE = 3f, PULL_GAP = 3.5f;
	public static final int PULL_MOST = 12, PULL_INK = 3;
	/** Its light, deep at the body to the shadow's light at the knot (never paler than A070E0). */
	static final Ramp PULL = new Ramp( SkillPalette.SHADOW.stop( 3 ), SkillPalette.SHADOW_LIGHT, SkillPalette.SHADOW.stop( 1 ) );
	static final int[][] PULL_GROW = { FxFrames.MOTE_1, FxFrames.MOTE_2 };

	//the stream feeding the knot from both ends, along the same chords as the bind's motes and over
	//its gather, every mote at the knot by the bite (render thread; no allocation a frame)
	static final class Pull extends Gizmo {
		private final float ax, ay, bx, by, kx, ky, nx, ny, gather, flight, every;
		private final float[] untilA = new float[PULL_MOST], untilB = new float[PULL_MOST];
		private int aliveA, aliveB, made;
		private float age, next;
		private boolean seen, begun;

		Pull( PointF a, PointF b ){
			ax = a.x;
			ay = a.y;
			bx = b.x;
			by = b.y;
			float duration = tetherTime( a, b );
			flight = LinkFX.flight( duration );
			gather = Math.max( 0f, duration - flight );
			float len = (float) Math.hypot( bx - ax, by - ay );
			kx = (ax + bx) / 2f;
			ky = (ay + by) / 2f + SkillMarkFX.sag( len );
			//the lanes' side: square to the bond
			nx = -(by - ay) / Math.max( 1f, len );
			ny = (bx - ax) / Math.max( 1f, len );
			//PULL_EVERY, or slower where its motes fly slower than PULL_GAP px a PULL_EVERY
			float speed = (float) Math.hypot( kx - ax, ky - ay ) / Math.max( 0.01f, flight );
			every = Math.max( PULL_EVERY, PULL_GAP / Math.max( 1f, speed ) );
		}

		@Override
		public void update(){
			super.update();
			if (!begun){
				begun = true;
				seen = Fx.seen( ax, ay ) || Fx.seen( bx, by );
			}
			age += Game.elapsed;
			if (!seen || age > gather + flight){
				Fx.dispose( this );
				return;
			}
			aliveA = prune( untilA, aliveA );
			aliveB = prune( untilB, aliveB );
			while (next <= age && next <= gather){
				next += every;
				boolean ink = ++made % PULL_INK == 0;
				if (aliveA < PULL_MOST && mote( ax, ay, PULL_LANE, ink )) untilA[aliveA++] = age + flight;
				if (aliveB < PULL_MOST && mote( bx, by, -PULL_LANE, ink )) untilB[aliveB++] = age + flight;
			}
		}

		private int prune( float[] until, int alive ){
			for (int i = 0; i < alive; ){
				if (until[i] <= age) until[i] = until[--alive];
				else i++;
			}
			return alive;
		}

		//one mote of an end's current, in its lane `lane` px to the side, to the knot's flank
		private boolean mote( float x0, float y0, float lane, boolean ink ){
			float ox = nx * lane, oy = ny * lane;
			float x = x0 + ox + FxRandom.Float( -0.5f, 0.5f ), y = y0 + oy + FxRandom.Float( -0.5f, 0.5f );
			float vx = (kx + ox - x) / flight, vy = (ky + oy - y) / flight;
			FxParticle p = ink ? SkillVfx.matter( x, y, FxBudget.P2 ) : SkillVfx.glow( x, y, FxBudget.P2 );
			if (p == null) return false;
			p.look( PULL_GROW, 0 ).vel( vx, vy ).life( flight ).alpha( 0.9f, 0.02f, 0.03f );
			if (ink) p.paint( SkillPalette.SHADOW_INK );
			else p.ramp( PULL, true );
			return true;
		}
	}

	/** The chain's own time to bite, s (Chains). */
	public static float tetherTime( PointF from, PointF to ){
		return (float) Math.hypot( to.x - from.x, to.y - from.y ) / 320f + 0.2f;
	}

	// ------------------------------------------------------------------ gathered in

	/**
	 * `n` things (at most 8) drawn into (x, y) from `radius` px round it over `life` s: matter
	 * (blood, bones) unless `light`, one of `frames` each, in `color`, turning a quarter now and then
	 * if `spin`; `then` runs (render thread) when they arrive. Any thread.
	 */
	public static void gather( final float x, final float y, final int[][] frames, final int color, final boolean light,
			final int n, final float radius, final float life, final boolean spin, final Runnable then ){
		Fx.post( () -> {
			if (Fx.seen( x, y )){
				float a0 = FxRandom.angle();
				for (int i = 0; i < Math.min( 8, n ); i++){
					float a = a0 + i * (float) (2 * Math.PI / n) + FxRandom.Float( -0.25f, 0.25f );
					float sx = x + (float) Math.cos( a ) * radius, sy = y + (float) Math.sin( a ) * radius * 0.6f;
					FxParticle p = light ? SkillVfx.glow( sx, sy, FxBudget.P2 ) : SkillVfx.matter( sx, sy, FxBudget.P2 );
					if (p == null) continue;
					p.look( frames[FxRandom.Int( frames.length )] ).paint( color )
							.vel( (x - sx) / life, (y - sy) / life ).life( life ).alpha( 1f, 0.04f, 0.04f );
					if (spin) p.spin( 0.06f );
					else if (frames[0][2] == 1) p.orient();
				}
			}
			if (then != null) FxTimeline.start().at( life, then );
		} );
	}

	/** Dark Pact's price: drops of the body's own blood drawn into its chest over 0.25 s, then the
	 *  mana blooming there (four motes of mana drawn in, deep to pale, and a glow of the mana's blue);
	 *  meanwhile a ring of PACT_RUNES runes of blood and one of mana circle its feet the two ways
	 *  round, from opposite sides, every rune meeting its twin after PACT_MEET s, the pair at its
	 *  front in a one-frame star: the pact sealed. Any thread. */
	public static void pact( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			final float mx = SkillVfx.midX( s ), my = SkillVfx.midY( s );
			gather( mx, my, new int[][]{ FxFrames.DROP_2 }, s.blood() & 0xFFFFFF, false, 6, 10f, 0.25f, false,
					() -> manaBloom( mx, my ) );
			RuneFX.ring( s, PACT_RUNES, SkillPalette.RUNE_BLOOD, PACT_RX, PACT_RY, PACT_TURN, PACT_LIFE, 0f );
			RuneFX.ring( s, PACT_RUNES, SkillPalette.RUNE_MANA, PACT_RX, PACT_RY, -PACT_TURN, PACT_LIFE, (float) Math.PI );
			FxTimeline.start().at( PACT_MEET, () -> {
				if (s.parent != null && s.visible) star( SkillVfx.midX( s ), s.y + s.height() - 2 + PACT_RY, SkillPalette.MANA.stop( 0 ) );
			} );
		} );
	}

	//the mana the blood paid for, blooming in the chest: motes of mana drawn in, then its glow
	private static void manaBloom( final float x, final float y ){
		if (!Fx.seen( x, y )) return;
		MoteFX.gather( x, y, 4, SkillPalette.MANA, 9f, 0.12f );
		FxTimeline.start().at( 0.12f, () -> FxLight.glow( x, y, 15, SkillPalette.MANA_LIGHT, PACT_GLOW ).life( 0.03f, 0.05f, 0.25f ) );
	}

	/** Summon Skeleton's bones knitting: six chips of bone drawn into the body from 12 px round it
	 *  over 0.2 s, then a small soul star. Any thread. */
	public static void knit( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null) return;
			final float mx = SkillVfx.midX( s ), my = SkillVfx.midY( s ) + 1;
			gather( mx, my, BONES, BONE, false, 6, 12f, 0.2f, true,
					() -> PixelFlare.show( mx, my, SOUL[2], PixelFlare.S, 0.3f ) );
		} );
	}

	private static final int[][] BONES = { FxFrames.PEBBLE_3, FxFrames.SHARD_GLASS[0], FxFrames.SHARD_GLASS[1] };

	// ------------------------------------------------------------------ strikes

	/** Ambush's stake: a short lance of dread light driven straight down into the shadow at the
	 *  feet, beside the body, a one-frame star where it bites, a pool of ink. Any thread. */
	public static void stake( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			final float x = SkillVfx.midX( s ) + (s.flipHorizontal ? -3 : 3), feet = s.y + s.height() - 2;
			LanceFX.stab( x, feet, 90, STAKE_RAMP, STAKE, 0.35f );
			pool( SkillVfx.midX( s ), feet, 0.55f, 0.3f, 0.4f );
			FxTimeline.start().at( LanceFX.STAB_IN, () -> star( x, feet - 1, NERVE ) );
		} );
	}

	/** A one-frame strike star at (x, y) (P0: never thinned). Any thread. */
	public static void star( final float x, final float y, final int color ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> star( x, y, color ) );
			return;
		}
		if (!Fx.seen( x, y )) return;
		FxParticle p = SkillVfx.glow( x, y, FxBudget.P0 );
		if (p != null) p.look( FxFrames.IMPACT_STAR[FxRandom.Int( FxFrames.IMPACT_STAR.length )] ).paint( color ).snap()
				.life( 0.06f ).alpha( 1f, 0, 0.02f );
	}

	/** Aimed Shot's pin: an arrow of light driven into the floor at the feet of the pinned body,
	 *  leaning away from the one who shot it (`fromX`), standing PIN_LIFE s and fading, a crack where
	 *  it went in. Any thread. */
	public static void pin( final CharSprite s, final float fromX ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible || GameScene.layer( GameScene.Layer.LIGHTS ) == null) return;
			float mx = SkillVfx.midX( s ), feet = s.y + s.height() - 2;
			boolean right = mx >= fromX;
			float tipX = mx + (right ? 4 : -4), tipY = feet;
			GameScene.light( new Pin( tipX, tipY, right ? 70 : 110 ) );
			FxDecal d = FxDecal.at( tipX, tipY, FxFrames.CRACK_11[FxRandom.Int( 2 )], Element.EARTH.ramp.stop( 4 ), 0.5f,
					0.6f, 0.4f, !right );
			if (d != null) d.revealCrop( FxDecal.FROM_MIDDLE, 0.06f );
		} );
	}

	//the arrow: driven PIN_IN s in from 5 px back along its way, standing, fading over its last 40%
	private static final class Pin extends FxPic {
		private final float tipX, tipY, cos, sin;
		private float time;
		private boolean struck;

		Pin( float tipX, float tipY, float deg ){
			super( SkillVfx.SHEET );
			this.tipX = tipX;
			this.tipY = tipY;
			cos = (float) Math.cos( Math.toRadians( deg ) );
			sin = (float) Math.sin( Math.toRadians( deg ) );
			additive = true;
			show( FxFrames.ARROW_9 );
			lit( PIN );
			origin.set( FxFrames.ARROW_9[2] / 2f, FxFrames.ARROW_9[3] / 2f );
			angle = deg;
			visible = false;
		}

		@Override
		public void update(){
			super.update();
			time += Game.elapsed;
			if (time >= PIN_LIFE){
				Fx.dispose( this );
				return;
			}
			float back = 5f * (1f - FxCurves.easeOut( Math.min( 1f, time / PIN_IN ) ));
			//its tip at the point (its middle 4 px back from it)
			float cx = tipX - cos * (4f + back), cy = tipY - sin * (4f + back);
			x = cx - origin.x;
			y = cy - origin.y;
			float p = time / PIN_LIFE;
			am = p < 0.6f ? 1f : (1f - p) / 0.4f;
			lit( Ramp.lerp( 0xFFFFFF, PIN, Math.min( 1f, time / 0.15f ) ) );
			visible = Fx.seen( tipX, tipY );
			if (!struck && time >= PIN_IN){
				struck = true;
				if (visible) FxLight.glow( tipX, tipY - 1, 7, PIN, 0.8f ).life( 0, 0.05f, 0.3f );
			}
		}
	}

	/** Venom on the blade: a glint of `color` (the poison's light) running down it at COAT_RUN px/s,
	 *  and COAT_DROPS drops of venom (PoisonParticle's greens, matter) dripping off it COAT_EVERY s
	 *  apart, each a pixel lower, falling under COAT_FALL and gone at the feet (no puddle). P2 drops,
	 *  P1 glint. Any thread. */
	public static void coat( final CharSprite s, final int color ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			final float hx = SkillVfx.midX( s ) + (s.flipHorizontal ? -4 : 4), hy = SkillVfx.midY( s ) - 1;
			final float feet = s.y + s.height() - 2;
			if (!Fx.seen( hx, hy )) return;
			MoteFX.glint( hx, hy - 2, FxFrames.PLUS_3, color, COAT_GLINT, 0, COAT_RUN );
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < COAT_DROPS; i++){
				final float y = hy + i;
				t.at( COAT_EVERY * (i + 1), () -> MoteFX.flick( hx, y, 1, FxFrames.DROP_2, VENOM, (float) (Math.PI / 2), 0f,
						COAT_RUN, COAT_RUN, 0f, COAT_FALL, 0.5f, feet - y, FxBudget.P2 ) );
			}
		} );
	}
}
