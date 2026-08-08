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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoTalent;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSkill;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * The one place your hero grows: a node-based skill tree. Ported Skillful PD
 * skills and Shattered talents hang from the same trunk, paid from the same
 * pool of points, unlocking outward from the root.
 */
public class SkillTreePane extends ScrollPane {

	private static final int NODE   = 18;
	private static final int COL_W  = 24;
	private static final int ROW_H  = 24;
	private static final int MARGIN = 6;
	private static final int TOP    = 14;

	//each branch owns two columns: the chain, and the right-hand half of its fork
	private static final int COL_PASSIVEA = 0;
	private static final int COL_TALENT1  = 2;
	private static final int COL_ACTIVE   = 3;
	private static final int COL_TALENT2  = 5;
	private static final int COL_PASSIVEB = 6;
	private static final int COL_FOURTH   = 8;

	private ArrayList<Node> nodes = new ArrayList<>();
	private ArrayList<ColorBlock> edges = new ArrayList<>();
	private ArrayList<NodeButton> buttons = new ArrayList<>();

	private RenderedTextBlock title;
	private int shownPoints = -1;
	private int gridRows = 0;
	private int gridCols = 0;

	public SkillTreePane(){
		super( new Component() );
		build();
	}

	//rebuilds the whole tree (cheap; called when structure changes)
	public void build(){
		content.clear();
		nodes.clear();
		edges.clear();
		buttons.clear();

		Hero hero = Dungeon.hero;
		CurrentSkills hs = hero.heroSkills;

		title = PixelScene.renderTextBlock(" ", 9);
		title.hardlight(Window.TITLE_COLOR);
		content.add(title);
		shownPoints = -1;

		// ---- the trunk ----
		Node root = Node.root( hs.branchPA, COL_ACTIVE, 0 );
		add(root);

		// ---- four schools of skills, each headed by its own emblem so the
		// ---- player can see what the column IS, then chained downward ----
		//the trunk IS the passiveA branch header (same Skill), so that column
		//hangs straight off it; the other three get their own emblem
		chain( hs.passiveASkills, COL_PASSIVEA, root, 2 );
		chain( hs.activeSkills,   COL_ACTIVE,   header( hs.branchA,  COL_ACTIVE,   root ), 2 );
		chain( hs.passiveBSkills, COL_PASSIVEB, header( hs.branchPB, COL_PASSIVEB, root ), 2 );
		chain( hs.fourthSkills,   COL_FOURTH,   header( hs.branchD,  COL_FOURTH,   root ), 2 );

		// ---- talents: tier 1 and 2 flank the schools, chained downward ----
		Node prev = root;
		int y = 1;
		for (Talent t : hero.talents.get(0).keySet()){
			prev = add( Node.talent( t, 1, COL_TALENT1, y++, prev ) );
		}
		prev = root;
		y = 1;
		for (Talent t : hero.talents.get(1).keySet()){
			prev = add( Node.talent( t, 2, COL_TALENT2, y++, prev ) );
		}

		int deep = 4;
		for (Node n : nodes) deep = Math.max(deep, n.gy + 1);

		// ---- the calling: subclass node opens the lower tree ----
		if (hero.subClass != HeroSubClass.NONE && hs.branchS != null){
			Node sub = add( Node.root( hs.branchS, COL_ACTIVE, deep ) );
			sub.parents.add(root);

			chain( hs.subSkills, COL_ACTIVE, sub, deep + 1 );

			prev = sub;
			y = deep + 1;
			for (Talent t : hero.talents.get(2).keySet()){
				prev = add( Node.talent( t, 3, COL_PASSIVEA, y++, prev ) );
			}
			if (hero.armorAbility != null && hero.talents.size() > 3){
				prev = sub;
				y = deep + 1;
				for (Talent t : hero.talents.get(3).keySet()){
					prev = add( Node.talent( t, 4, COL_TALENT2, y++, prev ) );
				}
			}
		}

		gridRows = 0;
		gridCols = 0;
		for (Node n : nodes){
			gridRows = Math.max(gridRows, n.gy + 1);
			gridCols = Math.max(gridCols, n.gx + 1);
		}

		// ---- edges first (under the buttons) ----
		for (Node n : nodes){
			for (Node p : n.parents){
				addEdge( p, n );
			}
		}

		// ---- buttons ----
		for (Node n : nodes){
			NodeButton btn = new NodeButton( n, this );
			buttons.add(btn);
			content.add(btn);
		}

		//NO layout() here: build() runs from the constructor, before this pane
		//is parented, so camera() is still null and ScrollPane.layout() would
		//NPE. The hosting window lays us out via setRect().
	}

