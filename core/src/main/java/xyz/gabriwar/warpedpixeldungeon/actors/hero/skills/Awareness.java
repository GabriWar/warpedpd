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
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WindParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * Huntress: a shot from a distance is slipped, and an arrow goes straight back at
 * whoever loosed it. Fully trained she blurs a tile aside as it happens, and the
 * afterimage she leaves where she stood keeps drawing enemies for a few turns.
 */
public class Awareness extends PassiveSkillA2 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	private static final float RETURN_DAMAGE = 0.5f;
	private static final float PIN_TURNS = 2f;

	{
		name = "Awareness";
		image = 75;
		tier = 2;
	}

	@Override
	public boolean rangedSource(){ return true; }

	//you read each enemy as it comes: its first attack on you, melee or ranged, can be slipped
	@Override
	public boolean dodgeChance( Char attacker ){
		Hero hero = Dungeon.hero;
		return level > 0 && hero != null && attacker != null && attacker != hero
				&& attacker.alignment == Char.Alignment.ENEMY && attacker.buff( Read.class ) == null
				&& Random.Int( 100 ) < 10 + 10 * level;
	}

	@Override
	public void onDodge( Char attacker ){
		final Hero hero = Dungeon.hero;
		if (hero == null || attacker == null) return;
		Buff.affect( attacker, Read.class );
		castTextYell();
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 1.4f );
		//answered once the enemy's own attack is over
		SkillInteractions.defer( () -> {
			if (hero.isAlive()) returnFire( hero, attacker );
		} );
	}

	@Override
	public void onDodgeFailed( Char attacker ){
		if (attacker != null && attacker.isAlive()) Buff.affect( attacker, Read.class );
	}

	private void returnFire( Hero hero, Char attacker ){
		SpiritBow bow = hero.belongings.getItem( SpiritBow.class );
		if (bow == null || !attacker.isAlive() || attacker.alignment != Char.Alignment.ENEMY
				|| new Ballistica( hero.pos, attacker.pos, Ballistica.PROJECTILE ).collisionPos != attacker.pos) return;
		SpiritBow.SpiritArrow arrow = bow.knockArrow();
		int damage = Math.max( 1, Math.round( arrow.damageRoll( hero ) * RETURN_DAMAGE ) );
		attacker.damage( damage, this );
		if (hero.sprite != null) hero.sprite.zap( attacker.pos );
		SpatialSound.play( Assets.Sounds.ATK_SPIRITBOW, hero, 1f, 1.3f );
		SkillFX.streak( hero.sprite, attacker.pos, arrow, () -> {
			SkillFX.flash( attacker );
			SpatialSound.play( Assets.Sounds.HIT_ARROW, attacker, 1f, 1.1f );
		} );
		//+3: the return arrow pins the attacker, so a melee enemy can't follow when you step back
		if (level >= MAX_LEVEL && attacker.isAlive() && !Char.hasProp( attacker, Char.Property.BOSS )){
			Buff.prolong( attacker, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots.class, PIN_TURNS );
		}
	}

	/** this enemy's first attack on you has already been read */
	public static class Read extends Buff {
		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
