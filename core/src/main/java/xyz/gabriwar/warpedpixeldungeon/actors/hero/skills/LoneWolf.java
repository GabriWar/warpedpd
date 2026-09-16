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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Huntress: she hunts better alone. With no ally within eight tiles and no summon
 * of hers on the floor, a kill makes her howl, and the enemies close to her flee in
 * terror for a few turns. Fully trained, a kill made alone also feeds her a little
 * health. The moment company arrives it switches off, and she says so.
 */
public class LoneWolf extends Skill {

	{
		tag = "PA5A";
		name = "Lone Wolf";
		castText = "";
		image = 133;
		tier = 4;
	}

	private static final int HOWL_RADIUS = 2;

	private boolean wasAlone = true;

	@Override
	protected boolean upgrade(){
		return true;
	}

	private boolean alone(){
		if (level <= 0) return false;
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level == null || Dungeon.level.mobs == null)
			return false;
		boolean alone = true;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy) continue;
			//any summon of yours on this floor breaks it, however far away
			if (m instanceof SummonedPet && m.isAlive()){ alone = false; break; }
			if (m.alignment == Char.Alignment.ALLY
					&& Dungeon.level.distance( hero.pos, m.pos ) <= 8){
				alone = false; break;
			}
		}
		if (alone != wasAlone){
			wasAlone = alone;
			castText = Messages.get( this, alone ? "cast_alone" : "cast_company" );
			castTextYell();
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( alone ? Speck.STAR : Speck.DUST ), 4 );
			Sample.INSTANCE.play( Assets.Sounds.MISS, 0.7f, alone ? 1.3f : 0.8f );
		}
		return alone;
	}

	@Override
	public String castText(){
		return castText;
	}

	//keeps the alone / company call-outs in step while she fights
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0) alone();
		return damage;
	}

	//a kill made alone: she howls and the enemies near her flee for 2/3/4 turns
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (hero == null || mob == null || !alone()) return;

		boolean howled = false;
		for (Mob m : Dungeon.level.mobs){
			if (m == mob || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (Dungeon.level.distance( hero.pos, m.pos ) > HOWL_RADIUS) continue;
			Buff.prolong( m, Terror.class, 1 + level ).object = hero.id();
			if (m.sprite != null) m.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( this, "fear" ) );
			howled = true;
		}
		if (howled){
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 3 );
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 0.8f, 1.3f );
		}

		//fully trained, a kill made alone mends 5% of full health
		if (level < MAX_LEVEL || hero.HP >= hero.HT) return;
		int heal = Math.min( hero.HT - hero.HP, Math.max( 1, Math.round( hero.HT * 0.05f ) ) );
		hero.HP += heal;
		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.POSITIVE, Integer.toString( heal ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 0.6f, 0.7f );
	}
}
