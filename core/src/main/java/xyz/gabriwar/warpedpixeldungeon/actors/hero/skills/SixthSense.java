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
import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CircleArc;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * Huntress: a sense, not a hide. Once in a while the forest warns her: the first
 * blow that would take more than a quarter of her health is caught at a quarter.
 * The warning then needs 40, 30 or 20 turns to come back, and fully trained it
 * turns the blow back on its striker, who is left dazed.
 */
public class SixthSense extends Skill {

	{
		tag = "PA4";
		name = "Sixth Sense";
		castText = "Not today.";
		image = 135;
		tier = 4;
	}

	public static final float DAZE_TURNS = 3f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	public int cooldown(){
		return 50 - 10 * level;
	}

	//the warning: a hit past a quarter of full health is caught at a quarter, once per cooldown.
	//poison, bleeding, burning and starvation are not blows, so they are never caught
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0) return 0;
		if (source instanceof Buff.DOTbuff || source instanceof Hunger) return 0;
		int quarter = Math.max( 1, hero.HT / 4 );
		if (damage <= quarter) return 0;
		if (hero.buff( Warned.class ) != null) return 0;
		Buff.affect( hero, Warned.class ).set( cooldown() );
		castTextYell();
		if (hero.sprite != null){
			//the warning: a green ring closing on her as the flare opens
			new Flare( 6, 20 ).color( 0x88CC66, true ).show( hero.sprite, 0.6f );
			if (hero.sprite.parent != null) new CircleArc( 16, 12 ).color( 0x88CC66, true ).show( hero.sprite, 0.45f );
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
		}
		Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 1.5f );
		if (level >= MAX_LEVEL && source instanceof Char && source != hero && ((Char) source).isAlive()){
			Char striker = (Char) source;
			Buff.prolong( striker, Daze.class, DAZE_TURNS );
			if (striker.sprite != null) striker.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		}
		return damage - quarter;
	}

	/** the sense catching its breath; while this sits on the hero it will not warn again */
	public static class Warned extends Buff {

		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		private int left = 0;
		private int total = 1;

		public void set( int turns ){
			left = turns;
			total = Math.max( 1, turns );
		}

		@Override
		public boolean act() {
			if (--left <= 0){
				detach();
				return true;
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.FORESIGHT;
		}

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ){
			icon.hardlight( 0.55f, 0.8f, 0.4f );
		}

		@Override
		public float iconFadePercent(){
			return Math.max( 0, Math.min( 1, left / (float) total ) );
		}

		@Override
		public String iconTextDisplay(){
			return Integer.toString( left );
		}

		@Override
		public String desc(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "desc", left );
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( LEFT, left );
			bundle.put( "total", total );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			left = bundle.getInt( LEFT );
			total = Math.max( left, bundle.contains( "total" ) ? bundle.getInt( "total" ) : left );
		}
	}
}
