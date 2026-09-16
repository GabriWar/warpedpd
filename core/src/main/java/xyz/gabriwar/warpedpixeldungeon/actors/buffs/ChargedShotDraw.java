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

import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * The Huntress holding her ChargedShot draw. She stands and pulls for ten, seven or
 * five turns, the light gathering on her each turn with the count over her head,
 * and on the last turn the lance is loosed along the line she chose, from wherever
 * she stands then.
 */
public class ChargedShotDraw extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	//seconds of real time each turn of the draw holds the screen
	private static final float DRAW_PAUSE = 0.8f;

	private int aim = -1;
	private int turnsLeft = 0;
	private int level = 1;
	private int depth = -1;
	private int branch = -1;

    /** Charging owns the hero's turns, so walking, attacking and item use cannot interleave. */
    public boolean holdTurn(Hero hero){
        if(!onSameFloor()||turnsLeft<=0){detach();return false;}
        hero.curAction=null;hero.busy();hero.spend(TICK);
        if(hero.sprite!=null&&hero.sprite.parent!=null){
            hero.sprite.parent.add(new Delayer(DRAW_PAUSE){
                @Override protected void onComplete(){hero.next();}
            });
        }else hero.next();
        return true;
    }
    public void aim(int cell){this.aim=cell;fx(true);}
    private com.watabou.noosa.Group preview;
    @Override public void fx(boolean on){
        if(preview!=null){preview.killAndErase();preview=null;}
        if(on && onSameFloor() && target!=null && target.sprite!=null && target.sprite.parent!=null){
            preview=new com.watabou.noosa.Group();
            xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica line=new xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica(target.pos,aim,xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica.WONT_STOP);
            for(int i=1;i<line.path.size()&&i<=8+2*level;i++){
                int cell=line.path.get(i);if(!Dungeon.level.heroFOV[cell])continue;
                com.watabou.noosa.Image im=xyz.gabriwar.warpedpixeldungeon.ui.Icons.get(xyz.gabriwar.warpedpixeldungeon.ui.Icons.TARGET);
                im.point(xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileToWorld(cell));im.hardlight(0xA6DBCF);im.alpha(.5f);preview.add(im);
            }
            target.sprite.parent.add(preview);
        }
    }
	public void set( int aim, int level, int turns ){
		this.aim = aim;
		this.level = level;
		this.turnsLeft = turns;
		this.depth = Dungeon.depth;
		this.branch = Dungeon.branch;
        fx(true);
		spend( TICK );
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || !onSameFloor()) {
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		fx(true);
        turnsLeft--;
		if (turnsLeft <= 0){
			ChargedShot.fire( hero, aim, level );
			detach();
			return true;
		}
		//the draw, seen: light gathering, the count over her head
		if (hero.sprite != null && hero.sprite.visible){
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 1 + (level > 2 ? 2 : level) );
			new Flare( 4, 8 + 2 * level ).color( 0xFFE9A0, true ).show( hero.sprite, 0.5f ).angularSpeed = 120;
			hero.sprite.showStatus( CharSprite.NEUTRAL, Integer.toString( turnsLeft ) );
			if (turnsLeft <= 3) Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.5f, 1.4f + 0.2f * (3 - turnsLeft) );
		}
		spend( TICK );
        // Hero.holdTurn advances the draw without allowing other actions.

		return true;
	}

	/** still on the floor she drew on; a stairway or a portal spoils the shot */
	private boolean onSameFloor(){
		return aim >= 0 && depth == Dungeon.depth && branch == Dungeon.branch;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String TARGET = "target", LEFT = "left", LEVEL = "level", DEPTH = "depth", BRANCH = "branch";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TARGET, aim );
		bundle.put( LEFT, turnsLeft );
		bundle.put( LEVEL, level );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		aim = bundle.getInt( TARGET );
		turnsLeft = bundle.getInt( LEFT );
		level = bundle.getInt( LEVEL );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
	}
}
