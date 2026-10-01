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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;


import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.util.ArrayList;

public class MassHeal extends SubSkill2 {

	private static final float MEND = 0.04f;
	private static final int REACH = 2;
	private static final int FADE_DAMAGE = 13;

	{
		name = "Mass Heal";
		castText = "Be mended!";
		image = 182;
		mana = 15;
		tier = 2;
	}

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && canPayMana( hero, getManaCost() ))
			actions.add(AC_CAST);
		return actions;
	}

	//seraph wings settle over the priest and mend him and every ally within 2 tiles each turn
	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && canPayMana( hero, getManaCost() )){
			if (!payMana( hero, getManaCost() )) return;
			Buff.affect( hero, Wings.class ).set( level, 2 + level );
			SkillSpectacleFX.show( SkillSpectacleFX.WINGS, hero.pos );
			new Flare( 8, 28 ).color( 0xAAFFAA, true ).show( hero.sprite, 1f );
			hero.sprite.emitter().burst( ShaftParticle.FACTORY, 6 );
			//the wings settle: one wide green pulse out to the reach of the mend, a warm second beat, motes after
			PulseRingFX.around( hero.sprite, 0xAAFFAA, 40, 0.7f );
			FxTimeline.start()
					.at( 0.2f, () -> PulseRingFX.around( hero.sprite, 0xFFF1A1, 24, 0.55f ) )
					.at( 0.45f, () -> hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 5 ) );
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.CHARMS, 1f, 1.0f );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** the wings over the priest: a mend each turn, a strike as they fade (+2), overflow hardening into shards (+3) */
	public static class Wings extends Buff implements SkillInteractions.HeroDamageSource {

		{
			type = buffType.POSITIVE;
		}

		int rank = 1;
		int left = 3;

		public void set( int rank, int turns ){
			this.rank = rank;
			this.left = turns;
		}

		@Override
		public boolean rangedSource(){ return false; }

		@Override
		public boolean act(){
			if (!(target instanceof Hero) || !target.isAlive()){
				detach();
				return true;
			}
			Hero hero = (Hero) target;
			mend( hero, hero );
			for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0] )){
				if (m.alignment == xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ALLY && m.isAlive() && Dungeon.level.distance( hero.pos, m.pos ) <= REACH)
					mend( hero, m );
			}
			if (--left <= 0){
				//+2: the wings strike the enemies around as they fade
				if (rank >= 2){
					for (int c : SkillInteractions.area( hero.pos, REACH )){
						xyz.gabriwar.warpedpixeldungeon.actors.Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( c );
						if (ch == null || ch.alignment != xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY || !ch.isAlive()) continue;
						ch.damage( FADE_DAMAGE, this );
						SkillInteractions.flare( c, 0xFFF1A1 );
					}
					Sample.INSTANCE.play( Assets.Sounds.RAY, 0.8f, 1.2f );
				}
				detach();
			} else {
				spend( TICK );
			}
			return true;
		}

		private void mend( Hero hero, xyz.gabriwar.warpedpixeldungeon.actors.Char ch ){
			int mend = SkillInteractions.ofHealth( ch.HT, MEND );
			int got = Math.min( mend, ch.HT - ch.HP );
			ch.HP += got;
			if (ch == hero){
				AsceticVow.tithe( hero, got );
				//+3: healing past full hardens into light shards that catch blows and cut back
				if (rank >= MAX_LEVEL && mend - got > 0){
					SkillInteractions.Mark shards = SkillInteractions.mark( hero, SkillInteractions.Mark.SHARDS, rank, 10 );
					shards.power = Math.min( 12, shards.power + mend - got );
				}
			}
			if (ch.sprite == null) return;
			ch.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.HEALING ), 2 );
			if (got > 0) ch.sprite.showStatus( CharSprite.POSITIVE, "+" + got );
			if (ch != hero && hero.sprite != null && hero.sprite.parent != null)
				hero.sprite.parent.add( new Beam.HealthRay( hero.sprite.center(), ch.sprite.center() ) );
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.HEALING; }

		@Override
		public String iconTextDisplay(){ return Integer.toString( left ); }

		@Override
		public String desc(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "desc", left );
		}

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "rank", rank );
			bundle.put( "left", left );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			rank = bundle.getInt( "rank" );
			left = bundle.getInt( "left" );
		}
	}
}
