# Maze generator
Advanced maze generator supporting many different maze types and generation algorithms, configurable in JSON. Mazes can be solved with the A* algorithm, braided, and exported to PNG, JPG, BMP, GIF or SVG.

### Download
Coming soon...

## Maze types
- **Orthogonal**: normal maze type, square cells.<br>
<img src="mazes\orthogonal_rb.png" alt="Orthogonal with Recursive Backtracker" height="200px"/> <img src="mazes\orthogonal_pr.png" alt="Orthogonal with Prim's" height="200px"/>
    - **Weave**: variation that allows passages to go over and under others.<br><img src="mazes\weaveOrthogonal_rb.png" alt="Weave orthogonal with Recursive Backtracker" height="200px"/>
    - **Unicursal**: variation with a single path spanning the entire maze, also called a labyrinth.<br><img src="mazes\unicursalOrthogonal_kr.png" alt="Unicursal orthogonal with Kruskal's" height="200px"/>
- **Delta**: triangle cells, the maze can be shaped like a rectangle, a triangle, a hexagon or a rhombus.<br><img src="mazes\delta_rectangle_rb.png" alt="Rectangle delta with Recursive Backtracker" height="120px"/> <img src="mazes\delta_triangle_rb.png" alt="Triangle delta with Recursive Backtracker" height="120px"/> <img src="mazes\delta_hexagon_rb.png" alt="Hexagon delta with Recursive Backtracker" height="120px"/> <img src="mazes\delta_rhombus_rb.png" alt="Rhombus delta with Recursive Backtracker" height="120px"/>
- **Sigma**: hexagon cells, the maze can be shaped like a rectangle, a triangle, a hexagon or a rhombus.<br><img src="mazes\sigma_rectangle_rb.png" alt="Rectangle sigma with Recursive Backtracker" height="140px"/> <img src="mazes\sigma_triangle_rb.png" alt="Triangle sigma with Recursive Backtracker" height="140px"/> <img src="mazes\sigma_hexagon_rb.png" alt="Hexagon sigma with Recursive Backtracker" height="140px"/> <img src="mazes\sigma_rhombus_rb.png" alt="Rhombus sigma with Recursive Backtracker" height="140px"/>
- **Theta**: circle maze with adjustable center radius and cell subdivision parameter.<br><img src="mazes\theta_rb.png" alt="Theta with Recursive Backtracker" height="200px"/> <img src="mazes\theta_ab.png" alt="Theta with Aldous-Broder's" height="200px"/>
- **Upsilon**: octogon and square cells.<br><img src="mazes\upsilon_rb.png" alt="Upsilon with Recursive Backtracker" height="200px"/> <img src="mazes\upsilon_wi.png" alt="Upsilon with Wilson's" height="200px"/>
- **Zeta**: like orthogonal but allows diagonal passages.<br><img src="mazes\zeta_rb.png" alt="Zeta with Recursive Backtracker" height="200px"/> <img src="mazes\zeta_pr.png" alt="Zeta with Prim's" height="200px"/>

## Generators
 Name | Supported maze types | Definition | Example
