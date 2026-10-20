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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;


public class SpellBlade extends SubSkill1 {

	{
		name = "Spell Blade";
		image = 177;
		tier = 1;
	}

	//what a proc turns into instead of mana when the pool is already full
	private static final int OVERFLOW_DAMAGE = 4;

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && com.watabou.utils.Random.Int(100) < 15 * level){
			Hero hero = Dungeon.hero;
			int effectiveMT = hero.MT + RingOfMagic.manaBonus( hero );
			//overflow: a full pool spills the drink into the blade instead
			if (level >= MAX_LEVEL && hero.MP >= effectiveMT){
				if (enemy.sprite != null){
					enemy.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
					enemy.sprite.flash();
				}
				inscribed( hero.sprite, enemy.sprite );
				SpatialSound.play( Assets.Sounds.HIT_MAGIC, enemy, 0.8f, 1.2f );
				return damage + OVERFLOW_DAMAGE;
			}
			hero.MP = Math.max( hero.MP, Math.min( effectiveMT, hero.MP + 1 + level ) );
			//the blade drinks, and it shows: mana's own blue light on her (never a wand recharging)
			if (hero.sprite != null){
				hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 2 + level );
				SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.4f, 1.6f );
			}
			inscribed( hero.sprite, enemy.sprite );
		}
		return damage;
	}

	/** The spell on the blow: SHUT runes of mana shutting on the struck in SHUT_FOR s (RuneFX.shut,
	 *  SHUT_RX x SHUT_RY), then a RUNE_5 stamped there in the mana's blue for STAMP s and SPARKS sparks
	 *  flicked off it (SPARK_2, 30 to 50 px/s, a drag of 4, SPARK_LIFE s); on the blade a ring of
	 *  HAND runes turning about the weapon hand (HAND_SIDE px toward the way she faces) for HAND_FOR
	 *  s. Nothing held after. Any thread. */
	public static final int SHUT = 4, SPARKS = 4, HAND = 6;
	public static final float SHUT_RX = 7f, SHUT_RY = 3f, SHUT_FOR = 0.15f, STAMP = 0.3f, SPARK_LIFE = 0.2f,
			HAND_SIDE = 5f, HAND_FOR = 0.3f;

	public static void inscribed( final CharSprite hero, final CharSprite struck ){
		if (struck != null) Fx.post( () -> {
			if (struck.parent == null || !struck.visible) return;
			RuneFX.shut( struck, SHUT, SkillPalette.RUNE_MANA, SHUT_RX, SHUT_RY, SHUT_FOR );
			FxTimeline.start().at( SHUT_FOR, () -> {
				if (struck.parent == null || !struck.visible) return;
				float x = SkillVfx.midX( struck ), y = SkillVfx.midY( struck );
				RuneFX.stamp( struck, FxRandom.element( FxFrames.RUNE_5 ), SkillPalette.MANA_LIGHT, STAMP );
				MoteFX.flick( x, y, SPARKS, FxFrames.SPARK_2, SkillPalette.MANA_LIGHT, 30f, 50f, 4f, 0f, SPARK_LIFE,
						struck.y + struck.height() - 2 - y, FxBudget.P2 );
			} );
		} );
		if (hero != null) Fx.post( () -> {
			if (hero.parent == null || !hero.visible) return;
			float x = SkillVfx.midX( hero ) + (hero.flipHorizontal ? -HAND_SIDE : HAND_SIDE);
			RuneFX.ring( x, SkillVfx.midY( hero ) + 1, HAND, SkillPalette.RUNE_MANA, RuneFX.RING_RX, RuneFX.RING_RY,
					RuneFX.RING_TURN, HAND_FOR );
		} );
	}
}
