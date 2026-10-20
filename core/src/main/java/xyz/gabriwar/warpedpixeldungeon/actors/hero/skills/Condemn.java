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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class Condemn extends Skill {

	{
		name = "Condemn";
		tag = "D1";
		image = 151;
		tier = 1;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null || !enemy.isAlive())
			return damage;

		//at level 3 the undead and the demonic are always branded, and the brand burns them
		boolean holy = level >= MAX_LEVEL
				&& (Char.hasProp( enemy, Char.Property.UNDEAD ) || Char.hasProp( enemy, Char.Property.DEMONIC ));
		if (!holy && Random.Int(100) >= 12 * level)
			return damage;

		Buff.prolong( enemy, Vulnerable.class, 3 + level );
		CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.LIGHT ), 3 );
		if (enemy.sprite != null){
			chipOn( enemy );
			PixelFlare.show( SkillVfx.midX( enemy.sprite ), SkillVfx.midY( enemy.sprite ), 0xF1D99C, PixelFlare.M, 0.35f );
			branded( enemy.sprite );
			if (holy) enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
		}
		//a judgement charging, never a curse's sound
		SpatialSound.play( Assets.Sounds.CHARGEUP, enemy, 0.6f, holy ? 1.1f : 1.4f );
		if (holy)
			enemy.damage( 6, this );

		return damage;
	}

	/** The brand (it is Vulnerable): SEAL holy runes on a SEAL_RX x SEAL_RY ring shutting on the body's
	 *  middle in SEAL_FOR s (RuneFX.shut), then the seal stamped there, RUNE_5's ring lit F1D99C for
	 *  STAMP s. On the body, never over the head; no reticle ring. Any thread. */
	public static final int SEAL = 4;
	public static final float SEAL_RX = 9f, SEAL_RY = 4.5f, SEAL_FOR = 0.2f, STAMP = 0.4f;

	public static void branded( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			RuneFX.shut( s, SEAL, SkillPalette.RUNE_HOLY, SEAL_RX, SEAL_RY, SEAL_FOR );
			FxTimeline.start().at( SEAL_FOR, () -> {
				if (s.parent != null && s.visible) RuneFX.stamp( s, FxFrames.RUNE_5[2], SkillPalette.mid( SkillPalette.RUNE_HOLY ), STAMP );
			} );
		} );
	}
}
