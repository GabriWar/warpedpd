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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * Mage: while active the hero is half light. Walking into an enemy carries him straight through
 * it (and more behind it as he trains) to the free tile beyond, and the light he leaves inside
 * each body bursts as a lance. At level 3 the lance splits into three as it leaves his back.
 */
public class Transcendence extends ActiveSkill {

	{
		tag = "PA4";
		name = "Transcendence";
		tier = 4;
		image = 52;
		level = 0;
	}

	//the mana one pass through bodies costs
	private static final int PASS_MANA = 3;

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals(Skill.AC_ACTIVATE)){
			//one mana ward at a time; switching Spirit Armor off from here clears its motes too
			Skill other = hero.heroSkills.get( SpiritArmor.class );
			if (other != null && other.active){
				other.active = false;
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.detach( hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes.class );
			}
		}
		if (action.equals(Skill.AC_ACTIVATE) && hero.sprite != null){
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 1f, 1.4f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			new Flare( 6, 18 ).color( 0xFFFFDD, true ).show( hero.sprite, 0.5f );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.5f, 0.8f );
		}
		//the half-light shows: a slow drift of light motes for as long as it is on
		xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAura.sync( hero );
	}

	//half light: walking into an enemy carries the hero straight through it to the free tile beyond,
	//and the light left behind inside every body passed bursts as a lance
	@Override
	public boolean onHeroBump( Hero hero, Char enemy ){
		if (!active || level <= 0 || hero == null || enemy == null || hero.rooted || hero.MP < PASS_MANA
				|| !Dungeon.level.adjacent( hero.pos, enemy.pos )) return false;
		xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAura.sync( hero );
		int dir = enemy.pos - hero.pos;
		java.util.ArrayList<Char> passed = new java.util.ArrayList<>();
		int cell = enemy.pos;
		while (true){
			Char ch = Actor.findChar( cell );
			if (ch == null) break;
			if (ch.alignment != Char.Alignment.ENEMY || passed.size() >= level
					|| ch.properties().contains( Char.Property.BOSS )
					|| ch.properties().contains( Char.Property.IMMOVABLE )) return false;
			passed.add( ch );
			int next = cell + dir;
			if (!SkillInteractions.valid( next ) || !Dungeon.level.adjacent( cell, next )) return false;
			cell = next;
		}
		if (Dungeon.level.solid[cell] || !Dungeon.level.passable[cell] || Dungeon.level.pit[cell]
				|| (Char.hasProp( hero, Char.Property.LARGE ) && !Dungeon.level.openSpace[cell])) return false;

		hero.MP -= PASS_MANA;
		final int from = hero.pos;
		final int land = cell;
		CellEmitter.center( from ).burst( Speck.factory( Speck.LIGHT ), 6 );
		hero.move( land, false );
		if (hero.sprite != null){
			hero.sprite.place( land );
			if (hero.sprite.parent != null){
				hero.sprite.parent.add( new Beam.LightRay( DungeonTilemap.raisedTileCenterToWorld( from ),
						DungeonTilemap.raisedTileCenterToWorld( land ) ) );
			}
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
		}
		Dungeon.level.occupyCell( hero );
		Dungeon.observe();
		xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog();

		int min = 2 + level, max = 4 + 2 * level;
		for (Char ch : passed){
			if (!ch.isAlive()) continue;
			ch.damage( Random.NormalIntRange( min, max ), this );
			if (ch.sprite != null) ch.sprite.flash();
			CellEmitter.center( ch.pos ).burst( Speck.factory( Speck.LIGHT ), 5 );
		}
		//+3: the lance splits into three as it leaves the hero's back
		if (level >= MAX_LEVEL) splitLance( hero, land, dir, min, max );
		Sample.INSTANCE.play( Assets.Sounds.RAY, 0.8f, 1.3f );
		hero.spendAndNext( Actor.TICK );
		return true;
	}

	private void splitLance( Hero hero, int land, int dir, int min, int max ){
		int w = Dungeon.level.width();
		int dx = dir % w, dy = dir / w;
		//a diagonal step reads as dx = +-1 with dy carrying the row; normalise to -1..1 each
		if (dx > 1) { dx -= w; dy += 1; }
		if (dx < -1){ dx += w; dy -= 1; }
		int[][] dirs = {
				{ dx, dy },
				{ Integer.signum( dx - dy ), Integer.signum( dx + dy ) },
				{ Integer.signum( dx + dy ), Integer.signum( dy - dx ) }
		};
		for (int[] d : dirs){
			int x = land % w, y = land / w, end = land;
			for (int step = 0; step < 2; step++){
				x += d[0]; y += d[1];
				if (x < 0 || x >= w || y < 0 || y >= Dungeon.level.height()) break;
				int c = x + y * w;
				if (Dungeon.level.solid[c]) break;
				end = c;
				Char ch = Actor.findChar( c );
				if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
					ch.damage( Math.max( 1, Random.NormalIntRange( min, max ) / 2 ), this );
					if (ch.sprite != null) ch.sprite.flash();
				}
			}
			if (end != land && hero.sprite != null && hero.sprite.parent != null){
				hero.sprite.parent.add( new Beam.LightRay( DungeonTilemap.raisedTileCenterToWorld( land ),
						DungeonTilemap.raisedTileCenterToWorld( end ) ) );
			}
		}
	}
}
