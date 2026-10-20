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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.utils.IntMap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * A blow as the eye takes it, on the defender's sprite, the moment it lands (Char.attack, right
 * beside the blood and the flash; drawn on the render thread, drawing nothing of the game's
 * Random): a hit star 4 px from the middle toward the attacker, white going to the creature's
 * blood in 0.08 s, and what the weapon's sound says it was -
 *
 *   SLASH  a crescent (SLASH_16, turned to the blow by its eight painted frames) sweeping in 0.12 s, 2 sparks
 *   STAB   a 1x5 puncture driven in along the blow, 2 sparks
 *   CRUSH  a ring of dust on the ground, 9 to 17 px in 0.35 s, 2 chips
 *   ARROW  2-3 splinters thrown back
 *   MAGIC  4 sparkles
 *   PARRY  4-6 metal sparks and a glint
 *   STRONG a bigger star (STAR4_7), and a 1 px shake of the camera when the hero is in it
 *          (shakes())
 *   HIT    (claws, fists, anything else) 3 quick streaks
 *
 * Every blow that lands throws a moment of light where it lands (an 11 px glow, 15 heavy, white
 * going to the blood, BLOOM_LEVEL, up at once, held a frame, gone in 0.12 s; 0.18 s heavy): what
 * reads at night, over the tint; only into room under the kit's light cap, so it never puts out a
 * light a burning body or a torch holds. A heavy slash sweeps the 24 px crescent.
 *
 * A blow of a third of the defender's health or more looks as a STRONG one besides (its star,
 * light and recoil), but shakes the camera only when it lands on the hero. The defender
 * recoils a pixel (two, heavy) away from the blow for 0.08 s, on the screen only. A blow that does
 * nothing (blocked) strikes metal sparks and a glint of a shield's arc on the side it came from;
 * a miss whooshes past the defender with a puff at its feet. At most a star and 8 quads a blow.
 *
 * An enchantment's colours (effects/fx/enchant): while a proc's stamp holds on the defender
 * (CharSprite.blowLook, till blowUntil) the star goes white to the element's light, the bloom is
 * half white, half that light, the crescent takes the element's ramp and a stab's blade and the
 * sparks the element (EnchantLook.strike: a curse's are VOID's, its bloom VOID's dim violet, no
 * white: the hex eats the light); a blow of an enchanted weapon that procs nothing (EnchantState.swingLook,
 * left by EnchantFx.swing) draws its star, crescent and blade EnchantLook.TINT of the way toward
 * the look. Only colours: not a quad more, nothing of either random.
 *
 * Inside a blow (effects/fx/combat/Blow) the star is the blow's piece (StarPiece): it leads a
 * plain blow and is drawn whole with any other lead, on the blow's frame, as it always was (a
 * crit's or a kill's lead lands it STRONG).
 */
public final class HitFx {

	private HitFx(){}

	/** What a blow was, by the weapon's sound. */
	public static final int HIT = 0, SLASH = 1, STAB = 2, CRUSH = 3, ARROW = 4, MAGIC = 5, PARRY = 6, STRONG = 7;

	/** The star's life, the crescent's, the puncture's, the dust ring's, the recoil's, s. */
	static final float STAR_FOR = 0.08f, SLASH_FOR = 0.12f, STAB_FOR = 0.1f, DUST_FOR = 0.35f, RECOIL_FOR = 0.08f;
	/** How far toward the attacker the star sits, px. */
	static final float STAR_OFF = 4f;
	/** A blow of this share of the defender's health or more lands heavy. */
	static final float HEAVY = 1 / 3f;
	/** The light a landed blow throws: its glow (heavy), its level, held and fading, s. */
	static final int BLOOM = 11, BLOOM_HEAVY = 15;
	static final float BLOOM_LEVEL = 0.7f, BLOOM_HOLD = 0.03f, BLOOM_FADE = 0.12f, BLOOM_FADE_HEAVY = 0.18f;
	/** How much of the blood the bloom takes (white the rest); of a stamp's light. */
	static final float BLOOM_BLOOD = 0.35f, STAMP_BLOOM = 0.5f;
	/** The most quads a blow makes beside its star. */
	public static final int MOST = 8;

	/** The style of a blow by the sound it makes (Assets.Sounds.HIT_*). */
	public static int style( String hitSound ){
		if (hitSound == null) return HIT;
		switch (hitSound){
			case Assets.Sounds.HIT_SLASH:  return SLASH;
			case Assets.Sounds.HIT_STAB:   return STAB;
			case Assets.Sounds.HIT_CRUSH:  return CRUSH;
			case Assets.Sounds.HIT_ARROW:  return ARROW;
			case Assets.Sounds.HIT_MAGIC:  return MAGIC;
			case Assets.Sounds.HIT_PARRY:  return PARRY;
			case Assets.Sounds.HIT_STRONG: return STRONG;
			default:                       return HIT;
		}
	}

