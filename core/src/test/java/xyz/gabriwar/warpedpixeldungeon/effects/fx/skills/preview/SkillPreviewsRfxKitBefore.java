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
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.School;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The second pass's new kit entries as f0783eacd shows the same moments (its calls only, so this
 * compiles there): dumped once in a detached f0783eacd worktree (FXPREVIEW_SIDE=before
 * FXPREVIEW_REGISTRIES=RfxKit) into rfx/previews/kit/json-before-new. The entries f0783 already
 * had take their before from the base dump. Nothing showed before for runes-sheet and link-held.
 */
public final class SkillPreviewsRfxKitBefore {

	private SkillPreviewsRfxKitBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "regeneration-kill", s -> {
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xB3D699, PixelFlare.S, 0.6f );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 + 3 );
		} ).heroClass( HeroClass.CLERIC );
		r.entry( "bolt-rune", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.SPIRIT, null ) ).rat( 7, 4 );
		r.entry( "bolt-volley-4", s -> {
			for (CharSprite a : s.d) SkillBolt.fly( s.h.parent, s.h.center(), a.destinationCenter(), SkillBolt.SPIRIT, null );
		} ).heroClass( HeroClass.MAGE ).rat( 7, 2 ).rat( 8, 4 ).rat( 7, 6 ).rat( 5, 7 ).view( 1, 0, 9, 9 );
		r.entry( "link-shadow", s -> {
			DreadFX.tether( s.h.parent, s.d[0].center(), s.d[1].center(), null );
			s.later( 1.1f, () -> DreadFX.tether( s.h.parent, s.d[2].center(), s.d[3].center(), null ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 3 ).rat( 7, 4 ).rat( 4, 6 ).rat( 5, 6 ).seconds( 2f );
		r.entry( "link-steel", s -> {
			for (int p = 0; p < 2; p++){
				SkillMarkFX.tether( s.d[2 * p].ch, s.d[2 * p + 1].ch, 0xB9DEEE, 0.6f, -2 );
				SkillMarkFX.tether( s.d[2 * p].ch, s.d[2 * p + 1].ch, 0xB9DEEE, 0.6f, 2 );
			}
		} ).rat( 5, 2 ).rat( 6, 2 ).rat( 2, 6 ).rat( 6, 6 ).view( 0, 1, 9, 7 );
		r.entry( "link-drain", s -> SkillBolt.mote( s.d[0], s.h, SkillBolt.SOUL, null ) ).heroClass( HeroClass.MAGE ).rat( 6, 3 );
		r.entry( "ray-holy", s -> {
			StreakFX.show( s.hero.pos, s.d[0].ch.pos, 0xF1D99C, 0.3f, 0.15f );
			s.later( 0.7f, () -> StreakFX.show( s.hero.pos, s.d[1].ch.pos, 0xF1D99C, 0.3f, 0.15f ) );
		} ).heroClass( HeroClass.CLERIC ).rat( 7, 3 ).rat( 6, 6 );
		r.entry( "ray-nature", s -> StreakFX.show( s.h.center(), DungeonTilemap.raisedTileCenterToWorld( s.d[0].ch.pos ), 0xDDEFD0, 0.5f, 0.3f ) )
				.heroClass( HeroClass.HUNTRESS ).rat( 8, 3 );
		r.entry( "ray-steel", s -> StreakFX.show( s.hero.pos, s.d[0].ch.pos, 0xB9DEEE, 0.45f, 0.25f ) ).heroClass( HeroClass.DUELIST ).rat( 7, 4 );
		r.entry( "ray-heal", s -> SkillBolt.mote( s.h, s.d[0], SkillBolt.HEAL, null ) ).heroClass( HeroClass.CLERIC ).rat( 6, 4 );
		r.entry( "ray-stream", s -> s.h.parent.add( new Beam.LightRay( s.d[0].center(), s.d[1].center() ) ) )
				.heroClass( HeroClass.CLERIC ).rat( 4, 3 ).rat( 7, 5 );
		r.entry( "motes-sheet", s -> {
			for (int k = 0; k < 2; k++){
				s.later( 1.2f * k, () -> s.center( at( 2, 2 ) ).burst( Speck.factory( Speck.HEALING ), 3 ) );
			}
		} ).view( 0, 0, 11, 9 ).hero( 5, 7 ).rat( 8, 2 ).seconds( 2.4f );
		r.entry( "pulse-tone", s -> {
			for (int i = 0; i < PULSES.length; i++) PulseRingFX.at( PULSE_AT[i], PULSES[i], 20, 0.6f );
		} ).view( 0, 0, 11, 9 ).hero( 5, 8 ).seconds( 1.25f );
		r.entry( "lightning-tints", s -> {
			s.h.parent.add( new Lightning( at( 1, 2 ), at( 8, 2 ), null ).tint( Lightning.STORM ) );
			s.h.parent.add( new Lightning( at( 1, 6 ), at( 8, 6 ), null ).tint( Lightning.ARCANE ) );
		} ).view( 0, 0, 10, 9 ).hero( 9, 4 );
		r.entry( "palette-sheet", SkillPreviewsRfxKitBefore::palette ).view( 0, 0, 11, 9 ).hero( 10, 8 ).seconds( 1.25f );
	}

	/** The pulses as SkillPreviewsRfxKit has them (the same colours: only the first ring's moves). */
	static final int[] PULSES = { 0x58E08C, 0xF1D99C, 0xFF8418, 0x44CCFF, 0x9070F0, 0xB9DEEE };
	static final int[] PULSE_AT = { at( 2, 2 ), at( 5, 2 ), at( 8, 2 ), at( 2, 5 ), at( 5, 5 ), at( 8, 5 ) };

	//the palette sheet in f0783's colours: for each of SkillPalette's meanings, the ramp or colour the
	//skills used for it then (squares of MOTE_2 at x2, a cross, a rune)
	static void palette( SkillPreviewDump.Scene s ){
		Object[] old = {
				SkillBursts.HEAL, SkillBursts.BLOOD, SkillBursts.BLOOD, ramp( ShoutFX.FURY ), SkillBursts.FIRE,
				Element.FROST.ramp, Element.POISON.ramp, ramp( ShoutFX.DREAD ), SkillBursts.SOUL, Element.VOID.ramp,
				SkillBursts.HOLY, new Ramp( 0xFFFFFF, 0xE3D6F7, 0xB89CE4, 0x7A5BB0 ), new Ramp( 0xD8C0FF, 0xB080FF, 0x8822FF ),
				new Ramp( 0xFFFFFF, 0xCFE4FF, 0x66AAFF, 0x2F5FB0 ), SkillBursts.STORM, ramp( LanceFX.STEEL ), ramp( ShoutFX.MARTIAL ),
				new Ramp( School.NATURE.color ), Element.GOLD.ramp, SkillBursts.HEAL,
				Element.ARCANE.ramp, new Ramp( 0x66AAFF ), SkillBursts.HOLY, Element.VOID.ramp, ramp( LanceFX.STEEL ),
				new Ramp( 0xD8C0FF, 0xB080FF, 0x8822FF ), new Ramp( 0xCC3355 ) };
		for (int i = 0; i < old.length; i++){
			Ramp r = (Ramp) old[i];
			float x = 16 * (1 + i % 9), y = 16 * (1 + 2 * (i / 9));
			for (int k = 0; k < Math.min( 4, r.size() ); k++) pic( FxFrames.MOTE_2, r.stop( k ), x + 4 * k, y, 2 );
			int mid = r.stop( Math.min( 1, r.size() - 1 ) );
			pic( FxFrames.PLUS_5, mid, x, y + 7, 1 );
			pic( FxFrames.RUNE_3[i % 4], mid, x + 8, y + 8, 1 );
		}
		//the daze: the stun's stars were Speck.STAR's
		int daze = Ramp.lerp( Element.METAL.ramp.stop( 1 ), 0xCCAA44, 0.6f );
		pic( FxFrames.STAR4_5, daze, 16 * 1, 16 * 7, 1 );
	}

	static Ramp ramp( int[] stops ){
		return new Ramp( stops );
	}

	static void pic( int[] frame, int color, float x, float y, int scale ){
		FxPic p = new FxPic( SkillVfx.SHEET );
		p.additive = true;
		p.show( frame ).lit( color );
		p.scale.set( scale );
		p.x = x;
		p.y = y;
		GameScene.light( p );
	}
}
