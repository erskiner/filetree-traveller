package de.n8t.examples;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple CountNodesExample.
 */
public class TestApp
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestApp(String testName)
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( TestApp.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        Assert.assertTrue(true);
    }
}
