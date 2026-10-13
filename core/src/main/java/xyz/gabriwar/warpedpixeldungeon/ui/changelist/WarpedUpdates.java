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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Stealth;
import xyz.gabriwar.warpedpixeldungeon.sprites.FortuneTellerFolkSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ChaosMageSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DwarfKingTombSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MimicSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PortalGateSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SnakeSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.TowerSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Every Warped update a player sees, newest first: the Changes screen's version tabs (one per
 * major version, a square per version, addMajors), and the rows the patched screen shows (WelcomeScene) for whatever is
 * newer than the version the player last ran. Every change that reaches players gets an entry here
 * (CLAUDE.md): its own icon, a short title, a one-line summary for the patched screen and the
 * fuller text for the Changes screen. Nothing about the debug menu ever goes in.
 *
 * `since` is the first version code the entry ships in (any later code also has it): a version
 * code is the commit count plus 79 (build.gradle), so an entry written now takes the next one.
 */
public final class WarpedUpdates {

	private WarpedUpdates(){}

	public static final class Entry {
		public final int since;
		public final Supplier<Image> icon;
		public final String title, summary, body;

		Entry( int since, Supplier<Image> icon, String title, String summary, String body ){
			this.since = since;
			this.icon = icon;
			this.title = title;
			this.summary = summary;
			this.body = body;
		}
	}

	private static Supplier<Image> item( int sprite ){
		return () -> new ItemSprite( sprite );
	}

	private static Supplier<Image> icon( Icons icon ){
		return () -> Icons.get( icon );
	}

	//the branch stairways' own art, the temple's (custom_tiles/branch_stairs.png, tile 1)
	private static Image branchStairs(){
		Image i = new Image( Assets.Environment.BRANCH_STAIRS );
		i.frame( i.texture.uvRectBySize( 16, 0, 16, 16 ) );
		return i;
	}

	//the portal gate as the town's always stands: lit
	private static Image litGate(){
		PortalGateSprite s = new PortalGateSprite();
		s.onStateChange( PortalGate.State.DEAD, PortalGate.State.ACTIVE );
		return s;
	}

