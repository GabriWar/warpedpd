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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;


public class FinishingBlow extends SubSkill3 {

	{
		name = "Finishing Blow";
		castText = "Finish him!";
		image = 168;
		tier = 3;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//the share of the hit an enemy may be left with and still be executed: 20% / 35% / 50%
	private float share(){
		return 0.05f + 0.15f * level;
	}

	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null || !enemy.isAlive() || damage <= 0
				|| enemy.properties().contains(xyz.gabriwar.warpedpixeldungeon.actors.Char.Property.BOSS)
				|| enemy.properties().contains(xyz.gabriwar.warpedpixeldungeon.actors.Char.Property.MINIBOSS)) return damage;
		int remaining = enemy.HP + enemy.shielding() - damage;
		if (remaining <= 0 || remaining > damage * share()) return damage;

		castTextYell();
		Wound.hit( enemy );
		if (enemy.sprite != null && enemy.sprite.visible) enemy.sprite.emitter().burst( BloodParticle.FACTORY, 8 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 0.8f );
		Camera.main.shake( 1, 0.2f );
		//+3: the execution sprays blood into the eyes of every enemy beside it
		if (level >= MAX_LEVEL){
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
				xyz.gabriwar.warpedpixeldungeon.actors.Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( enemy.pos + n );
				if (ch == null || ch == enemy || ch.alignment != xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY || !ch.isAlive()) continue;
				Buff.prolong( ch, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness.class, 2f );
				if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst( BloodParticle.BURST, 6 );
			}
		}
		return enemy.HP + enemy.shielding() + damage;
	}
}
