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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;

import java.util.List;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Beat;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Flash;

/**
 * The looks of the gameplay pieces the rarity lines and the type perks added (docs/fx/enchant.md,
 * "New pieces"): a crit, a lifesteal, a free reload, a spared round, a heave (its charge, its
 * release, its hit and an alpha's shockwave), a scythe's sweep, a second seed, a keen throw flying
 * back and its catch, a boomerang's bites and its return's telegraph and catch, and a gun's longer
 * reach. (The enemies an Ambush blade senses through walls are SenseFx's.)
 *
 * Every entry is any thread (it hands its work to the render thread, Fx.post), draws nothing of the
 * game's Random (FxRandom only), changes nothing of the game, and is called by one line placed after
 * the gameplay it shows. Each keeps to its own cap of quads (a Beat's tally), at most one light,
 * shakes only at a strong moment with the hero in it (Fx.shake), and plays at most one sting, an
 * existing sound, only where the code plays none of its own. The host sends each to co-op guests
 * (EnchantNet PIECE: a piece kind, then its numbers), who play it from their own sprites.
 */
public final class PieceFx {

	private PieceFx(){}

	/** The pieces on the wire (EnchantNet PIECE's first number): append only, never renumbered. */
	public static final int CRIT = 0, LIFESTEAL = 1, RELOAD = 2, SPARE = 3, CHARGE = 4, RELEASE = 5, HEAVE = 6,
			SHOCKWAVE = 7, SWEEP = 8, SEED = 9, KEEN_BACK = 10, CATCH = 11, BITE = 12, TELEGRAPH = 13, REACH = 14, DISARM = 15,
			EXTRA = 16;
	/** The weapon types' pieces (TypeFx): a splinter, a felling blow, a daze, a cracked floor, a note, a
	 *  chord, a resonance, a lash's drag, a disarm, a perk's accent (TypeFx.SUNDER ..), a siphon's mana. */
	public static final int SPLINTER = 17, FELL = 18, DAZE = 19, CRACK = 20, NOTE = 21, CHORD = 22, RESONANCE = 23, LASH = 24,
			UNARM = 25, PERK = 26, SIPHON = 27;
	static final int KINDS = 28;

	/** Own quads by piece (ART 8): a crit's red X (two strokes and a glint); a lifesteal's
	 *  beads; a reload's wink, shells and ring; a spared round's ghost and glint; a charge's ring at
	 *  the hand, motes and ring at the feet (its armed mote apart); a release's ghosts (castBloom's
	 *  four motes besides); a heave's hit; a shockwave, and SHOCK_PER more for each neighbour; a
	 *  sweep, and SWEEP_PER more for each one swept; a second seed; a keen throw's ghosts home; a
	 *  catch; a rich bite (the later ones LATER_BITE); a telegraph's dots; a reach's tip. */
	public static final int CRIT_CAP = 3, LIFESTEAL_CAP = 3, RELOAD_CAP = 6, SPARE_CAP = 2, CHARGE_CAP = 8,
			RELEASE_CAP = 3, HEAVE_CAP = 14, SHOCK_CAP = 14, SHOCK_PER = 2, SWEEP_CAP = 8, SWEEP_PER = 1, SEED_CAP = 3,
			KEEN_CAP = 2, CATCH_CAP = 2, BITE_CAP = 4, LATER_BITE = 1, TELEGRAPH_CAP = 6, REACH_CAP = 2;
	/** How many neighbours, swept foes and bites get the full look; the rest less. */
	public static final int RICH = 4;

	/** How long a gold wink's two bright frames hold (a free reload's). */
	public static final float STAR_HOLD = 0.067f;
	/** A lifesteal's beads: the first one's way, and each next one's lag. */
	public static final float THREAD_FOR = 0.24f, THREAD_LAG = 0.03f;
	/** A heave's charge, s; the armed mote's turn (rev/s) and its pulse (Hz); how long a guest's or
	 *  a restaged armed mote lasts with no release. */
	public static final float CHARGE_FOR = 0.5f, ARMED_TURN = 1f, ARMED_PULSE = 0.8f, ARMED_UNHELD = 5f;
	/** How late a shockwave comes after its heave, and each neighbour's star after the last. */
	public static final float WAVE_LATE = 0.08f, WAVE_STEP = 0.03f;
	/** A sweep's way round the wielder, s. */
	public static final float SWEEP_FOR = 0.15f;
	/** How far apart a throw's bites come, s; how long a telegraph's dot lingers once the flight
	 *  home has passed it, s. */
	public static final float BITE_GAP = 0.07f, TELEGRAPH_FADE = 0.3f;
	/** The missiles' speed (MissileSprite.SPEED), px/s: a keen throw's ghosts follow it home. */
	public static final float FLIGHT_SPEED = 240f;

	/** A reach's tip: a flamethrower's, a laser's. */
	public static final int FLAME_TIP = Element.FIRE.light, BEAM_TIP = Element.PRISM.light;
	/** Brass: a spent shell. A seed's husk. */
	public static final int BRASS = 0xD8A848, HUSK = 0x9A7A3A, SPROUT = 0x9CF070, WHITE = 0xFFFFFF;

	// ------------------------------------------------------------------ looks the pieces draw in

	//a look of no enchantment, to carry a beat (its colours, its family): never sent, never in the registry
	private static EnchantLook look( Family f, Element el ){
		return new EnchantLook( -1, PieceFx.class, f, el, el.light, el.light, el.light, Motif.GLINT, Profile.STEADY, null, 0f, 1f,
				false, null, null, false, false );
	}

	static final EnchantLook GOLD = look( Family.FORTUNE, Element.GOLD ), STEEL = look( Family.CUT, Element.STEEL ),
			LIFE = look( Family.DRAIN, Element.LIFE ), NATURE = look( Family.VERDANT, Element.NATURE );
	private static final EnchantLook[] FORCE = new EnchantLook[Element.values().length];

	/** FORCE's beat in an element (a heave's: the earth, or an enchanted throw's own). */
	static synchronized EnchantLook force( Element el ){
		EnchantLook l = FORCE[el.ordinal()];
		if (l == null) l = FORCE[el.ordinal()] = look( Family.FORCE, el );
		return l;
	}

	static final Ramp WHITE_STEEL = new Ramp( WHITE, Element.STEEL.ramp.stop( 2 ) );

	static final int[][] RING_5 = { FxFrames.RING_5 }, RING_9 = { FxFrames.RING_9 };
	/** A ring closing on the hand, widest first. */
	static final int[][] CLOSING = { FxFrames.RING_17, FxFrames.RING_13, FxFrames.RING_9, FxFrames.RING_5 };

	/** A weapon's primary look (a known curse's, or an enchantment's), null for none or not a weapon. */
	static EnchantLook lookOf( Item item ){
		return item instanceof Weapon ? EnchantFx.lookOf( (Weapon) item ) : null;
	}

	static int code( EnchantLook l ){
		return l != null ? l.code : -1;
	}

	//an element a look strikes in (a curse's VOID), else the one given
	static Element strike( EnchantLook l, Element dflt ){
		return l != null ? l.strike : dflt;
	}

	// ------------------------------------------------------------------ the crit

	/**
	 * A crit (the CRIT line's, a Wild, Ambush or Keen perk's, True Edge's; WeaponPerk.crit): no word,
	 * a red X beside the damage number - the hit card's left slot, flashing white (HitCard's stamp) on
	 * the crit's beat. Inside a blow it is the blow's piece (CritPiece): it leads a plain blow (after
	 * the STRONG star, at X_AFTER s), or is an accent with a tick of its sound, and draws nothing over
	 * the struck one (the card carries it); only the first crit of the frame on a target counts.
	 * A crit with no card on its target (outside a blow) draws the X over the struck one instead
	 * (drawX): stroke A, then B over it XB_LATE s later (its rim cuts A at the crossing), a white
	 * glint at the crossing for two frames, held till X_HOLD s, then both gone by X_FOR s drifting
	 * 2 px up. Three quads, never thinned: no light, no shake, no sparks, never turned.
	 */
	public static void crit( Char a, Char d ){
		if (d == null || d.sprite == null) return;
		final CharSprite as = a != null ? a.sprite : null, ds = d.sprite;
		final boolean seen = EnchantFx.fightSeen( a, d ), hero = EnchantFx.involvesHero( a, d );
		if (Blow.offer( new CritPiece( d, as, ds, seen, hero ) )) return;
		Fx.post( () -> runCrit( as, ds, seen, hero, true ) );
	}

