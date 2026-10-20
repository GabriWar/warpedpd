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


import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;

public class ManaShield extends SubSkill2 {

	{
		name = "Mana Shield";
		castText = "Arcane ward";
		image = 176;
		mana = 8;
		tier = 2;
	}

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			Buff.affect( hero, ManaShieldWard.class ).raise( SkillInteractions.ofHealth( hero.HT, 0.04f + 0.04f * level ) );
			hero.MP -= getManaCost();
			castTextYell();
			SpatialSound.play( Assets.Sounds.MELD, hero, 1f, 1.2f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
			raised( Dungeon.hero.sprite );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	/** a blow (never a tick of poison, fire or the like) is landing right now: the ward may break on it.
	 *  Skills are asked before shields in Hero.damage, so this runs just ahead of the ward's absorbDamage */
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		if (level > 0 && !Skill.isTickDamage( source )){
			ManaShieldWard.blowAt = Actor.now();
			//where it comes from: the side the ward takes it on
			ManaShieldWard.blowFrom = source instanceof Char ? ((Char) source).pos : -1;
		}
		return 0;
	}

	/** The ward's runes, mana blue: RAISED runes on an RX x RY ring round the middle, snapping in
	 *  GAP s apart and each held HOLD s before it fades (RuneFX.lock, WardFx's ward). */
	public static final int RAISED = 4;
	public static final float RX = 8f, RY = 4f, GAP = 0.08f, HOLD = 0.3f;

	/** The ward raised round a body: its runes locking in, one by one. Nothing held after. Any thread. */
	public static void raised( CharSprite s ){
		if (s != null) RuneFX.lock( s, RAISED, SkillPalette.RUNE_MANA, RX, RY, GAP, HOLD );
	}

	//a broken ward's shards sit in the weapon: each melee hit spends one as extra magic damage
	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || level <= 0 || hero == null || enemy == null) return damage;
		Charged charged = hero.buff( Charged.class );
		if (charged == null || charged.hits <= 0) return damage;
		charged.use();
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
			enemy.sprite.flash();
		}
		SpatialSound.play( Assets.Sounds.HIT_MAGIC, enemy, 1f, 1.1f );
		return damage + Math.round( com.watabou.utils.Random.NormalIntRange( 3, 8 ) * SkillInteractions.heroPower() );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** crystal shards of a broken ward, charged into the Battlemage's weapon */
	public static class Charged extends Buff {

		{
			type = buffType.POSITIVE;
		}

		int hits = 0;

		public void set( int hits ){
			this.hits = Math.max( this.hits, hits );
		}

		void use(){
			if (--hits <= 0) detach();
		}

		//only Mana Shield spends the shards: once it is gone or back at nothing (debug) they go too
		@Override
		public boolean act(){
			if (CurrentSkills.skillLevel( target, ManaShield.class ) <= 0){
				detach();
				return true;
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.RECHARGING; }

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight( 0.35f, 0.65f, 1f ); }

		@Override
		public String iconTextDisplay(){ return Integer.toString( hits ); }

		@Override
		public String desc(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "desc", hits );
		}

		private static final String HITS = "hits";

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( HITS, hits );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			hits = bundle.getInt( HITS );
		}
	}
}