	/** The style of an attacker's blows: the hero's by the weapon he strikes with, anyone else's a
	 *  plain HIT (as their sound is). Reads only; draws nothing of the game's Random. */
	public static int styleOf( Char attacker ){
		if (attacker instanceof Hero){
			Hero h = (Hero) attacker;
			KindOfWeapon w = h.belongings != null ? h.belongings.attackingWeapon() : null;
			return w != null ? style( w.hitSoundAsset() ) : HIT;
		}
		return HIT;
	}

	/** Whether a blow of `damage` lands heavy on a body of `ht` health. */
	public static boolean heavy( int damage, int ht ){
		return damage > 0 && ht > 0 && damage >= ht * HEAVY;
	}

	/** Whether a landed blow shakes the camera: a STRONG one with the hero in the fight, or a
	 *  heavy one on the hero. */
	static boolean shakes( int style, boolean heavy, boolean heroInvolved, boolean onHero ){
		return (style == STRONG && heroInvolved) || (heavy && onHero);
	}

	/** Which of eight 45-degree steps a direction (dx, dy) points at: 0 east, 2 south (down), 4
	 *  west, 6 north - as SLASH_16 and RIM_ARC_23 are painted. */
	public static int octant( float dx, float dy ){
		double a = Math.atan2( dy, dx );
		int k = (int)Math.round( a / (Math.PI / 4) );
		return ((k % 8) + 8) % 8;
	}

	//each crescent sweeping into its final frame from the one before, either way
	private static final int[][][] SWEEP_CW = new int[8][][], SWEEP_CCW = new int[8][][];
	private static final int[][][] WIDE_CW = new int[8][][], WIDE_CCW = new int[8][][];
	static {
		for (int k = 0; k < 8; k++){
			SWEEP_CW[k] = new int[][]{ FxFrames.SLASH_16[(k + 7) % 8], FxFrames.SLASH_16[k] };
			SWEEP_CCW[k] = new int[][]{ FxFrames.SLASH_16[(k + 1) % 8], FxFrames.SLASH_16[k] };
			WIDE_CW[k] = new int[][]{ FxFrames.SLASH_24[(k + 7) % 8], FxFrames.SLASH_24[k] };
			WIDE_CCW[k] = new int[][]{ FxFrames.SLASH_24[(k + 1) % 8], FxFrames.SLASH_24[k] };
		}
	}

	//the stars' ramps, white to each blood
	private static final IntMap<Ramp> starRamps = new IntMap<>();

	static synchronized Ramp starRamp( int blood ){
		int key = blood & 0xFFFFFF;
		Ramp r = starRamps.get( key );
		if (r == null){
			r = new Ramp( 0xFFFFFF, key );
			starRamps.put( key, r );
		}
		return r;
	}

	// ------------------------------------------------------------------ a blow

	/**
	 * A blow landing on `d` from (fromX, fromY) for `damage` (0: blocked), in a style. Any thread:
	 * the pictures are made on the render thread.
	 */
	public static void hit( final CharSprite d, final float fromX, final float fromY, final int damage,
			final int style, final boolean heroInvolved ){
		if (d == null) return;
		if (!Fx.onRenderThread()){
			Fx.post( () -> hit( d, fromX, fromY, damage, style, heroInvolved ) );
			return;
		}
		draw( d, fromX, fromY, damage, style, heroInvolved, true, false );
	}

