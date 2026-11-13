### Changed

- Chain meshes are now baked once and reused across frames instead of being rebuilt per frame.
- Chain quads are single sided and no longer rely on duplicated back faces.
- Render types and biome tints are cached instead of being resolved every frame.

### Added

- Walking along a chain or rope with chain collisions enabled now sways the camera, like Supplementaries ropes.
