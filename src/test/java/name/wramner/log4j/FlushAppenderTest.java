package name.wramner.log4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link FlushAppender}.
 * <p>
 * Note that the test depends heavily on the log4j.xml configuration file!
 * 
 * @author erik.wramner
 */
public class FlushAppenderTest {

    /**
     * Remove all log files and configure Log4j.
     */
    @Before
    public void configureLoggingAndRemoveOldLogFiles() {
        final File[] files = new File(".").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".log");
            }
        });
        for (File f : files) {
            if (!f.delete()) {
                Assert.fail("Failed to delete " + f.getName() + " before test");
            }
        }
        DOMConfigurator.configure(getClass().getResource("/log4j.xml"));
    }

    /**
     * Test the flushing behavior.
     * <ul>
     * <li>Immediate files should get all log events immediately, or possibly with a short delay if using the async
     * appender.</li>
     * <li>Buffered files should not get events until they are flushed (or until a large number of events has been
     * logged).</li>
     * <li>A repeatable flush should flush all events, but then buffering should work again (events not logged at once).
     * </li>
     * <li>A one-shot flush should leave appenders unbuffered and all events should be logged immediately.</li>
     * <li>Directly attached file appenders and file appenders reached through other appenders (such as AsyncAppender)
     * should be flushed.</li>
     * <li>When the LogManager closes the appender all attached appenders should be flushed and closed as well.</li>
     * </ul>
     * 
     * @throws Exception on errors.
     */
    @Test
    public void testFlushing() throws Exception {
        Logger logger = Logger.getLogger(getClass());
        Logger flushOnceLogger = Logger.getLogger("name.wramner.log4j.FlushOneshot");
        Logger flushRepeatableLogger = Logger.getLogger("name.wramner.log4j.FlushRepeatable");

        logger.info("test1");
        Assert.assertEquals("test1", getLastLineFromFile("immediate-1.log"));
        Assert.assertNull(getLastLineFromFile("buffered-1.log"));
        Assert.assertNull(getLastLineFromFile("buffered-2.log"));
        Thread.sleep(10L);
        Assert.assertEquals("test1", getLastLineFromFile("immediate-async.log"));
        Assert.assertNull(getLastLineFromFile("buffered-async.log"));

        flushRepeatableLogger.info("This should not flush");
        Assert.assertEquals("test1", getLastLineFromFile("immediate-1.log"));
        Assert.assertNull(getLastLineFromFile("buffered-1.log"));
        Assert.assertNull(getLastLineFromFile("buffered-2.log"));
        Thread.sleep(10L);
        Assert.assertEquals("test1", getLastLineFromFile("immediate-async.log"));
        Assert.assertNull(getLastLineFromFile("buffered-async.log"));

        flushRepeatableLogger.error("This should flush");
        Assert.assertEquals("test1", getLastLineFromFile("immediate-1.log"));
        Assert.assertEquals("*** FLUSH ***", getLastLineFromFile("buffered-2.log"));
        Assert.assertNull(getLastLineFromFile("buffered-1.log"));
        Thread.sleep(10L);
        Assert.assertEquals("test1", getLastLineFromFile("immediate-async.log"));
        Assert.assertEquals("*** FLUSH ***", getLastLineFromFile("buffered-async.log"));

        logger.info("test2");
        Assert.assertEquals("test2", getLastLineFromFile("immediate-1.log"));
        Assert.assertNull(getLastLineFromFile("buffered-1.log"));
        Assert.assertEquals("*** FLUSH ***", getLastLineFromFile("buffered-2.log"));
        Thread.sleep(10L);
        Assert.assertEquals("test2", getLastLineFromFile("immediate-async.log"));
        Assert.assertEquals("*** FLUSH ***", getLastLineFromFile("buffered-async.log"));

        flushOnceLogger.debug("Flush the logs!");
        Assert.assertEquals("test2", getLastLineFromFile("immediate-1.log"));
        Assert.assertEquals("*** Flush", getLastLineFromFile("buffered-1.log"));
        Assert.assertEquals("*** FLUSH ***", getLastLineFromFile("buffered-2.log"));
        Thread.sleep(10L);
        Assert.assertEquals("test2", getLastLineFromFile("immediate-async.log"));
        Assert.assertEquals("*** Flush", getLastLineFromFile("buffered-async.log"));

        logger.info("test3");
        Thread.sleep(10L);
        Assert.assertEquals("test3", getLastLineFromFile("immediate-1.log"));
        Assert.assertEquals("test3", getLastLineFromFile("buffered-1.log"));
        Assert.assertEquals("*** FLUSH ***", getLastLineFromFile("buffered-2.log"));
        Assert.assertEquals("test3", getLastLineFromFile("immediate-async.log"));
        Assert.assertEquals("test3", getLastLineFromFile("buffered-async.log"));

        logger.debug("test4");

        LogManager.shutdown();
        Assert.assertEquals("test4", getLastLineFromFile("immediate-1.log"));
        Assert.assertEquals("test4", getLastLineFromFile("buffered-1.log"));
        Assert.assertEquals("test4", getLastLineFromFile("buffered-2.log"));
        Assert.assertEquals("test4", getLastLineFromFile("immediate-async.log"));
        Assert.assertEquals("test4", getLastLineFromFile("buffered-async.log"));
    }

    private String getLastLineFromFile(String fileName) throws IOException {
        if (!new File(fileName).isFile()) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        try {
            String line = reader.readLine();
            String lastLine = line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
            return lastLine;
        } finally {
            reader.close();
        }
    }

}
