package marvel.junitTests;

import org.junit.Test;
import marvel.*;
import java.io.IOException;

/**
 * This class contains a set of test cases that can be used to test the implementation of the
 * MarvelParser class.
 *
 * @Rule public Timeout globalTimeout = Timeout.seconds(30); // 30 seconds max per method tested
 */
public class MarvelParserTest {

    @Test(expected=NullPointerException.class)
    public void testParseNullFile() throws IOException {
        MarvelParser.parseData(null);
    }

}
