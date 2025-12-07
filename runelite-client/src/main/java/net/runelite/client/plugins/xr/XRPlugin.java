/*
 * Copyright (c) 2024, Nicholas Davis <https://github.com/NicholasDavis5>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.xr;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Plugin providing virtual reality support for RuneLite via OpenXR.
 */
@PluginDescriptor(
	name = "RuneLite XR",
	description = "Virtual reality support for RuneLite",
	tags = {"vr", "xr", "openxr", "virtual reality"}
)
@Slf4j
public class XRPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private XRConfig config;

	@Inject
	private OpenXRManager openXRManager;

	private XRPanel panel;
	private NavigationButton navButton;

	@Provides
	XRConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(XRConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.debug("XRPlugin starting up");

		// Create the panel
		panel = injector.getInstance(XRPanel.class);

		// Load icon (will use placeholder if not found)
		final BufferedImage icon = loadIcon();

		// Create navigation button
		navButton = NavigationButton.builder()
			.tooltip("RuneLite XR")
			.icon(icon)
			.priority(100)
			.panel(panel)
			.build();

		// Add to toolbar
		clientToolbar.addNavigation(navButton);

		// Check if auto-start is enabled
		if (config.startOnLaunch())
		{
			log.info("Auto-start enabled, initializing VR runtime");
			boolean success = openXRManager.initialize();
			if (success)
			{
				panel.updateStatus(true);
			}
			else
			{
				log.warn("Auto-start failed to initialize VR runtime");
			}
		}

		log.debug("XRPlugin started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("XRPlugin shutting down");

		// Remove from toolbar
		clientToolbar.removeNavigation(navButton);

		// Shutdown OpenXR if initialized
		if (openXRManager.isInitialized())
		{
			log.info("Shutting down VR runtime on plugin shutdown");
			openXRManager.shutdown();
		}

		log.debug("XRPlugin shutdown complete");
	}

	/**
	 * Load the XR icon, falling back to a placeholder if not found.
	 */
	private BufferedImage loadIcon()
	{
		try
		{
			BufferedImage icon = ImageUtil.loadImageResource(getClass(), "xr_icon.png");
			if (icon != null)
			{
				return icon;
			}
		}
		catch (Exception e)
		{
			log.debug("XR icon not found, using placeholder", e);
		}

		// Create simple placeholder icon (16x16 light blue square)
		BufferedImage placeholder = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = placeholder.createGraphics();
		g.setColor(new Color(100, 100, 200));
		g.fillRect(0, 0, 16, 16);
		g.dispose();
		return placeholder;
	}
}
