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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PrecisionPin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingSpear;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Callback;

public class Precision extends PassiveSkillB1 {

	//+3: how far the glint jumps from a marked enemy you killed
	private static final int HOP_RANGE = 3;

	{
		name = "Precision";
		image = 86;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//2 / 3 / 4 turns
	private float markTurns(){
		return 1 + level;
	}

	//the first melee cut on an unhurt enemy finds a gap in its guard.
	//onHitProc runs before the blow lands, so full health here means untouched
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || ranged || enemy == null || !enemy.isAlive() || enemy.HP < enemy.HT) return damage;
		mark( enemy, -1 );
		return damage;
	}

	//blows on a marked enemy go through the gap: its armour is not rolled
	@Override
	public boolean ignoresArmor( Char target ){
		return level > 0 && target != null && target.buff( Gap.class ) != null;
	}

	//+3: killing a marked enemy sends the glint on to the nearest enemy within 3 tiles
	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (level < MAX_LEVEL || mob == null || mob.buff( Gap.class ) == null) return;
		Mob next = null;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m == mob || !m.isAlive() || m.alignment != Char.Alignment.ENEMY
					|| m.buff( Gap.class ) != null
					|| Dungeon.level.distance( mob.pos, m.pos ) > HOP_RANGE
					|| !SkillInteractions.clear( mob.pos, m.pos )) continue;
			if (next == null || Dungeon.level.trueDistance( mob.pos, m.pos )
					< Dungeon.level.trueDistance( mob.pos, next.pos )) next = m;
		}
		if (next != null) mark( next, mob.pos );
	}

	private void mark( Char enemy, int from ){
		Buff.prolong( enemy, Gap.class, markTurns() );
		if (enemy.sprite != null){
			new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 4, 14 ).color( 0xE6EDF5, true ).show( enemy.sprite, 0.5f );
			enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "pinned" ) );
		}
		if (from >= 0) SkillInteractions.flare( from, 0xE6EDF5 );
		SpatialSound.play( Assets.Sounds.HIT_PARRY, enemy, 0.8f, 1.5f );
	}

	/** a gap found in this enemy's guard: blows on it ignore its armour */
	public static class Gap extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff {
		{
			type = buffType.NEGATIVE;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}
}
