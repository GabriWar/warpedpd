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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Explosion;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EnergyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.Arrays;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The hunt-elements package's moments as the game drew them before it (master 72b40617b): the
 * hearts, wool, darts, the raptor, the target crosshairs, the trap mechanism, the stench, the bomb,
 * the wands' bolts, the blue-white lightning, the level-up stars, the old flares. Each skill's
 * visual calls of that code restaged on the dump's stage (SkillPreviewDump), CellEmitter's bursts
 * through the scene's stand-in emitters. Compiles against that code and this, so its "before" side
 * can be dumped from a checkout of the old code (tools/fxpreview/README.md).
 */
public final class SkillPreviewsHuntBefore {

	private SkillPreviewsHuntBefore(){}

	static final HeroClass HUNTRESS = HeroClass.HUNTRESS, MAGE = HeroClass.MAGE, ROGUE = HeroClass.ROGUE;

	public static void register( SkillPreviewDump.Registry r ){
		//huntress
		r.entry( "heartseeker-beats", SkillPreviewsHuntBefore::heartseeker ).rat( 6, 4 ).heroClass( HUNTRESS ).seconds( 2.5f );
		r.entry( "heartseeker-seek", s -> {
			s.d[0].emitter().burst( BloodParticle.FACTORY, 12 );
			new Flare( 6, 24 ).color( 0xCC2222, true ).show( s.d[0], 0.7f );
			SkillFX.streak( s.d[0].ch.pos, s.d[1].ch.pos, new Dart(), () -> {
				s.d[1].flash();
				s.d[1].emitter().burst( Speck.factory( Speck.HEART ), 3 );
			} );
		} ).rat( 5, 4 ).rat( 7, 3 ).heroClass( HUNTRESS );
		r.entry( "ember-activate", s -> {
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			s.h.emitter().burst( FlameParticle.FACTORY, 6 );
		} ).heroClass( HUNTRESS );
		r.entry( "ember-leap", s -> {
			flames( s.d[0] );
			((MagicMissile) s.d[0].parent.recycle( MagicMissile.class )).reset( MagicMissile.FIRE, s.d[0].center(), s.d[1].center(),
					() -> flames( s.d[1] ) );
		} ).rat( 5, 4 ).rat( 7, 3 ).heroClass( HUNTRESS );
		r.entry( "ember-kill", s -> {
			int pos = at( 6, 4 );
			s.center( pos ).burst( FlameParticle.FACTORY, 10 );
			int ring = 0;
			for (int c : around( pos )) s.get( c ).startDelayed( FlameParticle.FACTORY, 0, 5, 0.05f + 0.03f * ring++ );
		} ).heroClass( HUNTRESS );
		r.entry( "frost-activate", s -> {
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 );
			s.h.emitter().burst( SnowParticle.FACTORY, 6 );
		} ).heroClass( HUNTRESS );
		r.entry( "fletching-burst", s -> {
			s.d[0].emitter().burst( Speck.factory( Speck.WOOL ), 6 );
			for (int i = 1; i < s.d.length; i++){
				final CharSprite v = s.d[i];
				SkillFX.streak( s.d[0].ch.pos, v.ch.pos, new Dart(), v::flash );
			}
		} ).rat( 5, 4 ).rat( 7, 3 ).rat( 7, 5 ).rat( 4, 6 ).heroClass( HUNTRESS );
		r.entry( "fletching-pluck", s -> s.h.emitter().burst( Speck.factory( Speck.WOOL ), 2 ) ).heroClass( HUNTRESS );
		r.entry( "arrow-storm-cast", s -> {
			final int target = at( 7, 6 );
			final PointF top = DungeonTilemap.tileCenterToWorld( target );
			final int n = 5;
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < n; i++){
				final int k = i;
				t.at( 0.07f * i, () -> {
					PointF sky = new PointF( top.x + (k - (n - 1) / 2f) * 6f, top.y - DungeonTilemap.SIZE * 6 );
					((MissileSprite) s.h.parent.recycle( MissileSprite.class )).reset( s.h.center(), sky, new Dart(), null );
				} );
			}
			int order = 0;
			for (int c : area( target )){
				final int at = c, k = order++;
				t.at( 0.45f + 0.05f * k, () -> s.bottom( at ).burst( Speck.factory( Speck.DUST ), 3 ) );
			}
		} ).hero( 3, 6 ).view( 0, 0, 11, 9 ).heroClass( HUNTRESS ).seconds( 1.5f );
		r.entry( "arrow-rain-volley", s -> {
			int center = at( 6, 6 );
			SkillSpectacleFX.show( SkillSpectacleFX.RAPTOR, center );
			Dart look = new Dart();
			int i = 0;
			for (int c : volley( center )) SkillFX.rain( c, look, 1 + (i++ % 2), null );
		} ).hero( 2, 6 ).view( 1, 0, 9, 9 ).rat( 6, 6 ).rat( 7, 5 ).heroClass( HUNTRESS ).seconds( 1.5f );
		r.entry( "charged-draw", s -> {
			Group preview = new Group();
			for (int c : lane( 3, 4, 10 )){
				Image im = Icons.get( Icons.TARGET );
				im.point( DungeonTilemap.tileToWorld( c ) );
				im.hardlight( 0xA6DBCF );
				im.alpha( 0.5f );
				preview.add( im );
			}
			s.h.parent.add( preview );
			for (int k = 0; k < 3; k++) s.later( 0.8f * k, () -> {
				s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
				new Flare( 4, 14 ).color( 0xFFE9A0, true ).show( s.h, 0.5f ).angularSpeed = 120;
			} );
		} ).hero( 2, 4 ).view( 1, 1, 10, 7 ).rat( 7, 4 ).heroClass( HUNTRESS ).seconds( 2.5f );
		r.entry( "charged-release", s -> {
			int far = at( 10, 4 );
			s.h.parent.add( new Beam.LightRay( s.h.center(), DungeonTilemap.raisedTileCenterToWorld( far ) ) );
			SkillFX.streak( s.h, far, new Dart(), null );
			FxTimeline t = FxTimeline.start();
			int i = 0;
			for (int c : lane( 3, 4, 10 )){
				final int at = c;
				t.at( 0.02f * i++, () -> s.center( at ).burst( Speck.factory( Speck.DUST ), 2 ) );
			}
			for (CharSprite d : s.d) d.flash();
		} ).hero( 2, 4 ).view( 1, 1, 10, 7 ).rat( 5, 4 ).rat( 8, 4 ).heroClass( HUNTRESS );
		r.entry( "bear-trap-throw", s -> {
			final int at = at( 6, 4 );
			Item look = new Item(){ { image = ItemSpriteSheet.TRAP_MECHANISM; } };
			SkillFX.streak( s.h, at, look, () -> {
				s.get( at ).burst( Speck.factory( Speck.DUST ), 4 );
				s.center( at ).burst( SparkParticle.FACTORY, 3 );
				SkillSpectacleFX.show( SkillSpectacleFX.JAW, at );
			} );
		} ).heroClass( HUNTRESS );
		r.entry( "bear-trap-spring", s -> {
			final int pos = at( 6, 4 );
			SkillSpectacleFX.show( SkillSpectacleFX.JAW, pos );
			s.center( pos ).burst( Speck.factory( Speck.STAR ), 4 );
			s.center( pos ).burst( SparkParticle.FACTORY, 8 );
			FxTimeline.start().at( 0.14f, () -> {
				SkillSpectacleFX.show( SkillSpectacleFX.JAW, pos );
				s.center( pos ).burst( SparkParticle.FACTORY, 4 );
			} );
			Wound.hit( s.d[0].ch );
			s.d[0].flash();
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "briar", s -> {
			int center = at( 6, 4 );
			for (CharSprite d : s.d) SkillSpectacleFX.fly( SkillSpectacleFX.THORN, center, d.ch.pos, 0, 0.45f );
			SkillSpectacleFX.show( SkillSpectacleFX.JAW, center );
		} ).rat( 6, 4 ).rat( 7, 3 ).rat( 5, 5 ).heroClass( HUNTRESS );
		r.entry( "wild-call-cast", s -> {
			s.h.emitter().burst( Speck.factory( Speck.STAR ), 4 );
			s.get( s.d[0].ch.pos ).burst( LeafParticle.GENERAL, 10 );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "wild-call-bite", s -> {
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( LeafParticle.GENERAL, 4 );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "thorns-lash", s -> {
			s.d[0].emitter().burst( LeafParticle.GENERAL, 5 );
			s.bottom( s.d[0].ch.pos ).burst( EarthParticle.FACTORY, 4 );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "poacher-bait", s -> {
			int cell = at( 6, 4 );
			s.get( cell ).burst( Speck.factory( Speck.STENCH ), 6 );
			PixelFlare.show( SkillVfx.cx( cell ), SkillVfx.cy( cell ), 0xB5D67A, PixelFlare.S, 0.4f );
			s.later( 0.8f, () -> s.get( cell ).burst( Speck.factory( Speck.STENCH ), 2 ) );
			s.later( 1.6f, () -> s.get( cell ).burst( Speck.factory( Speck.STENCH ), 2 ) );
		} ).heroClass( HUNTRESS ).seconds( 2.5f );
		r.entry( "poacher-snare", s -> {
			int cell = s.d[0].ch.pos;
			s.d[0].flash();
			s.get( cell ).burst( LeafParticle.GENERAL, 10 );
			s.get( cell ).burst( Speck.factory( Speck.ROCK ), 4 );
		} ).rat( 6, 4 ).heroClass( HUNTRESS );
		r.entry( "sixth-sense", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0x88CC66, PixelFlare.M, 0.6f );
			ClosingRing.on( s.h, 31, 9, 0.45f, 0x88CC66 );
			s.h.emitter().burst( Speck.factory( Speck.STAR ), 6 );
		} ).heroClass( HUNTRESS );

		//mage, fire, frost, storm and arcane
		r.entry( "meteor-mark", s -> {
			int cell = at( 6, 4 );
			new Flare( 5, 14 ).color( 0xFF6622, true ).show( s.h, 0.6f );
			new Flare( 6, 16 ).color( 0xFF6622, true ).show( s.h.parent, DungeonTilemap.tileCenterToWorld( cell ), 0.7f );
			s.center( cell ).burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			StaggerFX.ring( cell, 1, 0.15f, ( c, rr ) -> s.get( c ).burst( Speck.factory( Speck.RED_LIGHT ), 2 ) );
			s.center( cell ).burst( Speck.factory( Speck.RED_LIGHT ), 4 );
		} ).heroClass( MAGE ).seconds( 2f );
		r.entry( "meteor-fall", s -> {
			final int cell = at( 6, 6 );
			SkillFX.rain( cell, new Bomb(), 1, () -> {
				Explosion.meteor( cell );
				s.center( cell ).burst( BlastParticle.FACTORY, 30 );
				for (int c : area( cell )) s.get( c ).burst( FlameParticle.FACTORY, 4 );
			} );
		} ).hero( 2, 6 ).view( 1, 0, 9, 9 ).rat( 6, 6 ).rat( 7, 7 ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "arcane-echo", s -> echo( s, false ) ).rat( 4, 4 ).heroClass( MAGE );
		r.entry( "arcane-echo-chain", s -> echo( s, true ) ).rat( 4, 4 ).rat( 5, 5 ).heroClass( MAGE );
		r.entry( "blink-chain", SkillPreviewsHuntBefore::blink ).hero( 2, 4 ).rat( 3, 4 ).rat( 5, 5 ).heroClass( ROGUE ).seconds( 1.5f );
		r.entry( "spark", SkillPreviewsHuntBefore::spark ).rat( 5, 4 ).rat( 7, 4 ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "sorcerer-overload", s -> {
			s.d[0].centerEmitter().burst( SparkParticle.FACTORY, 8 );
			s.later( 0.6f, () -> s.d[0].centerEmitter().burst( SparkParticle.FACTORY, 5 ) );
			s.later( 1.0f, () -> {
				for (CharSprite d : s.d){
					if (d != s.d[0]) s.h.parent.add( new Lightning( s.d[0].ch.pos, d.ch.pos, null ) );
					d.centerEmitter().burst( SparkParticle.FACTORY, 6 );
					d.flash();
				}
			} );
		} ).rat( 6, 4 ).rat( 7, 3 ).rat( 7, 5 ).heroClass( MAGE ).seconds( 1.6f );
		r.entry( "wizard-ricochet", s -> {
			((MagicMissile) s.h.parent.recycle( MagicMissile.class )).reset( MagicMissile.MAGIC_MISSILE, s.d[0].ch.pos, s.d[1].ch.pos, null );
			s.d[1].centerEmitter().burst( EnergyParticle.FACTORY, 6 );
		} ).rat( 5, 4 ).rat( 8, 5 ).heroClass( MAGE );
		r.entry( "pyre-leap", s -> {
			s.d[0].emitter().burst( FlameParticle.FACTORY, 6 );
			new Flare( 5, 12 ).color( 0xFF8418, true ).show( s.d[0], 0.4f );
			((MagicMissile) s.h.parent.recycle( MagicMissile.class )).reset( MagicMissile.FIRE, s.d[0].ch.pos, s.d[1].ch.pos, () -> {
				s.d[1].emitter().burst( FlameParticle.FACTORY, 6 );
				s.d[1].flash();
			} );
		} ).rat( 5, 4 ).rat( 6, 5 ).heroClass( MAGE );
		r.entry( "cinder-gather", s -> {
			s.h.centerEmitter().burst( FlameParticle.FACTORY, 6 );
			s.h.centerEmitter().burst( SnowParticle.RISING_FACTORY, 6 );
			new Flare( 6, 14 ).color( 0xFFAA66, true ).show( s.h, 0.5f );
			StaggerFX.after( 0.15f, () -> new Flare( 6, 14 ).color( 0x88DDFF, true ).show( s.h, 0.5f ) );
		} ).heroClass( MAGE );
		r.entry( "spirit-volley", s -> {
			for (CharSprite d : s.d){
				((MagicMissile) s.h.parent.recycle( MagicMissile.class )).reset( MagicMissile.WARD, s.h.center(), d.destinationCenter(), null );
				SpiritArmorFX.spend( s.hero, d.ch );
				d.flash();
			}
		} ).rat( 6, 3 ).rat( 6, 5 ).heroClass( MAGE );
		r.entry( "spell-blade", s -> s.h.emitter().burst( EnergyParticle.FACTORY, 5 ) ).heroClass( MAGE );
		r.entry( "storm-call", SkillPreviewsHuntBefore::stormCall ).hero( 2, 4 ).rat( 6, 4 ).rat( 7, 2 ).heroClass( MAGE ).seconds( 1.5f );
		r.entry( "mana-shield-break", s -> {
			new Flare( 8, 28 ).color( 0x55AAFF, true ).show( s.h, 0.8f );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
		} ).heroClass( MAGE );
	}

	// ------------------------------------------------------------------ the moments before

	//HeartseekerArrow: the hearts as it lodges, two beats 0.6 s apart, the burst's blood and red flare
	static void heartseeker( SkillPreviewDump.Scene s ){
		final CharSprite e = s.d[0];
		e.emitter().burst( Speck.factory( Speck.HEART ), 3 );
		s.later( 0.6f, () -> e.emitter().burst( Speck.factory( Speck.HEART ), 2 ) );
		s.later( 1.2f, () -> e.emitter().burst( Speck.factory( Speck.HEART ), 3 ) );
		s.later( 1.8f, () -> {
			e.emitter().burst( BloodParticle.FACTORY, 12 );
			new Flare( 6, 24 ).color( 0xCC2222, true ).show( e, 0.7f );
		} );
	}

	static void flames( CharSprite c ){
		c.centerEmitter().burst( FlameParticle.FACTORY, 4 );
		c.emitter().start( FlameParticle.FACTORY, 0.05f, 6 );
	}

	static void echo( SkillPreviewDump.Scene s, boolean chain ){
		s.h.parent.add( new Lightning( s.hero.pos, s.d[0].ch.pos, null ) );
		s.d[0].emitter().burst( SparkParticle.FACTORY, 5 );
		s.d[0].flash();
		if (!chain) return;
		s.h.parent.add( new Lightning( s.d[0].ch.pos, s.d[1].ch.pos, null ) );
		s.d[1].emitter().burst( SparkParticle.FACTORY, 5 );
		s.d[1].flash();
	}

	static void blink( SkillPreviewDump.Scene s ){
		int[][] jumps = { { at( 2, 4 ), at( 4, 4 ), 0 }, { at( 4, 4 ), at( 6, 6 ), 1 } };
		jump( s, jumps, 0 );
	}

	private static void jump( SkillPreviewDump.Scene s, int[][] jumps, int i ){
		if (i >= jumps.length){
			s.h.emitter().burst( SparkParticle.FACTORY, 10 );
			return;
		}
		int[] j = jumps[i];
		CharSprite target = s.d[j[2]];
		s.h.place( j[0] );
		PointF start = s.h.center();
		s.h.place( j[1] );
		PointF end = s.h.center();
		PointF middle = target.center();
		ArrayList<Lightning.Arc> arcs = new ArrayList<>( Arrays.asList( new Lightning.Arc( start, middle ), new Lightning.Arc( middle, end ) ) );
		target.emitter().burst( SparkParticle.FACTORY, 6 );
		target.flash();
		s.h.parent.add( new Lightning( arcs, () -> jump( s, jumps, i + 1 ) ) );
	}

	static void spark( SkillPreviewDump.Scene s ){
		final int cell = s.d[1].ch.pos;
		s.h.centerEmitter().burst( SparkParticle.FACTORY, 4 );
		MagicMissile.boltFromChar( s.h.parent, MagicMissile.MAGIC_MISSILE, s.h, cell, () -> {
			s.center( cell ).burst( SparkParticle.FACTORY, 10 );
			arc( s, s.hero.pos, new int[]{ s.d[0].ch.pos, cell }, 0 );
		} );
	}

	private static void arc( SkillPreviewDump.Scene s, int from, int[] struck, int i ){
		if (i >= struck.length) return;
		int to = struck[i];
		s.center( to ).burst( SparkParticle.FACTORY, 6 );
		CharSprite b = bodyAt( s, to );
		if (b != null) b.flash();
		s.h.parent.add( new Lightning( from, to, () -> arc( s, to, struck, i + 1 ) ) );
	}

	static void stormCall( SkillPreviewDump.Scene s ){
		stormArc( s, new int[][]{ { -1, s.d[0].ch.pos }, { s.d[0].ch.pos, s.d[1].ch.pos } }, 0 );
	}

	private static void stormArc( SkillPreviewDump.Scene s, int[][] arcs, int i ){
		if (i >= arcs.length) return;
		int[] a = arcs[i];
		s.center( a[1] ).burst( SparkParticle.FACTORY, 8 );
		CharSprite b = bodyAt( s, a[1] );
		if (b != null) b.flash();
		Callback then = () -> stormArc( s, arcs, i + 1 );
		if (a[0] < 0){
			PointF foot = DungeonTilemap.raisedTileCenterToWorld( a[1] );
			s.h.parent.add( new Lightning( new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 ), a[1], then ).major().aloft() );
		} else {
			s.h.parent.add( new Lightning( a[0], a[1], then ).major() );
		}
	}

	// ------------------------------------------------------------------ the stage

	static CharSprite bodyAt( SkillPreviewDump.Scene s, int cell ){
		for (CharSprite d : s.d) if (d.ch.pos == cell) return d;
		return null;
	}

	static int[] around( int cell ){
		int w = SkillPreviewDump.W, x = cell % w, y = cell / w, n = 0;
		int[] out = new int[8];
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (dx != 0 || dy != 0) out[n++] = at( x + dx, y + dy );
		return out;
	}

	static int[] area( int cell ){
		int w = SkillPreviewDump.W, x = cell % w, y = cell / w, n = 0;
		int[] out = new int[9];
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) out[n++] = at( x + dx, y + dy );
		return out;
	}

	static int[] volley( int center ){
		int w = SkillPreviewDump.W, x = center % w, y = center / w;
		int[] out = Arrays.copyOf( area( center ), 13 );
		out[9] = at( x - 2, y );
		out[10] = at( x + 2, y );
		out[11] = at( x, y - 2 );
		out[12] = at( x, y + 2 );
		return out;
	}

	static ArrayList<Integer> lane( int x0, int y, int x1 ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int x = x0; x <= x1; x++) out.add( at( x, y ) );
		return out;
	}
}
