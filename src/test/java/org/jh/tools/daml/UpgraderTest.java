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

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains("CarbonCert"));
        Assert.assertTrue(result.contains("CarbonCertProposal"));
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
        String darPath1 = "daml-examples/scenario2/v1/.daml/dist/carbon-1.0.0.dar";
        String darPath2 = "daml-examples/scenario2/v2/.daml/dist/carbon-1.0.0.dar";
        Dar dar1 = Dar.readDar(darPath1);
        Dar dar2 = Dar.readDar(darPath2);
        Assert.assertEquals("Files with same contents should have the same hash",
                dar1.getDamlLf().getHash(), dar2.getDamlLf().getHash());

        List<String> result = Upgrader.createUpgrades(darPath1, darPath2, "target");

        Assert.assertEquals(0, result.size());
    }
}
