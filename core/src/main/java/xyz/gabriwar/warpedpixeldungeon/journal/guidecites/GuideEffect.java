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

package xyz.gabriwar.warpedpixeldungeon.journal.guidecites;

/**
 * The game's particles a guide page can pour on a chip (ui/guide/CiteArt wires each to its
 * factory). Only what is safe off the game's world is here: nothing it makes reads the level,
 * the hero's sight, the effects budget's frame or the game's scene. Pouring:
 *
 *   FIRE           FlameParticle, a tongue of Flames in Element.FIRE (not FlameParticle.FACTORY:
 *                  its pour takes the frame's spawns and hides flames the hero does not see)
 *   GREEN_FIRE     ElmoParticle, a tongue of Flames in Element.ELMO
 *   SOUL_FIRE      HalomethaneFlameParticle, a tongue of Flames in Element.HALOMETHANE
 *   TOXIC_GAS      Speck.factory( Speck.TOXIC )
 *   PARALYTIC_GAS  Speck.factory( Speck.PARALYSIS )
 *   CONFUSION_GAS  Speck.factory( Speck.CONFUSION )
 *   CORROSIVE_GAS  Speck.factory( Speck.CORROSION )
 *   STENCH         Speck.factory( Speck.STENCH )
 *   SMOKE          Speck.factory( Speck.SMOKE )
 *   STORM_CLOUD    Speck.factory( Speck.STORM )
 *   BLIZZARD       Speck.factory( Speck.BLIZZARD )
 *   INFERNO        Speck.factory( Speck.INFERNO )
 *   STEAM          Speck.factory( Speck.STEAM )
 *   MIASMA         Speck.factory( Speck.MIASMA )
 *   POP_GAS        Speck.factory( Speck.POPGAS )
 *   SLOWING_GAS    Speck.factory( Speck.SLOW )
 *   DEPRESSANT     Speck.factory( Speck.DEPRESSANT )
 *   DIRT_CLOUD     Speck.factory( Speck.DIRTCLOUD )
 *   CORRUPTION     Speck.factory( Speck.CLOUDOFCORR )
 *   LIGHT          Speck.factory( Speck.LIGHT )
 *   BUBBLES        Speck.factory( Speck.BUBBLE )
 *   NOTES          Speck.factory( Speck.NOTE )
 *   HEARTS         Speck.factory( Speck.HEART )
 *   GOO            GooSprite.GooParticle, Goo's black drops, without the splat or ring they leave
 *                  on a level's floor
 *   FIREDAMP       FiredampParticle.haze, firedamp's yellow wisps (not its FACTORY: that takes
 *                  the cave's draught and hides them where the hero does not see)
 *
 * Bursting every 2-3 s:
 *
 *   HEALING        Speck.factory( Speck.HEALING )
 *   COINS          Speck.factory( Speck.COIN )
 *   ROCKS          Speck.factory( Speck.ROCK )
 *   BLOOD          BloodParticle.BURST
 *   ARCANE         PurpleParticle.BURST
 *   RAINBOW        RainbowParticle.BURST
 *   BONES          Speck.factory( Speck.BONE ), a skeleton's bones as it bursts
 *   BLAST          a bomb's fire as Explosion draws it: the core's flash, its tongues thrown up and
 *                  out, its embers (not BlastParticle.FACTORY: its Explosion lights, rings and
 *                  scorches the level's floor and shakes the camera)
 *   WEB            a web's painted silk, as WebParticle strings it on a cell, held a while
 *
 * Left out: the factories of BlastParticle and WebParticle (above), and whatever reaches the light
 * layer, the level or the budget's frame (ShadowParticle, SparkParticle, GasStyle/GasFactory,
 * FireBlobFx, the rest of effects/fx).
 */
public enum GuideEffect {
	FIRE, GREEN_FIRE, SOUL_FIRE,
	TOXIC_GAS, PARALYTIC_GAS, CONFUSION_GAS, CORROSIVE_GAS, STENCH, SMOKE, STORM_CLOUD, BLIZZARD,
	INFERNO, STEAM, MIASMA, POP_GAS, SLOWING_GAS, DEPRESSANT, DIRT_CLOUD, CORRUPTION,
	LIGHT, BUBBLES, NOTES, HEARTS, GOO, FIREDAMP,
	HEALING, COINS, ROCKS, BLOOD, ARCANE, RAINBOW, BONES, BLAST, WEB
}
