package marvel.junitTests;

import org.junit.Test;
import marvel.*;
import graph.*;
import java.io.IOException;

/**
 * This class contains a set of test cases that can be used to test the implementation of the
 * MarvelPaths class.
 *
 * @Rule public Timeout globalTimeout = Timeout.seconds(30); // 30 seconds max per method tested
 */
public class MarvelPathsTest {

    @Test(expected=NullPointerException.class)
    public void testBuildNullGraph() throws IOException {
        MarvelPaths.buildGraph(null);
    }


    @Test(expected=NullPointerException.class)
    public void testFindPathWithNullGraph() {
        MarvelPaths.findPath(null, "", "");
    }

    @Test(expected=NullPointerException.class)
    public void testFindPathWithNullStart() {
        Graph<String,String> graph = new Graph<>();
        MarvelPaths.findPath(graph, null, "");
    }

    @Test(expected=NullPointerException.class)
    public void testFindPathWithNullDest() {
        Graph<String,String> graph = new Graph<>();
        MarvelPaths.findPath(graph, "", null);
    }


}
