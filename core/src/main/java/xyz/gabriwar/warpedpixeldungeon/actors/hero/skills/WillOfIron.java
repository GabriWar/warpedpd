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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Mage: the will braces against heavy blows, paying for part of them with mana. A blow braced
 * in full is thrown back: the attacker is hurled away, and at level 3 the shockwave also shoves
 * every other enemy standing next to the hero.
 */
public class WillOfIron extends Skill {

	{
		tag = "PA5B";
		name = "Will of Iron";
		tier = 4;
		image = 53;
		level = 0;
	}

	private static final String STORED = "WILL_OF_IRON_STORED";

	//braced blows waiting in the staff for the next damaging zap
	private int stored = 0;
	private float bracedAt = -10f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	//an emptying pool hardens the will: with mana below half, one blow of 4+ a turn from an enemy is
	//braced for free, and the braced force is kept in the staff
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage < SkillInteractions.ofHealth( hero.HT, 0.05f ) || !(source instanceof Char) || source == hero
				|| Skill.isTickDamage( source )) return 0;
		int maxMana = hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero );
		if (hero.MP * 2 >= maxMana || Actor.now() - bracedAt < 1f) return 0;
		bracedAt = Actor.now();
		int absorbed = Math.min( SkillInteractions.ofHealth( hero.HT, 0.05f * level ), damage );
		stored = Math.min( level >= MAX_LEVEL ? 2 : 1, stored + 1 );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 2 + absorbed );
			new Flare( 6, 20 ).color( 0x88AAFF, true ).show( hero.sprite, 0.5f );
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "brace" ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.MELD, 0.8f, 1.3f );
		return absorbed;
	}

	//the next damaging zap or bolt spell carries a stored brace out with it
	@Override
	public void onMagicDamage( Char target, int damage, Object source ){
		if (level <= 0 || stored <= 0 || target == null || !target.isAlive()) return;
		stored--;
		target.damage( 2 * level, this );
		if (target.sprite != null){
			target.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
			target.sprite.flash();
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 1f, 0.8f );
		Camera.main.shake( 1, 0.15f );
	}

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STORED, stored );
	}

	@Override
	public void restoreInBundle( com.watabou.utils.Bundle bundle ){
		super.restoreInBundle( bundle );
		stored = bundle.getInt( STORED );
	}
}
