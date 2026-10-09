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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;


public class Retribution extends SubSkill3 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	private static final int BURST_DEBT = 10;
	private static final int BURST_RANGE = 2;

	{
		name = "Retribution";
		image = 179;
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

	//30% / 45% / 60% of what a blow took from you becomes a debt of light branded on the attacker
	private float share(){
		return 0.15f + 0.15f * level;
	}

	@Override
	public void onDamageTaken( int hpLost, int shieldLost, Object source ){
		if (level <= 0 || !(source instanceof Char) || source == Dungeon.hero) return;
		Char attacker = (Char) source;
		if (!attacker.isAlive() || attacker.alignment != Char.Alignment.ENEMY) return;
		int owed = Math.round( (hpLost + shieldLost) * share() );
		if (owed <= 0) return;
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( attacker, Debt.class ).owed += owed;
		if (attacker.sprite != null) attacker.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
	}

	//your next melee hit on a branded enemy pays the debt back as holy light, twice over on the unholy
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || ranged || enemy == null || !enemy.isAlive()) return damage;
		Debt debt = enemy.buff( Debt.class );
		if (debt == null || debt.owed <= 0) return damage;
		int owed = debt.owed;
		debt.detach();
		int paid = owed;
		if (enemy.properties().contains( Char.Property.UNDEAD ) || enemy.properties().contains( Char.Property.DEMONIC )) paid *= 2;
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
			enemy.sprite.flash();
		}
		SpatialSound.play( Assets.Sounds.HIT_MAGIC, enemy, 0.9f, 0.9f );

		//+3: a great debt bursts on into one more enemy nearby
		if (level >= MAX_LEVEL && owed >= BURST_DEBT){
			final Hero hero = Dungeon.hero;
			final int half = Math.max( 1, paid / 2 );
			for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
				if (m == enemy || m.alignment != Char.Alignment.ENEMY || !m.isAlive()
						|| Dungeon.level.distance( enemy.pos, m.pos ) > BURST_RANGE || !Dungeon.level.heroFOV[m.pos]) continue;
				final Mob next = m;
				if (enemy.sprite != null && m.sprite != null && hero.sprite != null && hero.sprite.parent != null)
					hero.sprite.parent.add( new Beam.LightRay( enemy.sprite.center(), m.sprite.center() ) );
				SkillInteractions.defer( () -> { if (next.isAlive()) next.damage( half, Retribution.this ); } );
				break;
			}
		}
		return damage + paid;
	}

	/** light owed back to this enemy for the blows it landed on the Paladin */
	public static class Debt extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff {

		{
			type = buffType.NEGATIVE;
		}

		int owed = 0;

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "owed", owed );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			owed = bundle.getInt( "owed" );
		}
	}
}
