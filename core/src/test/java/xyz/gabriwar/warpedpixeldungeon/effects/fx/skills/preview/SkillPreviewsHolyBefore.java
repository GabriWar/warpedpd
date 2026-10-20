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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.BlobFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.HolyFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ConeAOE;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.function.IntConsumer;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The holy-light package's moments as the game drew them before it (master 72b40617b): each
 * cleric skill's visual calls of that code, restaged on the dump's stage (SkillPreviewDump) -
 * CellEmitter's bursts through the scene's stand-in emitters. Compiles against that code and this,
 * so its "before" side can be dumped from a checkout of the old code. The legacy Flare (a mesh of
 * rays, not an Image) is called where the code called it but is not dumped.
 */
public final class SkillPreviewsHolyBefore {

	private SkillPreviewsHolyBefore(){}

	static final HeroClass C = HeroClass.CLERIC;

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "divine-wrath-activate", s -> {
			s.h.emitter().burst( FlameParticle.FACTORY, 8 );
			PulseRingFX.around( s.h, 0xFFB050, 12, 0.4f );
			s.h.parent.add( new StanceAuraFX( s.hero, 0xFFB050, FlameParticle.FACTORY, 1.1f ) );
		} ).heroClass( C ).seconds( 2f );
		r.entry( "divine-wrath-ground", SkillPreviewsHolyBefore::wrathGround ).heroClass( C ).rat( 5, 4 ).seconds( 2.5f );
		r.entry( "avatar-cast", s -> {
			s.h.emitter().start( Speck.factory( Speck.LIGHT ), 0.3f, 12 );
			s.h.emitter().burst( ShaftParticle.FACTORY, 8 );
			new Flare( 12, 44 ).color( 0xFFEE88, true ).show( s.h, 1.2f ).angularSpeed = 60;
			ShieldHalo halo = new ShieldHalo( s.h );
			s.h.parent.add( halo );
			halo.putOut();
			PillarRiseFX.show( s.hero.pos, 0xFFEE88, null );
			PulseRingFX.around( s.h, 0xFFEE88, 18, 0.6f );
			FxTimeline.start()
					.at( 0.25f, () -> { s.h.emitter().burst( ShaftParticle.FACTORY, 5 ); PulseRingFX.around( s.h, 0xFFF6C8, 12, 0.45f ); } )
					.at( 0.6f, () -> s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 ) );
			HolyFx.halo( s.h, 2 );
		} ).heroClass( C ).seconds( 2f );
		r.entry( "avatar-sear", s -> {
			HolyFx.halo( s.h, 2 );
			s.later( 0.2f, () -> {
				s.h.parent.add( new Beam.LightRay( s.h.center(), s.d[0].center() ) );
				s.d[0].flash();
			} );
		} ).heroClass( C ).rat( 5, 3 ).seconds( 1.5f );
		r.entry( "aegis", s -> {
			ShieldHalo halo = new ShieldHalo( s.h );
			s.h.parent.add( halo );
			halo.putOut();
			new Flare( 6, 24 ).color( 0xFFEE88, true ).show( s.h, 0.5f );
			PulseRingFX.around( s.h, 0xFFEE88, 12, 0.35f );
			//the recharge, turns later: its flare (not dumped)
			s.later( 0.9f, () -> new Flare( 5, 16 ).color( 0xFFEE88, true ).show( s.h, 0.4f ) );
		} ).heroClass( C ).rat( 4, 4 ).seconds( 1.5f );
		r.entry( "holy-smite", s -> {
			int cell = s.d[0].ch.pos;
			SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, cell );
			//SkillFX.land: the dust, then its landing ring and the ground's answer
			s.bottom( cell ).burst( Speck.factory( Speck.DUST ), 8 );
			SkillFX.landing( cell );
			s.d[0].flash();
			new Flare( 6, 14 ).color( 0xFFE9A0, true ).show( s.d[0], 0.35f );
			PulseRingFX.at( cell, 0xFFE9A0, 16, 0.45f );
		} ).heroClass( C ).rat( 5, 4 );
		r.entry( "holy-charge", s -> {
			int from = s.hero.pos, dest = at( 6, 4 );
			s.h.parent.add( new Beam.SunRay( s.h.center(), DungeonTilemap.raisedTileCenterToWorld( dest ) ) );
			for (int x = 3; x <= 6; x++) s.center( at( x, 4 ) ).burst( Speck.factory( Speck.LIGHT ), 3 );
			charge( s, from, dest, () -> {
				s.d[0].flash();
				new Flare( 8, 28 ).color( 0xFFEE88, true ).show( s.d[0], 0.6f );
				s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 8 );
			} );
		} ).heroClass( C ).hero( 2, 4 ).rat( 7, 4 );
		r.entry( "litany-cast", s -> {
			s.h.emitter().burst( Speck.factory( Speck.NOTE ), 2 );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		} ).heroClass( C );
		r.entry( "litany-mend", s -> {
			s.h.emitter().burst( Speck.factory( Speck.NOTE ), 2 );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
			for (CharSprite a : s.d){
				a.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
				s.h.parent.add( new Beam.HealthRay( s.h.center(), a.center() ) );
			}
		} ).heroClass( C ).rat( 5, 3 ).rat( 4, 6 );
		r.entry( "last-rites", s -> {
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 12 );
			s.h.emitter().burst( ShaftParticle.FACTORY, 6 );
			new Flare( 8, 36 ).color( 0xFFEE88, true ).show( s.h, 1.2f ).angularSpeed = 120;
			s.h.parent.add( new Beam.LightRay( new PointF( s.h.center().x, s.h.center().y - DungeonTilemap.SIZE * 6 ), s.h.center() ) );
		} ).heroClass( C ).view( 0, 0, 9, 8 );
		r.entry( "grace", s -> {
			MagicMissile.boltFromChar( s.h.parent, MagicMissile.LIGHT_MISSILE, s.d[0], s.hero.pos, null );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
		} ).heroClass( C ).rat( 7, 4 );
		r.entry( "pillar-of-light", s -> {
			int cell = s.d[0].ch.pos;
			PillarRiseFX.show( cell, 0xFFEE88, () -> { SkillInteractions.flare( cell, 0xFFF1A1 ); s.d[0].flash(); } );
			SkillSpectacleFX.show( SkillSpectacleFX.SPIRE, cell );
		} ).heroClass( C ).rat( 6, 4 ).view( 1, 0, 9, 8 );
		r.entry( "cathedral", s -> cathedral( s, at( 6, 4 ) ) ).heroClass( C ).rat( 6, 4 ).view( 2, 0, 9, 9 ).seconds( 2f );
		r.entry( "righteous-strikes", s -> {
			new Flare( 6, 20 ).color( 0xFFEE88, true ).show( s.d[0], 0.6f );
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
			FxTimeline chain = FxTimeline.start();
			for (int i = 0; i + 1 < s.d.length; i++){
				final CharSprite a = s.d[i], b = s.d[i + 1];
				chain.at( 0.1f * (i + 1), () -> ray( a, b ) );
			}
			chain.at( 0.1f * (s.d.length + 1), () -> ray( s.d[s.d.length - 1], s.h ) );
		} ).heroClass( C ).rat( 4, 4 ).rat( 5, 2 ).rat( 7, 3 ).view( 1, 0, 9, 8 );
		r.entry( "retribution", s -> {
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
			s.d[0].flash();
			s.h.parent.add( new Beam.LightRay( s.d[0].center(), s.d[1].center() ) );
		} ).heroClass( C ).rat( 4, 4 ).rat( 5, 2 );
		r.entry( "purify", s -> {
			for (CharSprite a : s.d){
				a.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				s.h.parent.add( new Beam.LightRay( s.h.center(), a.center() ) );
			}
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			s.h.emitter().burst( ShaftParticle.FACTORY, 5 );
			Splash.around( s.h, 0xFFFFFF, 6 );
			PulseRingFX.around( s.h, 0xFFFFFF, 14, 0.45f );
		} ).heroClass( C ).rat( 6, 3 );
		r.entry( "mass-heal", s -> {
			SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos );
			new Flare( 8, 28 ).color( 0xAAFFAA, true ).show( s.h, 1f );
			s.h.emitter().burst( ShaftParticle.FACTORY, 6 );
			PulseRingFX.around( s.h, 0xAAFFAA, 40, 0.7f );
			FxTimeline.start()
					.at( 0.1f, () -> PulseRingFX.around( s.h, 0xFFF1A1, 24, 0.55f ) )
					.at( 0.45f, () -> s.h.emitter().burst( Speck.factory( Speck.HEALING ), 5 ) );
			//a turn later, the mend
			s.later( 1f, () -> {
				s.h.emitter().burst( Speck.factory( Speck.HEALING ), 2 );
				for (CharSprite a : s.d){
					a.emitter().burst( Speck.factory( Speck.HEALING ), 2 );
					s.h.parent.add( new Beam.HealthRay( s.h.center(), a.center() ) );
				}
			} );
		} ).heroClass( C ).rat( 5, 3 ).rat( 4, 6 ).seconds( 2f );
		r.entry( "scouring-flame", s -> scour( s, end -> MagicMissile.boltFromChar( s.h.parent, MagicMissile.LIGHT_MISSILE, s.h, end, null ) ) ).heroClass( C ).rat( 5, 4 ).rat( 6, 3 ).seconds( 1.5f );
		r.entry( "sanctuary", s -> {
			SkillCastFX.play( new Sanctuary(), s.hero );
			ShieldHalo halo = new ShieldHalo( s.h );
			s.h.parent.add( halo );
			halo.putOut();
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			s.h.parent.add( new StanceAuraFX( s.hero, 0xFFF1A1, ShaftParticle.FACTORY, 1.6f ) );
			//turns later the circle is laid
			s.later( 0.9f, () -> {
				int[] cells = SkillPreviewsKitBefore.around( 3, 4 );
				SkillPreviewsKitBefore.field( SkillField.SANCTUARY, cells );
				for (int c : cells) SkillInteractions.flare( c, 0xFFF1A1 );
				new Flare( 8, 32 ).color( 0xFFEE88, true ).show( s.h, 0.8f );
			} );
		} ).heroClass( C ).seconds( 2.5f );
		r.entry( "reckoning", s -> {
			FxTimeline flashes = FxTimeline.start();
			for (int i = 0; i < s.d.length; i++){
				final CharSprite m = s.d[i];
				final boolean pillar = i != 1;
				flashes.at( 0.12f * i, () -> {
					if (pillar) PillarRiseFX.show( m.ch.pos, 0xFFEE88, null );
					else s.get( m.ch.pos ).burst( Speck.factory( Speck.LIGHT ), 4 );
					SkillInteractions.flare( m.ch.pos, 0xFFF1A1 );
					m.flash();
				} );
			}
			s.h.emitter().start( Speck.factory( Speck.STAR ), 0.3f, 10 );
			new Flare( 10, 32 ).color( 0xFFEE88, true ).show( s.h, 1.2f ).angularSpeed = 90;
		} ).heroClass( C ).rat( 4, 3 ).rat( 5, 5 ).rat( 6, 3 ).view( 0, 0, 9, 8 ).seconds( 1.5f );
		r.entry( "fervor-kindle", s -> s.h.emitter().burst( FlameParticle.FACTORY, 10 ) ).heroClass( C );
		r.entry( "fervor-eruption", s -> {
			s.d[0].emitter().burst( FlameParticle.FACTORY, 12 );
			new Flare( 6, 24 ).color( 0xFFAA33, true ).show( s.d[0], 0.6f );
			for (int i = 1; i < s.d.length; i++) s.d[i].emitter().burst( FlameParticle.FACTORY, 6 );
		} ).heroClass( C ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		r.entry( "shield-faithful-catch", SkillPreviewsHolyBefore::shield ).heroClass( C ).rat( 4, 4 ).rat( 7, 2 ).seconds( 2.5f );
		r.entry( "transcendence", s -> {
			int from = s.hero.pos, land = at( 5, 4 );
			s.center( from ).burst( Speck.factory( Speck.LIGHT ), 6 );
			s.hero.pos = land;
			s.h.place( land );
			s.h.parent.add( new Beam.LightRay( DungeonTilemap.raisedTileCenterToWorld( from ), DungeonTilemap.raisedTileCenterToWorld( land ) ) );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
			for (CharSprite b : s.d){
				b.flash();
				s.center( b.ch.pos ).burst( Speck.factory( Speck.LIGHT ), 5 );
			}
			for (int end : new int[]{ at( 7, 4 ), at( 7, 6 ), at( 7, 2 ) }){
				s.h.parent.add( new Beam.LightRay( DungeonTilemap.raisedTileCenterToWorld( land ), DungeonTilemap.raisedTileCenterToWorld( end ) ) );
			}
		} ).heroClass( C ).hero( 2, 4 ).rat( 3, 4 ).rat( 4, 4 );
		r.entry( "sacred-plant", s -> {
			int cell = at( 6, 4 );
			SkillFX.rain( cell, new Sword(), 1, () -> s.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 ) );
			s.h.parent.add( planted( cell ) );
		} ).heroClass( C ).rat( 5, 4 ).view( 1, 0, 9, 8 );
		r.entry( "sacred-cut", s -> {
			s.h.parent.add( planted( at( 6, 4 ) ) );
			s.later( 0.3f, () -> {
				SkillSpectacleFX.fly( SkillSpectacleFX.SABER, at( 6, 4 ), s.d[0].ch.pos, 0, .25f );
				s.d[0].flash();
			} );
		} ).heroClass( C ).rat( 5, 4 );
		r.entry( "sacred-throw", s -> {
			ItemSprite sword = planted( at( 6, 4 ) );
			s.h.parent.add( sword );
			s.later( 0.3f, () -> {
				SkillFX.streak( at( 6, 4 ), s.d[0].ch.pos, new Sword(), () -> s.d[0].flash() );
				s.center( at( 6, 4 ) ).burst( Speck.factory( Speck.LIGHT ), 4 );
				sword.killAndErase();
			} );
		} ).heroClass( C ).rat( 8, 2 ).view( 2, 0, 9, 7 );
		r.entry( "healing-prayer", s -> {
			SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos );
			s.h.emitter().start( Speck.factory( Speck.HEALING ), 0.4f, 4 );
			s.h.emitter().burst( ShaftParticle.FACTORY, 5 );
			PulseRingFX.around( s.h, 0xBBFFCC, 14, 0.55f );
			FxTimeline.start()
					.at( 0.08f, () -> { PulseRingFX.around( s.h, 0xFFF1A1, 10, 0.45f ); s.h.emitter().burst( Speck.factory( Speck.HEALING ), 4 ); } )
					.at( 0.42f, () -> s.h.emitter().burst( ShaftParticle.FACTORY, 3 ) );
		} ).heroClass( C ).seconds( 1.5f );
		r.entry( "condemn", s -> {
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.LIGHT ), 3 );
			new Flare( 4, 10 ).color( 0xFFEE88, true ).show( s.d[0], 0.35f );
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
		} ).heroClass( C ).rat( 4, 4 );
		r.entry( "second-wind", s -> {
			new Flare( 6, 24 ).color( 0x88FF88, true ).show( s.h, 0.6f );
			s.h.emitter().burst( Speck.factory( Speck.UP ), 5 );
		} ).heroClass( C );
		r.entry( "inner-peace", s -> s.h.emitter().burst( Speck.factory( Speck.STEAM ), 5 ) ).heroClass( C );
		r.entry( "regeneration", s -> s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 ) ).heroClass( C );
	}

	// ------------------------------------------------------------------ the moments before

	//Divine Wrath at +2 on a blow: the struck rat's glints, the flame under all nine cells, the
	//ground laid (a glint on each new cell, BlobFx.WRATH poured), a turn later it burns the rat,
	//and at 2 s the ground goes
	static void wrathGround( SkillPreviewDump.Scene s ){
		CharSprite rat = s.d[0];
		int[] cells = SkillPreviewsKitBefore.around( 5, 4 );
		rat.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 5 );
		rat.flash();
		new Flare( 5, 12 ).color( 0xFFB050, true ).show( rat, 0.3f );
		for (int c : cells) s.floor( c ).burst( FlameParticle.FACTORY, 2 );
		for (int c : cells) s.get( c ).burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
		final com.watabou.noosa.particles.Emitter[] pours = new com.watabou.noosa.particles.Emitter[cells.length];
		for (int i = 0; i < cells.length; i++){
			pours[i] = s.get( cells[i] );
			pours[i].pour( BlobFx.WRATH, 0.3f );
		}
		s.later( 1f, () -> {
			rat.flash();
			s.center( rat.ch.pos ).burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
		} );
		//the ground goes
		s.later( 2f, () -> { for (com.watabou.noosa.particles.Emitter e : pours) e.on = false; } );
	}

	//the hero charging along the floor to `dest` (CharSprite.jump with no height), then `then`
	static void charge( SkillPreviewDump.Scene s, int from, int dest, Runnable then ){
		float d = 0.04f * Math.abs( dest % SkillPreviewDump.W - from % SkillPreviewDump.W );
		s.h.jump( from, dest, 0f, d, () -> {
			s.hero.pos = dest;
			s.h.place( dest );
			then.run();
		} );
	}

	//Pillar of Light's +3: CATHEDRAL at rank 2, a ring of spires a turn (each a SPIRE spectacle)
	static void cathedral( SkillPreviewDump.Scene s, int center ){
		PillarRiseFX.show( center, 0xFFEE88, null );
		for (int age = 1; age <= 2; age++){
			final int a = age;
			s.later( (age - 1) * 0.9f, () -> {
				for (int dy = -a; dy <= a; dy++) for (int dx = -a; dx <= a; dx++){
					if ((dx == 0 || dy == 0) && Math.max( Math.abs( dx ), Math.abs( dy ) ) == a || Math.abs( dx ) == a && Math.abs( dy ) == a){
						SkillSpectacleFX.show( SkillSpectacleFX.SPIRE, center + dx + dy * SkillPreviewDump.W );
					}
				}
			} );
		}
	}

	static void ray( CharSprite a, CharSprite b ){
		a.parent.add( new Beam.LightRay( a.center(), b.center() ) );
		b.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
	}

	//Scouring Flame at +3 toward (7, 4): a bolt down each ray of the cone (`bolt` to its end), the
	//white fire rolling out ring by ring, the rats in it flashing in it
	static void scour( SkillPreviewDump.Scene s, IntConsumer bolt ){
		Ballistica aim = new Ballistica( s.hero.pos, at( 7, 4 ), Ballistica.STOP_SOLID );
		ConeAOE cone = new ConeAOE( aim, 5, 50, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET );
		for (Ballistica ray : cone.rays) bolt.accept( ray.path.get( ray.dist ) );
		FxTimeline rings = FxTimeline.start();
		ArrayList<Integer> scoured = new ArrayList<>( cone.cells );
		for (int c : scoured){
			final int ring = xyz.gabriwar.warpedpixeldungeon.Dungeon.level.distance( s.hero.pos, c );
			rings.at( 0.07f * ring, () -> s.center( c ).burst( HolyFlameParticle.FACTORY, 3 ) );
			for (CharSprite m : s.d){
				if (m.ch.pos != c) continue;
				rings.at( 0.07f * ring, () -> {
					m.flash();
					s.center( c ).burst( HolyFlameParticle.FACTORY, 7 );
				} );
			}
		}
	}

	//Shield of the Faithful at +3 holding three blows: the round shield's item hovering at his side,
	//a bubble on each catch, the second a shot from afar thrown back, the third breaking it
	static void shield( SkillPreviewDump.Scene s ){
		final boolean[] broken = { false };
		final CharSprite owner = s.h;
		ItemSprite shield = new ItemSprite( ItemSpriteSheet.ROUND_SHIELD ){
			private float time;
			@Override
			public void update(){
				super.update();
				time += Game.elapsed;
				visible = owner.visible && !broken[0];
				PointF c = owner.center();
				x = c.x + 5 - width() / 2f;
				y = c.y - 1 + (float) Math.sin( time * 3 ) * 1.5f - height() / 2f;
				alpha( 0.65f + 0.15f * (float) Math.sin( time * 4 ) );
			}
		};
		shield.scale.set( 0.6f );
		shield.hardlight( 0xFFE9A6 );
		owner.parent.add( shield );
		for (int k = 0; k < 3; k++){
			final int n = k;
			s.later( 0.4f + 0.6f * k, () -> {
				ShieldHalo halo = new ShieldHalo( owner );
				owner.parent.add( halo );
				halo.putOut();
				owner.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				if (n == 1) MagicMissile.boltFromChar( owner.parent, MagicMissile.LIGHT_MISSILE, owner, s.d[1].ch.pos, null );
				if (n == 2){
					broken[0] = true;
					s.center( s.hero.pos ).burst( Speck.factory( Speck.LIGHT ), 8 );
				}
			} );
		}
	}

	//a planted sword as SacredWeaponSwords.Planted drew it: the sword item tilted, tinted, bobbing
	static ItemSprite planted( final int cell ){
		ItemSprite sword = new ItemSprite(){
			private float time;
			@Override
			public void update(){
				super.update();
				time += Game.elapsed;
				PointF c = DungeonTilemap.tileCenterToWorld( cell );
				x = c.x - width / 2f;
				y = c.y - height / 2f - 3 + (float) Math.sin( time * 3 + cell );
				alpha( 0.6f + 0.2f * (float) Math.sin( time * 4 ) );
			}
		};
		sword.view( new Sword() );
		sword.origin.set( sword.width / 2f, sword.height / 2f );
		sword.angle = 135;
		sword.hardlight( 0xFFF1B8 );
		return sword;
	}
}
