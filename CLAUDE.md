# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project (requires Java 21)
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
- **BrainGlobeAtlasMap** - Implements `AtlasMap`. Opens the TIFF volumes with SCIFIO in `CELL` mode (lazy, cached) and wraps them as BDV `SourceAndConverter<?>` objects.
- **BrainGlobeHelper** - Parses a BrainGlobe `structures.json` payload into an `AtlasNode` tree (pure Java, no Python needed).
- **BrainGlobeAtlasCommand** - SciJava Command (`Plugins > BIOP > Atlas > Open BrainGlobe Atlas`)
- **BrainGlobeListAtlasesCommand** - SciJava Command (`Plugins > BIOP > Atlas > List BrainGlobe Atlases`)

Data flow: Java → Appose (Pixi env with `brainglobe-atlasapi`) → Python downloads the atlas and writes each volume as a plain TIFF → the file paths and a metadata/ontology JSON come back as task outputs → Java opens the TIFFs lazily as `SourceAndConverter` (BDV).

BrainGlobe atlases are automatically registered in `AtlasChooserCommand` on first use. If the Python environment fails to build (no Pixi, no network), the built-in Allen/Waxholm atlases remain available.

#### brainglobe-atlasapi 3.x layout

`BrainGlobeAppose.BG_VERSION` pins the API version, and 3.x changed the on-disk model completely:

- An atlas is a `manifest.json` under `~/.brainglobe/brainglobe-atlasapi/atlases/<name>/<version>/` that references separately versioned components (template, annotation set, terminology, coordinate space) living in sibling directories. `atlas.root_dir` is the shared store, **not** the atlas folder.
- Images are remote OME-Zarr pyramids fetched chunk by chunk from S3; there are no `reference.tiff`/`annotation.tiff`/`hemispheres.tiff` files any more, and `atlas.reference` is deprecated in favour of `atlas.template`.
- The ontology is a `terminology.csv`, not a `structures.json`.
- Every 3.x atlas is stored in `asr` orientation.

`fetchAtlasScript` bridges that gap: it materializes the full-resolution volumes once (the "old fashioned" whole-brain download rather than lazy multiscale access) and caches them as plain TIFFs next to the manifest, and it rebuilds the `structures.json` payload from `atlas.structures_list`. Download progress is reported by swapping the hard-coded `fsspec` `TqdmCallback` in `brainglobe_atlasapi.core` and `.bg_atlas` for a callback that forwards to the Appose task.

### Creating Custom Atlases

Use `AtlasFromSourcesHelper` for programmatic atlas creation:
- `fromImagePlus(name, image, labelImage, precision)` - From ImagePlus
- `fromSources(sources, labelSource, pixelSizeMm)` - From SourceAndConverter array
- `dummyOntology()` or `ontologyFromLabelImage()` - Generate ontology automatically
