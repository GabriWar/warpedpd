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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blink;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The review fixes' new entries (rfx/fixes.md) as f0783eacd shows the same moments (its calls only,
 * so this compiles there): dumped once in a detached f0783eacd worktree (FXPREVIEW_SIDE=before
 * FXPREVIEW_REGISTRIES=RfxFixes) into rfx/previews/fixes/json-before-new. Blink cast in its
 * silhouette's shadow; the sanctuary's and Blackout's mends drew nothing; Frost Arrows' hoarfrost
 * and thaw were the game's mana sparkle (Speck.BLUE_LIGHT).
 */
public final class SkillPreviewsRfxFixesBefore {

	private SkillPreviewsRfxFixesBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "blink-cast", s -> {
			Skill k = new Blink();
			k.level = 1;
			SkillCastFX.play( k, s.hero );
		} ).heroClass( HeroClass.ROGUE );
		r.entry( "sanctuary-mend", s -> SkillPreviewsKitBefore.field( SkillField.SANCTUARY, SkillPreviewsKitBefore.around( 3, 4 ) ) )
				.heroClass( HeroClass.CLERIC ).rat( 4, 4 ).seconds( 1.5f );
		r.entry( "blackout-fed", s -> SkillSpectacleFX.fly( SkillSpectacleFX.SHADOW, at( 5, 4 ), s.d[0].ch.pos, 0, .45f ) )
				.heroClass( HeroClass.ROGUE ).hero( 2, 4 ).rat( 7, 4 ).seconds( 1.5f );
		r.entry( "frost-arrows-hit", s -> {
			CharSprite e = s.d[0];
			e.bottomEmitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
			e.centerEmitter().startDelayed( Speck.factory( Speck.BLUE_LIGHT ), 0, 3, 0.12f );
			e.emitter().startDelayed( SnowParticle.FACTORY, 0, 4, 0.24f );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 6, 4 );
		r.entry( "frost-deactivate", s -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 2 ) ).heroClass( HeroClass.HUNTRESS );
	}
}