:---: | :------------------: | ---------  | -------
**Aldous-Broder's** | All | Performs a random walk, connecting the cells walked to if they were unvisited, until all cells are visited | <img src="mazes\orthogonal_ab.png" width="400px"/>
**Binary Tree** | Orthogonal | Repeatedly carve passage north and east for each cell. The side bias can be changed. | <img src="mazes\orthogonal_bt.png" width="400px"/>
**Eller's** | Orthogonal | Assign each cell of the first row to a different cell. For each row, randomly connect cells together if they are not in the same set, then carve at least one passage down for each set. | <img src="mazes\orthogonal_el.png" width="400px"/>
**Growing Tree** | All | Randomly walk around, connecting cells together, adding cells to a stack. When stuck, go back to a cell in the stack. The cell is chosen randomly between the newest, the oldest or a random cell. | <img src="mazes\orthogonal_gt.png" width="400px"/>
**Hunt-and-kill** | All | Randomly walk around, connecting cells together. When stuck, scan the maze for an unvisited cell next to a visited cell and start again from there. | <img src="mazes\orthogonal_hk.png" width="400px"/>
**Kruskal's** | All but zeta and weave orthogonal | Each cell start in a different set. Randomly remove walls between cells of different sets, merging their sets together. | <img src="mazes\orthogonal_kr.png" width="400px"/>
**Prim's** | All | Starting with a random cell, add all of its unvisited neighbors to a "frontier" set, and connect it with one of them. Repeat that with a cell from the set until the maze is complete | <img src="mazes\orthogonal_pr.png" width="400px"/>
**Recursive Backtracker** | All | Randomly walk around, connecting cells together, adding cells to a stack. When stuck, pop a cell from the stack and continue walking. | <img src="mazes\orthogonal_rb.png" width="400px"/>
**Recursive Division** | Orthogonal | Recursively divide the maze area in two, carving a passage in the wall made. | <img src="mazes\orthogonal_rd.png" width="400px"/>
**Sidewinder** | Orthogonal | For each cell in each row, randomly carve passage east or north | <img src="mazes\orthogonal_sw.png" width="400px"/>
**Wilson's** | All but zeta and weave orthogonal | Similar to Aldous-Broder's. Performs a random walk until a visited cell is found. Carve the path used to get there and mark the cells as visited. Start walking again from a random cell. | <img src="mazes\orthogonal_wi.png" width="400px"/>

## Configuration
The generator is configured with a JSON file. There are many attributes but most of them are optional. In fact, here's the minimal configuration file:
```json
{"mazes": [{"size": 10}]}
```
This will generate a single 10x10 orthogonal maze, export it to the current path with default styling settings.

Here's another more complete example:
```json
{
  "mazes": [
    {
      "name": "labyrinth",
      "count": 1,
      "type": "orthogonal",
      "size": 10,
      "algorithm": "aldous-broder",
      "braid": "50%",
      "openings": [["S", "S"], ["E", "E"]],
      "solve": true
    }
  ],
  "output": {
    "format": "svg",
    "path": "mazes/",
    "svgOptimize": true,
    "svgPrecision": 2
  },
  "style": {
    "cellSize": 30,
    "backgroundColor": "#00FFFFFF",
    "color": "#000000",
    "strokeWidth": 3,
    "solutionColor": "#0000FF",
    "solutionStrokeWidth": 3,
    "strokeCap": "round",
    "antialiasing": true
  }
}
```
This will generate a 10x10 orthogonal maze with Aldous-Broder's algorithm, export it to `mazes/labyrinth.svg` in optimized SVG format. Maze will be solved for the top-left to the bottom-right corners. Half deadends will be removed (braid). Custom styling settings are used, but in this case they all match default ones.

More examples of configuration file are available at [`/mazes/config/`](mazes/config).

