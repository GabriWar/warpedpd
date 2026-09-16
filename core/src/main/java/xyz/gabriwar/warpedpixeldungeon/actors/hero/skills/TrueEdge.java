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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class TrueEdge extends Skill {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	private static final float CRIT_MULTIPLIER = 1.5f;

	{
		tag = "PB4";
		name = "True Edge";
		image = 146;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//5% / 10% / 15% of melee hits crit; a crit on an adjacent enemy slashes on through
	//1 / 2 / 3 tiles behind it
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| Random.Int( 100 ) >= 5 * level){
			return damage;
		}
		int crit = Math.round( damage * CRIT_MULTIPLIER );
		if (enemy.sprite != null){
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "crit" ) );
			enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			Wound.hit( enemy );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 1.4f );
		if (level >= MAX_LEVEL){
			Buff.affect( enemy, Bleeding.class ).set( crit / 4f );
		}
		if (Dungeon.level.adjacent( hero.pos, enemy.pos )){
			slashThrough( hero, enemy, crit / 2 );
		}
		return crit;
	}

	//the cut is plain damage dealt by the hero, not another swing, so it can never proc skills again
	private void slashThrough( Hero hero, Char enemy, int damage ){
		int w = Dungeon.level.width();
		int dx = enemy.pos % w - hero.pos % w;
		int dy = enemy.pos / w - hero.pos / w;
		int end = enemy.pos;
		for (int i = 1; i <= level; i++){
			int x = enemy.pos % w + dx * i;
			int y = enemy.pos / w + dy * i;
			if (x < 0 || x >= w || y < 0 || y >= Dungeon.level.height()) break;
			int c = x + y * w;
			if (Dungeon.level.solid[c]) break;
			end = c;
			Char behind = Actor.findChar( c );
			if (behind == null || !behind.isAlive() || behind.alignment != Char.Alignment.ENEMY) continue;
			if (behind.sprite != null) Wound.hit( behind );
			if (level >= MAX_LEVEL) Buff.affect( behind, Bleeding.class ).set( Math.max( 1, damage ) / 4f );
			if (damage > 0) behind.damage( damage, hero );
		}
		if (end != enemy.pos && hero.sprite != null && hero.sprite.parent != null){
			hero.sprite.parent.add( new Beam.LightRay(
					DungeonTilemap.tileCenterToWorld( hero.pos ), DungeonTilemap.tileCenterToWorld( end ) ) );
			Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 1f, 0.8f );
		}
	}
}
