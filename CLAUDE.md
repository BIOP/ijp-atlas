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
├── mouse/allen/         # Allen Brain Mouse atlas implementations (CCF v3, v3.1, v3.1-ASR)
├── rat/waxholm/         # Waxholm Rat atlas implementations (v4, v4.2, v4.2-ASR)
└── brainglobe/          # BrainGlobe integration via Appose
```

Each built-in atlas version has its own package with a `command/` subpackage holding its SciJava
`Command`. `brainglobe/` has no `command/` subpackage: BrainGlobe atlases are surfaced through
`AtlasChooserCommand`.

### SciJava Plugin Integration

- **AtlasChooserCommand** - Dynamic command presenting available atlases, extensible via `registerAtlas()`
- **AtlasPreprocessor** - Auto-fills missing Atlas inputs in commands
- **AtlasPostProcessor** - Registers Atlas outputs in ObjectService and SourceAndConverterService

Each concrete atlas (Allen, Waxholm) has a Command class that extends its atlas implementation and implements `Command`, annotated with `@Plugin`. This pattern enables auto-discovery.

`AtlasChooserCommand` also takes a comma-separated list of *additional* atlas names; when it is
non-empty the resolved atlases are merged into a `CompositeAtlas` (`struct/CompositeAtlas`,
`struct/CompositeAtlasMap`), which exposes the structural channels of every member while the
ontology and label image come from the principal atlas.

### Data Loading

- Atlas maps load from BigDataViewer XML/HDF5 format (SpimData)
- Ontologies load from JSON files with hierarchical region definitions
- Both support remote URLs with local caching

### BrainGlobe Integration (via Appose)

All BrainGlobe atlases (https://brainglobe.info/) are available through an Appose-based bridge to the Python `brainglobe-atlasapi`. This replaced an earlier pyimagej/JPype approach, which has since been removed from the repository.

Key classes in `ch.epfl.biop.atlas.brainglobe`:

- **BrainGlobeAppose** - Manages the Pixi/Python environment and runs scripts to list atlases or fetch atlas data. Provides session-level caching of the atlas list with try-once-then-give-up semantics if the environment build fails.
- **BrainGlobeAtlas** - Implements `Atlas`. Orchestrates fetching data via `BrainGlobeAppose`, building the ontology via `BrainGlobeHelper`, and constructing the `BrainGlobeAtlasMap`.
- **BrainGlobeAtlasMap** - Implements `AtlasMap`. Opens the TIFF volumes with SCIFIO in `CELL` mode (lazy, cached) and wraps them as BDV `SourceAndConverter<?>` objects.
- **BrainGlobeHelper** - Parses a BrainGlobe `structures.json` payload into an `AtlasNode` tree (pure Java, no Python needed).
- **BrainGlobeStructures** - Gson data-binding classes for that `structures.json` payload.
- **BrainGlobeAtlasId** - The versioned identifier `name@version` (see below).
- **BrainGlobeLocalInventory** - Which atlases are materialized on disk. Pure Java: no Python, no network, never throws.

#### Atlas identity: `name@version`

A BrainGlobe atlas is always named `allen_mouse_50um@3.1`, in the chooser, in the
`additionalAtlases` list, in scripts, and from `Atlas.getName()`. **A bare name is a hard error** —
atlas versions renumber regions, so silently resolving to "latest" is how a dataset gets
reinterpreted against an ontology it was never aligned to. `@latest` is deliberately not accepted.

`@` is our convention, not BrainGlobe's — upstream keeps name and version strictly apart
(`AtlasName` is a literal type of bare names, `BrainGlobeAtlas(name, version=...)` takes two
arguments, on disk they are two path segments, `last_versions.conf` is an INI mapping). **Never pass
a combined string into Python**: split it first. `@` was picked because no published atlas name
contains one, whereas 14 contain dots (`kocher_bumblebee_2.542um`) and many contain hyphens — so
parsing splits on `@` and never on a dot. `registerAtlas` rejects `@` to keep it meaning one thing.

`fetchAtlasScript` passes `version=` and `check_latest=False`, so a pinned, already-materialized
atlas opens with no network access whatsoever.

There is no BrainGlobe-specific SciJava `Command`. BrainGlobe is reached from `AtlasChooserCommand`
(`Plugins > BIOP > Atlas > Open Atlas`) through its `Get BrainGlobe Atlases...` dropdown entry: the
`checkBrainGlobe` callback builds the Python environment behind a modal dialog, registers every
atlas name via `AtlasChooserCommand.registerAtlas`, then repopulates the dropdown.

Data flow: Java → Appose (Pixi env with `brainglobe-atlasapi`) → Python downloads the atlas and writes each volume as a plain TIFF → the file paths and a metadata/ontology JSON come back as task outputs → Java opens the TIFFs lazily as `SourceAndConverter` (BDV).

BrainGlobe atlases are registered in `AtlasChooserCommand` on first use. If the Python environment fails to build (no Pixi, no network), the built-in Allen/Waxholm atlases remain available.

#### brainglobe-atlasapi 3.x layout

`BrainGlobeAppose.BG_VERSION` pins the API version, and 3.x changed the on-disk model completely:

- An atlas is a `manifest.json` under `~/.brainglobe/brainglobe-atlasapi/atlases/<name>/<version>/` that references separately versioned components (template, annotation set, terminology, coordinate space) living in sibling directories. `atlas.root_dir` is the shared store, **not** the atlas folder.
- Images are remote OME-Zarr pyramids fetched chunk by chunk from S3; there are no `reference.tiff`/`annotation.tiff`/`hemispheres.tiff` files any more, and `atlas.reference` is deprecated in favour of `atlas.template`.
- The ontology is a `terminology.csv`, not a `structures.json`.
- Every 3.x atlas is stored in `asr` orientation.

`fetchAtlasScript` bridges that gap: it materializes the full-resolution volumes once and caches them as plain TIFFs next to the manifest (inside the versioned folder, so the cache is per-version), and it rebuilds the `structures.json` payload from `atlas.structures_list`. Download progress is reported by swapping the hard-coded `fsspec` `TqdmCallback` in `brainglobe_atlasapi.core` and `.bg_atlas` for a callback that forwards to the Appose task.

**Design decision — always materialize, never stream.** An atlas is downloaded in full and written
to local TIFFs before it is used; the lazy remote OME-Zarr access offered by 3.x is deliberately not
used. Atlas sizes are very manageable, and streaming would make every downstream operation depend on
network availability and latency. Do not replace this with chunk-on-demand access. The consequence
is that a download is slow and worth announcing to the user up front, and that "downloaded" has
three distinct meanings on disk:

| State | On disk under `atlases/<name>/<version>/` | Cost to open |
|---|---|---|
| absent | nothing | full download |
| registered by `brainglobe-atlasapi` | `manifest.json` + metadata JSON/CSV only | still a full volume download |
| materialized for ABBA | `reference.tiff`, `annotation.tiff`, `hemispheres.tiff` | ~0, works offline |

`brainglobe_atlasapi.list_atlases.get_downloaded_atlases()` reports the second state as downloaded,
so it is *not* a valid check for "ready to use here". Only the presence of the TIFFs is.

### Creating Custom Atlases

Use `AtlasFromSourcesHelper` for programmatic atlas creation:
- `fromImagePlus(name, image, labelImage, precision)` - From ImagePlus
- `fromSources(sources, labelSource, pixelSizeMm)` - From SourceAndConverter array
- `dummyOntology()` or `ontologyFromLabelImage()` - Generate ontology automatically
