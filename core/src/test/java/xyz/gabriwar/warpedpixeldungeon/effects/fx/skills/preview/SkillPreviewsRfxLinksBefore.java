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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The links package's new entries (rfx plan.md section 7) as f0783eacd shows the same moments (its
 * calls only, so this compiles there): dumped once in a detached f0783eacd worktree
 * (FXPREVIEW_SIDE=before FXPREVIEW_REGISTRIES=RfxLinks) into rfx/previews/links/json-before-new.
 * The entries f0783 already had take their before from the base dump. Its helpers (held, launch)
 * play the same calls on both sides: SkillPreviewsRfxLinks uses them too.
 */
public final class SkillPreviewsRfxLinksBefore {

	private SkillPreviewsRfxLinksBefore(){}

	static final HeroClass CLERIC = HeroClass.CLERIC;

	public static void register( SkillPreviewDump.Registry r ){
		//Shadow Link's marks held: the rope in pink between a pair, the target icon on a lone one
		r.entry( "shadow-link-held", SkillPreviewsRfxLinksBefore::held ).heroClass( HeroClass.ROGUE )
				.rat( 5, 3 ).rat( 7, 5 ).rat( 5, 6 ).seconds( 2.5f );
		//Soul Drain's mastery: the soul home through the Warlock, his HP mended, the wisps alone
		r.entry( "soul-drain-mend", s -> {
			s.d[0].die();
			SkillBolt.fly( s.h.parent, s.d[0].ch.pos, s.hero.pos, SkillBolt.SOUL, null );
			s.h.emitter().burst( SkillBursts.SOUL_WISP, 4 );
		} ).heroClass( HeroClass.MAGE ).rat( 7, 4 );
		//Avatar of Light's +3 mending an ally inside the halo: nothing showed it
		r.entry( "avatar-halo-mend", s -> HolyFx.halo( s.h, 2 ) ).heroClass( CLERIC ).rat( 4, 3 ).seconds( 1.5f );
		//Cinder Trail: an orb of fire and one of frost launched at two enemies
		r.entry( "cinder-launch", SkillPreviewsRfxLinksBefore::launch ).heroClass( HeroClass.MAGE ).rat( 7, 3 ).rat( 7, 6 );
		//a shards mark mending an ally: a pale gold flare
		r.entry( "shards-heal", s -> PixelFlare.show( SkillVfx.midX( s.d[0] ), SkillVfx.midY( s.d[0] ), 0xFFF1AB, PixelFlare.S, .2f ) ).rat( 5, 4 );
		//stepping on a soul mote: a lilac flare
		r.entry( "soul-field-mana", s -> PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0xBCA9FF, PixelFlare.S, .2f ) )
				.heroClass( HeroClass.MAGE );
	}

	/** Shadow Link held: rats 0 and 1 linked to each other, rat 2 linked to itself (the focus), each
	 *  with the mark's SkillMarkFX (the rats made actors for the marks' lookups, gone at the end). */
	static void held( SkillPreviewDump.Scene s ){
		final Char a = s.d[0].ch, b = s.d[1].ch, c = s.d[2].ch;
		Actor.add( a );
		Actor.add( b );
		Actor.add( c );
		mark( s, a, b );
		mark( s, b, a );
		mark( s, c, c );
		s.later( 2.4f, () -> {
			Actor.remove( a );
			Actor.remove( b );
			Actor.remove( c );
		} );
	}

	private static void mark( SkillPreviewDump.Scene s, Char on, Char with ){
		SkillInteractions.Mark m = new SkillInteractions.Mark();
		m.kind = SkillInteractions.Mark.CUT;
		m.rank = 3;
		m.target = on;
		m.other = with.id();
		s.h.parent.add( new SkillMarkFX( m ) );
	}

	/** Cinder Trail's launches: a fire orb at rat 0, a frost orb at rat 1 a beat later, each flashing
	 *  its target on its impact. */
	static void launch( SkillPreviewDump.Scene s ){
		if (s.hero.sprite == null) s.hero.sprite = s.h;
		final CharSprite f = s.d[0], i = s.d[1];
		ElementalOrbitFX.launch( s.hero, f.ch.pos, 0, 2, f::flash );
		s.later( 0.25f, () -> ElementalOrbitFX.launch( s.hero, i.ch.pos, 1, 2, i::flash ) );
	}
}
