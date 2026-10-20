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
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.BoltFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * A skill's own flight, where a skill borrowed a wand's or a mob's bolt (MagicMissile) or a beam,
 * drawn as Shattered's own bolts are (docs/fx/skills.md, the concentrated core): many small light
 * squares stepping down in whole pixels behind a painted head, so the trail reads as one solid
 * tapering comet; a head of its look with a 1 px heart and a glow, a pool of its light under it, a
 * bloom and sparks as it leaves, what it sheds, and an arrival of its own.
 *
 * - fly: the MagicMissile replacement. An Emitter that, in its reset, pours its trail once: exactly
 *   MagicMissile's one draw of the game's Random, on the caller's thread. It flies at SPEED
 *   (MagicMissile's 200 px/s) and calls back on the frame MagicMissile would (its time counted down
 *   the same way, from the same distance). With no group to fly in it makes that one draw and calls
 *   back at once.
 * - mote: the same picture with no draw of the game's Random at all (an FxEmitter's start), for
 *   what replaces a Beam (which drew none): a pickup flying home, a mend. Also at SPEED.
 * - peel (chainable): n motes leave a body over the first PEEL_FOR s, each aimed where the head will
 *   be 0.15 to 0.25 s later, dying as they meet it (the fear leaving a victim, the soul drawn out).
 *
 * The core: each emit (every TRAIL_EVERY s) lays CORE_RICH squares, one at the emit point and one
 * CORE_BACK px back along the way, stepping STEP_4 or STEP_3 down the look's ramp (its pale coloured
 * stop only for the first quarter of their life), alpha 0.85 out over their last 40%, drifting at
 * most CORE_DRIFT px/s, living CORE_LIFE s, P2: about 32 alive a rich bolt. The first RICH bolts at
 * once (a count per scene) are rich: their core, the head's pool, the launch (a GLOW_11 at the hand
 * and LAUNCH_SPARKS sparks thrown toward the target) and what they shed (at most SHEDS alive, HOLY
 * SHEDS_HOLY). Later ones are lean: one square an emit, LEAN_LIFE s, P3, no sheds, no launch, no
 * pool, their impact at 0.6. At most TRAIL_MAX particles a bolt (core, sheds, peel).
 *
 * Looks: SOUL (the mage's cold lavender-grey swaying), MANA (sky blue swaying), HEX (ink sinking
 * behind a VOID heart, light drawn into it, an implosion, a closing ring and an ink pool), EMBER (a
 * cinder's hop: a little tongue, the fire's core, embers, a scorch; never a firebolt's tongues where
 * nothing burns), HOLY (Guiding Light's star and its ghost, its arrival the game's own
 * LIGHT_MISSILE impact), HEAL (a green cross shedding red crosses, landing in the heal's crosses),
 * SPARK (ball lightning crackling white-blue), ARCANE (a RUNE_5 head, a blue-violet core, runes shed,
 * a ring of runes where it lands), SPIRIT (the old look, kept for the previews), RUNE (Spirit Volley's
 * ward bolt exactly as 72b40617b flew it: MagicMissile.WardParticle squares poured every
 * WARD_EVERY s in the bolt itself, the game's own BoltFx WARD head, sheds and impact). A bolt is Lit
 * and carried over the night's tint while its own particles are light (RUNE's), as MagicMissile is.
 *
 * Draws only FxRandom past the one pour; no GL until its first update; let go of with Fx.dispose.
 */
public class SkillBolt extends Emitter implements Lit {

	public static final int SOUL = 0, MANA = 1, HEX = 2, EMBER = 3, HOLY = 4, HEAL = 5, SPARK = 6, ARCANE = 7, SPIRIT = 8, RUNE = 9;
	public static final int LOOKS = 10;
	/** MagicMissile's speed, px/s; the trail's emits, s; its particles at most. */
	public static final float SPEED = 200f, TRAIL_EVERY = 0.02f;
	public static final int TRAIL_MAX = 48;
	/** The ward bolt's pour (MagicMissile.WARD's interval). */
	public static final float WARD_EVERY = 0.015f;
	/** Bolts at once in full (a count per scene). */
	public static final int RICH = 4;
	/** The core: squares an emit (rich), how far back the second lies (px), its drift (px/s), its life (s). */
	public static final int CORE_RICH = 2;
	public static final float CORE_BACK = 2f, CORE_DRIFT = 3f, CORE_LIFE = 0.32f, CORE_LIFE_SPARK = 0.2f, CORE_LIFE_EMBER = 0.3f,
			LEAN_LIFE = 0.2f, CORE_ALPHA = 0.85f;
	/** Sheds alive at most (HOLY's fewer: its cone flies many at once). */
	public static final int SHEDS = 8, SHEDS_HOLY = 5;
	/** A spark's crackle alive at most. */
	public static final int CRACKLE = 6;
	/** Motes a peel at most, over this long (s). */
	public static final int PEEL = 8;
	public static final float PEEL_FOR = 0.15f;
	/** The launch: its glow's level and life (s), its sparks. */
	public static final float LAUNCH_GLOW = 0.6f, LAUNCH_LIFE = 0.1f;
	public static final int LAUNCH_SPARKS = 3;
	/** The head's glow and the pool under it (levels). */
	public static final float POOL_LEVEL = 0.2f;
	/** How far it has flown (px) before its core begins: never a wash over the caster's body. */
	public static final float CLEAR = 6f;
	/** A lean bolt's impact, a share of a rich one's. */
	public static final float LEAN_IMPACT = 0.6f;

	//per look: the head's colour (SPARK swaps to HEAD_ALT), its heart's (-1: none), the glow's colour
	//and level, the pool's colour (-1: none), the sway of its path either side (px)
	private static final int[] HEAD = { 0xB4B4EC, 0xD0F6FF, SkillPalette.SHADOW_INK, 0xFFD24A, SkillPalette.HOLY_RAY,
			0xA8FFC0, 0xFFFFFF, 0xD8C8FF, 0x66AAFF, 0xD8C0FF };
	private static final int[] HEART = { 0xDCDCFA, 0xFFFFFF, 0xD8B8FF, 0xFFF3C8, 0xFFFFFF, SkillPalette.HEAL_HEART, -1, -1, -1, -1 };
	private static final int[] GLOW = { 0xB4B4EC, 0x44CCFF, SkillPalette.VOID_LIGHT, SkillPalette.FIRE_LIGHT, SkillPalette.HOLY_LIGHT,
			SkillPalette.HEAL_LIGHT, SkillPalette.LIGHTNING_LIGHT, 0x9070F0, 0x66AAFF, SkillPalette.WARD_LIGHT };
	private static final float[] GLOW_LEVEL = { 0.5f, 0.5f, 0.35f, 0.45f, 0.45f, 0.5f, 0.55f, 0.5f, 0.9f, 0f };
	private static final int[] POOL = { 0x7C7AC4, 0x227AD0, -1, SkillPalette.FIRE_LIGHT, SkillPalette.HOLY_LIGHT,
			SkillPalette.HEAL_LIGHT, 0xA8C8FF, SkillPalette.ARCANE_LIGHT, -1, -1 };
	private static final float[] SWAY = { 4f, 3f, 0, 0, 0, 2f, 0, 0, 0, 0 };
	private static final int HEAD_ALT = 0xE4F0FF;
	//per look: the core's ramp and steps
	private static final Ramp[] CORE = { SkillPalette.SOUL, SkillPalette.MANA, SkillPalette.VOID, SkillPalette.FIRE, SkillPalette.HOLY,
			SkillPalette.HEAL, SkillPalette.LIGHTNING, SkillPalette.ARCANE, null, SkillPalette.WARD };
	private static final int[][][] STEPS = { FxFrames.STEP_3, FxFrames.STEP_4, FxFrames.STEP_3, FxFrames.STEP_3, FxFrames.STEP_3,
			FxFrames.STEP_3, FxFrames.STEP_3, FxFrames.STEP_4, null, null };
	//the old spirit (SPIRIT: kept as it was)
	private static final Ramp SPIRIT_RAMP = new Ramp( 0xFFFFFF, 0x66AAFF );
	private static final Ramp SPIRIT_SPARKS = new Ramp( 0x66AAFF, 0xFFFFFF );
	static final int[][] GROW_2 = { FxFrames.MOTE_1, FxFrames.MOTE_2 };
	//a holy shed's mote, a heal's red shed
	private static final int HOLY_SHED = 0xFFE070;

	private static final SkillBolt NONE = new SkillBolt();

	//the rich bolts alive, in the scene they are counted in
	private static int richLive;
	private static Group countedIn;

	private Callback callback;
	private int look;
	private boolean quiet, fresh, rich, counted, landed;
	private float sx, sy, time, total, flown, age;
	private float ox, oy, dirX, dirY, dist, waves;
	private float offX, offY;
	private Visual target;
	private FxPic head, heart, ghost;
	private FxLight glow, pool;
	private int glowSetups, poolSetups;
	private float jitter, shedIn;
	private int jx, jy;
	//what it has alive (until when, on its own clock): all of it, and its sheds
	private final float[] until = new float[TRAIL_MAX];
	private int alive;
	private final float[] shedUntil = new float[SHEDS];
	private int sheds;
	//its peel
	private CharSprite peelFrom;
	private int peelN, peeled;
	//the ward bolt's own look, and its stand-in in the light layer
	private BoltFx ward;
	private Gizmo carrier;

	/** A bolt of a look from one point to another in `parent` (the group a MagicMissile flew in),
	 *  calling back on MagicMissile's arrival frame. Draws the game's Random once, here. */
	public static SkillBolt fly( Group parent, PointF from, PointF to, int look, Callback cb ){
		return make( parent, from, to, null, look, cb, false );
	}

	public static SkillBolt fly( Group parent, Visual from, Visual to, int look, Callback cb ){
		return make( parent, from.center(), to.center(), to, look, cb, false );
	}

	public static SkillBolt fly( Group parent, Visual from, int to, int look, Callback cb ){
		return make( parent, from.center(), DungeonTilemap.raisedTileCenterToWorld( to ), null, look, cb, false );
	}

	public static SkillBolt fly( Group parent, int from, int to, int look, Callback cb ){
		return make( parent, DungeonTilemap.raisedTileCenterToWorld( from ), DungeonTilemap.raisedTileCenterToWorld( to ),
				null, look, cb, false );
	}

	/** The same picture, drawing nothing of the game's Random (a Beam's replacement), in the hero's
	 *  layer; with none, it calls back at once. */
	public static SkillBolt mote( PointF from, PointF to, int look, Callback cb ){
		return make( SkillVfx.heroLayer(), from, to, null, look, cb, true );
	}

	public static SkillBolt mote( Visual from, Visual to, int look, Callback cb ){
		return make( SkillVfx.heroLayer(), from.center(), to.center(), to, look, cb, true );
	}

	public static SkillBolt mote( int from, int to, int look, Callback cb ){
		return make( SkillVfx.heroLayer(), DungeonTilemap.raisedTileCenterToWorld( from ),
				DungeonTilemap.raisedTileCenterToWorld( to ), null, look, cb, true );
	}

	private static SkillBolt make( Group parent, PointF from, PointF to, Visual target, int look, Callback cb, boolean quiet ){
		if (parent == null){
			//nothing to fly in: the one draw the pour makes, and the moment it waits for at once
			if (!quiet) Random.Float( TRAIL_EVERY );
			if (cb != null) cb.call();
			return NONE;
		}
		SkillBolt b = new SkillBolt();
		b.reset( from, to, target, Math.max( 0, Math.min( LOOKS - 1, look ) ), cb, quiet );
		parent.add( b );
		return b;
	}

	/** n (at most PEEL) motes leave `from`'s body over its first PEEL_FOR s, each aimed where its
	 *  head will be 0.15 to 0.25 s later, deep to pale, dying as they meet it (P2). Chainable. */
	public SkillBolt peel( CharSprite from, int n ){
		if (this == NONE || from == null || look == RUNE || look == SPIRIT) return this;
		peelFrom = from;
		peelN = Math.max( 0, Math.min( PEEL, n ) );
		return this;
	}

	/** Rich bolts alive in this scene. */
	public static synchronized int richLive(){
		return countedIn == GameScene.layer( GameScene.Layer.LIGHTS ) ? richLive : 0;
	}

	//MagicMissile.reset's flight: the same distance, speed and count-down; its pour last
	private void reset( PointF from, PointF to, Visual target, int look, Callback cb, boolean quiet ){
		this.callback = cb;
		this.look = look;
		this.quiet = quiet;
		this.target = target;
		x = from.x;
		y = from.y;
		width = height = 0;
		ox = from.x;
		oy = from.y;
		float dx = to.x - from.x, dy = to.y - from.y;
		dist = (float) Math.sqrt( dx * dx + dy * dy );
		dirX = dist > 0 ? dx / dist : 0;
		dirY = dist > 0 ? dy / dist : 0;
		PointF speed = new PointF( dx, dy ).normalize().scale( SPEED );
		sx = speed.x;
		sy = speed.y;
		time = dist / SPEED;
		total = time;
		flown = 0;
		waves = Math.max( 1, Math.round( dist / 48f ) );
		fresh = true;
		size( 4 );
		//the ward bolt pours the ward's own squares into itself, as the missile did
		Emitter.Factory f = look == RUNE ? MagicMissile.WardParticle.FACTORY : TRAILS[look];
		float every = look == RUNE ? WARD_EVERY : TRAIL_EVERY;
		if (quiet){
			startDelayed( f, every, 0, FxRandom.Float( every ) );
		} else {
			pour( f, every );
		}
		revive();
	}

	private void size( float size ){
		x -= size / 2;
		y -= size / 2;
		width = height = size;
	}

	/** How far through its flight, 0..1. */
	public float progress(){
		return total > 0 ? Math.min( 1f, flown / total ) : 1f;
	}

	/** Whether it has every layer (one of the first RICH at once). */
	public boolean rich(){
		return rich;
	}

	@Override
	protected boolean isFrozen(){
		return false;
	}

	//its trail's spawn points from the effects' own random numbers, on its path; a core's emit where
	//the bolt was at that emit's moment (several emits fall in one long frame: spread along the way it
	//flew, not heaped where the frame began). The emitter's own clock (super.time: how long ago this
	//emit's moment was, this field's `time` being the flight's count-down) puts it that far along the
	//frame's way, never past where the flight ends
	@Override
	protected void emit( int index ){
		float ahead = look == RUNE || look == SPIRIT || !on ? 0f : Math.max( 0f, Math.min( Game.elapsed - super.time, time ) );
		factory.emit( this, index, x + offX + sx * ahead + FxRandom.Float( width ), y + offY + sy * ahead + FxRandom.Float( height ) );
	}

	@Override
	public void update(){
		if (fresh && on){
			fresh = false;
			setOff();
		}
		super.update();
		float d = Game.elapsed;
		age += d;
		if (on){
			x += sx * d;
			y += sy * d;
			flown += d;
			path( progress() );
			step( d );
			if ((time -= d) <= 0){
				on = false;
				arrive();
				if (callback != null) callback.call();
				//the ward's squares live in the bolt itself: it goes when they have
				if (look != RUNE) Fx.dispose( this );
			}
		} else if (landed && look == RUNE && countLiving() == 0){
			Fx.dispose( this );
		}
	}

	//where its path lies off the straight line at a share u of its flight (offX, offY)
	private void path( float u ){
		offX = offY = 0;
		if (look == EMBER){
			//a cinder's hop
			offY = -(10f + 0.1f * dist) * 4f * u * (1f - u);
		} else if (SWAY[look] > 0){
			float s = SWAY[look] * (float) Math.sin( 2 * Math.PI * waves * u );
			offX = -dirY * s;
			offY = dirX * s;
		}
	}

	// ------------------------------------------------------------------ setting off

	//its head, its light and its launch, made on its first frame
	private void setOff(){
		float hx = x + width / 2f, hy = y + height / 2f;
		if (look == RUNE){
			ward = new BoltFx();
			ward.begin( MagicMissile.WARD, hx, hy, sx, sy, callback != null );
			if (carrier == null || !carrier.exists || carrier.parent == null) carrier = GameScene.carryLight( this );
			return;
		}
		Group scene = GameScene.layer( GameScene.Layer.LIGHTS );
		synchronized (SkillBolt.class){
			if (scene != countedIn){
				countedIn = scene;
				richLive = 0;
			}
			rich = scene != null && look != SPIRIT && richLive < RICH;
			if (rich){
				richLive++;
				counted = true;
			}
		}
		path( 0 );
		hx += offX;
		hy += offY;
		head = new FxPic( SkillVfx.SHEET );
		head.additive = look != HEX;
		head.show( headFrame() );
		head.lit( HEAD[look] );
		head.visible = false;
		if (look != HEX) GameScene.light( head );
		else if (GameScene.layer( GameScene.Layer.EFFECTS ) != null) GameScene.layer( GameScene.Layer.EFFECTS ).add( head );
		head.place( hx, hy );
		if (HEART[look] != -1){
			heart = new FxPic( SkillVfx.SHEET );
			heart.additive = true;
			heart.show( FxFrames.MOTE_1 );
			heart.lit( HEART[look] );
			heart.visible = false;
			GameScene.light( heart );
		}
		if (look == HOLY && rich){
			ghost = new FxPic( SkillVfx.SHEET );
			ghost.additive = true;
			ghost.show( FxFrames.STAR4_5 );
			ghost.lit( SkillPalette.HOLY.stop( 1 ) );
			ghost.am = 0.4f;
			ghost.visible = false;
			GameScene.light( ghost );
		}
		if (look == SPIRIT){
			glow = FxLight.glow( hx, hy, 7, GLOW[SPIRIT], GLOW_LEVEL[SPIRIT] ).follow( head, 0, 0 );
			glowSetups = glow.setups();
			return;
		}
		//its light: a glow on the head, and a pool under a rich one (later bolts' pools give way
		//when the lights are full)
		if (FxLight.room( 1, 0 )){
			glow = FxLight.glow( hx, hy, 11, GLOW[look], GLOW_LEVEL[look] ).follow( head, 0, 0 );
			glowSetups = glow.setups();
		}
		if (rich && POOL[look] != -1 && FxLight.room( 1, 0 )){
			pool = FxLight.pool( hx, hy + 6, 15, POOL[look], POOL_LEVEL ).follow( head, 0, 6 );
			poolSetups = pool.setups();
		}
		if (rich && Fx.seen( hx, hy )) launch( hx, hy );
	}

	private int[] headFrame(){
		switch (look){
			case SOUL: case MANA: return FxFrames.SQ_3;
			case HEX: return FxFrames.INK_CORE;
			case EMBER: return FxFrames.TONGUE_S[0];
			case HOLY: return FxFrames.STAR4_5;
			case HEAL: return FxFrames.PLUS_5;
			case SPARK: return FxFrames.CROSS_5;
			case ARCANE: return FxFrames.RUNE_5[0];
			default: return FxFrames.MOTE_2;
		}
	}

	//off: a bloom on the hand and three sparks of its ramp thrown the bolt's way
	private void launch( float x, float y ){
		if (FxLight.room( 1, 0 )) FxLight.glow( x, y, 11, GLOW[look], LAUNCH_GLOW ).life( 0, 0, LAUNCH_LIFE );
		float a0 = (float) Math.atan2( dirY, dirX );
		for (int i = 0; i < LAUNCH_SPARKS; i++){
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 12f ).ramp( CORE[look], true )
					.polar( a0 + FxRandom.Float( -0.45f, 0.45f ), FxRandom.Float( 40f, 70f ) ).drag( 4f )
					.life( FxRandom.Float( 0.15f, 0.25f ) ).alpha( 1f, 0, 0.1f );
		}
	}

	// ------------------------------------------------------------------ in flight

	private void step( float d ){
		float hx = x + width / 2f + offX, hy = y + height / 2f + offY;
		if (ward != null){
			ward.step( x + width / 2f, y + height / 2f, d );
			return;
		}
		if (head == null) return;
		switch (look){
			case SPARK:
				if ((jitter -= d) <= 0){
					jitter = 0.03f;
					jx = FxRandom.IntRange( -1, 1 );
					jy = FxRandom.IntRange( -1, 1 );
				}
				//ball lightning: a cross and a star swapping every frame, white and white-blue
				boolean b = ((int) (age * 60f) & 1) == 1;
				head.show( b ? FxFrames.STAR4_5 : FxFrames.CROSS_5 ).lit( b ? HEAD_ALT : HEAD[SPARK] );
				break;
			case HOLY:
				head.show( ((int) (age * 10f) & 1) == 0 ? FxFrames.STAR4_5 : FxFrames.PLUS_5 );
				break;
			case EMBER:
				head.show( FxFrames.TONGUE_S[(int) (age * 12f) % FxFrames.TONGUE_S.length] );
				hy -= 1;
				break;
			case ARCANE:
				head.show( FxFrames.RUNE_5[(int) (age * 8f) % FxFrames.RUNE_5.length] );
				break;
			case SPIRIT:
				head.lit( Ramp.lerp( HEAD[SPIRIT], 0xFFFFFF, progress() ) );
				break;
			default:
				break;
		}
		boolean seen = Fx.seen( hx, hy );
		head.place( hx + jx, hy + jy );
		head.visible = seen;
		if (heart != null){
			heart.place( hx + jx, (look == EMBER ? hy + 1 : hy) + jy );
			heart.visible = seen;
		}
		if (ghost != null){
			ghost.show( head == null ? FxFrames.STAR4_5 : (((int) (age * 10f) & 1) == 0 ? FxFrames.STAR4_5 : FxFrames.PLUS_5 ) );
			ghost.place( hx - dirX * 4, hy - dirY * 4 );
			ghost.visible = seen;
		}
		forget();
		if (rich && seen && (shedIn -= d) <= 0) shedIn = shed( hx, hy );
		if (peeled < peelN && seen) peel();
	}

	//its counts brought up to now: what died since let go of
	private void forget(){
		for (int i = 0; i < alive; ){
			if (until[i] <= age) until[i] = until[--alive];
			else i++;
		}
		for (int i = 0; i < sheds; ){
			if (shedUntil[i] <= age) shedUntil[i] = shedUntil[--sheds];
			else i++;
		}
	}

	//room for one more of its particles living `life` s (and a shed's): taken if there is
	private boolean take( float life, boolean shed ){
		if (alive >= TRAIL_MAX) return false;
		if (shed){
			if (sheds >= (look == HOLY ? SHEDS_HOLY : SHEDS)) return false;
			shedUntil[sheds++] = age + life;
		}
		until[alive++] = age + life;
		return true;
	}

	//one emit of its core: CORE_RICH stepped squares (one, lean), or the hex's ink
	void core( float x, float y ){
		forget();
		//nothing over the caster's body: the core begins once the bolt has left it
		if (flown * SPEED < CLEAR) return;
		if (look == HEX){
			int n = rich ? 2 : 1;
			for (int i = 0; i < n; i++){
				if (!take( 0.35f, false )) return;
				FxParticle p = SkillVfx.matter( x + FxRandom.Float( -1f, 1f ), y + FxRandom.Float( -1f, 1f ), rich ? FxBudget.P2 : FxBudget.P3 );
				if (p != null) p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( FxRandom.Float( 0.3f, 0.5f ) ) )
						.vel( FxRandom.Float( -2f, 2f ), FxRandom.Float( 3f, 6f ) ).turbulence( 1f, 4f ).life( 0.35f )
						.alpha( 0.75f, 0.03f, 0.2f ).smooth().source( x, y );
				else alive--;
			}
			return;
		}
		int n = rich ? CORE_RICH : 1;
		float life = !rich ? LEAN_LIFE : (look == SPARK ? CORE_LIFE_SPARK : (look == EMBER ? CORE_LIFE_EMBER : CORE_LIFE));
		for (int i = 0; i < n; i++){
			if (!take( life, false )) return;
			float px = x - dirX * CORE_BACK * i, py = y - dirY * CORE_BACK * i;
			FxParticle p = SkillVfx.glow( px, py, rich ? FxBudget.P2 : FxBudget.P3 );
			if (p == null){
				alive--;
				continue;
			}
			p.look( STEPS[look], 0 ).ramp( CORE[look], true ).life( life ).alpha( CORE_ALPHA, 0, life * 0.4f ).source( px, py );
			if (look == EMBER) p.vel( FxRandom.Float( -1.5f, 1.5f ), -FxRandom.Float( 4f, 8f ) );
			else p.vel( FxRandom.Float( -CORE_DRIFT, CORE_DRIFT ), FxRandom.Float( -CORE_DRIFT, CORE_DRIFT ) ).drag( 2f );
		}
	}

	//sheds one bit of its look; the seconds to the next
	private float shed( float x, float y ){
		FxParticle p;
		switch (look){
			case SOUL:
				if ((p = shedOne( x, y, 0.5f, FxBudget.P2 )) != null)
					p.look( FxFrames.MOTE_1 ).paint( 0xB4B4EC ).vel( 0, -FxRandom.Float( 6f, 10f ) ).sway( 9f, 6f ).life( 0.5f ).alpha( 0.9f, 0, 0.25f );
				return 0.06f;
			case MANA: {
				float s = FxRandom.sign() * 6f;
				if ((p = shedOne( x, y, 0.4f, FxBudget.P2 )) != null)
					p.look( FxFrames.MOTE_1 ).paint( SkillPalette.MANA_LIGHT ).vel( -dirY * s, dirX * s ).life( 0.4f ).alpha( 0.9f, 0, 0.2f ).snap();
				return 0.06f;
			}
			case HEX: {
				//a mote of void pulled into the head from 8 px out
				float a = FxRandom.angle();
				float mx = x + (float) Math.cos( a ) * 8f, my = y + (float) Math.sin( a ) * 8f;
				if ((p = shedOne( mx, my, 0.12f, FxBudget.P2 )) != null)
					p.look( GROW_2, 0 ).ramp( SkillPalette.VOID, true ).vel( (x - mx) / 0.12f, (y - my) / 0.12f ).life( 0.12f ).alpha( 1f, 0.02f, 0.03f );
				return TRAIL_EVERY;
			}
			case EMBER:
				if ((p = shedOne( x + FxRandom.Float( -1f, 1f ), y, 0.6f, FxBudget.P2 )) != null)
					p.look( FxFrames.EMBER_2 ).ramp( SkillPalette.FIRE, true ).vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 8f, 14f ) )
							.sway( 6f, 5f ).life( 0.6f ).alpha( 1f, 0, 0.3f );
				return 0.05f;
			case HOLY:
				if ((p = shedOne( x, y, 0.5f, FxBudget.P3 )) != null)
					p.look( FxFrames.MOTE_1 ).paint( HOLY_SHED ).vel( FxRandom.Float( -2f, 2f ), -6f ).drag( 2f ).life( 0.5f ).alpha( 1f, 0, 0.25f ).snap();
				return 0.07f;
			case HEAL:
				if ((p = shedOne( x, y, 0.5f, FxBudget.P2 )) != null)
					p.look( FxFrames.PLUS_3 ).paint( SkillPalette.HEAL_CROSS ).vel( FxRandom.Float( -2f, 2f ), -10f ).life( 0.5f ).alpha( 1f, 0, 0.25f ).snap();
				return 0.12f;
			case SPARK: {
				//crackle: a spark thrown off to one side or the other (CRACKLE alive at most)
				float s = FxRandom.sign() * FxRandom.Float( 60f, 90f );
				if (sheds < CRACKLE && (p = shedOne( x, y, 0.08f, FxBudget.P2 )) != null)
					p.look( FxFrames.SPARK_3 ).orient().ramp( SkillPalette.LIGHTNING, true ).vel( -dirY * s, dirX * s ).life( 0.08f ).alpha( 1f, 0, 0.04f );
				return 0.02f;
			}
			case ARCANE:
				if (take( RuneFX.SHED_LIFE, true )) RuneFX.shed( x, y, SkillPalette.RUNE_ARCANE );
				return 0.1f;
			default:
				return 1f;
		}
	}

	//one shed's particle of light, counted (null, and nothing counted, when there is no room)
	private FxParticle shedOne( float x, float y, float life, int priority ){
		if (!take( life, true )) return null;
		FxParticle p = SkillVfx.glow( x, y, priority );
		if (p == null){
			//given back: take() put it last in both counts
			alive--;
			sheds--;
		}
		return p;
	}

	//the peel: a mote off the body at its time, aimed where the head will be
	private void peel(){
		if (peelFrom == null || peelFrom.parent == null){
			peelN = 0;
			return;
		}
		while (peeled < peelN && flown >= PEEL_FOR * peeled / peelN){
			peeled++;
			float bx = SkillVfx.midX( peelFrom ) + FxRandom.Float( -peelFrom.width() / 4f, peelFrom.width() / 4f );
			float by = SkillVfx.midY( peelFrom ) + FxRandom.Float( -peelFrom.height() / 4f, peelFrom.height() / 4f );
			float t = Math.min( total, flown + FxRandom.Float( 0.15f, 0.25f ) );
			float lead = t - flown;
			if (lead <= 0.02f || !take( lead, false )) continue;
			path( t / Math.max( 0.0001f, total ) );
			float tx = ox + dirX * SPEED * t + offX, ty = oy + dirY * SPEED * t + offY;
			path( progress() );
			FxParticle p = SkillVfx.glow( bx, by, FxBudget.P2 );
			if (p == null){
				alive--;
				continue;
			}
			p.look( GROW_2, 0 ).ramp( MoteFX.reversed( CORE[look] ), true ).vel( (tx - bx) / lead, (ty - by) / lead ).life( lead )
					.alpha( 1f, 0.03f, 0.02f );
		}
	}

	// ------------------------------------------------------------------ landing

	//what it lands with, on the arrival frame before its callback
	private void arrive(){
		landed = true;
		float ax = x + width / 2f, ay = y + height / 2f;
		if (ward != null){
			ward.arrive( ax, ay );
			return;
		}
		letGo();
		if (!Fx.seen( ax, ay )) return;
		float scale = rich ? 1f : LEAN_IMPACT;
		float gy = ay + 6;
		switch (look){
			case SOUL:
				Arc.core( ax, ay, 0xDCDCFA );
				Arc.ring( ax, gy, 9, 17, 0.35f, 0xB4B4EC, 0.5f, true );
				Arc.motes( ax, ay, Math.max( 1, Math.round( 6 * scale ) ), 0xB4B4EC, 15f, 28f, 0.35f );
				pool( ax, gy, 15, 0x7C7AC4, 0.5f, 0.35f );
				break;
			case MANA:
				Arc.core( ax, ay, 0xD0F6FF );
				Arc.ring( ax, gy, 9, 17, 0.35f, SkillPalette.MANA_LIGHT, 0.5f, true );
				MoteFX.flick( ax, ay, Math.max( 1, Math.round( 6 * scale ) ), FxFrames.MOTE_2, SkillPalette.MANA_LIGHT, 20f, 40f, 3f, 0f,
						0.35f, 14f, FxBudget.P2 );
				pool( ax, gy, 15, 0x227AD0, 0.5f, 0.35f );
				break;
			case HEX:
				implode( ax, ay, Math.max( 4, Math.round( 12 * scale ) ) );
				if (target instanceof CharSprite) ClosingRing.on( (CharSprite) target, 23, 5, 0.25f, 0x6B4F94 );
				if (!WaterFX.onWater( ax, gy )) FxDecal.at( ax, gy, FxFrames.SPLAT[2], SkillPalette.SHADOW_POOL, 0.4f, 0.35f, 0.4f, FxRandom.chance( 0.5f ) );
				break;
			case EMBER:
				Arc.core( ax, ay, SkillPalette.FIRE_CORE );
				FxRecipes.embers( ax, ay, Element.FIRE, Math.max( 1, Math.round( 6 * scale ) ) );
				if (WaterFX.onWater( ax, gy )){
					FxRecipes.steam( ax, gy, 3 );
					WaterFX.ring( ax, gy, WaterFX.M, 0xFFC090, 0.6f );
				} else {
					FxDecal.at( ax, gy, FxFrames.SCORCH_S, SkillPalette.SCORCH, 0.7f, 2f, 1f, FxRandom.chance( 0.5f ) );
				}
				pool( ax, gy, 23, SkillPalette.FIRE_LIGHT, 0.6f, 0.4f );
				break;
			case HOLY:
				//the cleric's own Guiding Light lands
				BoltFx.impact( MagicMissile.LIGHT_MISSILE, ax, ay, scale );
				break;
			case HEAL:
				MoteFX.crosses( ax, ay, 3 );
				Arc.ring( ax, gy, 9, 17, 0.35f, SkillPalette.HEAL_LIGHT, 0.5f, true );
				pool( ax, gy, 15, SkillPalette.HEAL_LIGHT, 0.5f, 0.35f );
				break;
			case SPARK:
				Arc.core( ax, ay, 0xFFFFFF );
				FxRecipes.sparks( ax, ay, Element.ELECTRIC, Math.max( 1, Math.round( 6 * scale ) ) );
				if (FxLight.room( 1, 0 )) FxLight.glow( ax, ay, 11, SkillPalette.LIGHTNING_LIGHT, 0.6f ).life( 0, 0.04f, 0.15f );
				break;
			case ARCANE:
				Arc.core( ax, ay, 0xD8C8FF );
				Arc.ring( ax, gy, 9, 17, 0.35f, 0x9070F0, 0.6f, true );
				for (int i = 0; i < 4; i++){
					float a = (float) (i * Math.PI / 2 + Math.PI / 4);
					FxParticle p = SkillVfx.glow( Math.round( ax + 7 * (float) Math.cos( a ) ), Math.round( gy + 3.5f * (float) Math.sin( a ) ), FxBudget.P2 );
					if (p != null) p.look( FxFrames.RUNE_3[i] ).paint( 0xD8C8FF ).life( 0.3f ).alpha( 1f, 0, 0.12f ).snap();
				}
				pool( ax, gy, 23, SkillPalette.ARCANE_LIGHT, 0.6f, 0.35f );
				break;
			default:
				//the old spirit's sparks
				for (int i = 0; i < 4; i++){
					FxParticle p = SkillVfx.glow( ax, ay, FxBudget.P2 );
					if (p != null) p.look( FxFrames.SPARK_2 ).orient().ramp( SPIRIT_SPARKS, true ).polar( FxRandom.angle(), 50f * FxRandom.Float( 0.6f, 1f ) )
							.drag( 3f ).life( 0.25f ).alpha( 1f, 0, 0.12f );
				}
				break;
		}
	}

	//a pool of light on the ground fading over `life` s (none when the lights are full)
	private static void pool( float x, float gy, int size, int color, float level, float life ){
		if (FxLight.room( 1, 0 )) FxLight.pool( x, gy, size, color, level ).life( 0, 0.03f, life );
	}

	//the hex's end: motes of void pulled in from 10 px over 0.25 s and a glow of it
	private static void implode( float x, float y, int n ){
		for (int i = 0; i < n; i++){
			float a = (float) (i * 2 * Math.PI / n) + FxRandom.Float( -0.2f, 0.2f );
			float mx = x + (float) Math.cos( a ) * 10f, my = y + (float) Math.sin( a ) * 10f;
			FxParticle p = SkillVfx.glow( mx, my, FxBudget.P2 );
			if (p != null) p.look( GROW_2, 0 ).ramp( SkillPalette.VOID, true ).vel( (x - mx) / 0.25f, (y - my) / 0.25f )
					.life( 0.25f ).alpha( 1f, 0.05f, 0.05f );
		}
		if (FxLight.room( 1, 0 )) FxLight.glow( x, y, 15, SkillPalette.VOID_LIGHT, 0.6f ).life( 0, 0.1f, 0.2f );
	}

	//its head and its light let go of, and it is no longer counted rich
	private void letGo(){
		if (head != null){
			Fx.dispose( head );
			head = null;
		}
		if (heart != null){
			Fx.dispose( heart );
			heart = null;
		}
		if (ghost != null){
			Fx.dispose( ghost );
			ghost = null;
		}
		if (glow != null){
			if (glow.setups() == glowSetups) glow.putOut( 0.08f );
			glow = null;
		}
		if (pool != null){
			if (pool.setups() == poolSetups) pool.putOut( 0.08f );
			pool = null;
		}
		synchronized (SkillBolt.class){
			if (counted){
				counted = false;
				if (countedIn == GameScene.layer( GameScene.Layer.LIGHTS )) richLive = Math.max( 0, richLive - 1 );
			}
		}
	}

	@Override
	public void kill(){
		super.kill();
		letGo();
		if (ward != null){
			ward.end();
			ward = null;
		}
		if (carrier != null){
			carrier.killAndErase();
			carrier = null;
		}
	}

	// ------------------------------------------------------------------ as light

	@Override
	public void draw(){
		if (carrier != null && carrier.exists && carrier.parent != null) return;
		super.draw();
	}

	@Override
	public void drawLit(){
		super.draw();
	}

	// ------------------------------------------------------------------ the trails

	/** What each look's emit makes (index: the look): its core, through the bolt; SPIRIT's old
	 *  motes; RUNE's is the ward's own (MagicMissile.WardParticle.FACTORY, poured in reset). */
	static final Emitter.Factory[] TRAILS = new Emitter.Factory[LOOKS];
	static {
		for (int l = 0; l < LOOKS; l++){
			TRAILS[l] = new Emitter.Factory(){
				@Override
				public void emit( Emitter emitter, int index, float x, float y ){
					if (emitter instanceof SkillBolt) ((SkillBolt) emitter).core( x, y );
				}
			};
		}
		TRAILS[SPIRIT] = new Emitter.Factory(){
			@Override
			public void emit( Emitter emitter, int index, float x, float y ){
				FxParticle p = SkillVfx.glow( x, y, FxBudget.P2 );
				if (p == null) return;
				p.look( FxFrames.MOTE_1 ).ramp( SPIRIT_RAMP, true ).vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -3f, 3f ) )
						.drag( 2f ).life( 0.3f ).alpha( 0.9f, 0, 0.3f * 0.6f ).source( x, y );
			}
		};
		TRAILS[RUNE] = MagicMissile.WardParticle.FACTORY;
	}

	/** A new scene: none of the last one's rich bolts counted. */
	public static synchronized void reset(){
		richLive = 0;
		countedIn = null;
	}
}
