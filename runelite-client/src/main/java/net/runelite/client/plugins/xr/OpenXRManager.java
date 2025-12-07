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
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrApplicationInfo;
import org.lwjgl.openxr.XrInstance;
import org.lwjgl.openxr.XrInstanceCreateInfo;
import org.lwjgl.system.MemoryStack;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.IntBuffer;

import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Manages the OpenXR runtime lifecycle for VR support.
 * This singleton class handles initialization, shutdown, and state management
 * of the OpenXR instance.
 */
@Singleton
@Slf4j
public class OpenXRManager
{
	private volatile boolean initialized = false;
	private XrInstance xrInstance;

	@Inject
	public OpenXRManager()
	{
		log.debug("OpenXRManager constructed");
	}

	/**
	 * Initialize the OpenXR runtime.
	 *
	 * @return true if initialization successful, false otherwise
	 */
	public synchronized boolean initialize()
	{
		if (initialized)
		{
			log.warn("OpenXR already initialized");
			return true;
		}

		try
		{
			log.info("Initializing OpenXR runtime...");

			// Create XrInstance using LWJGL OpenXR bindings
			try (MemoryStack stack = MemoryStack.stackPush())
			{
				// Check for OpenXR runtime availability
				if (!checkOpenXRSupport(stack))
				{
					log.error("OpenXR runtime not available on this system");
					return false;
				}

				// Create application info
				XrApplicationInfo appInfo = XrApplicationInfo.malloc(stack)
					.applicationName(stack.UTF8("RuneLite XR"))
					.applicationVersion((int) XR10.XR_MAKE_VERSION(1, 0, 0))
					.engineName(stack.UTF8("RuneLite"))
					.engineVersion((int) XR10.XR_MAKE_VERSION(1, 0, 0))
					.apiVersion(XR_CURRENT_API_VERSION);

				// Create instance create info
				XrInstanceCreateInfo createInfo = XrInstanceCreateInfo.malloc(stack)
					.type(XR_TYPE_INSTANCE_CREATE_INFO)
					.next(NULL)
					.createFlags(0)
					.applicationInfo(appInfo);

				// Create the instance
				PointerBuffer pp = stack.mallocPointer(1);
				int result = xrCreateInstance(createInfo, pp);

				if (result != XR_SUCCESS)
				{
					log.error("Failed to create OpenXR instance. Error code: {}", result);
					return false;
				}

				xrInstance = new XrInstance(pp.get(0), createInfo);
				log.info("OpenXR instance created successfully");
			}

			initialized = true;
			log.info("OpenXR runtime initialized successfully");
			return true;
		}
		catch (Exception e)
		{
			log.error("Failed to initialize OpenXR runtime", e);
			cleanup();
			return false;
		}
	}

	/**
	 * Check if OpenXR runtime is available on the system.
	 */
	private boolean checkOpenXRSupport(MemoryStack stack)
	{
		try
		{
			// Query available API layers to verify runtime presence
			IntBuffer layerCount = stack.mallocInt(1);
			int result = xrEnumerateApiLayerProperties(layerCount, null);

			if (result != XR_SUCCESS)
			{
				log.debug("OpenXR runtime not detected (error code: {})", result);
				return false;
			}

			log.debug("OpenXR runtime detected, {} API layers available", layerCount.get(0));
			return true;
		}
		catch (Exception e)
		{
			log.debug("OpenXR runtime check failed", e);
			return false;
		}
	}

	/**
	 * Shutdown the OpenXR runtime and release resources.
	 */
	public synchronized void shutdown()
	{
		if (!initialized)
		{
			log.debug("OpenXR not initialized, nothing to shutdown");
			return;
		}

		log.info("Shutting down OpenXR runtime...");
		cleanup();
		initialized = false;
		log.info("OpenXR runtime shutdown complete");
	}

	/**
	 * Internal cleanup method for releasing OpenXR resources.
	 */
	private void cleanup()
	{
		if (xrInstance != null)
		{
			try
			{
				xrDestroyInstance(xrInstance);
				log.debug("XrInstance destroyed");
			}
			catch (Exception e)
			{
				log.error("Error destroying XrInstance", e);
			}
			finally
			{
				xrInstance = null;
			}
		}
	}

	/**
	 * Check if OpenXR runtime is currently initialized.
	 *
	 * @return true if initialized, false otherwise
	 */
	public boolean isInitialized()
	{
		return initialized;
	}
}
