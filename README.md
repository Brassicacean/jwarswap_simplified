## History:

[WaRSwap](http://genomebiology.com/2013/14/8/R85) is a tool developed by the Megraw Lab to enable fast random sampling of directed graphs with a specific degree sequence (configuration models). It also enumerated subgraphs in the input graph and the random output graphs to discover network motifs. WaRSwap was based on the work of Bayati, Kim, and Saberi, who published an algorithm for the generation of configuration models with near-uniform sampling when the input degree sequence is sufficiently uniform. A major difference is that the constant, 4, in the weight function is replaced with 6 to prevent negative sampling weights from being computed on some of the test input graphs.
This is a re-implementation of the [Java version of WaRSwap](https://github.com/ansariom/WaRSwapSoftApp), featuring faster run-time and a more generalized, polynomial weighting function. The purpose of this generalized function was to try to customize the weights so that a uniform sample could be generated for any degree sequence. In fact, this generalized weighting function can generate samples that are apparently more uniform than the asymptotic performance of edge-switching algorithms.

In later work, I show that while this generalized weighting function may be optimized to maximize some measure of sampling uniformity for a given degree sequence, the samples it produces are still biased.

## Usage:

I don't recommend using this program for serious research. Other algorithms such as Curveball or mfinder will almost always be better (FANMOD will also work, as long as you set the option for edge-switch retries to 0), and their biases are generally small and easier to notice.

To build from source, run `mvn install` from the main directory. To run the program from the command line:

`$ cd target`

`$ java -jar jwarswap-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

As of now, this program is only tested to run on Linux, but should probably work on Mac and Windows.
