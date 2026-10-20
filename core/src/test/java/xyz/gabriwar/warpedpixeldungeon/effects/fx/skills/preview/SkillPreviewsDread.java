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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DecoySprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonSprite;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.tweeners.AlphaTweener;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewsDreadBefore.ring;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewsDreadBefore.sprite;

/**
 * The dread-shadow package's moments as the game draws them now: the same stages as
 * SkillPreviewsDreadBefore, each skill's visual calls of this code restaged (CellEmitter's bursts
 * through the scene's stand-in emitters), at rank 3 where the rank shows.
 */
public final class SkillPreviewsDread {

	private SkillPreviewsDread(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "demoralize-hit", s -> demoralizeHit( s.d[0] ) ).rat( 4, 4 ).heroClass( HeroClass.WARRIOR );
		r.entry( "demoralize-terror", s -> {
			CharSprite e = s.d[0];
			demoralizeHit( e );
			e.emitter().burst( SkillBursts.TREMBLE, 1 );
			PixelFlare.show( SkillVfx.midX( e ), e.y + 2, DreadFX.MADNESS, PixelFlare.M, .4f );
			ShoutFX.dread( e, false, 0 );
		} ).rat( 4, 4 );
		r.entry( "lone-wolf-howl", s -> {
			s.h.emitter().burst( SkillBursts.ROAR, 3 );
			ShoutFX.howl( s.h, ShoutFX.WOLF, 3, 0.08f );
			float hx = SkillVfx.midX( s.h ), hy = SkillVfx.midY( s.h );
			for (CharSprite m : s.d){
				ShoutFX.flinch( m, hx, hy, DreadFX.WOLF_FLINCH, 0.06f + 0.04f * Dungeon.level.distance( s.hero.pos, m.ch.pos ) );
			}
		} ).heroClass( HeroClass.HUNTRESS ).rat( 4, 4 ).rat( 5, 3 ).rat( 2, 5 );
		r.entry( "lone-wolf-alone", s -> s.h.emitter().burst( SkillBursts.FLOURISH, 4 ) ).heroClass( HeroClass.HUNTRESS );
		r.entry( "dread-howl", SkillPreviewsDread::dreadHowl ).heroClass( HeroClass.ROGUE ).hero( 5, 4 ).view( 0, 0, 11, 9 )
				.rat( 7, 4 ).rat( 5, 2 ).rat( 3, 6 ).rat( 8, 6 );
		r.entry( "panic-harvest-reap", s -> {
			s.get( s.d[0].ch.pos ).burst( SkillBursts.SOUL_WISP, 5 );
			SkillBolt.fly( s.h.parent, s.d[0].center(), s.h.center(), SkillBolt.MANA,
					() -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 );
		r.entry( "panic-harvest-spread", s -> {
			int dead = s.d[0].ch.pos;
			for (int i = 1; i < s.d.length; i++){
				s.get( s.d[i].ch.pos ).burst( SkillBursts.DREAD, 4 );
				ShoutFX.creep( dead, s.d[i].ch.pos, 0.05f * (i - 1) );
				ShoutFX.dread( s.d[i], true, 0.05f * (i - 1) + 0.15f );
			}
			s.get( dead ).burst( SkillBursts.DREAD, 3 );
			PixelFlare.show( SkillVfx.cx( dead ), SkillVfx.cy( dead ), ShoutFX.DREAD[2], PixelFlare.M, 0.5f );
		} ).heroClass( HeroClass.ROGUE ).rat( 5, 4 ).rat( 6, 4 ).rat( 5, 3 ).rat( 6, 5 );
		r.entry( "hex", s -> {
			final CharSprite ch = s.d[0];
			//Hex.Curser: no character on the cell in the dump, so MagicMissile.boltFromChar's second way
			SkillBolt.fly( s.h.parent, s.h, ch.ch.pos, SkillBolt.HEX, () -> {
				ch.emitter().burst( SkillBursts.DREAD, 6 );
				ch.flash();
				ClosingRing.on( ch, 23, 5, 0.25f, ShoutFX.DREAD[3] );
			} );
		} ).heroClass( HeroClass.MAGE ).rat( 7, 4 );
		r.entry( "silent-death", s -> {
			Wound.hit( s.d[0].ch );
			s.get( s.d[0].ch.pos ).burst( SkillBursts.DREAD, 8 );
			DreadFX.sink( s.d[0] );
			s.h.emitter().burst( SkillBursts.INK_DRIFT, 6 );
			s.get( s.d[1].ch.pos ).burst( SkillBursts.INK_DRIFT, 3 );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 4 ).rat( 6, 3 );
		r.entry( "vanish", s -> {
			s.h.emitter().burst( SkillBursts.INK_DRIFT, 10 );
			DreadFX.vanish( s.h );
			//mob.sprite.hideLost() then showLost(): the game's lost icon (an EmoIcon wants the game's
			//scene, so the dump stands its picture where EmoIcon.Lost puts it)
			for (CharSprite m : s.d) lost( s, m );
			s.h.alpha( 0.4f );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 ).rat( 5, 2 );
		r.entry( "stealth-decoy", s -> {
			int cell = at( 4, 3 );
			sprite( s, new DecoySprite(), cell );
			s.get( cell ).burst( SkillBursts.INK_DRIFT, 8 );
			SkillSpectacleFX.show( SkillSpectacleFX.SHADOW, cell );
			s.h.emitter().burst( SkillBursts.INK_DRIFT, 6 );
		} ).heroClass( HeroClass.ROGUE ).rat( 7, 4 ).seconds( 2.5f );
		r.entry( "dark-pact", s -> {
			for (CharSprite m : s.d){
				m.emitter().burst( SkillBursts.DREAD, 6 );
				ShoutFX.dread( m, true, 0.1f );
			}
			s.h.emitter().burst( SkillBursts.INK_DRIFT, 6 );
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 4 );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 7 );
			s.h.flash();
			DreadFX.pact( s.h );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 4 ).rat( 3, 3 );
		r.entry( "ambush-stake", s -> {
			Wound.hit( s.d[0].ch );
			s.get( s.d[0].ch.pos ).burst( SkillBursts.DREAD, 8 );
			s.bottom( s.d[0].ch.pos ).burst( Speck.factory( Speck.DUST ), 4 );
			DreadFX.stake( s.d[0] );
		} ).heroClass( HeroClass.ROGUE ).rat( 5, 4 );
		r.entry( "shadow-link", s -> {
			final CharSprite a = s.d[0], b = s.d[1];
			s.center( a.ch.pos ).burst( SkillBursts.DREAD, 4 );
			DreadFX.tether( a.parent, a.center(), b.destinationCenter(), () -> {
				b.flash();
				b.emitter().burst( SkillBursts.DREAD, 4 );
			} );
		} ).heroClass( HeroClass.ROGUE ).rat( 5, 4 ).rat( 7, 2 );
		r.entry( "soul-drain", s -> {
			final CharSprite next = s.d[1];
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, next.ch.pos, SkillBolt.SOUL,
					() -> ClosingRing.on( next, 23, 9, 0.3f, DreadFX.SOUL[2] ) );
			next.emitter().burst( SkillBursts.SOUL_WISP, 6 );
		} ).heroClass( HeroClass.MAGE ).rat( 4, 5 ).rat( 7, 3 );
		r.entry( "soul-detonation", s -> {
			CharSprite pet = s.d[0];
			int origin = pet.ch.pos;
			SkillBolt.mote( pet, s.h, SkillBolt.SOUL, null );
			s.bottom( origin ).burst( SkillBursts.SOUL_WISP, 12 );
			pet.die();
			PixelFlare.show( SkillVfx.cx( origin ), SkillVfx.cy( origin ), DreadFX.SOUL[2], PixelFlare.M, 0.5f );
			s.later( 0.3f, () -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 ) );
		} ).heroClass( HeroClass.MAGE ).rat( 6, 4 );
		r.entry( "summon-skeleton", s -> {
			int grave = at( 4, 4 );
			final CharSprite body = sprite( s, new SkeletonSprite(), grave );
			body.alpha( 0 );
			s.h.parent.add( new AlphaTweener( body, 1, 0.6f ) );
			s.bottom( grave ).start( SkillBursts.INK_DRIFT, 0.06f, 10 );
			StaggerFX.after( 0.35f, () -> {
				s.get( grave ).burst( SkillBursts.GRIT, 6 );
				DreadFX.knit( body );
			} );
		} ).heroClass( HeroClass.MAGE );
		r.entry( "blackout", s -> {
			int cell = at( 6, 4 );
			s.get( cell ).burst( SkillBursts.INK_DRIFT, 6 );
			PixelFlare.show( SkillVfx.cx( cell ), SkillVfx.cy( cell ), ShoutFX.DREAD[3], PixelFlare.M, 0.6f );
			FxTimeline t = FxTimeline.start();
			for (int r0 = 0; r0 <= 2; r0++){
				final int ring = r0;
				t.at( 0.09f * r0, () -> {
					for (int c : ring( cell, ring )) s.get( c ).burst( SkillBursts.DREAD, 3 );
				} );
			}
			for (CharSprite m : s.d) s.get( m.ch.pos ).burst( SkillBursts.INK_DRIFT, 4 );
		} ).heroClass( HeroClass.ROGUE ).hero( 2, 4 ).rat( 6, 3 ).rat( 7, 5 );
		r.entry( "phantom-strike", s -> {
			SkillPreviewsDreadBefore.company( s, false );
			//this code's ink where the old rose a curse: the same bursts, swapped
			int[] dests = { at( 6, 3 ), at( 7, 4 ), at( 6, 5 ) };
			FxTimeline t = FxTimeline.start();
			for (int k = 0; k < dests.length; k++){
				final int dest = dests[k];
				t.at( 0.15f * k + 0.3f, () -> s.get( dest ).burst( SkillBursts.INK_DRIFT, 6 ) );
			}
			s.h.emitter().burst( SkillBursts.INK_DRIFT, 6 );
		} ).heroClass( HeroClass.ROGUE ).seconds( 1.6f );
		r.entry( "ninja-bomb", s -> {
			final int cell = at( 7, 4 );
			SkillFX.streak( s.h, cell, SkillFX.Streak.PELLET,
					() -> SkillPreviewsDreadBefore.ninjaSmoke( s, cell, Speck.factory( Speck.SMOKE ) ) );
		} ).heroClass( HeroClass.ROGUE ).seconds( 1.6f );
		r.entry( "master-thief-coin", s -> {
			final CharSprite ch = s.d[0];
			SkillFX.streak( s.h, ch.ch.pos, SkillFX.Streak.COIN, () -> {
				ch.emitter().burst( SkillBursts.GOLD_GLINT, 4 );
				DreadFX.star( SkillVfx.midX( ch ), ch.y + 4, Element.GOLD.light );
			} );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 );
		r.entry( "necrotoxin", s -> {
			CharSprite e = s.d[0];
			s.center( e.ch.pos ).burst( PoisonParticle.SPLASH, 10 );
			s.get( e.ch.pos ).burst( SkillBursts.ICHOR, 6 );
			PixelFlare.show( SkillVfx.midX( e ), SkillVfx.midY( e ), Element.POISON.light, PixelFlare.M, 0.6f );
			SkillPreviewsDreadBefore.necroSpray( s );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		r.entry( "venom", s -> {
			s.h.emitter().burst( SkillBursts.FLOURISH, 4 );
			DreadFX.coat( s.h, Element.POISON.light );
		} ).heroClass( HeroClass.ROGUE );
		r.entry( "stance-accents", s -> SkillPreviewsDreadBefore.accents( s, new Emitter.Factory[]{ SkillBursts.INK_DRIFT,
				Speck.factory( Speck.LIGHT ), SkillBursts.FLOURISH, SkillBursts.FUSE, SkillBursts.FROST_GLINT, SkillBursts.EMBER_MOTE } ) )
				.heroClass( HeroClass.HUNTRESS ).hero( 5, 7 ).view( 0, 0, 11, 9 ).seconds( 2.5f );
		r.entry( "aimed-pin", s -> {
			CharSprite e = s.d[0];
			s.get( e.ch.pos ).burst( SkillBursts.GRIT, 4 );
			s.bottom( e.ch.pos ).startDelayed( Speck.factory( Speck.DUST ), 0, 4, 0.15f );
			e.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
			DreadFX.pin( e, SkillVfx.midX( s.h ) );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 7, 4 );
		r.entry( "double-shot", s -> {
			s.h.emitter().burst( SkillBursts.FLOURISH, 6 );
			PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), 0xDF9979, PixelFlare.S, 0.6f );
			s.d[0].emitter().burst( SkillBursts.FLOURISH, 3 );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 6, 4 );
		r.entry( "bombvoyage-fuse", s -> {
			s.h.emitter().burst( SkillBursts.FUSE, 5 );
			s.later( 0.3f, () -> s.h.emitter().burst( SkillBursts.FUSE, 3 ) );
			s.later( 0.6f, () -> {
				int c0 = s.d[0].ch.pos;
				s.center( c0 ).burst( BlastParticle.FACTORY, 30 );
				PixelFlare.show( SkillVfx.cx( c0 ), SkillVfx.cy( c0 ), 0xFFAA44, PixelFlare.M, 0.4f );
				SkillPreviewsDreadBefore.blastSmoke( s, c0 );
			} );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 7, 4 ).seconds( 1.6f );
	}

	// ------------------------------------------------------------------ the moments now

	//EmoIcon.Lost's picture over a sprite, where it stands (its centre point 2.5 + w/4, 2.5 + 3h/4)
	static void lost( SkillPreviewDump.Scene s, CharSprite m ){
		Image i = Icons.get( Icons.LOST );
		i.x = m.x + m.width() - (2.5f + 0.25f * i.width());
		i.y = m.y - (2.5f + 0.75f * i.height());
		s.rig.stage.add( i );
	}

	//Demoralize.onHitProc's every blow
	static void demoralizeHit( CharSprite e ){
		e.emitter().burst( SkillBursts.TREMBLE, 1 );
		DreadFX.nerve( e );
	}

	//DreadHowl.execute's pictures: rats 0 and 1 terrified already (maddened), 2 and 3 not (mastery)
	static void dreadHowl( SkillPreviewDump.Scene s ){
		int hero = s.hero.pos;
		float hx = SkillVfx.midX( s.h ), hy = SkillVfx.midY( s.h );
		for (int i = 2; i < 4; i++){
			s.get( s.d[i].ch.pos ).burst( SkillBursts.DREAD, 4 );
			ShoutFX.flinch( s.d[i], hx, hy, DreadFX.DREAD_FLINCH, 0.06f * Dungeon.level.distance( hero, s.d[i].ch.pos ) );
		}
		FxTimeline t = FxTimeline.start();
		int w = SkillPreviewDump.W;
		t.at( 0.06f, () -> ShoutFX.howl( s.h, ShoutFX.DREAD, 5, 0.06f ) );
		for (int r = 1; r <= 5; r++){
			final int ring = r;
			t.at( 0.06f * r, () -> {
				for (int c : ring( hero, ring )){
					if (((c % w) + (c / w) + ring) % 2 == 0) s.get( c ).burst( SkillBursts.DREAD, 1 );
				}
			} );
		}
		for (int i = 0; i < 2; i++){
			final CharSprite mad = s.d[i];
			t.at( 0.06f * Dungeon.level.distance( hero, mad.ch.pos ), () -> {
				s.get( mad.ch.pos ).burst( SkillBursts.TREMBLE, 2 );
				s.get( mad.ch.pos ).burst( SkillBursts.DREAD, 3 );
				mad.flash();
				DreadFX.madness( mad );
			} );
		}
		s.h.emitter().burst( SkillBursts.INK_DRIFT, 8 );
	}
}