	/**
	 * The blow's star, whole: its bloom, its style's piece and sparks; its shake only when
	 * `mayShake` (and its own rule); landing STRONG whatever its weight when `forceStrong` (a crit's
	 * or a finisher's lead: the bigger star and light, the weapon's own piece still drawn). Render
	 * thread.
	 */
	public static void draw( CharSprite d, float fromX, float fromY, int damage, int style, boolean heroInvolved,
			boolean mayShake, boolean forceStrong ){
		if (d == null || !d.visible || d.parent == null) return;
		float cx = d.x + d.width() / 2f, cy = d.y + d.height() / 2f;
		float dx = cx - fromX, dy = cy - fromY;
		float len = (float)Math.sqrt( dx * dx + dy * dy );
		if (len < 0.5f){
			dx = d.flipHorizontal ? 1f : -1f;
			dy = 0;
		} else {
			dx /= len;
			dy /= len;
		}
		if (damage <= 0){
			blocked( d, cx, cy, dx, dy );
			return;
		}
		int ht = d.ch != null ? d.ch.HT : 0;
		boolean heavy = heavy( damage, ht );
		boolean strong = style == STRONG || heavy || forceStrong;
		float sx = cx - dx * STAR_OFF, sy = cy - dy * STAR_OFF;
		//an enchantment's stamp on the blow, else the tint of an enchanted weapon's plain blow
		float now = Game.timeTotal;
		EnchantLook stamp = now < d.blowUntil ? d.blowLook : null, tint = null;
		if (stamp == null){
			EnchantState st = EnchantState.get( d );
			if (st != null && now < st.swingUntil) tint = st.swingLook;
		}
		int blood = tint != null ? Ramp.lerp( d.blood() & 0xFFFFFF, tint.strike.light, EnchantLook.TINT ) : d.blood();
		star( sx, sy, stamp != null ? stamp.star : starRamp( blood ), strong );
		bloom( sx, sy, stamp == null ? bloomColor( blood )
				: stamp.curse ? stamp.strike.light : Ramp.lerp( 0xFFFFFF, stamp.strike.light, STAMP_BLOOM ), strong );
		Ramp steel = stamp != null ? stamp.strike.ramp : tint != null ? tint.tint : Element.STEEL.ramp;
		Element sparks = stamp != null ? stamp.strike : Element.STEEL;
		int blade = stamp != null ? stamp.strike.light : tint != null ? Ramp.lerp( 0xFFFFFF, tint.strike.light, EnchantLook.TINT ) : 0xFFFFFF;
		switch (style){
			case SLASH:  slash( d, cx, cy, sx, sy, dx, dy, strong, steel, sparks ); break;
			case STAB:   stab( sx, sy, dx, dy, blade, sparks ); break;
			case CRUSH:  crush( d, cx, sx, sy ); break;
			case ARROW:  splinters( d, sx, sy, dx, dy ); break;
			case MAGIC:  sparkles( sx, sy ); break;
			case PARRY:  parry( sx, sy ); break;
			case STRONG: break;
			default:     streaks( sx, sy, dx, dy ); break;
		}
		if (mayShake && shakes( style, heavy, heroInvolved, d.ch != null && d.ch == Dungeon.hero )) Fx.shake( 1f, 0.1f );
		int m = strong ? 2 : 1;
		d.nudge( Math.round( dx * m ), Math.round( dy * m ), RECOIL_FOR );
	}

