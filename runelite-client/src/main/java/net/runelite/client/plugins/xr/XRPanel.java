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

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

/**
 * UI panel for controlling VR initialization and status.
 */
@Slf4j
public class XRPanel extends PluginPanel
{
	private final OpenXRManager openXRManager;

	private JLabel statusLabel;
	private JButton toggleButton;

	@Inject
	public XRPanel(OpenXRManager openXRManager)
	{
		super();
		this.openXRManager = openXRManager;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		// Create main content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new GridLayout(0, 1, 0, 10));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Status label
		statusLabel = new JLabel("VR Status: Stopped");
		statusLabel.setForeground(Color.WHITE);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		contentPanel.add(statusLabel);

		// Toggle button
		toggleButton = new JButton("Initialize VR");
		toggleButton.setFocusable(false);
		toggleButton.addActionListener(e -> toggleVR());
		contentPanel.add(toggleButton);

		add(contentPanel, BorderLayout.NORTH);

		// Update UI to reflect current state
		updateStatus(openXRManager.isInitialized());
	}

	/**
	 * Toggle VR initialization state.
	 */
	private void toggleVR()
	{
		boolean currentState = openXRManager.isInitialized();

		if (currentState)
		{
			// Shutdown VR
			log.info("User requested VR shutdown");
			openXRManager.shutdown();
			updateStatus(false);
		}
		else
		{
			// Initialize VR
			log.info("User requested VR initialization");
			toggleButton.setEnabled(false);
			toggleButton.setText("Initializing...");

			// Run initialization on separate thread to avoid blocking UI
			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>()
			{
				@Override
				protected Boolean doInBackground()
				{
					return openXRManager.initialize();
				}

				@Override
				protected void done()
				{
					try
					{
						boolean success = get();
						updateStatus(success);

						if (!success)
						{
							JOptionPane.showMessageDialog(
								XRPanel.this,
								"Failed to initialize VR runtime.\n" +
								"Make sure SteamVR or another OpenXR runtime is installed.",
								"VR Initialization Failed",
								JOptionPane.ERROR_MESSAGE
							);
						}
					}
					catch (Exception e)
					{
						log.error("Error in VR initialization worker", e);
						updateStatus(false);
					}
				}
			};

			worker.execute();
		}
	}

	/**
	 * Update UI to reflect current VR initialization state.
	 * Can be called from plugin or internally.
	 *
	 * @param isRunning true if VR is running, false otherwise
	 */
	public void updateStatus(boolean isRunning)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (isRunning)
			{
				statusLabel.setText("VR Status: Running");
				statusLabel.setForeground(new Color(0, 200, 0)); // Green
				toggleButton.setText("Stop VR");
				toggleButton.setEnabled(true);
			}
			else
			{
				statusLabel.setText("VR Status: Stopped");
				statusLabel.setForeground(Color.WHITE);
				toggleButton.setText("Initialize VR");
				toggleButton.setEnabled(true);
			}
		});
	}
}
