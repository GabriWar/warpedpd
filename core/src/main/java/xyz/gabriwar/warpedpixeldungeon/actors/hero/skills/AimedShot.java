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


import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff;

public class AimedShot extends ActiveSkill1 {

	{
		name = "Aimed Shot";
		castText = "I see him";
		image = 93;
		tier = 1;
		mana = 3;
	}

	//when the last pin was paid for, so a kill in that same shot can hand the mana back
	private float paidAt = -1;

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 1f, 1.5f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			//by class, not by place: on another class's hero those places hold his own stances
			switchOff( hero, DoubleShot.class );
			switchOff( hero, Bombvoyage.class );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Double.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Fuse.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Aimed.class, true );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			SpatialSound.play( Assets.Sounds.DEGRADE, hero, 0.6f, 1.2f );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Aimed.class, false );
		}
	}

	//while on, each ranged hit spends mana to pin its target to the ground for 2/3/4 turns
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (!ranged || !active || level <= 0 || enemy == null || hero == null) return damage;
		//nothing to pin: a flyer or a fixture keeps your mana
		if (enemy.flying || Char.hasProp( enemy, Char.Property.IMMOVABLE )) return damage;
		if (hero.MP < getManaCost()) return damage;

		hero.MP -= getManaCost();
		paidAt = Actor.now();
		castTextYell();
		Buff.prolong( enemy, Roots.class, 1 + level );
		if (Dungeon.level.heroFOV[enemy.pos]){
			//the arrow drives it into the ground: dust kicks up, then settles in a second puff
			CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
			CellEmitter.bottom( enemy.pos ).startDelayed( Speck.factory( Speck.DUST ), 0, 4, 0.15f );
			if (enemy.sprite != null){
				enemy.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
				enemy.sprite.showStatus( CharSprite.NEGATIVE, "Pinned" );
			}
		}
		SpatialSound.play( Assets.Sounds.HIT_ARROW, enemy, 1f, 0.7f );
		return damage;
	}

	//+3: an aimed shot that kills gives its mana back
	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (!ranged || level < 3 || paidAt != Actor.now()) return;
		paidAt = -1;
		Hero hero = Dungeon.hero;
		hero.MP = Math.min( hero.MT, hero.MP + getManaCost() );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 );
			hero.sprite.showStatus( CharSprite.POSITIVE, "+" + getManaCost() );
		}
		SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.6f, 2f );
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	/** the three bow stances (Aimed Shot, Double Shot, Bombvoyage) take turns: one going up lowers another, if the hero has it */
	static void switchOff( Hero hero, Class<? extends Skill> stance ){
		Skill s = hero.heroSkills.get( stance );
		if (s != null) s.active = false;
	}
}
