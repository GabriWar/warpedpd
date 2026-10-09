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


import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Berserk;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Carnage extends SubSkill1 {

	{
		name = "Carnage";
		image = 171;
		tier = 1;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//every melee kill stokes the berserker's rage by 10% per level. Berserk.damage() is the
	//rage's own intake (power grows by a quarter of the share of max health it is fed),
	//so 0.4 * HT per level is +10% power; it takes nothing while already berserking.
	//Only a Berserker has it at home; borrowed by another hero (debug), it starts a rage of his own
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || level <= 0 || hero == null) return;
		Buff.affect( hero, Berserk.class ).damage( Math.round( hero.HT * 0.4f * level ) );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 2 + level );
		}
		SpatialSound.play( Assets.Sounds.CHALLENGE, hero, 0.5f, 1.3f );

		//fully trained, a kill while berserking pours back into the berserk shield
		Berserk berserk = hero.buff( Berserk.class );
		if (level >= MAX_LEVEL && berserk != null && berserk.berserking()){
			int top = Math.max( 1, Math.round( hero.HT * 0.05f ) );
			berserk.incShield( top );
			CellEmitter.center( mob.pos ).burst( BloodParticle.BURST, 10 );
			if (hero.sprite != null){
				new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 6, 18 ).color( 0xFF2222, true ).show( hero.sprite, 0.5f );
				hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( top ), xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.SHIELDING );
			}
		}
	}
}
