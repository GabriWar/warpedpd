/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Preparation;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndChooseSubclass;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

public class TengusMask extends Item {
	
	private static final String AC_WEAR	= "WEAR";
	
	{
		stackable = false;
		image = ItemSpriteSheet.MASK;

		defaultAction = AC_WEAR;

		unique = true;
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_WEAR );
		return actions;
	}
	
	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_WEAR )) {
			
			curUser = hero;

			GameScene.show( new WndChooseSubclass( this, hero ) );
			
		}
	}
	
	@Override
	public boolean doPickUp(Hero hero, int pos) {
		Badges.validateMastery();
		return super.doPickUp( hero, pos );
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	public void choose( HeroSubClass way ) {
		
		detach( curUser.belongings.backpack );
		Catalog.countUse( getClass() );
		
		curUser.spend( Actor.TICK );
		curUser.busy();
		
		curUser.subClass = way;
		//skill tree: the 4th branch opens with the calling
		curUser.heroSkills.initSubclassBranch( curUser );
		Talent.initSubclassTalents(curUser);

		if (way == HeroSubClass.ASSASSIN && curUser.invisible > 0){
			Buff.affect(curUser, Preparation.class);
		}
		
		curUser.sprite.operate( curUser.pos );
		Sample.INSTANCE.play( Assets.Sounds.MASTERY );
		
		Emitter e = curUser.sprite.centerEmitter();
		e.pos(e.x-2, e.y-6, 4, 4);
		e.start(Speck.factory(Speck.MASK), 0.05f, 20);
		GLog.p( Messages.get(this, "used"));
		
	}
}