	static void runCrit( final CharSprite as, final CharSprite ds, boolean seen, final boolean hero, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ CRIT, id( as ), id( ds ) } );
		if (!drawable( ds )) return;
		if (seen) sting( CRIT, Assets.Sounds.HIT_STRONG, ds.ch, X_VOLUME, X_PITCH, Sting.SAME_GAP );
		//beside its number when it has a card, else over the struck one
		if (onCard( ds )) mark( ds );
		else drawX( ds, 0f );
	}

	//its target's card already carries the X (a crit of this blow, or merged into it)
	static boolean onCard( CharSprite ds ){
		EnchantState st = EnchantState.get( ds );
		return st != null && st.card != null && st.card.alive && st.card.crit();
	}

	//when the crit came on it (only the first crit of a frame counts)
	static void mark( CharSprite ds ){
		EnchantState.of( ds ).critAt = Game.timeTotal;
	}

	/** The X's red (it lights its matter frames: their rim comes out 240808), its glint's alpha. */
	public static final int CRIT_RED = 0xFF3A3A;
	public static final float GLINT_ALPHA = 0.8f;
	/** The X: B after A, held, gone, how far it drifts up as it goes (px), how high over the middle
	 *  it sits (px), the glint's two frames (s), how late it comes after a crit lead's star. */
	public static final float XB_LATE = 0.05f, X_HOLD = 0.25f, X_FOR = 0.45f, X_DRIFT = 2f, X_UP = 2f,
			GLINT_FOR = 2 / 60f, X_AFTER = 0.03f;
	/** Its sound leading, and as an accent's tick. */
	public static final float X_VOLUME = 0.45f, X_PITCH = 1.25f, X_TICK = 0.3f;

	static final int[][] X_A = { FxFrames.X_9_A }, X_B = { FxFrames.X_9_B }, GLINT = { FxFrames.PLUS_3 };

	//the red X over the struck one, `delay` s on: its three quads. Render thread
	static void drawX( CharSprite ds, float delay ){
		if (ds.parent == null) return;
		mark( ds );
		float x = ds.x + ds.width() / 2f, y = ds.y + ds.height() / 2f - X_UP;
		float drift = -X_DRIFT / (X_FOR - X_HOLD);
		Flash a = Flash.make( false, false, X_A, 0f, x, y, CRIT_RED, 1f, X_FOR );
		if (a != null) a.delay( delay ).envelope( 0f, X_HOLD ).vel( 0f, drift, X_HOLD );
		Flash g = Flash.make( false, true, GLINT, 0f, x, y, 0xFFFFFF, GLINT_ALPHA, GLINT_FOR );
		if (g != null) g.delay( delay );
		Flash b = Flash.make( false, false, X_B, 0f, x, y, CRIT_RED, 1f, X_FOR - XB_LATE );
		if (b != null) b.delay( delay + XB_LATE ).envelope( 0f, X_HOLD - XB_LATE ).vel( 0f, drift, X_HOLD - XB_LATE );
	}

	/** A crit as a blow's piece: the X (its chip is the card's left slot, not a chip). */
	static final class CritPiece extends Piece {

		final CharSprite as, ds;
		final boolean seen;

		CritPiece( Char d, CharSprite as, CharSprite ds, boolean seen, boolean hero ){
			super( CRIT, Blow.CRIT, Blow.TARGET, d );
			priority = Blow.A_CRIT;
			this.as = as;
			this.ds = ds;
			this.seen = seen;
		}

		@Override
		public void send( int serial ){
			EnchantNet.sendPiece( new int[]{ CRIT, id( as ), id( ds ) }, serial );
		}

		@Override
		public void sting(){
			if (seen && ds != null) PieceFx.sting( CRIT, Assets.Sounds.HIT_STRONG, ds.ch, X_VOLUME, X_PITCH, Sting.SAME_GAP );
		}

		@Override
		public void lead( boolean mayShake ){
			//the card's X beside the number is its picture (HitCard's stamp on this beat)
			if (drawable( ds ) && first( ds )) mark( ds );
		}

		@Override
		public void accent( float x, float y ){
			if (!drawable( ds ) || !first( ds )) return;
			if (seen) PieceFx.sting( CRIT, Assets.Sounds.HIT_STRONG, ds.ch, X_TICK, X_PITCH, Sting.SAME_GAP );
			mark( ds );
		}

		//the first crit of the frame on it (pellets in one frame draw one X)
		private static boolean first( CharSprite ds ){
			EnchantState st = EnchantState.get( ds );
			return st == null || st.critAt != Game.timeTotal;
		}
	}

	// ------------------------------------------------------------------ the lifesteal

	/** A lifesteal (the LIFESTEAL line's heal; Quality.drain): a thin thread of three beads of the
	 *  defender's blood turning to LIFE's pink, arching over the weapon side into the wielder in
	 *  0.3 s. No mist and no sting (it can be frequent): the HEALING float brings the heal's look.
	 *  Inside a blow it is the wielder lane's first beat (BEADS_AT s on), and the heal's small card
	 *  and ring wait till the last bead lands (Blow.flowBack). */
	public static void lifesteal( Char a, Char d ){
		if (a == null || d == null || a.sprite == null || d.sprite == null) return;
		final CharSprite as = a.sprite, ds = d.sprite;
		if (Blow.offer( new Lifesteal( a, as, ds ) )){
			Blow.flowBack( a, BEADS_AT + BEADS_HOME );
			return;
		}
		Fx.post( () -> runLifesteal( as, ds, true ) );
	}

	/** Inside a blow: when the beads leave (the wielder lane's first beat), and how long the last
	 *  of them takes home (THREAD_FOR + THREAD_LAG x 2). */
	public static final float BEADS_AT = 0.08f, BEADS_HOME = THREAD_FOR + THREAD_LAG * 2;

	static void runLifesteal( CharSprite as, CharSprite ds, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ LIFESTEAL, id( as ), id( ds ) } );
		drawLifesteal( as, ds );
	}

	//the thread of beads
	static void drawLifesteal( CharSprite as, CharSprite ds ){
		if (as == null || as == ds || !drawable( ds ) || as.parent == null) return;
		Beat b = beat( LIFE, as, ds, false, LIFESTEAL_CAP );
		thread( b, b.hx, b.hy, b.sx, b.sy, b.wx, b.wy - DrainFx.RISE, 3, new Ramp( ds.blood() & 0xFFFFFF, DrainFx.LIFE ) );
	}

	/** A lifesteal as a blow's piece: the wielder lane's beads (its heal is the wielder's card). */
	static final class Lifesteal extends Piece {

		final CharSprite as, ds;

		Lifesteal( Char a, CharSprite as, CharSprite ds ){
			super( LIFESTEAL, Blow.MINOR, Blow.WIELDER, a );
			this.as = as;
			this.ds = ds;
		}

		@Override
		public void send( int serial ){
			EnchantNet.sendPiece( new int[]{ LIFESTEAL, id( as ), id( ds ) }, serial );
		}

		@Override
		public void lead( boolean mayShake ){
			drawLifesteal( as, ds );
		}

		@Override
		public void accent( float x, float y ){
			drawLifesteal( as, ds );
		}
	}

	//n beads on DrainFx's arch (through the weapon side), thinner and quicker: one pixel each, the
	//first THREAD_FOR s on the way, each next THREAD_LAG s slower
	static int thread( Beat b, float x0, float y0, float x1, float y1, float vx, float vy, int n, Ramp ramp ){
		float cy = Math.min( y0, y1 ) - DrainFx.RISE;
		float u = Math.abs( x1 - x0 ) < 4f ? -1f : (vx - x0) / (x1 - x0);
		if (u >= 0.15f && u <= 0.85f) cy = (vy - (1 - u) * (1 - u) * y0 - u * u * y1) / (2 * u * (1 - u));
		int made = 0;
		for (int i = 0; i < n; i++){
			FxParticle p = b.lit( x0, y0, i == 0 ? FxBudget.P1 : FxBudget.P2 );
			if (p == null) break;
			float t = THREAD_FOR + THREAD_LAG * i;
			p.look( FxFrames.MOTE_1 ).ramp( ramp, true ).vel( (x1 - x0) / t, 2f * (cy - y0) / t )
					.gravity( 2f * (y0 - 2f * cy + y1) / (t * t) ).life( t ).alpha( 0.9f, 0.03f, 0.05f );
			made++;
		}
		return made;
	}

	// ------------------------------------------------------------------ reloads and rounds

	/** A free reload (Quality.rollFreeReload): a brass click at the weapon side - a gold star
	 *  winking two frames, three spent shells kicked up and back that bounce once, a gold ring at
	 *  the hand. */
	public static void freeReload( Char shooter ){
		if (shooter == null || shooter.sprite == null) return;
		final CharSprite ss = shooter.sprite;
		Fx.post( () -> runFreeReload( ss, true ) );
	}

	static void runFreeReload( CharSprite ss, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ RELOAD, id( ss ) } );
		if (!drawable( ss )) return;
		sting( RELOAD, Assets.Sounds.UNLOCK, ss.ch, 0.35f, 1.6f, Sting.SAME_GAP );
		float wx = handX( ss ), wy = handY( ss ), back = ss.flipHorizontal ? 1f : -1f;
		Beat b = at( GOLD, ss, wx, wy, RELOAD_CAP );
		Flash wink = b.flash( true, EnchantFx.STAR_WINK, 0f, wx, wy, Element.GOLD.light, 0.9f, 0.12f );
		if (wink != null) wink.envelope( 0f, STAR_HOLD );
		FxLight g = b.glow( wx, wy, 7, Element.GOLD.light, 0.45f );
		if (g != null) g.life( 0f, 0.02f, 0.12f );
		for (int i = 0; i < 3; i++){
			FxParticle p = b.mat( wx - back * 2f, wy - 1f, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.SPARK_2 ).paint( BRASS ).vel( back * FxRandom.Float( 16f, 32f ), -FxRandom.Float( 40f, 58f ) )
					.gravity( 240f ).ground( b.feet + FxRandom.Float( -1f, 1f ), FxParticle.BOUNCE, 0.35f ).spin( 0.06f )
					.life( 0.7f ).alpha( 1f, 0.02f * i, 0.2f );
		}
		b.flash( false, true, RING_5, 0f, wx, wy + 1f, Element.GOLD.light, 0.8f, 0.25f );
	}

	/** A spared round (Quality.rollSpareAmmo): just after the shot, a faint copy of the round flies
	 *  back from the muzzle into the shooter, and a gold glint where it lands. */
	public static void sparedRound( Weapon launcher, Char shooter ){
		if (shooter == null || shooter.sprite == null) return;
		final CharSprite ss = shooter.sprite;
		final int image = roundImage( launcher );
		Fx.post( () -> runSpare( ss, image, true ) );
	}

	/** The round a launcher fires, as an item image; -1 for a flame's or a beam's (a bead stands in). */
	static int roundImage( Weapon launcher ){
		if (launcher instanceof BowWeapon) return ItemSpriteSheet.NORMAL_ARROW;
		if (launcher instanceof MiniGun) return ItemSpriteSheet.MINI_GUN_AMMO;
		if (launcher instanceof ShootGun) return ItemSpriteSheet.SHOOT_GUN_AMMO;
		return -1;
	}

	static void runSpare( final CharSprite ss, final int image, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ SPARE, id( ss ), image } );
		if (!drawable( ss )) return;
		sting( SPARE, Assets.Sounds.DEWDROP, ss.ch, 0.3f, 1.6f, Sting.SAME_GAP );
		final Beat b = at( GOLD, ss, handX( ss ), handY( ss ), SPARE_CAP );
		EnchantFx.later( 0.08f, () -> {
			if (ss.parent == null) return;
			float face = ss.flipHorizontal ? -1f : 1f;
			float mx = handX( ss ) + face * 5f, my = handY( ss ) - 1f;
			float tx = ss.x + ss.width() / 2f, ty = ss.y + ss.height() / 2f - 1f;
			float flight = 0.3f;
			if (image >= 0){
				float dx = tx - mx, dy = ty - my;
				float angle = image == ItemSpriteSheet.NORMAL_ARROW ? flightAngle( dx, dy ) : (float)Math.toDegrees( Math.atan2( dy, dx ) );
				b.ghost( image, mx, my, angle, Element.GOLD.glint, 0.4f, flight, dx, dy );
			} else {
				FxParticle p = b.lit( mx, my, FxBudget.P1 );
				if (p != null) p.look( FxFrames.MOTE_2 ).paint( Element.GOLD.light ).vel( (tx - mx) / flight, (ty - my) / flight )
						.life( flight ).alpha( 0.6f, 0f, 0.1f );
			}
			final float gx = tx, gy = ty;
			EnchantFx.later( flight, () -> {
				FxParticle p = b.lit( gx, gy, FxBudget.P1 );
				if (p != null) p.look( FxFrames.PLUS_3 ).paint( Element.GOLD.light ).vel( 0f, -8f ).life( 0.3f ).alpha( 1f, 0f, 0.2f ).snap();
			} );
		} );
	}

	// ------------------------------------------------------------------ the heave

	/**
	 * A heave armed (MissileWeapon's HEAVE, as it adds the Heave buff): a ring closing on the hand
	 * over half a second, six motes spiralling into it, the body drawn a pixel back, a ring closing
	 * on the feet; then, while armed, one mote circling the hand and pulsing until the throw goes
	 * (release) or the heave is called off.
	 */
	public static void heaveCharge( Char hero, Item item ){
		if (hero == null || hero.sprite == null) return;
		final CharSprite hs = hero.sprite;
		final EnchantLook l = lookOf( item );
		final Char holder = hero;
		Fx.post( () -> runCharge( hs, holder, l, true ) );
	}

	static void runCharge( CharSprite hs, Char holder, EnchantLook l, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ CHARGE, id( hs ), code( l ) } );
		if (!drawable( hs )) return;
		sting( CHARGE, Assets.Sounds.CHARGEUP, hs.ch, 0.4f, 1f, Sting.SAME_GAP );
		int color = strike( l, Element.EARTH ).light;
		float wx = handX( hs ), wy = handY( hs );
		Beat b = at( force( strike( l, Element.EARTH ) ), hs, wx, wy, CHARGE_CAP );
		Flash closing = b.flash( false, true, CLOSING, 0f, wx, wy, color, 0.75f, CHARGE_FOR );
		if (closing != null) closing.turn( 90f ).envelope( 0.08f, CHARGE_FOR * 0.85f );
		for (int i = 0; i < 6; i++){
			float a = (float)(2 * Math.PI * i / 6) + FxRandom.Float( -0.3f, 0.3f ), d = FxRandom.Float( 9f, 12f );
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.7f;
			FxParticle p = b.lit( wx + ox, wy + oy, FxBudget.P1 );
			if (p == null) break;
			float t = CHARGE_FOR - 0.04f * i;
			p.look( FxFrames.MOTE_2 ).paint( color ).vel( -ox / t - oy * 1.6f, -oy / t + ox * 1.1f ).life( t )
					.alpha( 0.9f, 0.1f, 0.06f );
		}
		hs.nudge( hs.flipHorizontal ? 1 : -1, 0, CHARGE_FOR );
		if (b.take( 1 ) == 1) ClosingRing.on( hs, 17, 9, CHARGE_FOR, color );
		Armed.arm( hs, holder, color );
	}

	/**
	 * The heave let go (the heave's cell picked, right before it is thrown): the armed mote gone, a
	 * bloom at the hand, and three faint copies of the item fading along the first twelve pixels of
	 * its way.
	 */
	public static void heaveRelease( Char hero, Item item, int target ){
		if (hero == null || hero.sprite == null || item == null) return;
		final CharSprite hs = hero.sprite;
		final EnchantLook l = lookOf( item );
		final int image = item.image();
		Fx.post( () -> runRelease( hs, image, target, l, true ) );
	}

	static void runRelease( final CharSprite hs, final int image, int target, EnchantLook l, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ RELEASE, id( hs ), image, target, code( l ) } );
		Armed.release( hs );
		if (!drawable( hs )) return;
		Element el = strike( l, Element.EARTH );
		final float wx = handX( hs ), wy = handY( hs );
		FxRecipes.castBloom( wx, wy, el );
		Level lv = Dungeon.level;
		if (lv == null || target < 0 || target >= lv.length()) return;
		PointF to = DungeonTilemap.raisedTileCenterToWorld( target );
		float dx = to.x - wx, dy = to.y - wy, len = (float)Math.sqrt( dx * dx + dy * dy );
		if (len < 1f) return;
		final float ux = dx / len, uy = dy / len, angle = flightAngle( dx, dy );
		final int color = l != null ? l.light : Element.STEEL.ramp.stop( 1 );
		final Beat b = at( force( el ), hs, wx, wy, RELEASE_CAP );
		for (int k = 1; k <= 3; k++){
			final int step = k;
			EnchantFx.later( 0.03f * (k - 1), () -> b.ghost( image, wx + ux * 4f * step, wy + uy * 4f * step, angle, color,
					0.55f - 0.12f * step, 0.25f, ux * 3f, uy * 3f ) );
		}
	}

	/** A heave landing (WeaponPerk's IMPACT, past its multiplier): FORCE's strong blow at half again
	 *  its size - a wide shock ring, a crack, dust and pebbles, the body thrown back - a light, a
	 *  hard shake with the hero in it. An enchanted throw's ring takes its light. */
	public static void heaveHit( Char a, Char d, Item weapon ){
		if (d == null || d.sprite == null) return;
		final CharSprite as = a != null ? a.sprite : null, ds = d.sprite;
		final EnchantLook l = lookOf( weapon );
		final boolean seen = EnchantFx.fightSeen( a, d ), hero = EnchantFx.involvesHero( a, d );
		//inside a blow: its twist, leading or an accent (BlowPresenter)
		if (Blow.offer( new Twist( HEAVE, d, as, ds, null, l, seen, hero ) )) return;
		Fx.post( () -> runHeave( as, ds, l, seen, hero, true ) );
	}

	static void runHeave( CharSprite as, CharSprite ds, EnchantLook l, boolean seen, boolean hero, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ HEAVE, id( as ), id( ds ), code( l ) } );
		if (!drawable( ds )) return;
		if (seen) sting( HEAVE, Assets.Sounds.HIT_STRONG, ds.ch, 0.5f, 0.8f, Sting.SAME_GAP );
		drawHeave( as, ds, l, hero, Float.NaN, 0f );
	}

	//its blow, at the hit or moved to (x, y) (a blow's accent beat); its shake with the hero in it
	static void drawHeave( CharSprite as, CharSprite ds, EnchantLook l, boolean hero, float x, float y ){
		EnchantLook f = force( strike( l, Element.EARTH ) );
		Beat b = beat( f, as, ds, hero, HEAVE_CAP );
		if (!Float.isNaN( x )) b.shift( x - b.hx, y - b.hy );
		b.scale = 1.5f;
		FxLight g = b.glow( b.hx, b.hy, 31, f.light, 0.55f );
		if (g != null) g.life( 0f, 0.03f, 0.25f );
		FxParticle core = b.lit( b.hx, b.hy, FxBudget.P0 );
		if (core != null) core.look( FxFrames.IMPACT_STAR, 0 ).ramp( f.star, true ).life( 0.12f ).alpha( 1f, 0f, 0.06f ).snap();
		ForceFx.play( b );
		if (hero) Fx.shake( 1.5f, 0.15f );
	}

	/**
	 * An alpha heave's shockwave (WeaponPerk's IMPACT, past the splash): a beat after the hit, a
	 * second ring rolling out over the floor, dust, three pebbles and a crack; each enemy it struck a
	 * small star (no blood), one after another outward, the nearest RICH of them a puff of dust too and
	 * a pixel's push; one shake with the hero in it. An enchanted throw's ring takes its light.
	 */
	public static void shockwave( Char a, Char d, Item weapon, List<Char> shaken ){
		if (d == null || d.sprite == null || shaken == null) return;
		final CharSprite as = a != null ? a.sprite : null, ds = d.sprite;
		final CharSprite[] around = sprites( shaken );
		final EnchantLook l = lookOf( weapon );
		final boolean seen = EnchantFx.fightSeen( a, d ), hero = EnchantFx.involvesHero( a, d );
		if (Blow.offer( new Twist( SHOCKWAVE, d, as, ds, around, l, seen, hero ) )) return;
		Fx.post( () -> runShockwave( as, ds, around, l, seen, hero, true ) );
	}

	static void runShockwave( final CharSprite as, final CharSprite ds, final CharSprite[] around, final EnchantLook l,
			final boolean seen, final boolean hero, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( withIds( new int[]{ SHOCKWAVE, id( as ), id( ds ), code( l ) }, around ) );
		if (!drawable( ds )) return;
		EnchantFx.later( WAVE_LATE, () -> drawShockwave( as, ds, around, l, seen, hero ) );
	}

	//the wave a beat after its heave
	private static void drawShockwave( CharSprite as, CharSprite ds, CharSprite[] around, EnchantLook l, boolean seen,
			boolean hero ){
		if (ds.parent == null) return;
		if (seen) sting( SHOCKWAVE, Assets.Sounds.ROCKS_LIGHT, ds.ch, 0.4f, 1f, Sting.SAME_GAP );
		EnchantLook f = force( strike( l, Element.EARTH ) );
		Beat b = beat( f, as, ds, hero, SHOCK_CAP + SHOCK_PER * around.length );
		b.ring( b.cx, b.feet, 13, 41, 0.45f, f.light, 0.7f, f.emissive );
		if (b.onWater( b.cx, b.feet )){
			if (b.takeAll( 2 )) WaterFX.ring( b.cx, b.feet, WaterFX.L );
		} else {
			b.decal( b.cx, b.feet + 1f, FxFrames.CRACK_17[1], ForceFx.CRACK, 0.6f, 2.5f, 1f );
		}
		if (b.takeAll( 4 )) FxRecipes.dust( b.cx, b.feet - 1f, 2 );
		for (int i = 0; i < 3; i++) ForceFx.pebble( b, b.cx + (i - 1) * 4f, b.feet - 3f );
		//outward: the nearest first
		order( around, b.cx, b.cy );
		for (int i = 0; i < around.length; i++){
			final CharSprite n = around[i];
			if (n == null) continue;
			final boolean rich = i < RICH;
			final Beat share = b.split( rich ? SHOCK_PER : 1 );
			final float ox = b.cx, oy = b.cy;
			EnchantFx.later( WAVE_STEP * i, () -> {
				if (n.parent == null || !n.visible) return;
				float nx = n.x + n.width() / 2f, ny = n.y + n.height() / 2f;
				FxParticle p = share.lit( nx, ny, FxBudget.P1 );
				if (p != null) p.look( FxFrames.IMPACT_STAR, 0 ).ramp( share.look.star, true ).life( 0.1f ).alpha( 0.9f, 0f, 0.05f ).snap();
				if (rich){
					share.feet = n.y + n.height() - 2f;
					ForceFx.dust( share, nx, share.feet - 1f );
					n.nudge( Math.round( Math.signum( nx - ox ) ), Math.round( Math.signum( ny - oy ) ), 0.08f );
				}
			} );
		}
		if (hero) Fx.shake( 1f, 0.1f );
	}

	// ------------------------------------------------------------------ the sweep

	/**
	 * A scythe's sweep (WeaponPerk's SCYTHE, past the sweep): a wide crescent sweeping round the
	 * wielder through the octants it touched in 0.15 s, each frame lit as it passes and a fainter
	 * copy trailing it; a small star on each swept foe as the blade reaches it (the first RICH also
	 * pushed a pixel), the enchantment's tint on both; three blades of grass flung from where grass
	 * stands.
	 */
	public static void sweep( Char a, Char d, Item weapon, List<Char> swept ){
		if (a == null || a.sprite == null || swept == null || swept.isEmpty()) return;
		final CharSprite as = a.sprite, ds = d != null ? d.sprite : null;
		final CharSprite[] around = sprites( swept );
		final EnchantLook l = lookOf( weapon );
		final boolean seen = EnchantFx.fightSeen( a, d );
		if (Blow.offer( new Twist( SWEEP, d != null ? d : a, as, ds, around, l, seen, false ) )) return;
		Fx.post( () -> runSweep( as, ds, around, l, seen, true ) );
	}

	static void runSweep( final CharSprite as, CharSprite ds, CharSprite[] around, EnchantLook l, boolean seen, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( withIds( new int[]{ SWEEP, id( as ), id( ds ), code( l ) }, around ) );
		if (!drawable( as )) return;
		if (seen) sting( SWEEP, Assets.Sounds.HIT_SLASH, as.ch, 0.35f, 0.8f, Sting.SAME_GAP );
		drawSweep( as, ds, around, l );
	}

	//the sweep: its blade round the wielder, a star on each swept, grass flung
	static void drawSweep( final CharSprite as, CharSprite ds, CharSprite[] around, EnchantLook l ){
		final float cx = as.x + as.width() / 2f, cy = as.y + as.height() / 2f;
		//the octants it touches: the one it struck, and each swept
		int mask = 0;
		if (ds != null) mask |= 1 << octantOf( cx, cy, ds );
		for (CharSprite n : around) if (n != null) mask |= 1 << octantOf( cx, cy, n );
		if (mask == 0) return;
		int[] way = sweepWay( mask );
		int span = way[1];
		int[][] run = new int[span][];
		//SLASH_24's frame k has its arc on side k: round the wielder, through each touched side
		for (int j = 0; j < span; j++) run[j] = FxFrames.SLASH_24[(way[0] + j) % 8];
		final Ramp ramp = l != null ? l.tint : Element.STEEL.ramp;
		final Ramp star = l != null ? l.star : WHITE_STEEL;
		final Beat b = at( STEEL, as, cx, cy, SWEEP_CAP + SWEEP_PER * around.length );
		FxParticle blade = b.lit( cx, cy, FxBudget.P0 );
		if (blade != null) blade.look( run, 0 ).ramp( ramp, true ).life( SWEEP_FOR ).alpha( 0.95f, 0f, SWEEP_FOR * 0.35f ).snap();
		final int[][] trail = run;
		final Beat after = b.split( 1 );
		EnchantFx.later( SWEEP_FOR / Math.max( 2, span ), () -> {
			FxParticle p = after.lit( cx, cy, FxBudget.P2 );
			if (p != null) p.look( trail, 0 ).ramp( ramp, true ).life( SWEEP_FOR ).alpha( 0.4f, 0f, SWEEP_FOR * 0.5f ).snap();
		} );
		//a star on each swept foe as the blade passes its octant; the first RICH also pushed a pixel
		//away from the wielder
		int stars = 0;
		for (CharSprite n : around){
			if (n == null) continue;
			int k = octantOf( cx, cy, n );
			int step = ((k - way[0]) % 8 + 8) % 8;
			final CharSprite foe = n;
			final Beat share = b.split( SWEEP_PER );
			final boolean rich = stars++ < RICH;
			EnchantFx.later( SWEEP_FOR * step / Math.max( 1, span ), () -> {
				if (foe.parent == null || !foe.visible) return;
				float fx = foe.x + foe.width() / 2f, fy = foe.y + foe.height() / 2f;
				FxParticle p = share.lit( fx, fy, FxBudget.P1 );
				if (p != null) p.look( FxFrames.IMPACT_STAR, 0 ).ramp( star, true ).life( 0.09f ).alpha( 0.85f, 0f, 0.04f ).snap();
				if (rich) foe.nudge( Math.round( Math.signum( fx - cx ) ), Math.round( Math.signum( fy - cy ) ), 0.08f );
			} );
		}
		//three blades of grass where grass stands under the touched
		Level lv = Dungeon.level;
		if (lv == null) return;
		int blades = 0;
		for (int i = -1; i < around.length && blades < 3; i++){
			CharSprite n = i < 0 ? ds : around[i];
			if (n == null || n.ch == null || !grassy( lv, n.ch.pos )) continue;
			float nx = n.x + n.width() / 2f, feet = n.y + n.height() - 2f;
			float out = Math.signum( nx - cx );
			for (int k = 0; k < 2 && blades < 3; k++, blades++){
				FxParticle p = b.mat( nx + FxRandom.Float( -3f, 3f ), feet - 1f, FxBudget.P2 );
				if (p == null) return;
				p.look( FxRandom.chance( 0.5f ) ? FxFrames.BLADE_3 : FxFrames.BLADE_4 ).paint( Element.NATURE.ramp.stop( 2 + FxRandom.Int( 2 ) ) )
						.vel( out * FxRandom.Float( 10f, 24f ) + FxRandom.Float( -6f, 6f ), -FxRandom.Float( 26f, 40f ) ).gravity( 160f )
						.spin( 0.08f ).life( 0.55f ).alpha( 1f, 0f, 0.2f );
			}
		}
	}

	static boolean grassy( Level lv, int cell ){
		if (cell < 0 || cell >= lv.length()) return false;
		int t = lv.map[cell];
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	/** The way a sweep takes round the touched octants (a bit each): its first octant and how many
	 *  it passes, going clockwise and leaving out the widest stretch it never touched. */
	static int[] sweepWay( int mask ){
		int bits = Integer.bitCount( mask & 0xFF );
		if (bits <= 1) return new int[]{ Integer.numberOfTrailingZeros( mask ), 1 };
		//the widest run of untouched octants: the sweep starts after it and ends before it
		int bestStart = 0, bestLen = -1;
		for (int s = 0; s < 8; s++){
			if ((mask & (1 << s)) != 0) continue;
			if ((mask & (1 << ((s + 7) % 8))) == 0) continue;
			int len = 0;
			while (len < 8 && (mask & (1 << ((s + len) % 8))) == 0) len++;
			if (len > bestLen){
				bestLen = len;
				bestStart = s;
			}
		}
		if (bestLen < 0) return new int[]{ 0, 8 };
		int first = (bestStart + bestLen) % 8;
		return new int[]{ first, 8 - bestLen };
	}

	private static int octantOf( float cx, float cy, CharSprite n ){
		return HitFx.octant( n.x + n.width() / 2f - cx, n.y + n.height() / 2f - cy );
	}

	// ------------------------------------------------------------------ the second seed

	/** A second seed (Quality.secondSeed: a reaper's cut crop or tuft of tall grass): two seeds
	 *  popping out of the cell and bouncing once, and a green glint. The harvest's own leaves and
	 *  its sound stay as they are. */
	public static void secondSeed( Char reaper, int cell ){
		Level lv = Dungeon.level;
		if (lv == null || cell < 0 || cell >= lv.length()) return;
		final boolean seen = lv.heroFOV != null && cell < lv.heroFOV.length && lv.heroFOV[cell];
		Fx.post( () -> runSeed( reaper, cell, seen, true ) );
	}

	static void runSeed( Char reaper, int cell, boolean seen, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ SEED, cell } );
		if (!seen || Fx.glowAir() == null) return;
		sting( SEED, Assets.Sounds.PLANT, reaper, 0.25f, 1.5f, Sting.SAME_GAP );
		PointF c = DungeonTilemap.tileCenterToWorld( cell );
		Beat b = Beat.at( NATURE, c.x, c.y, SEED_CAP );
		for (int i = 0; i < 2; i++){
			float side = i == 0 ? -1f : 1f;
			FxParticle p = b.mat( c.x + side, c.y + 2f, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.SEED ).paint( HUSK ).vel( side * FxRandom.Float( 10f, 18f ), -FxRandom.Float( 40f, 52f ) ).gravity( 160f )
					.ground( c.y + 5f, FxParticle.BOUNCE, 0.4f ).life( 0.8f ).alpha( 1f, 0f, 0.25f ).snap();
		}
		FxParticle g = b.lit( c.x, c.y - 2f, FxBudget.P1 );
		if (g != null) g.look( FxFrames.PLUS_3 ).paint( SPROUT ).vel( 0f, -8f ).life( 0.35f ).alpha( 0.9f, 0.05f, 0.2f ).snap();
	}

	// ------------------------------------------------------------------ an extra yield

	/** An extra yield's look: its ghost's gold, how far it rises out of the cell (px) and over how
	 *  long (s), its first alpha; its sound's volume. */
	public static final int EXTRA_GOLD = 0xFFD24A;
	public static final float EXTRA_RISE = 8f, EXTRA_FOR = 0.45f, EXTRA_ALPHA = 0.6f, EXTRA_VOLUME = 0.3f;
	public static final int EXTRA_CAP = 2;

	/**
	 * An extra yield (an extra log, stone or ore; a hunter's extra meat): a gold ghost of the item
	 * rising EXTRA_RISE px out of the cell over EXTRA_FOR s from EXTRA_ALPHA, a gold glint where it
	 * starts, ITEM at EXTRA_VOLUME and `pitch` (wood 1.3, stone and ore 1.2) - the second seed's grammar
	 * in gold. Only where the hero sees the cell; never a blow's piece (it plays at a cell). Any
	 * thread.
	 */
	public static void extra( int cell, Item item, float pitch ){
		Level lv = Dungeon.level;
		if (item == null || lv == null || cell < 0 || cell >= lv.length()) return;
		final boolean seen = lv.heroFOV != null && cell < lv.heroFOV.length && lv.heroFOV[cell];
		final int image = item.image();
		Fx.post( () -> runExtra( cell, image, pitch, seen, true ) );
	}

	/** The glint alone (a hunter's meat: the heap's own drop shows the item), silent. Any thread. */
	public static void extraGlint( int cell ){
		Level lv = Dungeon.level;
		if (lv == null || cell < 0 || cell >= lv.length()) return;
		final boolean seen = lv.heroFOV != null && cell < lv.heroFOV.length && lv.heroFOV[cell];
		Fx.post( () -> runExtra( cell, -1, 1f, seen, true ) );
	}

	//its ghost (an image of -1: the glint alone) and glint, the sound with the ghost
	static void runExtra( int cell, int image, float pitch, boolean seen, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ EXTRA, cell, image, Math.round( pitch * 1000f ) } );
		if (!seen || Fx.glowAir() == null) return;
		PointF c = DungeonTilemap.tileCenterToWorld( cell );
		Beat b = Beat.at( GOLD, c.x, c.y, EXTRA_CAP );
		if (image >= 0){
			float now = Game.timeTotal, last = stungAt[EXTRA];
			if (last < 0 || now < last || now - last >= Sting.SAME_GAP){
				stungAt[EXTRA] = now;
				SpatialSound.play( Assets.Sounds.ITEM, cell, EXTRA_VOLUME, pitch );
			}
			b.ghost( image, c.x, c.y, 0f, EXTRA_GOLD, EXTRA_ALPHA, EXTRA_FOR, 0f, -EXTRA_RISE );
		}
		FxParticle g = b.lit( c.x, c.y + 2f, FxBudget.P1 );
		if (g != null) g.look( FxFrames.PLUS_3 ).paint( EXTRA_GOLD ).vel( 0f, -8f ).life( 0.35f ).alpha( 0.9f, 0.05f, 0.2f ).snap();
	}

	// ------------------------------------------------------------------ throws coming home

	/** A keen throw flying back from `from` (MissileWeapon's return flight, as it starts): a soft
	 *  whoosh, and two faint copies of it shed along the way as it passes. */
	public static void keenBack( Char thrower, int from, Item item ){
		if (thrower == null || thrower.sprite == null || item == null) return;
		final CharSprite ts = thrower.sprite;
		final int image = item.image();
		final EnchantLook l = lookOf( item );
		Fx.post( () -> runKeenBack( ts, from, image, l, true ) );
	}

	static void runKeenBack( CharSprite ts, int from, int image, EnchantLook l, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ KEEN_BACK, id( ts ), from, image, code( l ) } );
		Level lv = Dungeon.level;
		if (!drawable( ts ) || lv == null || from < 0 || from >= lv.length()) return;
		sting( KEEN_BACK, Assets.Sounds.MISS, ts.ch, 0.2f, 1.3f, Sting.SAME_GAP );
		PointF f = DungeonTilemap.raisedTileCenterToWorld( from );
		final float fx = f.x, fy = f.y, tx = ts.x + ts.width() / 2f, ty = ts.y + ts.height() / 2f;
		float dx = tx - fx, dy = ty - fy, len = (float)Math.sqrt( dx * dx + dy * dy );
		if (len < 1f) return;
		final float ux = dx / len, uy = dy / len, angle = flightAngle( dx, dy ), flight = len / FLIGHT_SPEED;
		final int color = l != null ? l.light : EnchantLook.GHOST;
		final Beat b = Beat.at( GOLD, fx, fy, KEEN_CAP );
		for (int k = 1; k <= 2; k++){
			final float share = k / 3f;
			EnchantFx.later( flight * share, () -> b.ghost( image, fx + (tx - fx) * share, fy + (ty - fy) * share, angle, color,
					0.35f, 0.22f, ux * 5f, uy * 5f ) );
		}
	}

	/** A throw caught (a keen throw home, a boomerang back in the hand): a ring at the hand and a
	 *  glint rising off it. Silent: a boomerang's pickup plays its own sound. */
	public static void caught( Char thrower ){
		if (thrower == null || thrower.sprite == null) return;
		final CharSprite ts = thrower.sprite;
		Fx.post( () -> runCatch( ts, true ) );
	}

	static void runCatch( CharSprite ts, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ CATCH, id( ts ) } );
		if (!drawable( ts )) return;
		float wx = handX( ts ), wy = handY( ts );
		Beat b = at( GOLD, ts, wx, wy, CATCH_CAP );
		b.flash( false, true, RING_9, 0f, wx, wy + 1f, Element.GOLD.glint, 0.7f, 0.25f );
		FxParticle p = b.lit( wx, wy - 1f, FxBudget.P1 );
		if (p != null) p.look( FxFrames.PLUS_3 ).paint( Element.GOLD.glint ).vel( 0f, -10f ).life( 0.3f ).alpha( 1f, 0f, 0.2f ).snap();
	}

	/**
	 * A boomerang's bite (each foe an Arc throw flies through, and each it strikes on the way back),
	 * `index` the how-manieth of its flight: a star, three sparks and a pixel's push for the first
	 * RICH, the star alone after; each a little after the last, ringing higher each time.
	 */
	public static void bite( Char thrower, Char d, int index, Item item ){
		if (d == null || d.sprite == null) return;
		final CharSprite ts = thrower != null ? thrower.sprite : null, ds = d.sprite;
		final EnchantLook l = lookOf( item );
		final boolean seen = EnchantFx.fightSeen( thrower, d );
		Fx.post( () -> runBite( ts, ds, index, l, seen, true ) );
	}

	static void runBite( final CharSprite ts, final CharSprite ds, final int index, final EnchantLook l, final boolean seen, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ BITE, id( ts ), id( ds ), index, code( l ) } );
		if (!drawable( ds )) return;
		final boolean rich = index < RICH;
		final Beat b = beat( STEEL, ts, ds, false, rich ? BITE_CAP : LATER_BITE );
		EnchantFx.later( BITE_GAP * Math.max( 0, index ), () -> {
			if (ds.parent == null) return;
			//no pitch spread: each bite rings a step higher than the last, and a spread would blur the steps
			if (seen) sting( BITE, Assets.Sounds.HIT_PARRY, ds.ch, 0.4f, Math.min( 2f, 1f + 0.1f * index ), 0.04f, 0f );
			FxParticle star = b.lit( b.cx, b.cy, FxBudget.P0 );
			if (star != null) star.look( FxFrames.IMPACT_STAR, 0 ).ramp( l != null ? l.star : WHITE_STEEL, true ).life( 0.1f )
					.alpha( 1f, 0f, 0.05f ).snap();
			if (!rich) return;
			int sparks = b.burst( 3, FxBudget.P2 );
			float base = (float)Math.atan2( b.dy, b.dx );
			for (int i = 0; i < sparks; i++){
				FxParticle p = b.lit( b.cx, b.cy, FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.SPARK_2 ).ramp( l != null ? l.star : WHITE_STEEL, true ).orient()
						.polar( base + FxRandom.Float( -0.9f, 0.9f ), FxRandom.Float( 45f, 70f ) ).gravity( 120f ).drag( 2f )
						.life( FxRandom.Float( 0.18f, 0.26f ) ).alpha( 1f, 0f, 0.1f );
			}
			ds.nudge( Math.round( b.dx ), Math.round( b.dy ), 0.08f );
		} );
	}

	/** A boomerang's return telegraphed (its CircleBack's flight home, as it starts): a faint
	 *  dotted line from where it lay (`from`) to the cell it flies back to (`to`, the cell it was
	 *  thrown from, wherever the thrower stands now), all lit at once ahead of it, each dot fading
	 *  as the flight passes it. */
	public static void telegraph( int from, int to, Char thrower, Item item ){
		if (thrower == null || thrower.sprite == null) return;
		final CharSprite ts = thrower.sprite;
		final EnchantLook l = lookOf( item );
		Fx.post( () -> runTelegraph( from, to, ts, l, true ) );
	}

	/** `to` -1: toward the thrower's sprite (an older host sends no cell). */
	static void runTelegraph( int from, int to, CharSprite ts, EnchantLook l, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ TELEGRAPH, id( ts ), from, code( l ), to } );
		Level lv = Dungeon.level;
		if (!drawable( ts ) || lv == null || from < 0 || from >= lv.length() || to >= lv.length()) return;
		PointF f = DungeonTilemap.raisedTileCenterToWorld( from );
		float tx, ty;
		if (to >= 0){
			PointF t = DungeonTilemap.raisedTileCenterToWorld( to );
			tx = t.x;
			ty = t.y;
		} else {
			tx = ts.x + ts.width() / 2f;
			ty = ts.y + ts.height() / 2f;
		}
		float dx = tx - f.x, dy = ty - f.y, len = (float)Math.sqrt( dx * dx + dy * dy );
		if (len < 1f) return;
		//the flight home takes len / FLIGHT_SPEED s (MissileSprite): each dot holds until it is passed
		float flight = len / FLIGHT_SPEED;
		int n = Math.max( 2, Math.min( TELEGRAPH_CAP, Math.round( len / 10f ) ) );
		int color = l != null ? l.light : EnchantLook.GHOST;
		Beat b = Beat.at( GOLD, f.x, f.y, TELEGRAPH_CAP );
		for (int i = 0; i < n; i++){
			float share = (i + 0.5f) / n;
			FxParticle p = b.lit( f.x + dx * share, f.y + dy * share, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).paint( color ).life( flight * share + TELEGRAPH_FADE ).alpha( 0.5f, 0.03f, TELEGRAPH_FADE ).snap();
		}
	}

	// ------------------------------------------------------------------ a gun's reach

	/** A flamethrower's cone or a laser's beam reaching past its own range (the RANGE line), on the
	 *  shot's frame: a faint ring and a mote on the tile at its far end. Nothing when the line adds
	 *  nothing or the shot ends short of its own range. */
	public static void reach( Char shooter, Weapon gun, Ballistica aim, int base, int maxDist ){
		Level lv = Dungeon.level;
		if (shooter == null || aim == null || lv == null || maxDist <= base) return;
		int end = Math.min( Math.min( aim.dist, maxDist ), aim.path.size() - 1 );
		if (gun instanceof FT){
			//a flame's cone (ConeAOE) reaches maxDist in true distance, half a tile past each cell's
			//middle, and its rays (STOP_SOLID | IGNORE_SOFT_SOLID) stop short of the first cell that is
			//neither passable nor avoided: on through a closed door, short of a wall (a laser's beam
			//burns on through, step by step)
			int src = aim.path.get( 0 ), last = Math.min( aim.dist, aim.path.size() - 1 );
			end = 0;
			for (int i = 1; i <= last; i++){
				int c = aim.path.get( i );
				if (c < 0 || c >= lv.length() || !lv.passable[c] && !lv.avoid[c] || lv.trueDistance( src, c ) > maxDist + 0.5f) break;
				end = i;
			}
			if (lv.trueDistance( src, aim.path.get( end ) ) <= base + 0.5f) return;
		} else if (end <= base) return;
		int cell = aim.path.get( end );
		final CharSprite ss = shooter.sprite;
		final int color = gun instanceof FT ? FLAME_TIP : BEAM_TIP;
		final boolean seen = lv != null && lv.heroFOV != null && cell >= 0 && cell < lv.heroFOV.length && lv.heroFOV[cell];
		Fx.post( () -> runReach( ss, cell, color, seen, true ) );
	}

	static void runReach( CharSprite ss, int cell, int color, boolean seen, boolean send ){
		if (send && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ REACH, id( ss ), cell, color } );
		if (!seen || Fx.glowAir() == null) return;
		PointF c = DungeonTilemap.tileCenterToWorld( cell );
		Beat b = Beat.at( GOLD, c.x, c.y, REACH_CAP );
		b.flash( false, true, RING_5, 0f, c.x, c.y + 3f, color, 0.55f, 0.35f );
		FxParticle p = b.lit( c.x, c.y, FxBudget.P2 );
		if (p != null) p.look( FxFrames.MOTE_2 ).paint( color ).vel( 0f, -6f ).life( 0.3f ).alpha( 0.6f, 0f, 0.2f ).snap();
	}

	// ------------------------------------------------------------------ inside a blow

	/**
	 * A perk's big moment handed to the open blow (a heave landing, its shockwave, a scythe's
	 * sweep): it leads a blow no proc leads; otherwise it plays on its accent's beat. Whole either
	 * way, with its sound (a heave's blow moved to the beat's point; a shockwave and a sweep keep
	 * their own places round the struck and the wielder).
	 */
	static final class Twist extends Piece {

		final CharSprite as, ds;
		final CharSprite[] around;
		final EnchantLook l;
		final boolean seen, hero;

		Twist( int kind, Char on, CharSprite as, CharSprite ds, CharSprite[] around, EnchantLook l, boolean seen, boolean hero ){
			super( kind, Blow.TWIST, kind == HEAVE ? Blow.TARGET : Blow.PLACED, on );
			priority = Blow.A_TWIST;
			this.as = as;
			this.ds = ds;
			this.around = around;
			this.l = l;
			this.seen = seen;
			this.hero = hero;
		}

		@Override
		public void send( int serial ){
			switch (kind){
				case HEAVE:     EnchantNet.sendPiece( new int[]{ HEAVE, id( as ), id( ds ), code( l ) }, serial ); break;
				case SHOCKWAVE: EnchantNet.sendPiece( withIds( new int[]{ SHOCKWAVE, id( as ), id( ds ), code( l ) }, around ), serial ); break;
				default:        EnchantNet.sendPiece( withIds( new int[]{ SWEEP, id( as ), id( ds ), code( l ) }, around ), serial ); break;
			}
		}

		@Override
		public void sting(){
			if (!seen) return;
			//the shockwave's rings a beat late, with its wave
			if (kind == HEAVE && ds != null) PieceFx.sting( HEAVE, Assets.Sounds.HIT_STRONG, ds.ch, 0.5f, 0.8f, Sting.SAME_GAP );
			else if (kind == SWEEP && as != null) PieceFx.sting( SWEEP, Assets.Sounds.HIT_SLASH, as.ch, 0.35f, 0.8f, Sting.SAME_GAP );
		}

		@Override
		public void lead( boolean mayShake ){
			final boolean shake = hero && mayShake;
			//a shockwave leading still rolls out a beat after its blow, as it always did
			if (kind == SHOCKWAVE) EnchantFx.later( WAVE_LATE, () -> draw( shake, Float.NaN, 0f ) );
			else draw( shake, Float.NaN, 0f );
		}

		@Override
		public void accent( float x, float y ){
			sting();
			draw( false, x, y );
		}

		//its whole look: a heave's blow (at the hit, or at (x, y)), a shockwave, a sweep
		private void draw( boolean shake, float x, float y ){
			switch (kind){
				case HEAVE:
					if (drawable( ds )) drawHeave( as, ds, l, shake, x, y );
					break;
				case SHOCKWAVE:
					if (drawable( ds )) drawShockwave( as, ds, around, l, seen, shake );
					break;
				default:
					if (drawable( as )) drawSweep( as, ds, around, l );
					break;
			}
		}
	}

	// ------------------------------------------------------------------ guests

	/** What the host sent (EnchantNet PIECE), played from the guest's own sprites; a short payload,
	 *  an unknown piece or a missing sprite plays nothing. */
	static void replay( final int[] n, EnchantNet.Sprites sprites ){
		if (n.length < 2) return;
		switch (n[0]){
			case CRIT: {
				if (n.length < 3) return;
				final CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (ds == null) return;
				Fx.post( () -> runCrit( as, ds, ds.visible, heroIn( as, ds ), false ) );
				break;
			}
			case LIFESTEAL: {
				if (n.length < 3) return;
				final CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (as == null || ds == null) return;
				Fx.post( () -> runLifesteal( as, ds, false ) );
				break;
			}
			case RELOAD: {
				final CharSprite ss = sprite( sprites, n[1] );
				if (ss != null) Fx.post( () -> runFreeReload( ss, false ) );
				break;
			}
			case SPARE: {
				if (n.length < 3) return;
				final CharSprite ss = sprite( sprites, n[1] );
				if (ss != null) Fx.post( () -> runSpare( ss, n[2], false ) );
				break;
			}
			case CHARGE: {
				if (n.length < 3) return;
				final CharSprite hs = sprite( sprites, n[1] );
				if (hs != null) Fx.post( () -> runCharge( hs, null, EnchantLooks.byCode( n[2] ), false ) );
				break;
			}
			case RELEASE: {
				if (n.length < 5) return;
				final CharSprite hs = sprite( sprites, n[1] );
				if (hs != null) Fx.post( () -> runRelease( hs, n[2], n[3], EnchantLooks.byCode( n[4] ), false ) );
				break;
			}
			case HEAVE: {
				if (n.length < 4) return;
				final CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (ds != null) Fx.post( () -> runHeave( as, ds, EnchantLooks.byCode( n[3] ), ds.visible, heroIn( as, ds ), false ) );
				break;
			}
			case SHOCKWAVE: {
				if (n.length < 4) return;
				final CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				final CharSprite[] around = sprites( sprites, n, 4 );
				if (ds != null) Fx.post( () -> runShockwave( as, ds, around, EnchantLooks.byCode( n[3] ), ds.visible,
						heroIn( as, ds ), false ) );
				break;
			}
			case SWEEP: {
				if (n.length < 4) return;
				final CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				final CharSprite[] around = sprites( sprites, n, 4 );
				if (as != null) Fx.post( () -> runSweep( as, ds, around, EnchantLooks.byCode( n[3] ), as.visible, false ) );
				break;
			}
			case SEED: {
				final int cell = n[1];
				Level lv = Dungeon.level;
				if (lv == null || cell < 0 || cell >= lv.length()) return;
				final boolean seen = lv.heroFOV != null && cell < lv.heroFOV.length && lv.heroFOV[cell];
				Fx.post( () -> runSeed( Dungeon.hero, cell, seen, false ) );
				break;
			}
			case KEEN_BACK: {
				if (n.length < 5) return;
				final CharSprite ts = sprite( sprites, n[1] );
				if (ts != null) Fx.post( () -> runKeenBack( ts, n[2], n[3], EnchantLooks.byCode( n[4] ), false ) );
				break;
			}
			case CATCH: {
				final CharSprite ts = sprite( sprites, n[1] );
				if (ts != null) Fx.post( () -> runCatch( ts, false ) );
				break;
			}
			case BITE: {
				if (n.length < 5) return;
				final CharSprite ts = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (ds != null) Fx.post( () -> runBite( ts, ds, n[3], EnchantLooks.byCode( n[4] ), ds.visible, false ) );
				break;
			}
			case TELEGRAPH: {
				if (n.length < 4) return;
				final CharSprite ts = sprite( sprites, n[1] );
				final int to = n.length >= 5 ? n[4] : -1;
				if (ts != null) Fx.post( () -> runTelegraph( n[2], to, ts, EnchantLooks.byCode( n[3] ), false ) );
				break;
			}
			case REACH: {
				if (n.length < 4) return;
				final int cell = n[2];
				Level lv = Dungeon.level;
				if (lv == null || cell < 0 || cell >= lv.length()) return;
				final boolean seen = lv.heroFOV != null && cell < lv.heroFOV.length && lv.heroFOV[cell];
				final CharSprite ss = sprite( sprites, n[1] );
				Fx.post( () -> runReach( ss, cell, n[3], seen, false ) );
				break;
			}
			case DISARM: {
				final CharSprite hs = sprite( sprites, n[1] );
				if (hs != null) Fx.post( () -> Armed.release( hs ) );
				break;
			}
			case EXTRA: {
				if (n.length < 3) return;
				final int cell = n[1];
				Level lv = Dungeon.level;
				if (lv == null || cell < 0 || cell >= lv.length()) return;
				final boolean seen = lv.heroFOV != null && cell < lv.heroFOV.length && lv.heroFOV[cell];
				final float pitch = n.length >= 4 ? n[3] / 1000f : 1f;
				Fx.post( () -> runExtra( cell, n[2], pitch, seen, false ) );
				break;
			}
			case SPLINTER: case FELL: case DAZE: case CRACK: case NOTE: case CHORD: case RESONANCE: case LASH: case UNARM:
			case PERK: case SIPHON:
				TypeFx.replay( n, sprites );
				break;
			default:
				break;
		}
	}

	/** A PIECE a guest held with its blow, made again as the blow's piece on his sprites: a crit,
	 *  a lifesteal, a heave, its shockwave, a sweep, a weapon type's (TypeFx); null for any other (it
	 *  plays as it is). */
	static Piece pieceOf( final int[] n, EnchantNet.Sprites sprites ){
		if (n.length < 3) return null;
		switch (n[0]){
			case CRIT: {
				CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (ds == null || ds.ch == null) return null;
				return new CritPiece( ds.ch, as, ds, ds.visible, heroIn( as, ds ) );
			}
			case LIFESTEAL: {
				CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (as == null || ds == null || as.ch == null) return null;
				return new Lifesteal( as.ch, as, ds );
			}
			case HEAVE: case SHOCKWAVE: case SWEEP: {
				if (n.length < 4) return null;
				CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				CharSprite[] around = n[0] == HEAVE ? null : sprites( sprites, n, 4 );
				Char on = ds != null ? ds.ch : as != null ? as.ch : null;
				if (on == null || (n[0] != SWEEP && ds == null) || (n[0] == SWEEP && as == null)) return null;
				return new Twist( n[0], on, as, ds, around, EnchantLooks.byCode( n[3] ), (ds != null ? ds : as).visible,
						heroIn( as, ds ) );
			}
			case SPLINTER: case FELL: case DAZE: case CRACK: case NOTE: case CHORD: case RESONANCE: case LASH: case UNARM:
			case PERK: case SIPHON:
				return TypeFx.pieceOf( n, sprites );
			default:
				return null;
		}
	}

	private static CharSprite sprite( EnchantNet.Sprites s, int id ){
		return id < 0 ? null : s.of( id );
	}

	//the sprites of the ids from `from` on, those the guest has
	private static CharSprite[] sprites( EnchantNet.Sprites s, int[] n, int from ){
		int k = 0;
		CharSprite[] out = new CharSprite[Math.max( 0, n.length - from )];
		for (int i = from; i < n.length; i++){
			CharSprite c = sprite( s, n[i] );
			if (c != null) out[k++] = c;
		}
		return k == out.length ? out : java.util.Arrays.copyOf( out, k );
	}

	private static boolean heroIn( CharSprite as, CharSprite ds ){
		return EnchantFx.involvesHero( as != null ? as.ch : null, ds != null ? ds.ch : null );
	}

	// ------------------------------------------------------------------ helpers

	static int id( CharSprite s ){
		return s != null && s.ch != null ? s.ch.id() : -1;
	}

	//the numbers, then the ids of the sprites
	private static int[] withIds( int[] head, CharSprite[] around ){
		int[] out = java.util.Arrays.copyOf( head, head.length + around.length );
		for (int i = 0; i < around.length; i++) out[head.length + i] = id( around[i] );
		return out;
	}

	//the sprites of characters, those that have one, read where the call is made
	private static CharSprite[] sprites( List<Char> chars ){
		int k = 0;
		CharSprite[] out = new CharSprite[chars.size()];
		for (Char c : chars) if (c != null && c.sprite != null) out[k++] = c.sprite;
		return k == out.length ? out : java.util.Arrays.copyOf( out, k );
	}

	//nearest the middle first (an insertion sort: a handful at most)
	private static void order( CharSprite[] s, float cx, float cy ){
		for (int i = 1; i < s.length; i++){
			CharSprite k = s[i];
			float dk = dist2( k, cx, cy );
			int j = i - 1;
			while (j >= 0 && dist2( s[j], cx, cy ) > dk){
				s[j + 1] = s[j];
				j--;
			}
			s[j + 1] = k;
		}
	}

	private static float dist2( CharSprite s, float cx, float cy ){
		if (s == null) return Float.MAX_VALUE;
		float dx = s.x + s.width() / 2f - cx, dy = s.y + s.height() / 2f - cy;
		return dx * dx + dy * dy;
	}

	//a sprite on the game's scene that shows (and the scene's light layer standing)
	static boolean drawable( CharSprite s ){
		return s != null && s.parent != null && s.visible && Fx.glowAir() != null;
	}

	/** The weapon side of a sprite's middle (Crackle.WEAPON_SIDE), px: where a held weapon is. */
	static float handX( CharSprite s ){
		return s.x + s.width() / 2f + (s.flipHorizontal ? -EnchantFx.WEAPON_SIDE : EnchantFx.WEAPON_SIDE);
	}

	static float handY( CharSprite s ){
		return s.y + s.height() / 2f + 1f;
	}

	//a beat between two sprites, strong, rich
	static Beat beat( EnchantLook l, CharSprite as, CharSprite ds, boolean hero, int cap ){
		return new Beat( l, as, ds, EnchantFx.STRONG, 0, 0, 0, Beat.RICH, false, false, hero, cap );
	}

	//a beat at a point on a sprite (its hand): its feet the sprite's
	static Beat at( EnchantLook l, CharSprite s, float x, float y, int cap ){
		Beat b = Beat.at( l, x, y, cap );
		b.feet = s.y + s.height() - 2f;
		return b;
	}

	/** An item's angle flying (dx, dy), as MissileSprite turns a thrown one (drawn pointing up and
	 *  right), degrees. */
	static float flightAngle( float dx, float dy ){
		return 135f - (float)Math.toDegrees( Math.atan2( dx, dy ) );
	}

	//when each piece last stung
	private static final float[] stungAt = new float[KINDS];
	static {
		java.util.Arrays.fill( stungAt, -1f );
	}

	//a piece's sting: never the same piece's twice within `gap` s, its pitch 5% either way
	static void sting( int piece, String sample, Char at, float volume, float pitch, float gap ){
		sting( piece, sample, at, volume, pitch, gap, Sting.PITCH_SPREAD );
	}

	//a piece's sting, its pitch `spread` either way (0: exactly the pitch given)
	static void sting( int piece, String sample, Char at, float volume, float pitch, float gap, float spread ){
		float now = Game.timeTotal, last = stungAt[piece];
		if (last >= 0 && now >= last && now - last < gap) return;
		stungAt[piece] = now;
		Sting.out.play( sample, at, volume, spread > 0f ? pitch * FxRandom.Float( 1f - spread, 1f + spread ) : pitch );
	}

	// ------------------------------------------------------------------ the armed mote

	/**
	 * While a heave is armed: one mote circling the hand (ARMED_TURN rev/s) and pulsing at
	 * ARMED_PULSE Hz, gone as the heave is let go (release), as the Heave buff is (looked at every
	 * POLL s; the host then sends DISARM) or with its sprite. A guest's (and a restaged one) holds no
	 * buff to look at: it lasts until its release or its DISARM, ARMED_UNHELD s at most. At most MOST at once (the oldest goes). Pooled in the
	 * light layer, added. Render thread.
	 */
	public static final class Armed extends FxPic {

		static final int MOST = 4;
		static final float POLL = 0.1f;
		private static final Armed[] live = new Armed[MOST];

		private CharSprite s;
		private Char holder;
		private float age, poll;
		private boolean counted;

		public Armed(){
			super( Assets.Effects.FX_SHEET );
			additive = true;
			show( FxFrames.MOTE_2 );
		}

		static void arm( CharSprite s, Char holder, int color ){
			release( s );
			Armed a = GameScene.recycle( GameScene.Layer.LIGHTS, Armed.class );
			if (a == null) return;
			int slot = free();
			if (slot < 0){
				//the oldest goes
				Armed oldest = live[0];
				for (Armed o : live) if (o.age > oldest.age) oldest = o;
				oldest.kill();
				slot = free();
			}
			a.s = s;
			a.holder = holder;
			a.age = 0f;
			a.poll = POLL;
			a.resetColor();
			a.lit( color );
			a.am = 0f;
			a.visible = false;
			a.revive();
			if (!a.counted){
				a.counted = true;
				FxBudget.add( FxBudget.PARTICLES, 1 );
			}
			live[slot] = a;
		}

		//a slot of none, or of one left behind on a scene gone; -1 if all are circling
		private static int free(){
			for (int i = 0; i < MOST; i++){
				Armed a = live[i];
				if (a == null || !a.alive || a.parent == null) return i;
			}
			return -1;
		}

		/** A sprite's armed mote let go (its heave thrown). */
		static void release( CharSprite s ){
			for (int i = 0; i < MOST; i++) if (live[i] != null && live[i].s == s) live[i].kill();
		}

		/** How many are circling. */
		static int count(){
			int n = 0;
			for (Armed a : live) if (a != null && a.alive && a.parent != null) n++;
			return n;
		}

		@Override
		public void update(){
			super.update();
			if (s == null || s.parent == null){
				kill();
				return;
			}
			float dt = Game.elapsed;
			age += dt;
			if ((poll -= dt) <= 0f){
				poll = POLL;
				boolean armed = holder != null ? holder.buff( WeaponPerk.Heave.class ) != null : age < ARMED_UNHELD;
				if (!armed){
					//the host's heave called off or spent with no release seen: the guests' motes go too
					if (holder != null && NetManager.isHost()) EnchantNet.sendPiece( new int[]{ DISARM, id( s ) } );
					kill();
					return;
				}
			}
			float a = (float)(2 * Math.PI * ARMED_TURN * age);
			place( handX( s ) + (float)Math.cos( a ) * 3.5f, handY( s ) + (float)Math.sin( a ) * 1.5f );
			am = 0.55f + 0.35f * (float)Math.sin( 2 * Math.PI * ARMED_PULSE * age );
			visible = s.visible;
		}

		@Override
		public void kill(){
			super.kill();
			for (int i = 0; i < MOST; i++) if (live[i] == this) live[i] = null;
			s = null;
			holder = null;
			if (counted){
				counted = false;
				FxBudget.add( FxBudget.PARTICLES, -1 );
			}
		}
	}
}
