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

package xyz.gabriwar.warpedpixeldungeon.homestead.mounts;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HorseSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;

/**
 * Winning a wild horse (actors/mobs/Horse). It eats THATCH (hay), CORNWHEAT, APRICOS and raw
 * BLANDFRUIT (isFeed). A wild horse shies from a hero within SHY cells, one fewer for every meal it
 * has had from him (its trust), so the first meals are thrown: food lying within SCENT cells of a
 * calm wild horse, with no hero within its shyness of it, is walked up to and eaten, a piece a turn
 * (foodNear, eat). Once it shies from nobody it eats nothing more off the ground but lets him walk
 * up, and a tap with food in his pack feeds it by hand (handFeed: a turn); a cornered or penned one
 * can be fed by hand from the start. Its TAME_TRUST-th meal, always from his hand, makes it his. A
 * blow makes a wild horse forget every meal (Horse.damage).
 *
 * Everything runs on the host, for the host's hero.
 */
public final class Taming {

	private Taming(){}

	/** A wild horse's shyness before any meal (cells), and the meals that tame it. */
	public static final int SHY = 3, TAME_TRUST = 4;
	/** How far off a calm wild horse notices food on the ground. */
	public static final int SCENT = 4;

	/** How near a hero comes before a wild horse with this much trust bolts. */
	public static int shy( int trust ){
		return Math.max( 0, SHY - trust );
	}

	/** Does a horse eat it: thatch, cornwheat, an aprico, a blandfruit not yet cooked. */
	public static boolean isFeed( Item i ){
		return i instanceof Thatch || i instanceof Cornwheatshaft || i instanceof Aprico
				|| (i instanceof Blandfruit && ((Blandfruit) i).potionAttrib == null);
	}

	//the feeds in the order a hero hands them out: the cheapest first
	private static final Class<?>[] ORDER = { Thatch.class, Cornwheatshaft.class, Blandfruit.class, Aprico.class };

	/** The food in the hero's pack a horse would eat, the cheapest first; null for none. */
	public static Item feedIn( Hero hero ){
		for (Class<?> cls : ORDER){
			for (Item i : hero.belongings){
				if (cls.isInstance( i ) && isFeed( i )) return i;
			}
		}
		return null;
	}

	/** The food in a heap a horse would eat, null for none. */
	public static Item feedOf( Heap heap ){
		if (heap == null || heap.type != Heap.Type.HEAP) return null;
		for (Item i : heap.items) if (isFeed( i )) return i;
		return null;
	}

	/**
	 * The cell of the food lying nearest a wild horse within SCENT cells with no hero within its
	 * shyness of it; -1 for none, and always for one that no longer shies from him: its last meal
	 * is from his hand.
	 */
	public static int foodNear( Horse h ){
		if (h.trust >= TAME_TRUST - 1) return -1;
		Level level = Dungeon.level;
		int shy = shy( h.trust );
		int w = level.width(), hx = h.pos % w, hy = h.pos / w;
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int dy = -SCENT; dy <= SCENT; dy++){
			for (int dx = -SCENT; dx <= SCENT; dx++){
				int x = hx + dx, y = hy + dy;
				if (x < 0 || y < 0 || x >= w || y >= level.height()) continue;
				int c = x + y * w;
				if (feedOf( level.heaps.get( c ) ) == null) continue;
				if (OverworldLevel.heroDistance( level, c ) <= shy) continue;
				int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		return best;
	}

	/** A wild horse eats a piece of the food lying on a cell beside it (or under it): a meal. */
	public static void eat( Horse h, int cell ){
		Level level = Dungeon.level;
		Heap heap = level.heaps.get( cell );
		Item food = feedOf( heap );
		if (food == null) return;
		Item piece = food;
		if (food.quantity() > 1){
			food.quantity( food.quantity() - 1 );
			piece = food.duplicate();
			piece.quantity( 1 );
			if (heap.sprite != null) heap.sprite.view( heap ).place( heap.pos );
		} else {
			heap.remove( food );
		}
		fed( h, piece );
	}

	/**
	 * A tap on a wild horse (Horse.interact), the hero beside it: a piece of his food from his
	 * pack, eaten from his hand (a turn), or a word on what it might eat.
	 */
	public static void handFeed( Hero hero, Horse h ){
		Item food = feedIn( hero );
		if (food == null){
			GLog.i( Messages.get( Taming.class, "no_food" ) );
			return;
		}
		Item piece = food.detach( hero.belongings.backpack );
		if (piece == null) return;
		if (hero.sprite != null) hero.sprite.turnTo( hero.pos, h.pos );
		fed( h, piece );
		hero.spendAndNext( 1f );
	}

	//a meal from the hero: a little more trust, and his for good on the TAME_TRUST-th
	static void fed( Horse h, Item food ){
		boolean seen = seen( h );
		if (seen){
			munch( h );
			sound( Assets.Sounds.EAT, h.pos );
		}
		h.trust++;
		if (h.trust >= TAME_TRUST){
			tame( h );
			return;
		}
		if (seen){
			GLog.i( Messages.get( Taming.class, "eats", food.name() ) + " " + Messages.get( Taming.class, "trust_" + h.trust ) );
			hearts( h, 1 );
		}
	}

	//when a wild horse last bolted from the hero with a word on how to win one (Actor.now())
	private static float hinted = Float.NEGATIVE_INFINITY;
	/** How long the word waits before it is said again, turns. */
	static final float HINT_EVERY = 300f;

	/** A wild horse bolts from the hero (Horse.act): while he has not read the guide's page on
	 *  horses, he is told now and then, as he sees it go, to leave it food and keep away. */
	public static void shied( Horse h ){
		if (!seen( h ) || GuideProgress.isRead( Mounts.PAGE )) return;
		float now = Actor.now();
		if (now >= hinted && now - hinted < HINT_EVERY) return;
		hinted = now;
		GLog.i( Messages.get( Taming.class, "shied" ) );
	}

	/** The horse is his: it waits where it is from now on and takes a rope. */
	public static void tame( Horse h ){
		h.trust = TAME_TRUST;
		h.tame = true;
		h.settle();
		GLog.p( Messages.get( Taming.class, "tamed" ) );
		if (seen( h )){
			hearts( h, 5 );
			sound( Assets.Sounds.CHARMS, h.pos );
		}
		Mounts.seen();
	}

	//its head down over the food
	static void munch( Horse h ){
		if (h.sprite instanceof HorseSprite) ((HorseSprite) h.sprite).munch();
	}

	//hearts rising off it: trust given, health back
	static void hearts( Horse h, int n ){
		if (h.sprite != null && Game.scene() instanceof GameScene) CellEmitter.get( h.pos ).burst( Speck.factory( Speck.HEART ), n );
	}

	static boolean seen( Horse h ){
		Level level = Dungeon.level;
		return level != null && level.heroFOV != null && h.pos >= 0 && h.pos < level.heroFOV.length && level.heroFOV[h.pos];
	}

	static void sound( String id, int cell ){
		if (Game.scene() instanceof GameScene) SpatialSound.play( id, cell );
	}
}
