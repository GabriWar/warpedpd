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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blackout;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blink;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The second pass's review fixes (rfx/fixes.md) as the new code shows them, for the moments no
 * other registry stages: Blink's cast in the storm's static, Sanctuary's field mending, Blackout
 * feeding a shadow, Frost Arrows' hoarfrost and thaw in frost's glints. The fixes that only change
 * code an older registry's staging already calls (Dark Pact's seal, Mana Shield's break, the
 * huntress's flares and rings, the fields, the raptor, the pin) are previewed through those entries;
 * frost-activate (SkillPreviewsHunt) and soul-drain-mend (SkillPreviewsRfxLinks) are restaged where
 * they are. Their before side: the base dump (rfx/previews/_base/json-f0783) or, for the new names,
 * SkillPreviewsRfxFixesBefore.
 */
public final class SkillPreviewsRfxFixes {

	private SkillPreviewsRfxFixes(){}

	public static void register( SkillPreviewDump.Registry r ){
		//Skill.castTextYell's cast for Blink: the rogue's silhouette, crackling in the storm's static
		r.entry( "blink-cast", s -> SkillPreviewsRfxRunes.cast( new Blink(), s.hero ) ).heroClass( HeroClass.ROGUE );
		//SkillField.act on a sanctuary: an ally hurt in it and the hurt cleric mended this turn
		r.entry( "sanctuary-mend", s -> {
			SkillPreviewsKitBefore.field( SkillField.SANCTUARY, SkillPreviewsKitBefore.around( 3, 4 ) );
			s.later( 0.3f, () -> {
				SkillField.mended( s.d[0].ch );
				SkillField.mended( s.hero );
			} );
		} ).heroClass( HeroClass.CLERIC ).rat( 4, 4 ).seconds( 1.5f );
		//Blackout at mastery: the dark falls at (5, 4) and feeds a hurt shadow two cells on
		r.entry( "blackout-fed", s -> Blackout.fed( s.d[0].ch, at( 5, 4 ), true ) )
				.heroClass( HeroClass.ROGUE ).hero( 2, 4 ).rat( 7, 4 ).seconds( 1.5f );
		//FrostArrows.onHitProc, chilled (not frozen): hoarfrost creeping up the struck
		r.entry( "frost-arrows-hit", s -> hoarfrost( s.d[0] ) ).heroClass( HeroClass.HUNTRESS ).rat( 6, 4 );
		//FrostArrows.execute (deactivate): the rime thawing off her tips
		r.entry( "frost-deactivate", s -> s.h.emitter().burst( SkillBursts.FROST_GLINT, 2 ) ).heroClass( HeroClass.HUNTRESS );
	}

	static void hoarfrost( CharSprite e ){
		e.bottomEmitter().burst( SkillBursts.FROST_GLINT, 3 );
		e.centerEmitter().startDelayed( SkillBursts.FROST_GLINT, 0, 3, 0.12f );
		e.emitter().startDelayed( SnowParticle.FACTORY, 0, 4, 0.24f );
	}
}
