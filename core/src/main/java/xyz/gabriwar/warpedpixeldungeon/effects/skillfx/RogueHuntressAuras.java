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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;

/**
 * The ambient auras of the Rogue's and the Huntress's stances: two slow motes in the stance's
 * colour, and one accent particle every second or so that says which stance it is, in the skills'
 * own words (SkillBursts) and never as a state the hero is not in: ink drifting off him (Shadow
 * Link; never a curse's shadow rising), a mote of light (Aimed Shot), her own sparkle (Double Shot;
 * never a level-up's stars), a fuse's sparks (Bombvoyage; never smoke, as if she burned), a frost
 * glint standing still (Frost Arrows; never snow falling on her, a chill), an ember mote rising
 * (Ember Arrows; never a flame's tongue, Burning). Their motes take the colours by meaning
 * (SkillPalette): the huntress's leaf green for her bow (never the old sage that read cream), a
 * lit fuse's hot gold, fire's orange for the pitch (never peach). Each keeps its old rate. Each is a bundled
 * StanceAuraBuff, so it survives save/load and lets go when its stance is lowered.
 */
public final class RogueHuntressAuras {

	private RogueHuntressAuras(){}

	public static class ShadowLink extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return DoubleStab.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xB89CE4, SkillBursts.INK_DRIFT, 0.7f ); }
	}

	public static class Aimed extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return AimedShot.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, SkillPalette.NATURE_LIGHT, Speck.factory( Speck.LIGHT ), 0.8f ); }
	}

	public static class Double extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return DoubleShot.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, SkillPalette.NATURE_LIGHT, SkillBursts.FLOURISH, 0.9f ); }
	}

	public static class Fuse extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return Bombvoyage.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, SkillPalette.EMBER_HOT, SkillBursts.FUSE, 0.4f ); }
	}

	public static class Frost extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return FrostArrows.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, 0xA4E9FF, SkillBursts.FROST_GLINT, 0.5f ); }
	}

	public static class Ember extends StanceAuraBuff {
		@Override protected Class<? extends Skill> stance(){ return EmberArrows.class; }
		@Override protected StanceAuraFX build(){ return new StanceAuraFX( target, SkillPalette.FIRE.stop( 1 ), SkillBursts.EMBER_MOTE, 0.7f ); }
	}
}
