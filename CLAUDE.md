# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RuneLite XR is an extension of the open-source RuneLite OSRS client that adds a hybrid "Tabletop VR" experience. The project renders the standard 2D client alongside a 3D mixed-reality representation of the game world using OpenXR.

**Tech Stack:** Java 11, Maven, LWJGL 3.3.2, OpenXR, Google Guice, Swing/AWT

## Build and Development Commands

### Building the Project
```bash
# Clean build all modules
mvn clean install

# Build with CI script (includes shader compiler setup)
./ci/build.sh

# Build without tests
mvn clean install -DskipTests

# Run tests only
mvn test

# Run specific test
mvn test -Dtest=GpuPluginTest
```

### Running the Client
```bash
# Run from IDE: Execute the RuneLite class in runelite-client
# Main class: net.runelite.client.RuneLite

# Run from command line with shaded JAR
java -jar runelite-client/target/client-*-shaded.jar

# Development mode flags
--developer-mode  # Enable developer features
--debug           # Verbose logging
--safe-mode       # Disable GPU and external plugins
```

### Code Quality
```bash
# Run checkstyle (configured in checkstyle.xml)
mvn checkstyle:check

# Run PMD analysis
mvn pmd:check
```

## Project Structure

This is a multi-module Maven project:

- **cache/** - Libraries for reading/writing OSRS cache files
- **runelite-api/** - Pure interface definitions for the game client and objects
- **runelite-client/** - Main client implementation, plugin system, and XR rendering logic
- **runelite-jshell/** - REPL for development/debugging
- **runelite-maven-plugin/** - Custom Maven tooling

### Key Client Packages

- `net.runelite.client` - Application entry point and core infrastructure
- `net.runelite.client.plugins` - Plugin system and all plugins (including GPU and XR)
- `net.runelite.client.plugins.gpu` - GPU rendering plugin (2100+ lines, critical for XR)
- `net.runelite.client.xr` - XR/VR implementation (planned location)
- `net.runelite.client.eventbus` - Custom event bus for inter-component communication
- `net.runelite.client.callback` - Rendering callback hooks
- `net.runelite.client.ui` - Swing-based UI components

## Architecture

### Dependency Injection (Google Guice)
- All major components use `@Inject` constructor injection
- Use `@Singleton` for single-instance services
- Use `@Named` for string-based qualifiers
- Each plugin implements `Module` interface and overrides `configure(Binder binder)`
- Providers used for lazy initialization: `Provider<T>`

### Plugin System
All plugins extend the `Plugin` base class:
```java
public abstract class Plugin implements Module {
    protected void startUp() throws Exception { }  // Called when plugin enabled
    protected void shutDown() throws Exception { }  // Called when plugin disabled
    protected void configure(Binder binder) { }     // Guice configuration
}
```

Plugins are managed by `PluginManager` and follow a strict lifecycle. When creating a new plugin, ensure proper cleanup in `shutDown()` to prevent resource leaks.

### Event-Driven Architecture
- Custom EventBus (not Guava) - see `net.runelite.client.eventbus.EventBus`
- Subscribe to events using `@Subscribe` annotation
- Method naming convention: `onEventName(EventType event)`
- Events support priority-based ordering
- All game state changes trigger events (e.g., `GameStateChanged`, `GameTick`)

Example:
```java
@Subscribe
public void onGameTick(GameTick event) {
    // Handle game tick
}
```

### GPU Rendering System
The existing GPU plugin (`GpuPlugin.java`) is critical to understand for XR work:

- Implements `DrawCallbacks` interface to override client's default renderer
- Uses LWJGL for OpenGL bindings
- Scene data uploaded to GPU via `SceneUploader`
- Spatial partitioning using 8x8 tile `Zone` system
- Shaders in `runelite-client/src/main/resources/net/runelite/client/plugins/gpu/*.glsl`
- Rendering pipeline: AWTContext → Shader Compilation → VAO/VBO Upload → FBO Rendering

Key classes:
- `GpuPlugin` - Main OpenGL coordinator
- `SceneUploader` - Uploads scene geometry to GPU buffers
- `Zone` - Manages spatial partitioning
- `TextureManager` - Texture loading/management
- `Shader` - GLSL shader compilation/linking

### Scene API
The game world is represented as a grid of tiles:
- `Scene` - 4×104×104 array [plane][x][y] of tiles
- `Tile` - Individual tile containing game objects, ground objects, decorations
- Coordinate systems: `LocalPoint`, `WorldPoint`, `SceneLocation`
- `Perspective` class provides camera/projection utilities

Objects on tiles:
- `GameObject` - Interactive objects (doors, trees, etc.)
- `GroundObject` - Floor decorations
- `WallObject` - Walls and boundaries
- `DecorativeObject` - Non-interactive decorations
- `Model` - 3D mesh data (vertices, faces, textures)

### UI Architecture
- Swing/AWT based with FlatLAF theme
- `ClientUI` - Main window
- `ClientToolbar` - Sidebar navigation
- `PluginPanel` - Base class for plugin sidebar panels
- `NavigationButton` - Toolbar icons

## XR Implementation Plan

The XR system is being implemented in phases (see `documents/milestones/`):

### Phase 1: Foundation
- Add LWJGL OpenXR dependencies to `runelite-client/pom.xml`
- Create `net.runelite.client.xr` package structure
- Implement `OpenXRManager` singleton for OpenXR runtime lifecycle
- Create `XRPlugin` extending `Plugin`
- Add `XRPanel` extending `PluginPanel` for VR controls

### Phase 2: Rendering
- Create dedicated `VRRenderThread` running at 90Hz+ (decoupled from client thread)
- Separate OpenGL context (shared with main client GL context)
- Desktop mirror window for debugging/recording
- OpenXR session + swapchain management
- Basic skybox rendering

### Phase 3: Scene Integration
- Extract scene data via existing `Client` and `Scene` API
- Convert game coordinates to VR space
- Render 2D client to texture quad in VR
- 3D "tabletop" representation of game world

### Critical Integration Points
- Hook into existing GPU rendering pipeline
- Use `DrawCallbacks` for render interception
- Share OpenGL context between client and XR threads
- Subscribe to `GameTick` and scene update events

## Development Conventions

### Code Style
- Lombok annotations for boilerplate reduction (`@Getter`, `@Setter`, `@Slf4j`, etc.)
- Use `@Slf4j` for logging, not `System.out.println()`
- Checkstyle enforces style rules (see `checkstyle.xml`)
- Keep lines under 120 characters

### Thread Safety
- Main game logic runs on client thread
- Event bus handles cross-thread communication
- GPU rendering on separate thread via callbacks
- XR rendering will run on dedicated `VRRenderThread`
- Use proper synchronization when accessing shared state

### Resource Management
- Always clean up resources in `shutDown()` method
- Dispose of native resources (OpenGL objects, OpenXR handles)
- Unsubscribe from events when plugin disabled
- LWJGL MemoryStack for temporary native allocations

### Testing
- JUnit 4 framework
- Mockito for mocking
- Test structure mirrors source: `src/test/java/net/runelite/client/...`
- Mock `Client` interface for unit tests
- GPU tests require real OpenGL context (often skipped in CI)

## Common Patterns

### Getting Client State
```java
@Inject
private Client client;

public void example() {
    Scene scene = client.getScene();
    Tile[][][] tiles = scene.getTiles();
    Player localPlayer = client.getLocalPlayer();
}
```

### Creating UI Panel
```java
public class MyPanel extends PluginPanel {
    @Inject
    public MyPanel() {
        // Add Swing components
    }
}
```

### Adding Navigation Button
```java
@Inject
private ClientToolbar clientToolbar;

@Override
protected void startUp() {
    NavigationButton button = NavigationButton.builder()
        .tooltip("My Plugin")
        .icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
        .panel(myPanel)
        .build();
    clientToolbar.addNavigation(button);
}
```

## Prerequisites

- Java 11 JDK
- Maven 3.6+
- SteamVR or compatible OpenXR runtime (for XR features)
- OpenGL 4.3+ capable GPU
- For VR: Compatible VR headset

## Resources

- Project documentation: `documents/`
- Milestone tracking: `documents/milestones/`
- Shader sources: `runelite-client/src/main/resources/net/runelite/client/plugins/gpu/`
- API interfaces: `runelite-api/src/main/java/net/runelite/api/`
