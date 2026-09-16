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
import com.watabou.utils.PointF;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class LastRites extends Skill {

	//flat mana requirement, and the amount spent - paying it again is the only cooldown this skill has
	public static final int MANA_THRESHOLD = 20;

	//at max rank the light blinds every enemy standing beside you
	private static final float BLIND_TURNS = 3f;

	{
		name = "Last Rites";
		tag = "PA4";
		image = 154;
		tier = 4;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** the health the rites leave you on: 3 / 5 / 7 */
	private int survivingHealth(){
		return 1 + 2 * level;
	}

	//hooked into incoming damage rather than onDefendProc: this runs inside Hero.damage, so the
	//blow has already lost the armour's dr and is the damage the hero is really about to take.
	//poison, burning and starvation are not blows: the rites do not answer them
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		if (level <= 0 || source instanceof Buff.DOTbuff || source instanceof Hunger)
			return 0;

		Hero hero = Dungeon.hero;
		if (hero == null || damage < hero.HP + hero.shielding() || hero.MP < MANA_THRESHOLD)
			return 0;

		hero.MP -= MANA_THRESHOLD;

		int target = Math.min( hero.HT, survivingHealth() );
		hero.holdHealthAt( target );
		int reduction;
		if (hero.HP < target){
			hero.HP = target;
			reduction = damage;
		} else {
			reduction = damage - (hero.HP - target);
		}

		Buff.prolong( hero, Bless.class, 5 + 5 * level );
		if (level >= MAX_LEVEL){
			for (int offset : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( hero.pos + offset );
				if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
					Buff.prolong( ch, Blindness.class, BLIND_TURNS );
					if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				}
			}
		}

		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 12 );
			hero.sprite.emitter().burst( ShaftParticle.FACTORY, 6 );
			new Flare( 8, 36 ).color( 0xFFEE88, true ).show( hero.sprite, 1.5f ).angularSpeed = 120;
			hero.sprite.parent.add( new Beam.LightRay(
					new PointF( hero.sprite.center().x, hero.sprite.center().y - DungeonTilemap.SIZE * 6 ),
					hero.sprite.center() ) );
			Camera.main.shake( 2, 0.4f );
		}
		castText = Messages.get( this, "cast" );
		castTextYell();
		Sample.INSTANCE.play( Assets.Sounds.MASTERY, 1f, 1.1f );
		GLog.p( Messages.get(this, "trigger") );

		return reduction;
	}

	@Override
	public String castText(){
		return castText;
	}

	@Override
	public String info(){
		return Messages.get(this, "desc", MANA_THRESHOLD) + "\n"
				+ costUpgradeInfo();
	}
}
