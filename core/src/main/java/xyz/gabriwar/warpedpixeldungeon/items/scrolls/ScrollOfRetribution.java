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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class ScrollOfRetribution extends Scroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_RETRIB;
	}
	
	@Override
	public void doRead() {

		detach(curUser.belongings.backpack);
		GameScene.flash( 0x80FFFFFF );
		
		//scales from 0x to 1x power, maxing at ~10% HP
		float hpPercent = (curUser.HT - curUser.HP)/(float)(curUser.HT);
		float power = Math.min( 4f, 4.45f*hpPercent);
		
		Sample.INSTANCE.play( Assets.Sounds.BLAST );
		GLog.i(Messages.get(this, "blast"));

		ArrayList<Mob> targets = new ArrayList<>();

		//calculate targets first, in case damaging/blinding a target affects hero vision
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
			if (Dungeon.level.heroFOV[mob.pos]) {
				targets.add(mob);
			}
		}

		for (Mob mob : targets){
			//deals 10%HT, plus 0-90%HP based on scaling
			mob.damage(Math.round(mob.HT/10f + (mob.HP * power * 0.225f)), this);
			if (mob.isAlive()) {
				Buff.prolong(mob, Blindness.class, Blindness.DURATION);
			}
		}
		
		Buff.prolong(curUser, Weakness.class, Weakness.DURATION);
		Buff.prolong(curUser, Blindness.class, Blindness.DURATION);
		Dungeon.observe();

		identify();
		
		readAnimation();
		
	}
	
	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}
}
