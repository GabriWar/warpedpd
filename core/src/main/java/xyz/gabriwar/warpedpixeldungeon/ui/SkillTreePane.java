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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BranchSkill;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoTalent;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSkill;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * The one place your hero grows: a node-based skill tree. Ported Skillful PD
 * skills and Shattered talents hang from the same trunk, paid from the same
 * pool of points, unlocking outward from the root.
 *
 * Drawn as sockets set in stone: the rim of a socket says what the node is
 * (dark when locked, grey when open, green and glowing when you can take it,
 * bronze, silver and gold as points go in), pips under it count the points,
 * and the wires between nodes light up along the way you have gone.
 */
public class SkillTreePane extends ScrollPane {

	private static final int NODE   = SkillTreeArt.SOCKET;
	private static final int COL_W  = 26;
	private static final int ROW_H  = 27;
	private static final int MARGIN = 6;
	private static final int HEAD   = 14;           //the fixed strip with the points
	private static final int TOP    = HEAD + 4;
	private static final int PIPS   = 3;            //the strip under a socket that holds its pips
	private static final int RUN    = NODE + 4;     //where in a row the wires run sideways

	//each branch owns two columns: the chain, and the right-hand half of its fork
	private static final int COL_PASSIVEA = 0;
	private static final int COL_TALENT1  = 2;
	private static final int COL_ACTIVE   = 3;
	private static final int COL_TALENT2  = 5;
	private static final int COL_PASSIVEB = 6;
	private static final int COL_FOURTH   = 8;

	private static final int COL_LOCKED = 0x2c2c34;
	private static final int COL_OPEN   = 0x6a5a30;
	private static final int COL_AVAIL  = 0x77dd77;
	private static final int COL_LIT    = 0xe0b840;
	private static final int COL_LANE   = 0x0a1020;

	private ArrayList<Node> nodes = new ArrayList<>();
	private ArrayList<Edge> edges = new ArrayList<>();
	private ArrayList<NodeButton> buttons = new ArrayList<>();

	private SkinnedBlock stone;
	private Group backdrop;
	private Group edgeLayer;
	private Group nodeLayer;

	//fixed over the tree: the points to spend, the hero's level, a sideways thumb
	private ColorBlock headBg;
	private ColorBlock headLine;
	private Image headIcon;
	private RenderedTextBlock headText;
	private RenderedTextBlock headLvl;
	private ColorBlock hthumb;

	private int shownPoints = -1;
	private int shownLvl = -1;
	private int gridRows = 0;
	private int gridCols = 0;