	private Node add( Node n ){
		nodes.add(n);
		return n;
	}

	/** the branch emblem that heads a column; falls back to the trunk if absent */
	private Node header( Skill branch, int gx, Node trunk ){
		if (branch == null) return trunk;
		return add( Node.root( branch, gx, 1, trunk ) );
	}

	private void chain( java.util.List<Skill> branch, int gx, Node parent ){
		chain( branch, gx, parent, 1 );
	}

	/**
	 * Lays a branch out as a vertical chain in its own column. A mutually
	 * exclusive pair is drawn as a fork instead: both options hang off the same
	 * parent, side by side, and the pair always closes the branch.
	 */
	private void chain( java.util.List<Skill> branch, int gx, Node parent, int firstRow ){
		Node prev = parent;
		int y = firstRow;
		for (Skill s : branch){
			if (s == null) continue;
			if (s.exclusiveWith != null && branch.contains(s.exclusiveWith)){
				add( Node.skill( s, gx, y, prev ) );
				add( Node.skill( s.exclusiveWith, gx + 1, y, prev ) );
				return;
			}
			prev = add( Node.skill( s, gx, y, prev ) );
			y++;
		}
	}

	private float nodeX( Node n ){ return MARGIN + n.gx * COL_W; }
	private float nodeY( Node n ){ return TOP + n.gy * ROW_H; }

	private void addEdge( Node p, Node n ){
		float px = nodeX(p) + NODE/2f, py = nodeY(p) + NODE/2f;
		float cx = nodeX(n) + NODE/2f, cy = nodeY(n) + NODE/2f;
		int color = 0xFF3a3a3a;

		if ((int)px == (int)cx){
			ColorBlock v = new ColorBlock(1, Math.abs(cy - py), color);
			v.x = px; v.y = Math.min(py, cy);
			edges.add(v); content.add(v);
		} else if ((int)py == (int)cy){
			ColorBlock h = new ColorBlock(Math.abs(cx - px), 1, color);
			h.x = Math.min(px, cx); h.y = py;
			edges.add(h); content.add(h);
		} else {
			//L shape: horizontal from parent, then vertical into the child
			ColorBlock h = new ColorBlock(Math.abs(cx - px) + 1, 1, color);
			h.x = Math.min(px, cx); h.y = py;
			edges.add(h); content.add(h);
			ColorBlock v = new ColorBlock(1, Math.abs(cy - py), color);
			v.x = cx; v.y = Math.min(py, cy);
			edges.add(v); content.add(v);
		}
	}

	@Override
	public synchronized void update() {
		super.update();
		if (title != null && shownPoints != Skill.availableSkill){
			shownPoints = Skill.availableSkill;
			title.text("Skill Tree" + (shownPoints > 0 ? " - " + shownPoints + " points" : ""));
			title.setPos(( width - title.width()) / 2f, 2);
		}
	}

	@Override
	protected void layout() {
		if (camera() == null) return;   //not parented yet
		float w = Math.max(width, MARGIN * 2 + gridCols * COL_W);
		float h = TOP + gridRows * ROW_H + MARGIN;
		//sized before super.layout(), which reads content.height() to size the thumb
		content.setSize(w, Math.max(h, height));
		super.layout();
		for (NodeButton btn : buttons){
			btn.setRect( nodeX(btn.node), nodeY(btn.node), NODE, NODE );
		}
		if (title != null) title.setPos((width - title.width()) / 2f, 2);
	}

