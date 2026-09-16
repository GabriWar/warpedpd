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

package xyz.gabriwar.warpedpixeldungeon.ui.changelist;

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.TownsfolkSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * What Warped adds on top of Shattered. Written for players, by theme rather than by
 * patch: the small day-to-day fixes reach them through the in-game news feed instead.
 */
public class vWarped_Changes {

	public static void addAllChanges( ArrayList<ChangeInfo> changeInfos ){
		add_Overview(changeInfos);
		add_Online(changeInfos);
		add_Surface(changeInfos);
		add_Climate(changeInfos);
		add_Growing(changeInfos);
		add_Ported(changeInfos);
		add_Hero(changeInfos);
	}

	public static void add_Overview( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("Warped Pixel Dungeon", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(Icons.get(Icons.WPD), "What Warped is",
				"Warped is Shattered Pixel Dungeon with the world stretched out around it: a town and an overworld on the surface, weather and seasons that reach down the stairs, plants that keep growing while you are away, and pieces of half a dozen other open source Pixel Dungeon mods stitched in.\n" +
				"\n" +
				"The base game is still Shattered. Everything Evan Debenham has built over the years is in here, and the other tabs on this screen are his change history, kept as it was.\n" +
				"\n" +
				"The source is open under the GPL, and the About screen credits every mod Warped is built from."));

		changes.addButton( new ChangeButton(Icons.get(Icons.NEWS), "How updates reach you",
				"Warped updates often and in small steps. The news feed on the title screen lists what changed in each build, so this screen only covers the big picture."));
	}

	public static void add_Online( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("Playing together", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(Icons.get(Icons.CONTROLLER), "Local multiplayer",
				"Two players on the same network can run the dungeon together: one hosts, the other joins by address. Both heroes share the floor, the loot and the consequences.\n" +
				"\n" +
				"A spectator slot lets a third person watch a run without joining it."));

		changes.addButton( new ChangeButton(Icons.get(Icons.GOLD), "Online co-op",
				"Distant friends can play together too. The host gets a short room code, the friend types it in, and a relay server carries the game between them. No port forwarding, no address to share.\n" +
				"\n" +
				"The relay costs money to keep running, so it is what a monthly supporter subscription covers. Nothing in the dungeon itself is behind it: local play, every item, every floor and every class stay free for everyone."));
	}

	public static void add_Surface( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("The surface", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(new Image(new TownsfolkSprite()), "A town above the dungeon",
				"Runs start in a town on the surface rather than at the top of the sewers. Shops are indoors, townsfolk wander the streets, and the dungeon entrance opens from the square. A collapsed mine mouth on the edge of town leads somewhere else entirely."));

		changes.addButton( new ChangeButton(Icons.get(Icons.GRASS), "An overworld to walk",
				"Beyond the town is open country: forests, rivers with bridges, deep water, villages with signposts, and wildlife that lives its own life. Seasons and weather change what you find out there, and the place you are standing in is named on the HUD next to the depth."));

		changes.addButton( new ChangeButton(Icons.get(Icons.DISPLAY), "Loading art for every place",
				"Every region has its own loading illustration, including the town, the overworld and the branch floors, instead of only the five classic dungeon regions."));
	}

	public static void add_Climate( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("Weather, time and temperature", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(Icons.get(Icons.CALENDAR), "Day, night and seasons",
				"The world keeps a calendar. Days turn into nights, nights shrink your sight and wake things that sleep through the day, and the seasons roll on across a long run. The moon has phases and they matter.\n" +
				"\n" +
				"By default time advances with your turns. A setting ties it to the real clock instead, so the dungeon is dark when your evening is."));

		changes.addButton( new ChangeButton(new ItemSprite(ItemSpriteSheet.SUNDIAL), "Weather",
				"Rain, snow, wind and storms move through as fronts rather than flipping on and off. Lightning lights up the floor, wind throws your aim, and the sky darkens under cloud. Weather follows you into the upper dungeon and fades the deeper you go.\n" +
				"\n" +
				"The Dimensional Sundial trinket now reads the sky, and a small dial on the HUD shows the time of day."));

		changes.addButton( new ChangeButton(new ItemSprite(ItemSpriteSheet.TORCH), "Temperature",
				"Cold and heat are real. Snow and freezing nights push you toward hypothermia; heat and fire toward heatstroke. Warm armor, a fire nearby or a milder place bring you back.\n" +
				"\n" +
				"Every hero starts with two torches, one already lit, and a burning torch keeps the cold off."));
	}

	public static void add_Growing( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("Things that grow", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(Icons.get(Icons.SEED), "Living plants",
				"Seeds no longer just make a trap for whoever steps on them. Planted, they grow into living plants over time, with dozens of new species ported from Overgrown Pixel Dungeon alongside the ones Shattered already had. Fertilizer speeds them up; some grow food, some grow trouble."));

		changes.addButton( new ChangeButton(Icons.get(Icons.GRASS), "The dungeon regrows",
				"Trampled grass grows back. Plants you left behind keep growing while you are on another floor. Coming back to an old level means coming back to a place that kept living without you."));

		changes.addButton( new ChangeButton(new ItemSprite(ItemSpriteSheet.DEWDROP), "Dew and the deep floors",
				"From Sprouted Pixel Dungeon: the dew vial and its upgrades, coloured dewdrops, and a whole set of floors past the usual end of the dungeon, with their own bosses, vaults and Sokoban puzzles reached through Otiluke's journal."));
	}

	public static void add_Ported( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("From the other mods", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(new ItemSprite(ItemSpriteSheet.GUNSMITHING_TOOL), "Guns, bows and ammunition",
				"Firearms and their ammunition from Re-ARranged Pixel Dungeon, plus a gunsmithing tool to modify them. New bows and a crossbow sit beside them. Every ammo type is for sale in the shops."));

		changes.addButton( new ChangeButton(Icons.get(Icons.ALCHEMY), "New potions, items and enchantments",
				"Dozens of new potions from Overgrown, and items, enchantments and unique weapons from SPS-PD. Monsters and dungeon features from Unleashed; tiles and room layouts from Cursed; the town square, its buildings and the ice caves from Remixed Dungeon."));

		changes.addButton( new ChangeButton(new ItemSprite(ItemSpriteSheet.FOOD_POUCH), "A bigger backpack",
				"43 usable backpack slots, and three new bags to keep them tidy: a food pouch, an ankh chain and a key ring. The waterskin can sit in a quickslot."));
	}

	public static void add_Hero( ArrayList<ChangeInfo> changeInfos ) {

		ChangeInfo changes = new ChangeInfo("The hero", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton( new ChangeButton(Icons.get(Icons.TALENT), "Skills",
				"On top of Shattered's talents, every class has its own skill tree: passives that shape how the class plays and active skills you switch on and off. Skill points are spent on the tree, and active skills fire from a quick-use panel on the HUD."));
	}
}