	public SkillTreePane(){
		super( new Component() );

		headBg = new ColorBlock( 1, 1, 0xFF000000 );
		headBg.alpha( 0.7f );
		add( headBg );
		headLine = new ColorBlock( 1, 1, 0xFF3a3a44 );
		add( headLine );
		headIcon = Icons.get( Icons.TALENT );
		add( headIcon );
		headText = PixelScene.renderTextBlock( 6 );
		add( headText );
		headLvl = PixelScene.renderTextBlock( 6 );
		headLvl.hardlight( 0xAAAAAA );
		add( headLvl );
		hthumb = new ColorBlock( 1, 1, THUMB_COLOR );
		hthumb.alpha( THUMB_ALPHA );
		add( hthumb );

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

		//the textures must exist before anything asks the cache for them by key
		SkillTreeArt.get();
		SkillTreeArt.stone();
		stone = new SkinnedBlock( 1, 1, SkillTreeArt.STONE_KEY );
		content.add( stone );
		backdrop = new Group();
		content.add( backdrop );
		edgeLayer = new Group();
		content.add( edgeLayer );
		nodeLayer = new Group();
		content.add( nodeLayer );
		shownPoints = -1;
		shownLvl = -1;

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

		// ---- lanes under the talent columns, each headed by its tier ----
		lane( 1, 0 );
		lane( 2, 0 );
		lane( 3, deep );
		lane( 4, deep );

		// ---- buttons ----
		for (Node n : nodes){
			NodeButton btn = new NodeButton( n, this );
			buttons.add(btn);
			nodeLayer.add(btn);
		}

		// ---- wires ----
		for (NodeButton b : buttons){
			for (Node p : b.node.parents){
				edges.add( new Edge( p, b ) );
			}
		}
		for (Edge e : edges) edgeLayer.add( e );
		stackEdges();

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

	/** a tinted band down a talent column, headed "talents" and the level the tier asks for */
	private void lane( int tier, int labelRow ){
		int gx = -1, first = Integer.MAX_VALUE, last = -1;
		for (Node n : nodes){
			if (n.kind == Node.Kind.TALENT && n.talentTier == tier){
				gx = n.gx;
				first = Math.min( first, n.gy );
				last = Math.max( last, n.gy );
			}
		}
		if (gx < 0) return;

		float top = nodeY( labelRow ) - 2;
		float bottom = nodeY( last ) + NODE + PIPS + 3;
		ColorBlock band = new ColorBlock( COL_W, bottom - top, 0xFF000000 | COL_LANE );
		band.alpha( 0.55f );
		band.x = nodeX( gx ) - (COL_W - NODE) / 2f;
		band.y = top;
		backdrop.add( band );

		int need = Talent.tierLevelThresholds[tier] - 1;
		boolean reached = Dungeon.hero.lvl >= need;
		int color = reached ? 0x9fb0d8 : 0x5d6070;

		//"talents", then the level the tier asks for: skills have tiers too (their
		//price), so the tier number itself would only confuse
		RenderedTextBlock name = PixelScene.renderTextBlock( Messages.get( SkillTreePane.class, "talents" ), 6 );
		name.hardlight( color );
		name.setPos( nodeX( gx ) + (NODE - name.width()) / 2f, nodeY( labelRow ) + 1 );
		PixelScene.align( name );
		backdrop.add( name );

		if (need > 1){
			RenderedTextBlock lvl = PixelScene.renderTextBlock( Messages.get( SkillTreePane.class, "lvl", need ), 6 );
			lvl.hardlight( color );
			lvl.setPos( nodeX( gx ) + (NODE - lvl.width()) / 2f, name.bottom() + 1 );
			PixelScene.align( lvl );
			backdrop.add( lvl );
		}
	}

	/**
	 * Wires share pixels: every child of the trunk runs along the same bar, and
	 * the trunk-to-calling link runs down the whole active column. The long ones
	 * go underneath so a chain draws over the link, and among the rest the
	 * brighter state wins, so a lit stretch is never painted over by a dark one.
	 */
	private void stackEdges(){
		Collections.sort( edges, new Comparator<Edge>() {
			@Override
			public int compare( Edge a, Edge b ) {
				return a.span != b.span ? b.span - a.span : a.rank - b.rank;
			}
		} );
		for (Edge e : edges) edgeLayer.bringToFront( e );
		edgesDirty = false;
	}

	private boolean edgesDirty = false;

	private float nodeX( int gx ){ return MARGIN + gx * COL_W; }
	private float nodeY( int gy ){ return TOP + gy * ROW_H; }
	private float nodeX( Node n ){ return nodeX( n.gx ); }
	private float nodeY( Node n ){ return nodeY( n.gy ); }

	@Override
	public synchronized void update() {
		super.update();

		if (edgesDirty) stackEdges();

		if (shownPoints != Skill.availableSkill){
			shownPoints = Skill.availableSkill;
			if (shownPoints > 0){
				headText.text( Messages.get( this, "points", shownPoints ) );
				headText.hardlight( Window.TITLE_COLOR );
			} else {
				headText.text( Messages.get( this, "no_points" ) );
				headText.hardlight( 0x888888 );
			}
			layoutHead();
		}
		if (Dungeon.hero != null && shownLvl != Dungeon.hero.lvl){
			shownLvl = Dungeon.hero.lvl;
			headLvl.text( Messages.get( this, "hero_lvl", shownLvl ) );
			layoutHead();
		}

		//a sideways thumb, as the tree is wider than the window it sits in
		hthumb.visible = width < content.width();
		if (hthumb.visible){
			hthumb.scale.set( width * width / content.width(), 2 );
			hthumb.x = x + width * content.camera.scroll.x / content.width();
			hthumb.y = bottom() - 2;
		}
	}

	private void layoutHead(){
		headBg.size( width, HEAD );
		headBg.x = x;
		headBg.y = y;
		headLine.size( width, 1 );
		headLine.x = x;
		headLine.y = y + HEAD;
		headIcon.x = x + 2;
		headIcon.y = y + (HEAD - headIcon.height()) / 2f;
		PixelScene.align( headIcon );
		headText.setPos( headIcon.x + headIcon.width() + 3, y + (HEAD - headText.height()) / 2f );
		PixelScene.align( headText );
		headLvl.setPos( x + width - 5 - headLvl.width(), y + (HEAD - headLvl.height()) / 2f );
		PixelScene.align( headLvl );
	}

	@Override
	protected void layout() {
		if (camera() == null) return;   //not parented yet
		float w = Math.max(width, MARGIN * 2 + gridCols * COL_W);
		float h = TOP + gridRows * ROW_H + MARGIN;
		//sized before super.layout(), which reads content.height() to size the thumb
		content.setSize(w, Math.max(h, height));
		super.layout();
		stone.size( content.width(), content.height() );
		for (NodeButton btn : buttons){
			btn.setRect( nodeX(btn.node), nodeY(btn.node), NODE, NODE );
		}
		layoutHead();
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
				//an emblem wears the points spent in its branch
				default:     return skill instanceof BranchSkill ? ((BranchSkill) skill).spent() : 0;
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

		public String name(){
			return Messages.titleCase( kind == Kind.TALENT ? talent.title() : skill.name() );
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
					return Messages.get( SkillTreePane.class, "req_level", Talent.tierLevelThresholds[talentTier] - 1 );
				if (talentTier == 3 && h.subClass == HeroSubClass.NONE)
					return Messages.get( SkillTreePane.class, "req_subclass" );
				if (talentTier == 4 && h.armorAbility == null)
					return Messages.get( SkillTreePane.class, "req_armor" );
			}
			return Messages.get( SkillTreePane.class, "req_parent" );
		}

		public Image icon(){
			if (kind == Kind.TALENT) return new TalentIcon( talent );
			return new SkillSprite( skill.image() );
		}
	}

