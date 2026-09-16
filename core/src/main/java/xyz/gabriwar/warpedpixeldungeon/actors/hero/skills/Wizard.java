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
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EnergyParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Mage: a spent wand charge may refuse to die. It ricochets off the enemy the zap struck and
 * casts the same spell again at the nearest other enemy; with nobody to leap to, it snaps back
 * into the wand. At level 3 a wand's last charge always ricochets.
 */
public class Wizard extends PassiveSkillB1 {

	{
		name = "Wizard";
		image = 33;
		tier = 1;
	}

	private static final int BOUNCE_RANGE = 4;

	//the ricochet casts onZap, which must never ricochet again
	private static boolean bouncing = false;

	//whoever the hero's last clean zap landed on, handed over by Wand's zap callback just before wandUsed()
	private static Char aimed = null;

	public static void aimedAt( Char ch ){
		aimed = ch;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** Wand.wandUsed calls this right after one of the hero's own wands spent its charge */
	public static void onChargeSpent( Wand wand ){
		Char struck = aimed;
		aimed = null;
		Hero hero = Dungeon.hero;
		if (bouncing || hero == null || hero.heroSkills == null || wand.cursed) return;
		Wizard skill = hero.heroSkills.get( Wizard.class );
		if (skill == null || skill.level <= 0) return;

		boolean lastCharge = skill.level >= MAX_LEVEL && wand.curCharges <= 0;
		//15/25/35%
		if (!lastCharge && Random.Int( 100 ) >= 5 + 10 * skill.level) return;

		Char next = struck == null ? null : bounceTarget( hero, struck );
		if (next != null){
			ricochet( hero, wand, struck.pos, next );
		} else if (!lastCharge && wand.curCharges < wand.maxCharges){
			//nowhere to leap: the charge snaps back into the wand
			wand.curCharges++;
			Item.updateQuickslot();
			if (hero.sprite != null){
				hero.sprite.centerEmitter().burst( EnergyParticle.FACTORY, 8 );
				hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( Wizard.class, "snap" ) );
			}
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.7f, 1.5f );
		}
	}

	/** the zap's own target must be an enemy the hero can see; the leap goes to the nearest other one */
	private static Char bounceTarget( Hero hero, Char struck ){
		if (struck.alignment != Char.Alignment.ENEMY || struck.pos < 0 || struck.pos >= Dungeon.level.length()
				|| !Dungeon.level.heroFOV[struck.pos]) return null;
		Char best = null;
		for (Char ch : Actor.chars()){
			if (ch == struck || ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()
					|| !Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance( struck.pos, ch.pos ) > BOUNCE_RANGE
					|| new Ballistica( struck.pos, ch.pos, Ballistica.MAGIC_BOLT ).collisionPos != ch.pos) continue;
			if (best == null || Dungeon.level.trueDistance( struck.pos, ch.pos )
					< Dungeon.level.trueDistance( struck.pos, best.pos )) best = ch;
		}
		return best;
	}

	private static void ricochet( Hero hero, Wand wand, int from, Char next ){
		if (hero.sprite != null && hero.sprite.parent != null){
			((MagicMissile) hero.sprite.parent.recycle( MagicMissile.class )).reset(
					MagicMissile.MAGIC_MISSILE, from, next.pos, null );
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( Wizard.class, "ricochet" ) );
		}
		if (next.sprite != null) next.sprite.centerEmitter().burst( EnergyParticle.FACTORY, 6 );
		Sample.INSTANCE.play( Assets.Sounds.ZAP, 1f, 1.3f );
		bouncing = true;
		try {
			wand.onZap( new Ballistica( from, next.pos, wand.collisionProperties( next.pos ) ) );
		} finally {
			bouncing = false;
		}
	}
}
