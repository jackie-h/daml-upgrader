package org.jh.tools.daml;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UpgraderTest
{
    @Test
    public void testScenario1()
    {
        List<String> result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v2/.daml/dist/carbon-2.0.0.dar",
                "target");

        //Assert.assertEquals(1, result.size());
        //Assert.assertEquals("CarbonCert", result.get(0));
    }

    @Test
    public void testSameDarFileProducesNoChange()
    {
        List<String> result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "target");

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testDarFilesWithSameContentsAreSame()
    {
        List<String> result = Upgrader.createUpgrades("daml-examples/scenario2/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario2/v2/.daml/dist/carbon-1.0.0.dar",
                "target");

        Assert.assertEquals(0, result.size());
    }
}
