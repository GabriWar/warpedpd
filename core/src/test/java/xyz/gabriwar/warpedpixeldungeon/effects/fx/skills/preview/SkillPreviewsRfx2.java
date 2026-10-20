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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;

/**
 * Round 2 (the owner's review of 10981) as the new code shows it: Sanctuary raised and its circle
 * consecrated round the cleric (reach 1 and reach 2), its ground mending, and its ground let go of.
 * Transcendence, Charged Shot's release and True Edge's stroke are the older registries' entries
 * (Holy's transcendence, Hunt's charged-release, RfxKit's ray-nature and ray-steel), which call the
 * skills' own code. The stagings are SkillPreviewsRfx2Before's (master f6f2758ae's calls only, so
 * that file runs them in a checkout of it); only the let-go differs: here the seal fades.
 */
public final class SkillPreviewsRfx2 {

	private SkillPreviewsRfx2(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "sanctuary", s -> SkillPreviewsRfx2Before.raised( s, 1 ) ).heroClass( HeroClass.CLERIC ).seconds( 2.5f );
		r.entry( "sanctuary-wide", s -> SkillPreviewsRfx2Before.raised( s, 2 ) ).heroClass( HeroClass.CLERIC ).seconds( 2.5f );
		r.entry( "sanctuary-mend", SkillPreviewsRfx2Before::mend ).heroClass( HeroClass.CLERIC ).rat( 4, 4 ).seconds( 2.5f );
		r.entry( "sanctuary-end", s -> {
			final SkillFieldFX fx = SkillPreviewsRfx2Before.held( s, 1 );
			s.later( 1.2f, fx::fadeAway );
		} ).heroClass( HeroClass.CLERIC ).seconds( 2f );
	}
}
