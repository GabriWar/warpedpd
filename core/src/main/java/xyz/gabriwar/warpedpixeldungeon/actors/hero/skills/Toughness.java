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


import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * Warrior: a heavy blow makes the hero grit his teeth and push the pain back, so the blow
 * lands over the next turns instead of all at once. Fully trained, killing the enemy that
 * dealt it lets the rest of the pain go.
 */
public class Toughness extends PassiveSkillA3 {

	{
		name = "Toughness";
		image = 3;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a blow worth a tenth of the hero's health is pushed back: it lands over the next turns instead
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0 || !(source instanceof Char) || source == hero) return 0;
		if (damage * 10 < hero.HT || hero.buff( HeldPain.class ) != null) return 0;

		Buff.affect( hero, HeldPain.class ).set( damage, 1 + level, ((Char) source).id() );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 6 );
			hero.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "grit" ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.STURDY, 1f, 0.8f );
		return damage;
	}

	//+3: killing the enemy whose blow is still being held back lets the rest of it go
	@Override
	public void onEnemyDeath( Mob mob, Object cause ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || mob == null) return;
		HeldPain pain = hero.buff( HeldPain.class );
		if (pain == null || pain.attacker != mob.id()) return;
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "spared", pain.left ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.3f );
		pain.detach();
	}

	/** a heavy blow the hero pushed back, landing a share each turn; damage over time, so skills don't soften it */
	public static class HeldPain extends Buff implements Buff.DOTbuff {

		{
			type = buffType.NEGATIVE;
		}

		int left = 0;
		int turns = 0;
		int attacker = -1;

		void set( int damage, int turns, int attacker ){
			left = damage;
			this.turns = turns;
			this.attacker = attacker;
			if (target != null) target.needsIncomingDOTUpdate = true;
		}

		@Override
		public boolean act(){
			if (!target.isAlive() || left <= 0){
				detach();
				return true;
			}
			int tick = (int) Math.ceil( left / (float) Math.max( 1, turns ) );
			left -= tick;
			turns--;
			if (target.sprite != null && target.sprite.visible){
				target.sprite.emitter().burst( BloodParticle.FACTORY, 3 );
			}
			target.damage( tick, this );
			if (target == Dungeon.hero && !target.isAlive()){
				Dungeon.fail( this );
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.n( Messages.get( this, "ondeath" ) );
			}
			if (left <= 0 || turns <= 0) detach();
			else spend( TICK );
			target.needsIncomingDOTUpdate = true;
			return true;
		}

		@Override
		public void detach(){
			if (target != null) target.needsIncomingDOTUpdate = true;
			super.detach();
		}

		@Override
		public int totalIncomingDMG(){ return left; }

		@Override
		public int icon(){ return BuffIndicator.ARMOR; }

		@Override
		public void tintIcon( Image icon ){ icon.hardlight( 1f, 0.3f, 0.25f ); }

		@Override
		public String iconTextDisplay(){ return Integer.toString( left ); }

		@Override
		public String desc(){
			return Messages.get( this, "desc", left, turns );
		}

		private static final String LEFT = "left";
		private static final String TURNS = "turns";
		private static final String ATTACKER = "attacker";

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( LEFT, left );
			bundle.put( TURNS, turns );
			bundle.put( ATTACKER, attacker );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			left = bundle.getInt( LEFT );
			turns = bundle.getInt( TURNS );
			attacker = bundle.contains( ATTACKER ) ? bundle.getInt( ATTACKER ) : -1;
		}
	}
}
