package com.animationindicator;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.Map;

public class AnimationIndicatorOverlay extends Overlay
{
	private final Client client;
	
	private final AnimationIndicatorPlugin plugin;
	
	private final AnimationIndicatorConfig config;
	
	@Inject
	private AnimationIndicatorOverlay(Client client, AnimationIndicatorPlugin plugin, AnimationIndicatorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	
	@Override
	public Dimension render(Graphics2D graphics2D)
	{
		for (Map.Entry<Integer, NPC> npcStore : plugin.animStorage)
		{
			NPC npc = npcStore.getValue();
			NPCComposition npcComposition = npc.getTransformedComposition();
			
			if (npc.isDead() || npcComposition == null)
			{
				continue;
			}
			
			int size = npcComposition.getSize();
			Color line = config.outlineColour();
			Color fill = config.fillColour();
			int lineAlpha = config.outlineColour().getAlpha();
			int fillAlpha = config.fillColour().getAlpha();
			Polygon tilePoly;
			
			LocalPoint lp = npc.getLocalLocation();
			if (lp != null)
			{
				tilePoly = Perspective.getCanvasTileAreaPoly(client, lp, size);
				if (tilePoly != null)
				{
					graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics2D.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), lineAlpha));
					graphics2D.setStroke(new BasicStroke(2f));
					graphics2D.draw(tilePoly);
					graphics2D.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fillAlpha));
					graphics2D.fill(tilePoly);
				}
			}
		}
		return null;
	}
}
