package jaccob.combatscript;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.GameObject.Type;
import org.powerbot.script.rt4.GeItem;
import org.powerbot.script.rt4.GroundItem;
import org.powerbot.script.rt4.Interactive;
import org.powerbot.script.rt4.Item;
import org.powerbot.script.rt4.ItemQuery;
import org.powerbot.script.rt4.Magic;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Locatable;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;
import org.powerbot.script.rt4.Actor;
import org.powerbot.script.rt4.BasicQuery;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Combat.Style;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Game;
import org.powerbot.script.rt4.Game.Tab;
import org.powerbot.script.rt4.Npc;
import org.powerbot.script.rt4.Path;
import org.powerbot.script.rt4.Path.TraversalOption;
import org.powerbot.script.rt4.Player;
import org.powerbot.script.rt4.PlayerQuery;
import org.powerbot.script.rt4.Skills;
import org.powerbot.script.rt6.LocalPath;
import org.powerbot.script.rt6.Menu;

@Script.Manifest(name = "NMZ", description = "Kills any pre-defined warrior and opens doors/gates", properties = "client=4; topic=0;")
public class NMZ extends PollingScript<ClientContext> implements PaintListener{
	private static final int PRIMARY = 000;
	private static final int SECONDARY = 000;
	
	private static final int ROCK_CAKE = 7510;
	private static final int[] OVERLOAD_IDS = new int[] {11733, 11732, 11731, 11730};
	private static final int[] _IDS = new int[] {11734, 11735, 11736, 11737};
	
	private static final int STATS_WIDGET_ID = 320;
	private static final int STATS_ATTACK_ID = 1;
	private static final int STATS_STRENGTH_ID = 2;
	private static final int STATS_DEFENCE_ID = 3;
	private static final int STATS_RANGED_ID = 4;
	private static final int STATS_MAGIC_ID = 6;
	private static final int STATS_HP_ID = 9;
	
	private static final int TRAINING_MODE = STATS_STRENGTH_ID;

	enum ScriptMode {
		IDLE, MOVING, TARGETTING, IN_COMBAT
	}
	
	ScriptMode mode = ScriptMode.IDLE;
	Npc targetted = null;
	Npc hovering = null;
	int points = getPoints();
	
	boolean isAfk = false;

	public NMZ() {

	}

	@Override
	public void start() {
		Condition.sleep(1000);
		ctx.input.speed(50);
		ctx.camera.pitch(true);
		
		switch (TRAINING_MODE) {
		case STATS_ATTACK_ID: ctx.combat.style(Style.ACCURATE); break;
		case STATS_STRENGTH_ID: ctx.combat.style(Style.AGGRESSIVE); break;
		case STATS_DEFENCE_ID: ctx.combat.style(Style.DEFENSIVE); break;
		}
		
		//walkToCombat();
		//left 3285, 3171
		//walkTo(new Tile(3290, 3164), doors, 6);
	}
	
	public int getCombatSkill() {
		switch (TRAINING_MODE) {
		case STATS_ATTACK_ID: return Constants.SKILLS_ATTACK;
		case STATS_STRENGTH_ID: return Constants.SKILLS_STRENGTH;
		case STATS_DEFENCE_ID: return Constants.SKILLS_DEFENSE;
		}
		
		return -1;
	}
	
	private void goAfk() {
		if (Math.random() > 0.2) {
			int x = Math.random() > 0.5 ? -1 : -2;
			int y = (int) (Math.random() * ctx.game.dimensions().height);
			
			System.out.println("Moving to " + x + ", " + y);
			ctx.input.move(new Point(x, y));
			
			isAfk = true;
		} else {
			isAfk = false;
		}
	}

	@Override
	public void poll() {
		antiban();
		ctx.game.tab(Tab.INVENTORY);
		int lvl = ctx.skills.level(getCombatSkill());
		int realLvl = ctx.skills.realLevel(getCombatSkill());
		
		boolean doneStuff = false;
		
		int hp = ctx.skills.level(Constants.SKILLS_HITPOINTS);
		
		if (lvl <= realLvl) {
			if (isAfk)
				Condition.sleep(3000 + randomNum(5000));
			
			isAfk = false;
			if (ctx.inventory.select().id(OVERLOAD_IDS).peek().interact("Drink")) {
				doneStuff = true;
			}
		} else if (hp > 1 && hp < 5) {
			if (isAfk)
				Condition.sleep(randomNum(5000));
			
			isAfk = false;
			if (ctx.inventory.select().id(ROCK_CAKE).peek().interact("Guzzle")) {
				Condition.sleep(2000 + randomNum(5000));
				doneStuff = true;
			}
		}
		
		if (getAbsorptionLevel() < minAbsorption) {
			for (int i = 0; i < (int)(Math.random() * 5); i++) {
				ctx.inventory.select().id(_IDS).peek().interact("Drink");
				Condition.sleep(500 + randomNum(400));
			}
			
			minAbsorption = 100 + randomNum(100);
			doneStuff = true;
			isAfk = false;
		}
		
		if (hp == 1 &&
			System.currentTimeMillis() >= randomTimeout) {
			
			randomTimeout = System.currentTimeMillis() + 17000 + (int)(Math.random()*20000);
			ctx.prayer.quickPrayer(true);
			Condition.sleep(300 + randomNum(300));
			ctx.prayer.quickPrayer(false);
			Condition.sleep(200 + randomNum(300));
			
			isAfk = false;
			doneStuff = true;
		}
		
		if (doneStuff) {
			System.out.println("Did stuff");
			goAfk();
		}
		
	}