	// ================= the node model =================

	public static class Node {

		public enum Kind { ROOT, SKILL, TALENT }

		public Kind kind;
		public Skill skill;      //SKILL and ROOT
		public Talent talent;    //TALENT
		public int talentTier;

		public int gx, gy;
		public ArrayList<Node> parents = new ArrayList<>();

		public static Node root( Skill branch, int gx, int gy, Node parent ){
			Node n = root( branch, gx, gy );
			if (parent != null) n.parents.add(parent);
			return n;
		}

		public static Node root( Skill branch, int gx, int gy ){
			Node n = new Node();
			n.kind = Kind.ROOT;
			n.skill = branch;
			n.gx = gx; n.gy = gy;
			return n;
		}

		public static Node skill( Skill s, int gx, int gy, Node parent ){
			Node n = new Node();
			n.kind = Kind.SKILL;
			n.skill = s;
			n.gx = gx; n.gy = gy;
			if (parent != null) n.parents.add(parent);
			return n;
		}

		public static Node talent( Talent t, int tier, int gx, int gy, Node parent ){
			Node n = new Node();
			n.kind = Kind.TALENT;
			n.talent = t;
			n.talentTier = tier;
			n.gx = gx; n.gy = gy;
			if (parent != null) n.parents.add(parent);
			return n;
		}

		public int level(){
			switch (kind){
				case TALENT: return Dungeon.hero.pointsInTalent(talent);
				case SKILL:  return skill.level;
				default:     return 0;
			}
		}

		public int maxLevel(){
			switch (kind){
				case TALENT: return talent.maxPoints();
				case SKILL:  return Skill.MAX_LEVEL;
				default:     return 0;
			}
		}

		public int cost(){
			return kind == Kind.TALENT ? 1 : skill.upgradeCost();
		}

		/** the fork sibling that was taken instead of this node, or null */
		private Skill lockedOutBy(){
			if (kind == Kind.SKILL && skill.exclusiveWith != null && skill.exclusiveWith.level > 0){
				return skill.exclusiveWith;
			}
			return null;
		}

		/** tier gates for talents; forks lock each other out; parent progress for everyone */
		public boolean unlocked(){
			if (kind == Kind.ROOT) return true;
			if (lockedOutBy() != null) return false;
			for (Node p : parents){
				if (p.kind != Kind.ROOT && p.level() < 1) return false;
			}
			if (kind == Kind.TALENT){
				Hero h = Dungeon.hero;
				if (h.lvl < Talent.tierLevelThresholds[talentTier] - 1) return false;
				if (talentTier == 3 && h.subClass == HeroSubClass.NONE) return false;
				if (talentTier == 4 && h.armorAbility == null) return false;
			}
			return true;
		}

		public boolean canSpend(){
			return unlocked() && level() < maxLevel() && Skill.availableSkill >= cost();
		}

		public String lockReason(){
			Skill other = lockedOutBy();
			if (other != null) return Messages.get(Skill.class, "exclusive_choice", other.name());
			if (kind == Kind.TALENT){
				Hero h = Dungeon.hero;
				if (h.lvl < Talent.tierLevelThresholds[talentTier] - 1)
					return "Requires level " + (Talent.tierLevelThresholds[talentTier] - 1) + ".";
				if (talentTier == 3 && h.subClass == HeroSubClass.NONE)
					return "Requires a subclass.";
				if (talentTier == 4 && h.armorAbility == null)
					return "Requires an armor ability.";
			}
			return "Advance the connected node first.";
		}

		public Image icon(){
			if (kind == Kind.TALENT) return new TalentIcon( talent );
			return new SkillSprite( skill.image() );
		}
	}

	// ================= node buttons =================

	public class NodeButton extends Button {

		public Node node;
		private SkillTreePane pane;

		private Image bg;
		private ColorBlock frame;
		private Image icon;
		private BitmapText level;

