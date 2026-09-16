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
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.PathFinder;

public class Ambush extends SubSkill1 {

	{
		name = "Ambush";
		image = 187;
		tier = 1;
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

	//the opener from hiding: the first surprise attack on each enemy, melee or thrown, drives a shadow
	//stake through its shadow and pins it
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || !(enemy instanceof Mob) || !enemy.isAlive()
				|| enemy.alignment != Char.Alignment.ENEMY || enemy.buff( Opened.class ) != null
				|| !((Mob) enemy).wasSurprisedByBlow()){
			return damage;
		}
		Buff.affect( enemy, Opened.class );
		Wound.hit( enemy );
		Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 1f, 0.7f );
		stake( enemy );

		//at mastery the stake splinters: every enemy beside the mark is staked too
		if (level >= Skill.MAX_LEVEL){
			Sample.INSTANCE.play( Assets.Sounds.CHAINS, 1f, 0.8f );
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( enemy.pos + n );
				if (ch instanceof Mob && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY){
					stake( ch );
				}
			}
		}
		return damage;
	}

	private void stake( Char ch ){
		if (ch.properties().contains( Char.Property.BOSS )) return;
		Buff.prolong( ch, Roots.class, 1 + level );
		//flyers shrug the stake off, and nothing is shown
		if (ch.buff( Roots.class ) == null) return;
		CellEmitter.get( ch.pos ).burst( ShadowParticle.UP, 8 );
		CellEmitter.bottom( ch.pos ).burst( Speck.factory( Speck.DUST ), 4 );
	}

	/** this enemy has already been opened on */
	public static class Opened extends Buff {
		@Override
		public int icon(){ return BuffIndicator.NONE; }
	}
}