	private int randomNum(int r) {
		return (int)(Math.random() * r);
	}
	
	int minAbsorption = 150;
	long randomTimeout = System.currentTimeMillis() + 45000 + (int)(Math.random()*10000);
	
	private int getPoints() {
		return Integer.parseInt(ctx.widgets.widget(202).component(1).component(3).text().replaceAll("Points:<br>", "").replaceAll(",", ""));
	}
	
	private int getAbsorptionLevel() {
		return Integer.parseInt(ctx.widgets.widget(202).component(1).component(9).text());
	}
	
	//320 = stats tab

	private void antiban() {
		if (!isAfk && Math.random() > 0.995) {
			ctx.camera.angle((int)(Math.random() * 360));
		} else if (Math.random() > 0.98) {
			ctx.game.tab(Tab.INVENTORY);
		}
	}
	
	private void hoverSkill(int id) {
		ctx.game.tab(Tab.STATS);
		ctx.widgets.widget(320).component(id).hover();
		Condition.sleep((int)(1500 + (Math.random() * 2000)));
	}

	/**
	 * Author - Enfilade Moves the mouse a random distance between minDistance
	 * and maxDistance from the current position of the mouse by generating
	 * random vector and then multiplying it by a random number between
	 * minDistance and maxDistance. The maximum distance is cut short if the
	 * mouse would go off screen in the direction it chose.
	 * 
	 * @param minDistance
	 *            The minimum distance the cursor will move
	 * @param maxDistance
	 *            The maximum distance the cursor will move (exclusive)
	 */
	public void moveRandomly(final int minDistance, final int maxDistance) {
		double xvec = Math.random();
		if (Random.nextInt(0, 2) == 1) {
			xvec = -xvec;
		}
		double yvec = Math.sqrt(1 - xvec * xvec);
		if (Random.nextInt(0, 2) == 1) {
			yvec = -yvec;
		}
		double distance = maxDistance;
		Point p = ctx.input.getLocation();
		int maxX = (int) Math.round(xvec * distance + p.x);
		distance -= Math.abs((maxX - Math.max(0, Math.min(ctx.game.dimensions().getWidth(), maxX))) / xvec);
		int maxY = (int) Math.round(yvec * distance + p.y);
		distance -= Math.abs((maxY - Math.max(0, Math.min(ctx.game.dimensions().getHeight(), maxY))) / yvec);
		if (distance < minDistance) {
			return;
		}
		distance = Random.nextInt(minDistance, (int) distance);
		ctx.input.move((int) (xvec * distance) + p.x, (int) (yvec * distance) + p.y);
	}
	
	private int lastXP = -1;
	private int startXP = -1;
	
	private int getXPFromCombatStyle() {
		switch (TRAINING_MODE) {
		case STATS_ATTACK_ID: return ctx.skills.experience(Constants.SKILLS_ATTACK);
		case STATS_STRENGTH_ID: return ctx.skills.experience(Constants.SKILLS_STRENGTH);
		case STATS_MAGIC_ID: return ctx.skills.experience(Constants.SKILLS_MAGIC);
		case STATS_RANGED_ID: return ctx.skills.experience(Constants.SKILLS_RANGE); 
		case STATS_DEFENCE_ID: return ctx.skills.experience(Constants.SKILLS_DEFENSE);
		}
		return 0;
	}
	
	private String getXPString() {
		if (startXP == -1) {
			startXP = getXPFromCombatStyle();
		}
		
		long runTime = getRuntime();
		int currentExp = getXPFromCombatStyle();
		int expGain = currentExp - startXP;
		int expPh = (int) (3600000d / (long) runTime * (double) (expGain));
		int xpNextLevel = ctx.skills.experienceAt(ctx.skills.realLevel(getCombatSkill()) + 1);
		
		if (expPh == 0)
			expPh = 1;
		
		int xpToLevel = xpNextLevel - currentExp;
		double ratio = xpToLevel / (double) expPh;
		int timeToLevel = (int)(ratio * 3600000);
				
		return (currentExp - startXP) + " (" + expPh + " / Hr, XTL: " + xpToLevel + ", TTL: " + formatInterval(timeToLevel, false) + ")";
	}
	
	private String getPointsString() {
		long runTime = getRuntime();
		int current = getPoints();
		int diff = current - points;
		int pointsPh = (int) (3600000d / (long) runTime * (double) (diff));
		
		if (pointsPh == 0)
			pointsPh = 1;

		return diff + " (" + pointsPh + " / Hr)";
	}
	
	private int getMoneyPerHour() {
		return 0;
	}
	
	public static String formatInterval(final long interval, boolean millisecs )
	{
	    final long hr = TimeUnit.MILLISECONDS.toHours(interval);
	    final long min = TimeUnit.MILLISECONDS.toMinutes(interval) %60;
	    final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) %60;
	    final long ms = TimeUnit.MILLISECONDS.toMillis(interval) %1000;
	    if( millisecs ) {
	        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
	    } else {
	        return String.format("%02d:%02d:%02d", hr, min, sec );
	    }
	}

	@Override
	public void repaint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.green);
		
		long runtime = getRuntime();
		
		g2.drawString("Time running: " + formatInterval(runtime, false), 10, 150);
		g2.drawString("Xp: " + getXPString(), 10, 170);
		g2.drawString("Points: " + getPointsString(), 10, 190);
		g2.drawString("Combat level: " + ctx.players.local().combatLevel(), 10, 210);
		g2.drawString("Skill level: " + ctx.skills.realLevel(getCombatSkill()), 10, 230);
		g2.drawString("Is AFK?: " + isAfk, 10, 250);
	}
}
