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
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * Mage: while active the hero is half light. Walking into an enemy carries him straight through
 * it (and more behind it as he trains) to the free tile beyond, and the light he leaves inside
 * each body bursts as a lance. At level 3 the lance splits into three as it leaves his back.
 */
public class Transcendence extends ActiveSkill {

	/** The lance of light's ramp (the HOLY school's, lightest first). */
	public static final int[] HOLY = { 0xFFFFFF, 0xFFF4D0, 0xF1D99C, 0xC9A05A };
	/** A lance bursting up out of a body: its tip LANCE_OVER px over the head, its shaft LANCE px
	 *  (the head and shaft end inside the body, above its feet: never a spike out of the ground). */
	public static final int LANCE = 5, LANCE_OVER = 8;
	/** How long it shows, s (gone before its fading tail could stand over the body like a spear). */
	public static final float LANCE_LIFE = 0.24f;

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
			SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 1f, 1.4f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			PixelFlare.show( SkillVfx.midX( hero.sprite ), SkillVfx.midY( hero.sprite ), 0xFFF4D0, PixelFlare.M, 0.5f );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.5f, 0.8f );
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
		//half light passing through: a streak of gold with his afterimages along it (never a beam)
		if (hero.sprite != null){
			hero.sprite.place( land );
			if (hero.sprite.parent != null) StreakFX.dash( from, land, 0xF1D99C, 0.45f, 0.25f, true );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
		}
		Dungeon.level.occupyCell( hero );
		Dungeon.observe();
		xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog();

		int min = 2 + level, max = 4 + 2 * level;
		for (Char ch : passed){
			if (!ch.isAlive()) continue;
			ch.damage( Random.NormalIntRange( min, max ), this );
			if (ch.sprite != null){
				ch.sprite.flash();
				//the light left inside bursts out of the body as a lance, upward
				LanceFX.stab( SkillVfx.midX( ch.sprite ), ch.sprite.y - LANCE_OVER, -90, HOLY, LANCE, LANCE_LIFE );
			}
			CellEmitter.center( ch.pos ).burst( Speck.factory( Speck.LIGHT ), 5 );
		}
		//+3: the lance splits into three as it leaves the hero's back
		if (level >= MAX_LEVEL) splitLance( hero, land, dir, min, max );
		SpatialSound.play( Assets.Sounds.RAY, hero, 0.8f, 1.3f );
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
			//each third of the lance a stroke of gold the way it really cuts (never a beam)
			if (end != land && hero.sprite != null && hero.sprite.parent != null){
				StreakFX.show( land, end, 0xF1D99C, 0.35f, 0.2f );
			}
		}
	}
}
