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
			Shape npcShape = null;
			switch (config.hightlightType())
			{
				default:
				case TILE:
				case TRUE_TILE:
					LocalPoint lp = config.hightlightType() == HighlightType.TILE ? npc.getLocalLocation() : LocalPoint.fromWorld(client, npc.getWorldLocation());
					if (lp != null)
					{
						if (config.hightlightType() == HighlightType.TRUE_TILE)
						{
							lp = new LocalPoint(lp.getX() + size * 128 / 2 - 64, lp.getY() + size * 128 / 2 - 64);
						}
						npcShape = Perspective.getCanvasTileAreaPoly(client, lp, size);
					}
					break;
				case HULL:
					npcShape = npc.getConvexHull();
					break;
			}
			if (npcShape != null)
			{
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics2D.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), lineAlpha));
				graphics2D.setStroke(new BasicStroke(2f));
				graphics2D.draw(npcShape);
				graphics2D.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fillAlpha));
				graphics2D.fill(npcShape);
			}
		}
		return null;
	}
}
