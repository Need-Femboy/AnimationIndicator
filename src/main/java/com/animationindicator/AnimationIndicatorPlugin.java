package com.animationindicator;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;

@Slf4j
@PluginDescriptor(
	name = "Animation Indicator",
	description = "Highlights the tiles of a monster when it changes animation",
	tags = {"combat"}
)
public class AnimationIndicatorPlugin extends Plugin
{
	@Inject
	private Client client;
	
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AnimationIndicatorConfig config;
	
	@Inject
	private AnimationIndicatorOverlay overlay;
	
	public ArrayList<String> npcNames = new ArrayList<>();
	
	public ArrayList<Map.Entry<Integer, NPC>> animStorage = new ArrayList<>();
	
	private Set<Integer> defensiveAnimation = ImmutableSet.of(AnimationID.IDLE);
	
	protected void startUp()
	{
		splitList(config.npcList(), npcNames);
		overlayManager.add(overlay);
	}
	
	protected void shutDown()
	{
		npcNames.clear();
		overlayManager.remove(overlay);
	}
	
	@Subscribe
	public void onGameTick(GameTick event)
	{
		animStorage.removeIf(x -> x.getKey() < client.getTickCount());
	}
	
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor().getAnimation() == AnimationID.IDLE || !(event.getActor() instanceof NPC))
		{
			return;
		}
		
		animStorage.add(new ImmutablePair<>(client.getTickCount(), ((NPC) event.getActor())));
	}
	
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int type = event.getType();
		if (type >= MENU_ACTION_DEPRIORITIZE_OFFSET)
		{
			type -= MENU_ACTION_DEPRIORITIZE_OFFSET;
		}
		
		final MenuAction menuAction = MenuAction.of(type);
		
		if (menuAction == MenuAction.EXAMINE_NPC)
		{
			final int id = event.getIdentifier();
			final NPC npc = client.getCachedNPCs()[id];
			
			if (npc != null)
			{
				if (npc.getName() != null)
				{
					String option = checkSpecificNameList(npc) ? "Ignore-Animation": "Track-Animation";
					
					if (!client.isKeyPressed(KeyCode.KC_SHIFT))
					{
						return;
					}
					
					String tagAllEntry = event.getTarget();
					
					int idx = -1;
					MenuEntry parent = client.createMenuEntry(idx)
							.setOption(option)
							.setTarget(tagAllEntry)
							.setIdentifier(event.getIdentifier())
							.setParam0(event.getActionParam0())
							.setParam1(event.getActionParam1())
							.setType(MenuAction.RUNELITE)
							.onClick(this::tagNPC);
				}
			}
		}
	}
	
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(AnimationIndicatorConfig.CONFIGNAME))
		{
			splitList(config.npcList(), npcNames);
		}
	}
	
	private void tagNPC(MenuEntry event)
	{
		if (event.getType() == MenuAction.RUNELITE)
		{
			if (event.getOption().contains("-Animation"))
			{
				final int id = event.getIdentifier();
				final NPC npc = client.getCachedNPCs()[id];
				boolean tag = event.getOption().contains("Track");
				if (npc.getName() != null)
				{
					config.setNpcList(configListToString(tag, npc.getName().toLowerCase(), npcNames));
				}
			}
		}
	}
	
	
	private void splitList(String configStr, ArrayList<String> strList)
	{
		strList.clear();
		if (!configStr.equals(""))
		{
			for (String str : configStr.split(","))
			{
				if (!str.trim().equals(""))
				{
					strList.add(str.trim().toLowerCase());
				}
			}
		}
	}
	
	private String configListToString(boolean tagOrHide, String name, ArrayList<String> strList)
	{
		if (tagOrHide)
		{
			boolean foundName = false;

			for (String str : strList)
			{
				if (str.startsWith(name + ":") || str.equalsIgnoreCase(name))
				{
					strList.set(strList.indexOf(str), name);
					foundName = true;
				}
			}
			
			if (!foundName)
			{
				strList.add(name);
			}
		}
		else
		{
			strList.removeIf(str -> str.toLowerCase().startsWith(name + ":") || str.equalsIgnoreCase(name));
		}
		return Text.toCSV(strList);
	}
	
	public boolean checkSpecificNameList(NPC npc)
	{
		if (npc.getName() != null)
		{
			String name = npc.getName().toLowerCase();
			for (String entry : npcNames)
			{
				String nameStr = entry;
				if (entry.contains(":"))
				{
					String[] strArr = entry.split(":");
					nameStr = strArr[0];
				}
				
				if (WildcardMatcher.matches(nameStr, name))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Provides
	AnimationIndicatorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AnimationIndicatorConfig.class);
	}
}