#### Attributes
- **`maze`** (*required*): array of maze set objects. A maze set is a group of mazes with the same properties, each with the following attributes
    - `name`: name of the set and of the file to be exported. Default is `maze`.
    - `count`: number of mazes to generate for this set. Default is `1`.
    - `type`: maze type, one of `orthogonal`, `weaveOrthogonal`, `unicursalOrthogonal`, `delta`, `sigma`, `theta`, `upsilon`, `zeta`. Default is `orthogonal`.
    - `size` (*required*): maze size, either an integer or an object. The object can have the following attributes:
        - `size`: maze size. If maze takes 2 dimensions, both will be the same. For theta mazes, use `radius` instead.
        - `width`: maze width.
        - `width`: maze height.
        - `radius`: maze radius for theta maze.
        - `centerRadius`: center radius for theta maze. The number is the ratio between the center radius and the size of a cell. Default is `1`.
        - `subdivision`: subdivision setting for theta maze. For example, if value is `1.5`, a cell will be split in two when its size is 1.5 times the base size. Default is `1.5`
        - `maxWeave`: maximum weave setting for weave orthogonal maze. The number is the maximum number of cells a passage can go over or under. If value is `0`, the maze won't weave. Default is `1`.
    - `shape`: maze shape for delta and sigma mazes. One of `rectangle`, `triangle`, `hexagon`, `rhombus` (parallelogram). Default is `rectangle`.
    - `algorithm`: algorithm to use for maze generation, either a string or an object. If a string, any value from `ab`, `bt`, `el`, `gt`, `hk`, `kr`, `pr`, `rb`, `rd`, `sw`, `wi`,`aldous-broder`, `binary-tree`, `eller`, `growing-tree`, `hunt-kill`, `kruskal`, `prim`, `recursive-backtracker`, `recursive-division`, `sidewinder`, `wilson`. Default is `rb`. If an object, it can take the following attributes:
        - `name` (*required*): algorithm name, see the values above.
        - `weights`: weights for the Growing Tree algorithm, an array of 3 integers. Values of the array are weights for choosing the random, the newest or the oldest in order. Default is `[1, 1, 0]`
        - `bias`: 
            - Side bias for the Binary Tree algorithm, one of `ne`, `nw`, `se`, `sw` (*n* being north, *e* is east, etc). Default is `ne`. 
            - Direction bias for Eller's algorithm, array of two percent values from `0%` (exclusive) to `100%`. First value in the percentage of the time a wall is carved east, second value is the percentage of the time a wall is carved south. Default is `["50%", "50%"]`.
    - `braid`: braiding setting, either an integer or a percent string, for example `50%`. Braiding removes a certain number of deadends from the maze by connecting them with neighbouring cells. Default is `0`.
    - `openings`: array of openings to carve in the maze. An opening is described by a position, which is another array. For example `["S", "S"]` describes a position at the horizontal and vertical start of the maze. Valid letters are `S` for start, `C` for center and `E` for end. Absolute position can also be described with coordinates, for example `[0, 0]`, which is the top-left corner in an orthogonal maze but also is the center cell in a theta maze. Openings position must be unique and valid.
    - `solve`: whether to solve the maze or not, a boolean. Default is `false`. Solving requires at least two openings, and if more are present, only the solution for the two first openings is displayed.
<br><br>
- **`output`**: Output settings, with the following attributes:
    - `format`: output format, one of `png`, `jpg`, `bmp`, `gif`, `svg`. Default is `png`.
    - `path`: output destination path. Default is the current folder.
    - `svgOptimize`: if format is SVG, whether to optimize the path data or not. Default is `false`.
    - `svgPrecision`: if format is SVG, the precision used for all numbers. Default is `2`.
<br><br>
- **`style`**: Styling settings, with the following attributes:
    - `cellSize`: the size of a cell, in pixels. Default is `30`.
    - `backgroundColor`: background color, a hex color string. Default is `#00FFFFFF` so that for PNG and SVG formats, it's transparent, but for other formats where the alpha channel is ignored, it's white.
    - `color`: the color to draw the maze with, a hex color string. Default is black, `#000000`.
    - `strokeWidth`: the stroke width to draw the maze with. Default is `3`.
    - `solutionColor`: the color to draw the solution with, a hex color string. Default is blue, `#0000FF`.
    - `solutionSrokeWidth`: the stroke width to draw the solution with. Default is `3`.
    - `strokeCap`: the stroke cap, one of `butt`, `round` or `square`. Default is `round`.
    - `antialiasing`: whether to use antialiasing or not. Default is `true`.

#### JSON schema and validation
A JSON schema file is available from [`config-schema.json`](config-schema.json) for autocomplete and basic validation. The maze generator itself validates that parameters have a valid value by printing `ERROR: ...`. However, JSON exceptions for wrong types and missing attributes are not caught.

## Changelog
View changelog at [`CHANGELOG.md`](CHANGELOG.md).

## Credits
- Jamis Buck for the explanation of [all maze generation algorithms](http://weblog.jamisbuck.org/2011/2/7/maze-generation-algorithm-recap) and for his book.
- Walter D. Pullen for [information on maze types](http://www.astrolog.org/labyrnth/algrithm.htm).

## License
Everything is licensed under the [MIT License](LICENSE.md).