		private int shownLevel = -1;
		private boolean shownSpend = false;
		private boolean shownActive = false;
		private boolean shownUnlocked = true;

		public NodeButton( Node node, SkillTreePane pane ){
			super();
			//let presses fall through to the pane so the tree can be drag-scrolled
			hotArea.blockLevel = PointerArea.NEVER_BLOCK;
			this.node = node;
			this.pane = pane;
			icon = node.icon();
			add(icon);
			sync();
		}

		private void sync(){
			shownLevel = node.level();
			shownSpend = node.canSpend();
			shownActive = node.kind == Node.Kind.SKILL && node.skill.active;
			shownUnlocked = node.unlocked();

			if (!shownUnlocked){
				bg.hardlight( 0x101010 );
				icon.alpha( 0.25f );
				frame.visible = false;
			} else {
				if (shownActive){
					bg.hardlight( 0x1d3a1d );
				} else if (shownLevel >= node.maxLevel() && node.kind != Node.Kind.ROOT){
					bg.hardlight( 0x3a2f10 );
				} else {
					bg.hardlight( 0x1f1f1f );
				}
				icon.alpha( node.kind == Node.Kind.SKILL && shownLevel == 0 ? 0.55f : 1f );
				frame.visible = shownSpend;
			}

			if (shownLevel > 0 && node.kind != Node.Kind.ROOT){
				level.text( Integer.toString( shownLevel ) );
			} else {
				level.text( "" );
			}
			level.measure();
		}

		@Override
		protected void createChildren(){
			super.createChildren();

			frame = new ColorBlock(1, 1, 0xFF77dd77);
			add(frame);

			bg = new Image( com.watabou.gltextures.TextureCache.createSolid( 0xFFFFFFFF ) );
			add( bg );

			level = new BitmapText( PixelScene.pixelFont );
			level.hardlight( 0xFFFFAA );
			add( level );
		}

		@Override
		public synchronized void update() {
			super.update();
			if (shownLevel != node.level() || shownSpend != node.canSpend()
					|| shownUnlocked != node.unlocked()
					|| shownActive != (node.kind == Node.Kind.SKILL && node.skill.active)){
				sync();
				layout();
			}
			//available nodes breathe
			if (frame.visible){
				frame.alpha( 0.6f + 0.4f * (float)Math.sin( com.watabou.noosa.Game.timeTotal * 5 ) );
			}
		}

		@Override
		protected void layout(){
			super.layout();

			frame.size( width + 2, height + 2 );
			frame.x = x - 1;
			frame.y = y - 1;

			bg.scale.set( width, height );
			bg.x = x;
			bg.y = y;

			if (icon != null){
				icon.x = x + (width - icon.width()) / 2;
				icon.y = y + (height - icon.height()) / 2;
				PixelScene.align( icon );
			}

			level.x = x + width - level.width();
			level.y = y + height - level.baseLine() - 1;
			PixelScene.align( level );
		}

		@Override
		protected void onClick(){

			//a locked node still opens read-only: only spending is gated (canSpend()
			//already requires unlocked()), so the player can always read what a node does
			if (!node.unlocked()){
				GLog.w( node.lockReason() );
			}

			//find the hosting window so casts can close it
			Group g = parent;
			while (g != null && !(g instanceof Window)) g = g.parent;
			Window host = (Window)g;

			if (node.kind == Node.Kind.TALENT){
				WndInfoTalent.TalentButtonCallback callback = null;
				if (node.canSpend()){
					callback = new WndInfoTalent.TalentButtonCallback() {
						@Override
						public String prompt() {
							return "Upgrade (1 point)";
						}
						@Override
						public void call() {
							Dungeon.hero.upgradeTalent( node.talent );
						}
					};
				}
				GameScene.show( new WndInfoTalent( node.talent, node.level(), callback ) );
			} else {
				GameScene.show( new WndSkill( host, node.skill,
						node.kind == Node.Kind.SKILL && node.canSpend() ) );
			}
		}
	}
}