	// ================= wires =================

	/**
	 * The wire from a parent down into a child: a dark outline three wide with
	 * a one-pixel core that takes the child's state. It leaves the parent under
	 * its pips, runs sideways in the gap below that row, and drops into the top
	 * of the child.
	 */
	private class Edge extends Group {

		private final NodeButton child;
		private final int span;
		private final ArrayList<ColorBlock> cores = new ArrayList<>();
		private int rank = -1;

		Edge( Node p, NodeButton child ){
			super();
			this.child = child;
			Node n = child.node;
			span = n.gy - p.gy;

			float px = nodeX(p) + NODE/2, cx = nodeX(n) + NODE/2;
			float top = nodeY(p) + NODE + (p.kind == Node.Kind.ROOT ? 0 : PIPS);
			float run = nodeY(p) + RUN;
			float bottom = nodeY(n);

			ArrayList<float[]> segs = new ArrayList<>();
			if ((int)px == (int)cx){
				segs.add( new float[]{ px, top, px, bottom } );
			} else {
				segs.add( new float[]{ px, top, px, run } );
				segs.add( new float[]{ px, run, cx, run } );
				segs.add( new float[]{ cx, run + 1, cx, bottom } );
			}
			//every outline under every core, so the joins stay clean
			for (float[] s : segs) add( block( s, 1, 0xFF0c0c10 ) );
			for (float[] s : segs){
				ColorBlock c = block( s, 0, 0xFFFFFFFF );
				cores.add( c );
				add( c );
			}
			sync();
		}

		/** a block along a horizontal or vertical segment, padded out by pad on every side */
		private ColorBlock block( float[] s, int pad, int color ){
			float x0 = Math.min( s[0], s[2] ), x1 = Math.max( s[0], s[2] );
			float y0 = Math.min( s[1], s[3] ), y1 = Math.max( s[1], s[3] );
			boolean horizontal = y0 == y1;
			float w = horizontal ? x1 - x0 + 1 : 1;
			float h = horizontal ? 1 : y1 - y0;
			ColorBlock b = new ColorBlock( w + 2*pad, h + 2*pad, color );
			b.x = x0 - pad;
			b.y = y0 - pad;
			return b;
		}

