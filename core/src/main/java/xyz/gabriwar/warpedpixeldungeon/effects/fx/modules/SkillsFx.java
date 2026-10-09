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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WhirlHitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import com.watabou.utils.PointF;

/**
 * Skills (the skills area: docs/fx/skills.md once written): every class skill's casts, arcs,
 * rings, pillars, streaks, marks, fields, spectacles and impacts.
 *
 * Seeded by the effects kit with a gallery page of today's, the "before" its builder works from.
 */
public class SkillsFx implements FxModule {

	@Override
	public String key(){
		return "skills";
	}

	@Override
	public String title(){
		return "Skills";
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "a crescent spin", 2f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -1, 1 ) );
			ArcSpinFX.slash( p.sprite, 0xFFE8A0, true );
			s.after( 0.6f, () -> ArcSpinFX.around( p.sprite, 0xA0D0FF, 14, 0.8f, 0, 720, 0.6f ) );
		} );
		page.add( "a pulse ring", 2f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -1, 1 ) );
			PulseRingFX.around( p.sprite, 0xFFD060, 20, 0.6f );
		} );
		page.add( "a pillar rising", 2.5f, s -> PillarRiseFX.show( s.cell( 0, -2 ), 0xFFE080, null ) );
		page.add( "a streak", 2f, s -> StreakFX.show( s.cell( -3, 1 ), s.cell( 2, 1 ), 0xFFFFFF, 3, 0.4f ) );
		page.add( "spectacles: hammer, shuriken, thorn, wings, jaw", 4f, s -> {
			int[] kinds = { SkillSpectacleFX.HAMMER, SkillSpectacleFX.SHURIKEN, SkillSpectacleFX.THORN,
					SkillSpectacleFX.WINGS, SkillSpectacleFX.JAW };
			for (int i = 0; i < kinds.length; i++){
				final int kind = kinds[i];
				s.after( 0.7f * i, () -> SkillSpectacleFX.show( kind, s.dryRat.pos ) );
			}
		} );
		page.add( "a warrior's impacts on the floor and in the pool", 2.5f, s -> {
			WarriorImpactFX.show( s.cell( -1, -1 ) );
			WarriorImpactFX.show( s.cell( 3, 0 ), true );
		} );
		page.add( "a whirl's hits", 2f, s -> {
			WhirlHitFX.show( s.dryRat.pos );
			WhirlHitFX.show( s.skeleton.pos );
		} );
		page.add( "an elemental orbit's bursts", 2f, s -> {
			PointF a = FxStage.center( s.cell( -2, -1 ) ), b = FxStage.center( s.cell( 0, -1 ) );
			if (s.dryRat.sprite == null) return;
			ElementalOrbitFX.burst( s.dryRat.sprite.parent, a.x, a.y, 0, 3, 10 );
			ElementalOrbitFX.burst( s.dryRat.sprite.parent, b.x, b.y, 1, 3, 10 );
		} );
		page.add( "a pillar of light and a landing", 2.5f, s -> {
			SkillFX.pillar( s.cell( 2, 3 ), 0xA0E0FF );
			SkillFX.land( s.cell( -2, 3 ) );
		} );
	}
}
