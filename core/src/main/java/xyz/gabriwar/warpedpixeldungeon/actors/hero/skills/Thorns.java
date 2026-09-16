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


import com.watabou.noosa.audio.Sample;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;

public class Thorns extends SubSkill1 {

	private static final float THORN_TURNS = 20f;

	{
		name = "Thorns";
		image = 190;
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

	//arrows and thrown weapons leave a thorn in what they hit
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || level <= 0 || enemy == null || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY) return damage;
		Buff.affect( enemy, Thorned.class ).left = THORN_TURNS;
		if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]) enemy.sprite.emitter().burst( LeafParticle.GENERAL, 2 );
		return damage;
	}

	//the next time a thorned enemy moves on its own, the thorn sprouts barbed vines around it
	@Override
	public void onCharMoved( Char ch, int from, boolean travelling ){
		if (level <= 0 || ch == null || !travelling) return;
		Thorned thorn = ch.buff( Thorned.class );
		if (thorn == null) return;
		thorn.detach();
		final Char victim = ch;
		SkillInteractions.defer( () -> lash( victim ) );
	}

	private void lash( Char enemy ){
		if (!enemy.isAlive()) return;
		enemy.damage( 2 * level, this );
		boolean seen = enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos];
		if (seen){
			enemy.sprite.emitter().burst( LeafParticle.GENERAL, 5 );
			Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 0.6f, 1.4f );
		}
		if (level >= MAX_LEVEL && enemy.isAlive() && !enemy.properties().contains( Char.Property.BOSS )){
			Buff.prolong( enemy, Roots.class, 1f );
			if (seen) CellEmitter.bottom( enemy.pos ).burst( EarthParticle.FACTORY, 4 );
		}
	}

	/** a barb waiting under the hide for the enemy's next step */
	public static class Thorned extends Buff {

		{
			type = buffType.NEGATIVE;
		}

		float left = THORN_TURNS;

		@Override
		public boolean act(){
			left -= TICK;
			if (left <= 0) detach();
			else spend( TICK );
			return true;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "left", left );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			left = bundle.getFloat( "left" );
		}
	}
}