	/** A kill with no proc to finish it (a finisher lead): a puff of the body's blood colour and a
	 *  ring at its feet. Render thread. */
	public static void killPuff( CharSprite d ){
		if (d == null || !d.visible || d.parent == null) return;
		float cx = d.x + d.width() / 2f, feet = d.y + d.height() - 2f;
		FxEmitter air = Fx.air();
		if (air != null && FxBudget.keep( FxBudget.P1 )){
			CombatFactories.make( air, cx, feet - 3f ).look( FxFrames.PUFF_S[FxRandom.Int( FxFrames.PUFF_S.length )] )
					.ramp( new Ramp( d.blood() & 0xFFFFFF, Ramp.lerp( d.blood() & 0xFFFFFF, 0, 0.5f ) ), false ).smooth()
					.life( 0.4f ).alpha( 0.6f, 0, 0.25f ).vel( 0, -6f ).drag( 3f ).priority( FxBudget.P1 );
		}
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), cx, feet, 9, 17, DUST_FOR, d.blood() & 0xFFFFFF, 0.5f, false );
	}

	/**
	 * The hit star as a blow's piece (CharSprite.hitFx inside an open blow): it leads a plain blow,
	 * and draws whole with the lead of any other (a proc's stamped colours, a crit's STRONG style, a
	 * finisher's STRONG style and kill puff). A host-only picture: a guest draws its star from the
	 * blood burst it is sent.
	 */
	public static final class StarPiece extends Piece {

		final CharSprite ds;
		final float fromX, fromY;
		final int damage, style;
		final boolean hero;
		/** STRONG style (a crit or a finisher leads); a kill puff (a finisher with no proc). */
		public boolean strong, puff;

		public StarPiece( Char on, float fromX, float fromY, int damage, int style, boolean hero ){
			super( -1, Blow.PLAIN, Blow.TARGET, on );
			this.ds = on.sprite;
			this.fromX = fromX;
			this.fromY = fromY;
			this.damage = damage;
			this.style = style;
			this.hero = hero;
		}

		@Override
		public void lead( boolean mayShake ){
			draw( ds, fromX, fromY, damage, style, hero, mayShake, strong );
			if (puff) killPuff( ds );
		}

		//the presenter draws the star with the lead; on a beat it would be the same whole star
		@Override
		public void accent( float x, float y ){
			lead( false );
		}
	}

	/** A blow that missed `d`: a whoosh past it and a puff at its feet. Any thread. */
	public static void dodged( final CharSprite d, final float fromX, final float fromY ){
		if (d == null) return;
		if (!Fx.onRenderThread()){
			Fx.post( () -> dodged( d, fromX, fromY ) );
			return;
		}
		if (!d.visible || d.parent == null) return;
		float cx = d.x + d.width() / 2f, cy = d.y + d.height() / 2f;
		float dx = cx - fromX, dy = cy - fromY;
		float len = (float)Math.sqrt( dx * dx + dy * dy );
		if (len < 0.5f){
			dx = d.flipHorizontal ? 1f : -1f;
			dy = 0;
		} else {
			dx /= len;
			dy /= len;
		}
		//past it on one side: the crescent of the swing, faint
		float side = FxRandom.sign();
		float px = -dy * side, py = dx * side;
		FxEmitter glow = Fx.glowAir();
		if (glow != null){
			FxBudget.keep( FxBudget.P0 );
			CombatFactories.make( glow, cx + px * 6f + dx * 3f, cy + py * 6f + dy * 3f )
					.look( (side > 0 ? SWEEP_CW : SWEEP_CCW)[octant( dx, dy )], 0 ).paint( 0xFFFFFF ).light( true )
					.life( SLASH_FOR ).alpha( 0.4f, 0, SLASH_FOR * 0.5f ).snap().priority( FxBudget.P0 );
		}
		FxEmitter air = Fx.air();
		if (air != null && FxBudget.keep( FxBudget.P2, cx, cy )){
			float feet = d.y + d.height() - 2f;
			CombatFactories.make( air, cx - px * 2f, feet ).look( FxFrames.PUFF_S[FxRandom.Int( FxFrames.PUFF_S.length )] )
					.ramp( Element.EARTH.smoke, false ).smooth().life( 0.4f ).alpha( 0.4f, 0, 0.25f )
					.vel( -px * 8f, -3f ).drag( 3f ).priority( FxBudget.P2 );
		}
	}

	// ------------------------------------------------------------------ the pieces

	//the hit star: two frames, white to the blood (or a stamp's light), the brightest on the blow's frame
	private static void star( float x, float y, Ramp ramp, boolean strong ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		FxBudget.keep( FxBudget.P0 );
		FxParticle p = CombatFactories.make( glow, x, y );
		if (strong) p.look( FxFrames.STAR4_7 );
		else p.look( FxFrames.IMPACT_STAR, 0 );
		p.ramp( ramp, true ).life( strong ? STAR_FOR * 1.25f : STAR_FOR ).alpha( 1f, 0, STAR_FOR * 0.4f )
				.snap().priority( FxBudget.P0 );
	}

	/** The bloom's colour for a creature's blood: white, BLOOM_BLOOD of the way to it. */
	static int bloomColor( int blood ){
		return Ramp.lerp( 0xFFFFFF, blood & 0xFFFFFF, BLOOM_BLOOD );
	}

	//the light the blow throws: a glow at the star, gone in a few frames (shown only where the hero
	//sees it, fuller at night: the kit's transient light)
	private static void bloom( float x, float y, int color, boolean strong ){
		if (FxLight.live() >= FxBudget.lightCap()) return;
		FxLight.glow( Math.round( x ), Math.round( y ), strong ? BLOOM_HEAVY : BLOOM, color, BLOOM_LEVEL )
				.life( 0f, BLOOM_HOLD, strong ? BLOOM_FADE_HEAVY : BLOOM_FADE );
	}

	private static void slash( CharSprite d, float cx, float cy, float sx, float sy, float dx, float dy, boolean strong,
			Ramp steel, Element sparks ){
		FxEmitter glow = Fx.glowAir();
		if (glow != null && FxBudget.keep( FxBudget.P1 )){
			int[][] sweep = strong ? (d.flipHorizontal ? WIDE_CCW : WIDE_CW)[octant( dx, dy )]
					: (d.flipHorizontal ? SWEEP_CCW : SWEEP_CW)[octant( dx, dy )];
			CombatFactories.make( glow, cx, cy ).look( sweep, 0 ).ramp( steel, true )
					.life( SLASH_FOR ).alpha( 0.9f, 0, SLASH_FOR * 0.5f ).snap().priority( FxBudget.P1 );
		}
		sparks( sx, sy, sparks, 2 );
	}

	private static void stab( float sx, float sy, float dx, float dy, int blade, Element sparks ){
		FxEmitter glow = Fx.glowAir();
		if (glow != null && FxBudget.keep( FxBudget.P1 )){
			CombatFactories.make( glow, sx, sy ).look( FxFrames.BLADE_5 ).paint( blade ).light( true ).orient()
					.vel( dx * 40f, dy * 40f ).drag( 10f ).life( STAB_FOR ).alpha( 0.9f, 0, STAB_FOR * 0.5f ).priority( FxBudget.P1 );
		}
		sparks( sx, sy, sparks, 2 );
	}

	private static void crush( CharSprite d, float cx, float sx, float sy ){
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), cx, d.y + d.height() - 2f, 9, 17, DUST_FOR,
				Element.EARTH.ramp.stop( 1 ), 0.5f, false );
		Emitter grit = GameScene.fxEmitter();
		if (grit != null){
			grit.pos( sx, sy );
			grit.burst( FxFactories.debris( Element.EARTH.ramp.stop( 1 ), Element.EARTH.ramp.stop( 3 ) ), 2 );
		}
	}

	private static void splinters( CharSprite d, float sx, float sy, float dx, float dy ){
		FxEmitter air = Fx.air();
		if (air == null) return;
		int n = FxBudget.allow( FxRandom.IntRange( 2, 3 ), FxBudget.P2 );
		float back = (float)Math.atan2( -dy, -dx );
		for (int i = 0; i < n; i++){
			CombatFactories.make( air, sx, sy ).look( FxFrames.SPARK_2 ).paint( FxRandom.chance( 0.5f ) ? TypeFx.WOOD : TypeFx.WOOD_DARK ).orient()
					.polar( back + FxRandom.Float( -0.6f, 0.6f ) - 0.4f, FxRandom.Float( 30f, 60f ) ).gravity( 160f )
					.ground( d.y + d.height() - FxRandom.Float( 1f, 3f ), FxParticle.BOUNCE, 0.3f )
					.life( FxRandom.Float( 0.4f, 0.6f ) ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P2 );
		}
	}

	private static void sparkles( float sx, float sy ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		int n = FxBudget.allow( 4, FxBudget.P2 );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			CombatFactories.make( glow, sx, sy ).look( i % 2 == 0 ? FxFrames.PLUS_3 : FxFrames.STAR4_5 )
					.ramp( Element.ARCANE.ember, true ).light( true )
					.polar( a0 + (float)(Math.PI / 2 * i) + FxRandom.Float( -0.3f, 0.3f ), FxRandom.Float( 25f, 35f ) ).drag( 4f )
					.life( 0.3f ).alpha( 1f, 0, 0.15f ).snap().priority( FxBudget.P2 );
		}
	}

	private static void parry( float sx, float sy ){
		sparks( sx, sy, Element.METAL, FxRandom.IntRange( 4, 6 ) );
		FxEmitter glow = Fx.glowAir();
		if (glow != null && FxBudget.keep( FxBudget.P1 )){
			CombatFactories.make( glow, sx, sy - 1f ).look( FxFrames.STAR4_5 ).paint( 0xFFFFFF ).light( true )
					.life( 0.1f ).alpha( 1f, 0, 0.05f ).snap().priority( FxBudget.P1 );
		}
	}

	private static void streaks( float sx, float sy, float dx, float dy ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		int n = FxBudget.allow( 3, FxBudget.P2 );
		float a = (float)Math.atan2( dy, dx );
		for (int i = 0; i < n; i++){
			CombatFactories.make( glow, sx, sy ).look( FxFrames.SPARK_2 ).paint( 0xFFFFFF ).light( true ).orient()
					.polar( a + (i - 1) * 0.7f + FxRandom.Float( -0.2f, 0.2f ), FxRandom.Float( 40f, 60f ) ).drag( 6f )
					.life( 0.12f ).alpha( 1f, 0, 0.06f ).priority( FxBudget.P2 );
		}
	}

	private static void sparks( float x, float y, Element e, int n ){
		Emitter em = GameScene.lightEmitter();
		if (em == null) return;
		em.pos( x, y );
		em.burst( FxFactories.spark( e ), n );
	}

	//nothing got through: sparks, and a glint of a shield's arc on the side the blow came from
	private static void blocked( CharSprite d, float cx, float cy, float dx, float dy ){
		sparks( cx - dx * STAR_OFF, cy - dy * STAR_OFF, Element.METAL, 3 );
		LightFlash.at( cx, cy, FxFrames.Light.RIM_ARC_23[octant( -dx, -dy )], 0xFFFFFF, 0.5f, 0.15f );
	}
}
