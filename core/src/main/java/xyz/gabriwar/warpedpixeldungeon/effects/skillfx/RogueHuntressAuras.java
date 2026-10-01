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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.AimedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Bombvoyage;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;

/**
 * The ambient auras of the Rogue's and the Huntress's stances: two slow motes in the stance's
 * colour, and one accent particle every second or so that says which stance it is (shadow
 * seeping up, a mote of light, a star, fuse smoke, a snowflake, a lick of flame). Each is a
 * bundled StanceAuraBuff, so it survives save/load and lets go when its stance is lowered.
 */
public final class RogueHuntressAuras {

	private RogueHuntressAuras(){}

	public static class ShadowLink extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return DoubleStab.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xB89CE4, 1, ShadowParticle.UP, 0.7f ); }
	}

	public static class Aimed extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return AimedShot.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xB3D699, 3, Speck.factory( Speck.LIGHT ), 0.8f ); }
	}

	public static class Double extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return DoubleShot.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xB3D699, 3, Speck.factory( Speck.STAR ), 0.9f ); }
	}

	public static class Fuse extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return Bombvoyage.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xDF9979, 2, SmokeParticle.FACTORY, 0.4f ); }
	}

	public static class Frost extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return FrostArrows.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xA4E9FF, 0, SnowParticle.FACTORY, 0.5f ); }
	}

	public static class Ember extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return EmberArrows.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xFF9A4A, 2, FlameParticle.FACTORY, 0.7f ); }
	}
}
