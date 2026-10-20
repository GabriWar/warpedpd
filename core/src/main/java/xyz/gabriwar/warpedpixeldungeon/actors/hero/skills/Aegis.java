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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AegisRecharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.AegisArcFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

/**
 * A shield of light that catches a heavy melee blow, swallows half of it and bashes
 * the attacker away, then needs a few turns to reform. At mastery the half it
 * swallowed is slammed back into the attacker.
 */
public class Aegis extends SubSkill1 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	public static final float REFORM = 8f;

	{
		name = "Aegis";
		image = 164;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}


	/** The arc's words: WARD holy runes snapping onto the arc's side (its painted direction toward
	 *  `from`), WARD_RX x WARD_RY round the middle on an arc SPREAD rad wide, GAP s apart, each held
	 *  HOLD s (RuneFX.lock). Any thread. */
	public static final int WARD = 2;
	public static final float WARD_RX = 9f, WARD_RY = 6f, SPREAD = 0.9f, GAP = 0.08f, HOLD = 0.2f;

	public static void warded( final CharSprite s, final int from ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float angle = (float) (AegisArcFX.facing( s, from ) * Math.PI / 4);
			RuneFX.lock( s, WARD, SkillPalette.RUNE_HOLY, WARD_RX, WARD_RY, GAP, HOLD, angle, SPREAD );
		} );
	}

	@Override
	public int onDefendProc( Char enemy, int damage ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| damage < Math.max( 1, hero.HT / 10 )
				|| !Dungeon.level.adjacent( hero.pos, enemy.pos )
				|| hero.buff( AegisRecharge.class ) != null){
			return damage;
		}

		int caught = damage / 2;
		damage -= caught;
		Buff.affect( hero, AegisRecharge.class, REFORM );

		//the aegis: an arc of gold light standing on the side the blow came from (never the
		//SHIELDED state's bubble), a star and a beat of light
		if (hero.sprite != null && hero.sprite.parent != null){
			AegisArcFX.flash( hero.sprite, enemy.pos, AegisArcFX.GOLD, 0.3f );
			warded( hero.sprite, enemy.pos );
			PixelFlare.show( SkillVfx.midX( hero.sprite ), SkillVfx.midY( hero.sprite ), 0xF1D99C, PixelFlare.M, 0.5f );
			PulseRingFX.around( hero.sprite, 0xF1D99C, 12, 0.35f );
			chipOn( hero );
		}
		SpatialSound.play( Assets.Sounds.HIT_PARRY, hero, 1f, 0.8f );
		SpatialSound.play( Assets.Sounds.HIT_STRONG, hero, 0.8f, 1.1f );
		Fx.shake( 1, 0.2f );

		//at mastery the swallowed half is slammed back into the attacker
		if (level >= MAX_LEVEL && caught > 0){
			enemy.damage( caught, this );
			if (enemy.sprite != null){
				enemy.sprite.flash();
				enemy.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			}
		}

		//the bash itself: 1/2/3 tiles straight away from you
		if (enemy.isAlive()){
			Ballistica trajectory = new Ballistica( enemy.pos, enemy.pos + (enemy.pos - hero.pos), Ballistica.MAGIC_BOLT );
			WandOfBlastWave.throwChar( enemy, trajectory, level, true, false, this );
		}
		return damage;
	}
}
