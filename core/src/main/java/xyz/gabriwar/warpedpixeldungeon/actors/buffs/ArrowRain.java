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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * The Huntress's Arrow Storm, once loosed: arrows keep falling on the same nine
 * cells for a few turns. Each turn every enemy under it takes a share of a shot
 * and is crippled, so the storm rewards holding an enemy in it and punishes
 * standing still, and the total lands well above the single volley it replaced
 * only if the target stays for all of it.
 */
public class ArrowRain extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int center = -1;
    private int floorDepth, floorBranch;
	private int turnsLeft = 0;
	private int level = 1;

	public void set( int center, int level, int turns ){
		this.center = center; floorDepth=Dungeon.depth; floorBranch=Dungeon.branch;
		this.level = level;
		this.turnsLeft = turns;
		volley();
		turnsLeft--;
		spend( TICK );
	}

	@Override
	public boolean act() {
		if (floorDepth != Dungeon.depth || floorBranch != Dungeon.branch || turnsLeft <= 0 || center < 0 || !(target instanceof Hero) || !target.isAlive()){
			detach();
			return true;
		}
		volley();
		turnsLeft--;
		if (turnsLeft <= 0){
			detach();
		} else {
			spend( TICK );
		}
		return true;
	}

	private void volley(){
		Hero hero = (Hero) target;
		Dart look = new Dart();
        java.util.ArrayList<Integer> impact=new java.util.ArrayList<>(xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.area(center,1));
        //+2: the four cross tips two tiles out
        if(level>=2)for(int c:xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.area(center,2)){
            int w=Dungeon.level.width();
            if((c/w==center/w||c%w==center%w)&&!impact.contains(c))impact.add(c);
        }
        xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX.show(xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX.RAPTOR,center);
		boolean struck = false;
		for (int c : impact){
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			if (Dungeon.level.heroFOV[c]){
				SkillFX.rain( c, look, 1 + Random.Int( 2 ), null );
			}
			Char ch = Actor.findChar( c );
			if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow bow = hero.belongings.getItem( xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow.class );
				int base = bow != null ? bow.knockArrow().damageRoll( hero ) : hero.damageRoll();
				int dmg = Math.round( base * (0.15f + 0.1f * level) );
				ch.damage( dmg, this );
				if (ch.isAlive()) Buff.prolong( ch, Cripple.class, 2f );
				struck = true;
			}
		}
        //+3: the arrows stay in the ground and bite whatever steps on them (rank 1: no bow-hit relaunch)
        if(level>=3)SkillField.place(hero,SkillField.ARROWS,1,4,impact);
		SpatialSound.play( struck ? Assets.Sounds.HIT_ARROW : Assets.Sounds.ATK_SPIRITBOW, center, 1f, Random.Float( 0.9f, 1.1f ) );
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String CENTER = "center", LEFT = "left", LEVEL = "level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put("floor_depth",floorDepth); bundle.put("floor_branch",floorBranch);
		bundle.put( CENTER, center );
		bundle.put( LEFT, turnsLeft );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		floorDepth=bundle.contains("floor_depth")?bundle.getInt("floor_depth"):Dungeon.depth; floorBranch=bundle.contains("floor_branch")?bundle.getInt("floor_branch"):Dungeon.branch;
		center = bundle.getInt( CENTER );
		turnsLeft = bundle.getInt( LEFT );
		level = bundle.getInt( LEVEL );
	}
}
