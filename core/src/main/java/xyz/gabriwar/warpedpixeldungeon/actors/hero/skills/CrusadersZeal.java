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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;


import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.Random;

public class CrusadersZeal extends Skill {

	{
		name = "Crusader's Zeal";
		tag = "CBA";
		image = 152;
		tier = 4;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** flames of zeal at most: 2 / 3 / 4 */
	private int cap(){
		return 1 + level;
	}

	//zeal builds while you strike unopposed: each melee hit you land adds a flame, and each flame
	//makes your blows 10% heavier
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero = xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;
		if (level <= 0 || ranged || enemy == null || hero == null) return damage;
		Zeal zeal = Buff.affect( hero, Zeal.class );
		boolean full = zeal.flames >= cap();
		if (!full) zeal.flames++;
		int bonus = Math.round( damage * 0.1f * zeal.flames );
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), zeal.flames );

		//+3: a blow struck at full zeal bursts in light, blinding the enemies beside you
		if (level >= MAX_LEVEL && full){
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
				Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( hero.pos + n );
				if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
				Buff.prolong( ch, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness.class, 2f );
				if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
			}
			if (hero.sprite != null){
				new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 8, 26 ).color( 0xFFE070, true ).show( hero.sprite, 0.6f );
				PulseRingFX.around( hero.sprite, 0xFFE070, 20, 0.5f );
				hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "crippled" ) );
			}
			SpatialSound.play( Assets.Sounds.RAY, hero, 1f, 1.1f );
			zeal.flames = 0;
		}
		return damage + bonus;
	}

	//taking a blow snuffs the flames
	@Override
	public void onDamageTaken( int hpLost, int shieldLost, Object source ){
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero = xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;
		if (level <= 0 || hero == null) return;
		Zeal zeal = hero.buff( Zeal.class );
		if (zeal == null || zeal.flames <= 0) return;
		zeal.detach();
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.SMOKE ), 3 );
	}

	/** flames of zeal gathered by unopposed blows */
	public static class Zeal extends Buff {

		{
			type = buffType.POSITIVE;
		}

		int flames = 0;

		//only the skill snuffs the flames: once it is gone (taken away in the debug window) they go too
		@Override
		public boolean act(){
			if (CurrentSkills.skillLevel( target, CrusadersZeal.class ) <= 0){
				detach();
				return true;
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.FIRE; }

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight( 1f, 0.9f, 0.4f ); }

		@Override
		public String iconTextDisplay(){ return Integer.toString( flames ); }

		@Override
		public String desc(){
			return Messages.get( this, "desc", flames );
		}

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "flames", flames );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			flames = bundle.getInt( "flames" );
		}
	}
}
