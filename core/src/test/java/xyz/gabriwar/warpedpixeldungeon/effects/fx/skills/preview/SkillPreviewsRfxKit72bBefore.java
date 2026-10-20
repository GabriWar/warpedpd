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
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

/**
 * The second pass's kit restorations as 72b40617b drew them, for the entries SkillPreviewsHolyBefore
 * has no moment of: Regeneration's kill heal (the legacy Flare 5/16 in 66FF66 and the heal's crosses).
 * Before side only, and outside the dump's REGISTRIES: dumped with FXPREVIEW_REGISTRIES=Holy,RfxKit72b
 * in a checkout of 72b40617b (rfx/render.sh --vs72b reads it). 72b's calls only, so it compiles there.
 */
public final class SkillPreviewsRfxKit72bBefore {

	private SkillPreviewsRfxKit72bBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		//Regeneration at +3: the kill that leaves no enemy in sight, the hero mended
		r.entry( "regeneration-kill", s -> {
			new Flare( 5, 16 ).color( 0x66FF66, true ).show( s.h, 0.6f );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 + 3 );
		} ).heroClass( HeroClass.CLERIC );
	}
}