	/** Newest first. */
	public static final Entry[] ALL = {
		new Entry( 10648, item( ItemSpriteSheet.SCROLL_HOLDER ), "Scholars' shelves",
				"Searched bookshelves give a scroll 8% of the time, and only five useful kinds.",
				"A bookshelf emptied by a search now holds a scroll 8% of the time instead of 10%, but always a scholar's kind: Identify, Remove Curse, Recharging, Transmutation or Magic Mapping." ),
		new Entry( 10646, icon( Icons.LANGS ), "Back to English",
				"This update starts the game in English once; switching language now asks first.",
				"This update starts the game in English once, since Warped is written in English and much of it is not translated yet. You can pick another language again in the settings, and it stays; changing the language now asks you to confirm first." ),
		new Entry( 10646, icon( Icons.MAGNIFY ), "Identifying three times a week",
				"The hermit identifies 3 items free a week; the fortune teller reads 3 a week.",
				"The hermit in his mountain hut now identifies three items for free each week instead of one each day, then charges his usual fee. The town's fortune teller now reads three items a week for her price instead of one a season. Both tell you how many are left this week." ),
		new Entry( 10646, icon( Icons.WARNING ), "A crash fixed",
				"Fixed a crash when a monster picked its target as another one appeared.",
				"Fixed a crash that could close the game while monsters were choosing whom to fight, when a creature appeared or left at the same moment." ),
		new Entry( 10645, item( ItemSpriteSheet.MASTERWORK_CORE ), "Masterwork in five steps",
				"Masterwork is 5 steps of a core each; the last doubles every bonus line.",
				"Masterwork now takes five steps, one core each. Each of the first four lifts every rarity bonus line by 15%, and the fifth, which still needs a legendary or exotic piece, doubles them. A fully masterworked piece stays fully masterworked, and the steps of one partly done keep their place." ),
		new Entry( 10644, item( ItemSpriteSheet.MASTERWORK_CORE ), "Masterwork in three steps",
				"Masterwork is 3 steps of a core each; the last doubles every bonus line.",
				"Masterwork now takes three steps, one core each. The first and the second lift every rarity bonus line by a quarter, and the third, which still needs a legendary or exotic piece, doubles them. Gear masterworked before keeps its place: steps one to five count as the first step, six to nine as the second, and the tenth as the third." ),
		new Entry( 10644, item( ItemSpriteSheet.STYLUS ), "Weaves are not kept",
				"Three weaves left unchosen at the pedestal are lost.",
				"The enchanting pedestal no longer keeps three weaves you paid to see: take one or they are gone, and seeing three more costs 20 again. Closing them unchosen still asks first." ),
		new Entry( 10643, item( ItemSpriteSheet.STYLUS ), "A new enchanting pedestal",
				"The pedestal is remade: socket cards, and 20 energy shows three new weaves.",
				"The enchanting pedestal is remade. Your piece rests in a ring of glyphs in its enchantments' colours, and each of its two sockets is a card: the enchantment or glyph named in its own colour, its level as pips, what it does, and Empower, Reroll and full details at hand. Weaving a new one or rerolling one now costs 20 energy to see three, shown side by side with what each does, and the one you take is free. Leave without taking one and they wait on the piece, free to come back to." ),
		new Entry( 10642, item( ItemSpriteSheet.SWORD ), "Strength lines that work",
				"A strength line takes off 1, or 2 on a good roll, on every weapon and armor.",
				"A bonus line that lowers the strength a piece needs now takes off what it says: 1, or 2 when it rolled at least half. It never reads -0 any more, and the guns, bows, Greataxe and other pieces that ignored the line now get it too." ),
		new Entry( 10642, icon( Icons.ALERT ), "No more stray squares",
				"Fixed the attack button and rarity frames flashing in the wrong place.",
				"Fixed the attack button flashing in the screen's top-left corner when a monster first comes in reach, a rare item's colored frame and sparks flashing at the wrong spot before its slot settled, and stray dots left over the map after watching a Descent Guide demo." ),
		new Entry( 10636, item( ItemSpriteSheet.MASTERY ), "A guide you fill as you play",
				"Descent Guide pages now write themselves as you learn, starting in the dungeon.",
				"The Descent Guide starts empty. Each First Steps page writes itself the first time its lesson comes up: your first level, wand, key, seed, Scroll of Upgrade, gear too heavy for you, or a fight in a doorway. Nothing writes itself until you first go down into the dungeon. Coming back up writes the world above's pages and its word on the road. A day in the dungeon writes Weather & Seasons, unlocking a hero writes Heroes & Classes, and a first building material or builder's satchel writes the homestead's pages. Pages you already read stay." ),
		new Entry( 10636, icon( Icons.JOURNAL ), "A calmer tutorial",
				"The journal button waits until you pick up the guide's first page.",
				"During the tutorial the journal button stays hidden until you pick up the guide's torn first page, and no other page makes it flash first, so pressing it can no longer break the tutorial." ),
		new Entry( 10635, item( ItemSpriteSheet.BOTTLE_OF_WATER ), "Bottles of water",
				"The potion of water is now a bottle of water, always known; fill empty bottles.",
				"The potion of water is now the bottle of water: always known, in a clear bottle of its own. Every shop sells empty bottles five to a stack; fill one at any water you stand in or beside, or in an alchemy pot, for a bottle of water." ),
		new Entry( 10635, item( ItemSpriteSheet.ARMOR_ROGUE ), "Class armor keeps its quality",
				"The crown and the armor kit keep your armor's rarity and type.",
				"Turning your armor into class armor, with the Dwarf King's crown or an armor kit, now keeps its rarity, its type, its bonus lines and its masterwork. Before, class armor came out plain." ),
		new Entry( 10635, item( ItemSpriteSheet.WOODCUTTERS_AXE ), "Walk over stumps",
				"A tap walks onto a stump; hold the tap to chop it out.",
				"Tree stumps no longer get in your way: a tap on one beside you walks onto it. Hold the tap with an axe in your pack to chop it out for one more log, as a tap did before." ),
		new Entry( 10635, item( ItemSpriteSheet.DWARF_HAMMER ), "Builder's tool in the satchel",
				"The builder's satchel now holds the builder's tool.",
				"The builder's tool goes in the builder's satchel now, with the materials and portable chests, instead of your backpack." ),
		new Entry( 10635, item( ItemSpriteSheet.SEED_ICECAP ), "Clearer ice clusters",
				"The Frozen Caves' ice clusters no longer look like open floor.",
				"The ice clusters that block the way in the Frozen Caves are redrawn as dark stone crusted with blue-white ice, outlined and shadowed, so they stand out from the floor around them." ),
		new Entry( 10635, () -> new ChaosMageSprite(), "Chaos mage drops",
				"Chaos mages drop a Potion of Experience 1 time in 50.",
				"A chaos mage used to drop a Potion of Experience every time it died. Now one does 1 time in 50." ),
		new Entry( 10634, icon( Icons.CHANGES ), "Changes by version",
				"The Changes screen lists Warped's updates by version, in tabs.",
				"Warped's updates are no longer one long list. As in Shattered's history, each version has its own square headed by its number, and each major version (v18, v17 and so on) its own tab. The screen opens on the newest; the WPD tab keeps the overview, and SPD leads to Shattered's history." ),
		new Entry( 10633, item( ItemSpriteSheet.SEED_FIREBLOOM ), "No stuck firelight",
				"A fire's glow no longer stays on you after the flames go out.",
				"Sometimes, after burning ended, the fire's light stayed on the hero for good: a still glow on the body and a pool of light at the feet, with no flames. It happened when a burning character moved a long way from one moment to the next, such as a teleport or the open world's view shifting. The glow now stays with its owner wherever it goes and goes out with the fire. The same holds for a torch's light and the other lights a character carries." ),
		new Entry( 10633, WarpedUpdates::litGate, "A portal gate in town",
				"Dolyahaven's square now has a portal gate that is always lit.",
				"A portal gate now stands in Dolyahaven's town square. It is always lit and costs nothing to use. From it you can travel to any lit shop gate you have tapped, and every lit shop gate now lists the town, landing you beside its gate. Each trip costs 150 hunger, as at any gate." ),
		new Entry( 10633, () -> new SkillSprite( new Stealth().image() ), "A double when you spot a foe",
				"The rogue's Stealth double now rises when you spot a new enemy.",
				"Stealth, the rogue's skill: its smoky double no longer pops up whenever some enemy notices you, even one you cannot see. It now rises beside you the moment you spot a new enemy, once per enemy, and never again while that enemy stays in view. An awake enemy hunts the double first; a sleeping one is left asleep." ),
		new Entry( 10633, () -> new TowerSprite(), "One tower for DM-300",
				"DM-300's arena has a single tower now, with half the health.",
				"Only one tower stands beside DM-300 now, instead of two, and it has 130 health instead of about 260. It still prints broken robots and calls the floor's monsters when hit." ),
		new Entry( 10633, item( ItemSpriteSheet.BUILDERS_SATCHEL ), "Tidier bags",
				"Mr Destructo fits the scroll holder; portable chests fit the builder's satchel.",
				"Mr Destructo, switched on or off, now goes in the scroll holder, and portable chests go in the builder's satchel instead of your backpack." ),
		new Entry( 10633, item( ItemSpriteSheet.CHAIN ), "Portals on the ankh chain",
				"The ankh chain now holds every item that takes you somewhere.",
				"Besides ankhs, rings and keys, the ankh chain now holds the books of the dead, the living and the heavens, the ancient coin, the inscribed bone, the conch shell, the forged SanChikarah, the return beacon, Lloyd's beacon and the vault's escape crystal." ),
		new Entry( 10622, () -> new DwarfKingTombSprite(), "Aim at the tomb",
				"The Dwarf King's tomb can now be targeted.",
				"The Dwarf King's tomb now counts as a foe: tapping it attacks it, and wands, thrown weapons and the attack button aim at it. It still never attacks and takes no damage while the king lives." ),
		new Entry( 10621, item( ItemSpriteSheet.TRUMPET ), "Instruments that play",
				"The trumpet, flute and harp play notes as you use them.",
				"The trumpet, the flute and the harp now play a note with every hit, picked at random from a pentatonic scale, so a fight with one plays a little tune. Their special abilities play a run of notes too." ),
		new Entry( 10620, item( ItemSpriteSheet.TENGU_SHOCKER ), "Bigger Tengu toys",
				"The rogue's shocker and fire bomb last longer and end with a blast.",
				"Tengu's Arsenal, the rogue's skill: the shocker now arcs lightning in all eight directions at once, 2 tiles out, for 6 turns instead of 3, then blows up like the smoke bomb. The fire bomb now sits where it lands and keeps setting the ground around it ablaze for 5 turns; at the skill's last level its flames reach 2 tiles out and it blows up at the end." ),
		new Entry( 10619, item( ItemSpriteSheet.CHAIN ), "Keys on the ankh chain",
				"The ankh chain now holds keys of every kind.",
				"Besides ankhs and rings, the ankh chain now holds every kind of key: the ancient keys that open portals to hidden areas and Tengu's hideout, the dungeon's door keys and the skeleton key." ),
		new Entry( 10610, () -> new FortuneTellerFolkSprite(), "One reading a season",
				"The fortune teller now reads one item each season.",
				"The town's fortune teller identifies one item each season, then asks you to come back when the next season begins; the same season a year later counts as a new one. Her price still follows the moon. She no longer reads your whole pack at once." ),
		new Entry( 10609, () -> new MimicSprite(), "No more monster boxes",
				"The monster box is gone; its chests are mimics now.",
				"The monster box, a plain-looking chest that turned into a monster when opened, is gone. The chests that hid one are now mimics, with the mimic's look and tricks. A monster box left unopened in a saved game is a plain chest now, and one already awake is a mimic." ),
		new Entry( 10608, item( ItemSpriteSheet.NORN_GREEN ), "Green stones for everyone",
				"Every hero can now find green Norn Stones.",
				"Gullin used to drop green Norn Stones only for the huntress. Now every hero finds all five colours, so anyone can forge Jupiter's Wraith, the thrown relic, at the temple altar." ),

		new Entry( 10602, () -> new SnakeSprite(), "A living guide",
				"Guide pages show what they name, alive, and act out how things work.",
				"Every Descent Guide page now shows the monsters, items, places, traps, plants, gases and statuses it names, each under the paragraph that names it: monsters idle and strike now and then, items float and glow, fire and gas pour. Tap one to read what the game says about it. Pages that explain how something works act it out in a small looping scene on the game's own tiles and sprites: Goo's charged slam, DM-300's rockfall, Tengu's traps, Yog's beams and fists, the Demon Lord's breath, a gnoll brute's last rage, a spinner's webs, and every lesson of First Steps. Tap a scene to pause it. The page cards on the guide's map show their subject moving too." ),
		new Entry( 10602, item( ItemSpriteSheet.MASTERY ), "The tome is in the guide",
				"The Tome of Dungeon Mastery is now the guide's first chapter.",
				"The Descent Guide opens with a new chapter, First Steps: the tome's 17 pages, rewritten to be true to Warped and open from the start. When the journal button flashes, press it to read the page that fits the moment, right over the game. The tome, its torn pages and its journal tab are gone, and the title screen's journal has one button fewer. Pages you had read stay read. Lore pages no longer wait until you have found every page of the tome: they turn up from the first floor." ),

		new Entry( 10581, item( ItemSpriteSheet.FOLLOWER_ARMOR ), "Redrawn gear",
				"Thirteen items redrawn to match the rest of the game's art.",
				"The ascetic wraps, follower overcoat, performer's jacket, soldier vest and ceramic armor, the arm cannon and its shot, Jupiter's wraith, the bee arrow, the woodcutter's axe, the mason's hammer, the trumpet, the war drum and the cup of coffee have new sprites: proper outlines, light from the top left and real shading, so they sit beside the other items as equals." ),

		new Entry( 10578, item( ItemSpriteSheet.ARTIFACT_BEACON ), "Lloyd's Beacon in the shop",
				"The first shop, on depth 6, now sells a Lloyd's Beacon.",
				"Lloyd's Beacon could not be found in a run, though Tengu, DM-300 and the Dwarf King each give a carried one a level. The shop on depth 6 now sells one: set it somewhere, and come back to that spot whenever you like." ),

		new Entry( 10577, icon( Icons.AUDIO ), "Stronger, adjustable sound",
				"Stereo and room acoustics are much stronger, each with its own slider.",
				"Sounds now pan much harder to the side they come from and fade sooner with distance, and rooms answer back louder: bigger reverb in halls and caverns, stronger echoes, and blows behind a door or the rock come through duller. Settings, Audio has a strength slider (1 to 5) under each switch: 2 is how they sounded before, 4 is the new default." ),

		new Entry( 10576, icon( Icons.DISPLAY_LAND ), "Play on PC",
				"Every release on GitHub now has a jar for Windows, macOS and Linux.",
				"Each release on the game's GitHub page (GabriWar/warpedpd) now carries a desktop build beside the APKs: one jar that runs on Windows, macOS and Linux. Install Java 17 or newer and open it, or run java -jar on it." ),

		new Entry( 10571, icon( Icons.DEPTH ), "Clearer place names",
				"The place name by the stairs icon is crisp and always fits.",
				"The name of the place you are in, under the stairs icon, is drawn in whole pixels so its letters no longer break up, and a long name (a village's, a homestead's) steps down a size or is shortened so it always fits." ),

		new Entry( 10570, () -> new TowerSprite(), "DM-300's towers",
				"The towers in DM-300's arena can be attacked, and are weaker.",
				"The two towers beside DM-300 can now be attacked like any monster. They have less health (about 260) and armor, and print at most two broken robots at a time, less often." ),
		new Entry( 10570, WarpedUpdates::branchStairs, "Branch stairways",
				"The spider nest, temple and frozen halls stairs have their own look.",
				"The stairs into the spider nest are webbed, the temple's are marble and gold, and the frozen halls' are glazed with ice, so none is taken for the floor's stairs down. Coming up out of a branch now puts you back on its own stairs: leaving the smith's mines no longer drops you on the temple's." ),
		new Entry( 10570, icon( Icons.COMPASS ), "A compass for every staircase",
				"Arrows round your portrait point to every staircase, in colour.",
				"The compass round your portrait now has an arrow for every staircase on the floor you have found: gold for stairs down, blue for stairs up, violet into a branch, green back out of one, white to the surface." ),

		new Entry( 10569, item( ItemSpriteSheet.ACORN ), "Planting the land back",
				"Axes clear bushes and stumps; acorns, pine cones and bush seeds grow.",
				"An axe clears a bush, and chops out a stump so it grows back no more. Felling an oak or a pine sometimes drops an acorn or a pine cone, and clearing a bush a bush seed. Plant them in grass or bare earth: a tree grows in 14 days of spring or summer, a bush in 6." ),
		new Entry( 10569, item( ItemSpriteSheet.TORCH ), "Torches in the ground",
				"Set a torch in the ground: it burns for good and keeps animals away.",
				"Out in the world a torch can be placed in the ground beside you. It burns for good, lights the ground round it and keeps wild animals from settling nearby; tap it to take it back. Burnt trees leave charcoal, and the workbench makes torches from charcoal and a plank." ),

		new Entry( 10564, item( ItemSpriteSheet.SEED_ICECAP ), "Frozen water",
				"Ice now frays into water in its own colours.",
				"Frozen pools in the sewers, prison, caves and mines are tinted from their own water, and the frayed edge where ice meets water uses the ice's own colours, with no pale seam." ),

		new Entry( 10563, item( ItemSpriteSheet.BUILDERS_SATCHEL ), "Homesteads",
				"Build anywhere in the open world: walls, roofs, doors and rooms.",
				"Out in the world you can build anywhere: log, plank, stone and brick walls, windows, plain and barred doors, fences, palisades, paths, bridges and five kinds of roof, from a new build menu. Walls and a roof make a room that keeps out wind and snow." ),
		new Entry( 10563, item( ItemSpriteSheet.WOODCUTTERS_AXE ), "Gathering and crafting",
				"Fell trees, quarry stone, dig clay and craft at stations.",
				"Fell trees for logs, break boulders for stone, dig clay and sand, cut thatch, and hunt for hides. Workbench, mason's bench, kiln, hearth, cooking pot, tanning rack, forge and quern turn them into what you build with." ),
		new Entry( 10563, icon( Icons.SLEEP ), "Rooms, hearths and safe sleep",
				"A lit hearth warms a room; a bed in a shut room is never ambushed.",
				"Rooms take a type from what is in them: bedroom, kitchen, workshop, storehouse, greenhouse, shrine. A lit hearth warms a room, and sleeping in a bed in a closed room is safe and leaves you well rested." ),
		new Entry( 10563, icon( Icons.SEED ), "Farms and animals",
				"Farm through the seasons, and rope animals into pens.",
				"Dig farm plots, water them, and grow food through the seasons; a glass roof keeps crops growing in winter. Rope a deer, goat or bunny and lead it into a fenced pen, where it stays." ),
		new Entry( 10563, icon( Icons.TALENT ), "Homes and waystones",
				"Hearthstones make homes; travel between waystones for hunger.",
				"A hearthstone makes a place your home. Homes draw wolves and bandits, so build defences. Touch a settlement's waystone to attune it, and travel between waystones and homes for hunger alone. The safe room is gone: old saves bring its belongings to the town." ),
	};

