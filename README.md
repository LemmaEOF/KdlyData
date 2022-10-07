<img src="icon.png" align="right" width="180px"/>

# Kdly Data


[>> Downloads <<](https://github.com/LemmaEOF/KdlyData/releases)

*Snuggle up and read something!*

**This mod is open source and under a permissive license.** As such, it can be included in any modpack on any platform without prior permission. We appreciate hearing about people using our mods, but you do not need to ask to use them. See the [LICENSE file](LICENSE.md) for more details.

Kdly Data is a little library that allows all `.json` files in resource or data packs to be replaced with [.kdl](https://kdl.dev) files instead, following the specification for [JSON-in-KDL 3.0.1](https://github.com/kdl-org/kdl/blob/main/JSON-IN-KDL.md).

Kdly Data currently does **not** support using KDL for .mcmeta files - this will require some complex changes to the odd system used for loading metadata. I may look into it in the future.

`.kdl` files used in place of `.json` will respect data pack priority, and filtering out the `.json` version of a resource will filter out the respective `.kdl` version too.