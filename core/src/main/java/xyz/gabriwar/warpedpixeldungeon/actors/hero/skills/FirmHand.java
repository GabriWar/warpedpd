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


import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class FirmHand extends PassiveSkillB1 {

	{
		name = "Firm Hand";
		image = 10;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	private int staggerTurns(){
		return 1 + 2 * level;
	}

	//the opening melee blow on an unhurt enemy staggers it: the next shove that moves it throws it
	//further and bursts it against whatever stops it (SkillInteractions.push reads the mark)
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null || !enemy.isAlive() || enemy.HP < enemy.HT) return damage;
		if (damage >= enemy.HP + enemy.shielding()) return damage;
		if (enemy.properties().contains( Char.Property.IMMOVABLE )) return damage;

		SkillInteractions.mark( enemy, SkillInteractions.Mark.STAGGER, level, staggerTurns() );
		if (enemy.sprite != null && enemy.sprite.visible){
			enemy.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 3 + level );
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "stagger" ) );
			Camera.main.shake( 1, 0.15f );
		}
		SpatialSound.play( Assets.Sounds.HIT_CRUSH, enemy, 0.8f, 0.9f );
		return damage;
	}

	//+3: an enemy killed while staggered sends the stagger through every enemy beside it
	@Override
	public void onKill( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, boolean ranged ){
		if (level < MAX_LEVEL || mob == null || SkillInteractions.get( mob, SkillInteractions.Mark.STAGGER ) == null) return;
		boolean spread = false;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( mob.pos + n );
			if (ch == null || ch == mob || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()
					|| ch.properties().contains( Char.Property.IMMOVABLE )) continue;
			SkillInteractions.mark( ch, SkillInteractions.Mark.STAGGER, level, staggerTurns() );
			if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 4 );
			spread = true;
		}
		if (spread){
			SpatialSound.play( Assets.Sounds.HIT_CRUSH, mob, 0.9f, 0.7f );
			Camera.main.shake( 1, 0.2f );
		}
	}
}
