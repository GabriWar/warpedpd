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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import com.watabou.utils.Random;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.HolyDart;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfExperience extends Potion {

	{
		icon = ItemSpriteSheet.Icons.POTION_EXP;

		bones = true;

		talentFactor = 2f;
	}
	
	@Override
	public void apply( Hero hero ) {
		identify();
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(hero.maxExp()), FloatingText.EXPERIENCE);
		hero.earnExp( hero.maxExp(), getClass() );
		new Flare( 6, 32 ).color(0xFFFF00, true).show( curUser.sprite, 2f );
	}
	
	@Override
	public int value() {
		return isKnown() ? 50 * quantity : super.value();
	}

	@Override
	public int energyVal() {
		return isKnown() ? 10 * quantity : super.energyVal();
	}


	@Override
	public void potionProc(Hero hero, Char enemy, float damage) {
		if (Char.hasProp(enemy, Char.Property.UNDEAD) || Char.hasProp(enemy, Char.Property.DEMONIC)) {
			enemy.damage(Random.NormalIntRange(10 + Dungeon.scalingDepth()/3, 20 + Dungeon.scalingDepth()/3), new HolyDart());
			enemy.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10+buffedLvl() );
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
		}
	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFFFF00 );
	}
}
