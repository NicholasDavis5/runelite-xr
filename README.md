Here is the updated `README.md` incorporating the **RuneLite XR** context while keeping it concise.

-----

# RuneLite XR [](https://github.com/runelite/runelite/actions?query=workflow%3ACI+branch%3Amaster) [](https://discord.gg/ArdAhnN)

RuneLite is a free, open source OldSchool RuneScape client. **RuneLite XR** extends this foundation to create a hybrid "Tabletop VR" experience, allowing users to play OSRS in a 3D mixed-reality environment.

## RuneLite XR Features

* **Hybrid Interface:** Renders the standard 2D client alongside a "table top" 3D  representation of the game world.
* **Desktop Mirror:** Includes a dedicated debug window to view/record the headset perspective.
* **Powered by:** Java, LWJGL, and OpenXR.

## Development Roadmap

| Phase                        | Milestone                 | Status  | Details                                                                     |
|------------------------------|---------------------------|---------|-----------------------------------------------------------------------------|
| **Phase 1: Foundation**      | Initialize OpenXR Runtime | DONE    | Launch the VR runtime directly from RuneLite startup using LWJGL            |
|                              | Basic Environment         | PLANNED | Render a skybox texture to the XR HMD to verify rendering pipeline          |
| **Phase 2: The Hybrid View** | 2D Window Injection       | PLANNED | Render the live RuneLite game window to a floating 3D plane in XR space     |
|                              | Scene Debugging           | PLANNED | Iterate through Scene data and render debug geometry at TileID coordinates  |
| **Phase 3: Interaction**     | Camera Controls           | PLANNED | Implement navigation (rotation/zoom) using 2D inputs translated to 3D space |

See [documents/project-overview.md](documents/project-overview.md) for detailed milestone documentation.

## Project Layout

- [cache](https://www.google.com/search?q=cache/src/main/java/net/runelite/cache) - Libraries for reading/writing cache files.
- [runelite-api](https://www.google.com/search?q=runelite-api/src/main/java/net/runelite/api) - RuneLite API interfaces.
- [runelite-client](https://www.google.com/search?q=runelite-client/src/main/java/net/runelite/client) - Game client, plugins, and **XR rendering logic** (`net.runelite.client.plugins.xr`).

## Usage

1.  **Prerequisites:** Ensure **SteamVR** (or a compatible OpenXR runtime) is installed and set as default.
2.  **Build & Run:** Open as a Maven project, build the root module, and run the `RuneLite` class in `runelite-client`.
3.  **Launch VR:** Once the client loads, open the **XR Panel** on the sidebar and click **Initialize VR**.

## Development Setup

### Prerequisites

- **Java 11 JDK** (OpenJDK or Oracle JDK)
- **Maven 3.6+** (or use IntelliJ's built-in Maven)
- **IntelliJ IDEA** (recommended) or another Java IDE
- **Git** for version control
- **OpenXR Runtime** (SteamVR, Oculus, or Windows Mixed Reality) - for VR testing only

### Clone and Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/NicholasDavis5/runelite-xr.git
   cd runelite-xr
   ```

2. **Open in IntelliJ IDEA:**
   - File → Open → Select the `runelite-xr` directory
   - IntelliJ will automatically detect the Maven project and import it

3. **Wait for Maven to download dependencies** (this may take a few minutes)

4. **Build the project:**
   - Open the Maven tool window (View → Tool Windows → Maven)
   - Expand `runelite-xr` → `Lifecycle`
   - Double-click `clean` then `install`
   - Or run from terminal: `mvn clean install -DskipTests`

### IntelliJ Debug Configuration

1. **Create a new Run Configuration:**
   - Run → Edit Configurations...
   - Click the `+` button → Application

2. **Configure the settings:**
   - **Name:** `RuneLite XR`
   - **Module:** `client` (or `runelite-client`)
   - **Main class:** `net.runelite.client.RuneLite`
   - **VM options:** `-ea` (enables assertions)
   - **Working directory:** `$MODULE_WORKING_DIR$` (auto-populated)
   - **Use classpath of module:** `client`

3. **Apply and Run:**
   - Click Apply → OK
   - Click the Debug button (🐛) or press Shift+F9

### Enabling the XR Plugin

After RuneLite launches:

1. **Open Plugin Configuration:**
   - Click the wrench/gear icon in RuneLite's sidebar

2. **Find and Enable XR:**
   - Search for "XR" or scroll to find "RuneLite XR"
   - Toggle the switch to **ON**
   - The plugin will initialize and log: `XRPlugin starting up`

3. **Access the XR Panel:**
   - A light blue square icon will appear in the left sidebar
   - Click it to open the XR control panel
   - You'll see "VR Status: Stopped" and an "Initialize VR" button

### Testing VR Initialization

- **On Windows/Linux with OpenXR runtime:**
  - Click "Initialize VR"
  - SteamVR (or your VR runtime) should launch
  - Status should change to "VR Status: Running" (green)

- **On macOS:**
  - VR initialization will fail (expected behavior)
  - macOS does not support OpenXR - Apple uses ARKit instead
  - The XR plugin code will compile and run, but VR features require Windows or Linux
