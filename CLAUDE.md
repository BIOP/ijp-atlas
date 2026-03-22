# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project (requires Java 8)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run a specific test class
mvn test -Dtest=TestSerialize

# Launch ImageJ for manual testing
mvn exec:java -Dexec.mainClass="SimpleIJLaunch"
```

## Architecture Overview

This is a Java/Maven library providing a standard API for brain atlases, used by the ABBA (Aligning Big Brains & Atlases) plugin for Fiji/ImageJ.

### Core Interfaces (ch.epfl.biop.atlas.struct)

The library is built on four key interfaces:

- **Atlas** - Top-level container combining a map and ontology
- **AtlasMap** - Manages imaging data (structural images, label/annotation image, derived images like borders and coordinates). All images are BigDataViewer `SourceAndConverter<?>` objects.
- **AtlasOntology** - Hierarchical tree of brain regions with fast id-to-node lookup
- **AtlasNode** - Tree node containing region properties (id, name, acronym, color, children)

### Package Structure

```
ch.epfl.biop.atlas
├── struct/              # Core API interfaces and helpers
├── scijava/             # SciJava integration (commands, pre/post processors)
├── custom/              # Factory for creating custom atlases from ImagePlus
├── mouse/allen/         # Allen Brain Mouse atlas implementations (CCF v3, v3.1)
├── rat/waxholm/         # Waxholm Rat atlas implementations (v4, v4.2)
├── brainglobe/          # BrainGlobe integration via Appose
│   └── command/         # SciJava commands for BrainGlobe atlases
```

### SciJava Plugin Integration

- **AtlasChooserCommand** - Dynamic command presenting available atlases, extensible via `registerAtlas()`
- **AtlasPreprocessor** - Auto-fills missing Atlas inputs in commands
- **AtlasPostProcessor** - Registers Atlas outputs in ObjectService and SourceAndConverterService

Each concrete atlas (Allen, Waxholm) has a Command class that extends its atlas implementation and implements `Command`, annotated with `@Plugin`. This pattern enables auto-discovery.

### Data Loading

- Atlas maps load from BigDataViewer XML/HDF5 format (SpimData)
- Ontologies load from JSON files with hierarchical region definitions
- Both support remote URLs with local caching

### BrainGlobe Integration (via Appose)

All BrainGlobe atlases (https://brainglobe.info/) are available through an Appose-based bridge to the Python `brainglobe-atlasapi`. This replaces a previous pyimagej/JPype approach (see `migration/pyimagej_wrapper/` for the old code).

Key classes in `ch.epfl.biop.atlas.brainglobe`:

- **BrainGlobeAppose** - Manages the Pixi/Python environment and runs scripts to list atlases or fetch atlas data. Provides session-level caching of the atlas list with try-once-then-give-up semantics if the environment build fails.
- **BrainGlobeAtlas** - Implements `Atlas`. Orchestrates fetching data via `BrainGlobeAppose`, building the ontology via `BrainGlobeHelper`, and constructing the `BrainGlobeAtlasMap`.
- **BrainGlobeAtlasMap** - Implements `AtlasMap`. Converts Appose `NDArray`s (shared memory) into BDV `SourceAndConverter<?>` objects via `ShmImg`.
- **BrainGlobeHelper** - Parses BrainGlobe `structures.json` into an `AtlasNode` tree (pure Java, no Python needed).
- **BrainGlobeAtlasCommand** - SciJava Command (`Plugins > BIOP > Atlas > Open BrainGlobe Atlas`)
- **BrainGlobeListAtlasesCommand** - SciJava Command (`Plugins > BIOP > Atlas > List BrainGlobe Atlases`)

Data flow: Java → Appose (Pixi env with `brainglobe-atlasapi`) → Python fetches atlas → image arrays returned as shared memory `NDArray`s → wrapped as `ShmImg` (ImgLib2) → `SourceAndConverter` (BDV). Ontology structures JSON is returned as a string and parsed in Java by `BrainGlobeHelper`.

BrainGlobe atlases are automatically registered in `AtlasChooserCommand` on first use. If the Python environment fails to build (no Pixi, no network), the built-in Allen/Waxholm atlases remain available.

**Open question**: the lifecycle of shared memory NDArrays after closing the Appose `Service` is not fully verified. Currently, the Service is closed after `fetchAtlas()` returns and `ShmImg` references keep the data accessible, but this relies on shared memory segments surviving Python process exit.

### Creating Custom Atlases

Use `AtlasFromSourcesHelper` for programmatic atlas creation:
- `fromImagePlus(name, image, labelImage, precision)` - From ImagePlus
- `fromSources(sources, labelSource, pixelSizeMm)` - From SourceAndConverter array
- `dummyOntology()` or `ontologyFromLabelImage()` - Generate ontology automatically