		private void sync(){
			int r = child.shownLevel > 0 ? 3 : child.shownSpend ? 2 : child.shownUnlocked ? 1 : 0;
			if (r != rank){
				int color = r == 3 ? COL_LIT : r == 2 ? COL_AVAIL : r == 1 ? COL_OPEN : COL_LOCKED;
				for (ColorBlock c : cores){
					c.hardlight( color );
					c.alpha( 1f );
				}
				//a wire that changed state is restacked against the ones it shares pixels with
				if (rank >= 0) edgesDirty = true;
				rank = r;
			}
			if (rank == 2){
				float a = 0.55f + 0.45f * (float)Math.sin( Game.timeTotal * 4 );
				for (ColorBlock c : cores) c.alpha( a );
			}
		}

		@Override
		public synchronized void update() {
			super.update();
			sync();
		}
	}

	// ================= node buttons =================

	/** a glow is added to what is under it, so it reads as light rather than paint */
	private static class Glow extends Image {

		Glow( int[] frame ){
			super( SkillTreeArt.get() );
			SkillTreeArt.frame( this, frame );
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	public class NodeButton extends Button {

		public Node node;
		private SkillTreePane pane;

		private Glow halo;      //bronze, silver or gold: the points already in the node
		private Glow ring;      //green, breathing: a point can go in now
		private Image socket;
		private Image icon;
		private Image pips;
		private Image plate;
		private BitmapText badge;

		int shownLevel = -1;
		boolean shownSpend = false;
		boolean shownActive = false;
		boolean shownUnlocked = true;
		private int shownPoints = -1;
		private float haloAlpha = 0;
		private boolean haloShimmer = false;

		public NodeButton( Node node, SkillTreePane pane ){
			super();
			//let presses fall through to the pane so the tree can be drag-scrolled
			hotArea.blockLevel = PointerArea.NEVER_BLOCK;
			this.node = node;
			this.pane = pane;
			icon = node.icon();
			add(icon);
			//the badge and its plate were created before the icon: they sit over it
			bringToFront(plate);
			bringToFront(badge);
			sync();
		}

		private boolean active(){
			return node.kind == Node.Kind.SKILL && node.skill.active;
		}

		private void sync(){
			shownLevel = node.level();
			shownSpend = node.canSpend();
			shownActive = active();
			shownUnlocked = node.unlocked();
			shownPoints = Skill.availableSkill;
			int max = node.maxLevel();

			int frame;
			if (!shownUnlocked){
				frame = SkillTreeArt.LOCKED;
			} else if (node.kind == Node.Kind.ROOT){
				frame = SkillTreeArt.ROOT;
			} else if (shownLevel <= 0){
				frame = shownSpend ? SkillTreeArt.AVAIL : SkillTreeArt.IDLE;
			} else {
				int metal = shownLevel >= max ? 2 : shownLevel == 1 ? 0 : 1;
				frame = (shownActive ? SkillTreeArt.L1A : SkillTreeArt.L1) + metal;
			}
			SkillTreeArt.frame( socket, SkillTreeArt.socket( frame ) );

			if (!shownUnlocked){
				icon.alpha( 0.25f );
			} else {
				icon.alpha( node.kind == Node.Kind.SKILL && shownLevel == 0 ? 0.55f : 1f );
			}
			//a skill you switch on and off keeps its colour only while it runs
			if (icon instanceof SkillSprite && node.kind == Node.Kind.SKILL && node.skill.toggleable()){
				((SkillSprite) icon).grey( !shownActive );
			}

			ring.visible = shownSpend;

			//the halo grows with the points in the node: a warm bronze at one, silver
			//at two, and a shimmering gold once it is full; an emblem with anything
			//below it wears a faint gold one
			int haloColor = 0;
			haloAlpha = 0;
			haloShimmer = false;
			if (shownUnlocked && shownLevel > 0){
				if (node.kind == Node.Kind.ROOT){
					haloColor = 0xffd040; haloAlpha = 0.22f;
				} else if (shownLevel >= max){
					haloColor = 0xffd040; haloAlpha = 0.75f; haloShimmer = true;
				} else if (shownLevel == 1){
					haloColor = 0xc07840; haloAlpha = 0.35f;
				} else {
					haloColor = 0xc8ccd8; haloAlpha = 0.5f;
				}
			}
			halo.visible = haloAlpha > 0;
			if (halo.visible){
				halo.hardlight( haloColor );
				halo.alpha( haloAlpha );
			}

			if (node.kind == Node.Kind.ROOT){
				pips.visible = false;
				//an emblem wears the points sunk into what hangs off it
				plate.visible = badge.visible = shownLevel > 0;
				badge.text( Integer.toString( shownLevel ) );
				badge.hardlight( 0xFFFFAA );
			} else {
				pips.visible = true;
				SkillTreeArt.frame( pips, SkillTreeArt.pips( max, shownLevel ) );
				//the price, on the nodes that cost more than one point
				int cost = node.cost();
				plate.visible = badge.visible = shownUnlocked && shownLevel < max && cost > 1;
				badge.text( Integer.toString( cost ) );
				badge.hardlight( shownPoints >= cost ? 0x8ce08c : 0xdd8877 );
			}
			badge.measure();
		}

		@Override
		protected void createChildren(){
			super.createChildren();

			halo = new Glow( SkillTreeArt.HALO );
			add( halo );

			ring = new Glow( SkillTreeArt.GLOW );
			ring.hardlight( 0x66ff66 );
			add( ring );

			socket = new Image( SkillTreeArt.get() );
			SkillTreeArt.frame( socket, SkillTreeArt.socket( SkillTreeArt.IDLE ) );
			add( socket );

			pips = new Image( SkillTreeArt.get() );
			SkillTreeArt.frame( pips, SkillTreeArt.pips( 3, 0 ) );
			add( pips );

			plate = new Image( SkillTreeArt.get() );
			SkillTreeArt.frame( plate, SkillTreeArt.PLATE );
			add( plate );

			badge = new BitmapText( PixelScene.pixelFont );
			add( badge );
		}

		@Override
		public synchronized void update() {
			super.update();
			int level = node.level();
			//an emblem's count rises with every point below it; only the node itself celebrates
			if (node.kind != Node.Kind.ROOT && shownLevel >= 0 && level > shownLevel){
				celebrate();
			}
			if (shownLevel != level || shownSpend != node.canSpend()
					|| shownUnlocked != node.unlocked()
					|| shownActive != active()
					|| shownPoints != Skill.availableSkill){
				sync();
				layout();
			}
			//available nodes breathe, and a full one shimmers
			if (ring.visible){
				ring.alpha( 0.5f + 0.5f * (float)Math.sin( Game.timeTotal * 4 ) );
			}
			if (haloShimmer){
				halo.alpha( haloAlpha + 0.1f * (float)Math.sin( Game.timeTotal * 1.5f ) );
			}
		}

		/** a point just went in here */
		private void celebrate(){
			Emitter emitter = (Emitter)recycle( Emitter.class );
			emitter.revive();
			emitter.pos( x + 3, y + 3, width - 6, height - 6 );
			emitter.burst( Speck.factory( Speck.STAR ), 8 );
			Sample.INSTANCE.play( Assets.Sounds.LEVELUP, 0.6f, 1.3f );
		}

		@Override
		protected void layout(){
			super.layout();

			socket.x = x;
			socket.y = y;
			ring.x = x - 3;
			ring.y = y - 3;
			halo.x = x - 6;
			halo.y = y - 6;

			if (icon != null){
				icon.x = x + (width - icon.width()) / 2;
				icon.y = y + (height - icon.height()) / 2;
				PixelScene.align( icon );
			}

			pips.x = x + (width - pips.width()) / 2;
			pips.y = y + height + 1;
			PixelScene.align( pips );

			if (node.kind == Node.Kind.ROOT){
				plate.x = x + width - plate.width();
				plate.y = y + height - plate.height();
			} else {
				plate.x = x - 1;
				plate.y = y - 1;
			}
			badge.x = plate.x + (plate.width() - badge.width()) / 2;
			badge.y = plate.y + 1;
			PixelScene.align( badge );
		}

		@Override
		protected void onPointerDown() {
			socket.brightness( 1.4f );
			icon.brightness( 1.4f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp() {
			socket.resetColor();
			icon.resetColor();
			sync();
		}

		@Override
		protected String hoverText() {
			if (node.kind == Node.Kind.ROOT) return node.name();
			return node.name() + " " + node.level() + "/" + node.maxLevel();
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
							return Messages.get( SkillTreePane.class, "upgrade_talent" );
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