	/** The entries newer than a version the player last ran, newest first. */
	public static ArrayList<Entry> since( int previousVersion ){
		ArrayList<Entry> out = new ArrayList<>();
		for (Entry e : ALL) if (e.since > previousVersion) out.add( e );
		return out;
	}

	/** The version name a version code shows as: build.gradle's code - 8787, as a.b.c. */
	public static String versionName( int code ){
		int v = code - 8787;
		return v > 0 ? (v / 100) + "." + (v / 10 % 10) + "." + (v % 10) : "3.4.0";
	}

	/** The major version (the a of a.b.c) a version code belongs to. */
	public static int major( int code ){
		return Math.max( 0, code - 8787 ) / 100;
	}

	/** Every major version an update shipped in, newest first. */
	public static ArrayList<Integer> majors(){
		ArrayList<Integer> out = new ArrayList<>();
		for (Entry e : ALL){
			int m = major( e.since );
			if (!out.contains( m )) out.add( m );
		}
		return out;
	}

	/** The updates of the given major versions, newest first, each version in its own square
	 *  headed by its number, as Shattered's own history is laid out. */
	public static void addMajors( ArrayList<ChangeInfo> changeInfos, List<Integer> majors ){
		ChangeInfo version = null;
		int at = -1;
		for (Entry e : ALL){
			if (!majors.contains( major( e.since ) )) continue;
			if (e.since != at){
				at = e.since;
				version = new ChangeInfo( "v" + versionName( at ), true, "" );
				version.hardlight( Window.TITLE_COLOR );
				changeInfos.add( version );
			}
			version.addButton( new ChangeButton( e.icon.get(), e.title, e.body ) );
		}
	}
}
