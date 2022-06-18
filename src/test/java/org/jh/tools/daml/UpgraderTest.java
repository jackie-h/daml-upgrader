package org.jh.tools.daml;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class UpgraderTest
{
    @BeforeClass
    public static void compileDaml() throws IOException
    {
        DamlCommand.cleanBuildDar("daml-examples/scenario1/v1");
        DamlCommand.cleanBuildDar("daml-examples/scenario1/v2");
        DamlCommand.cleanBuildDar("daml-examples/scenario2/v1");
        DamlCommand.cleanBuildDar("daml-examples/scenario2/v2");
    }

    @Test
    public void testScenario1()
    {
        Map<String, List<Module>> modulesResult = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v2/.daml/dist/carbon-2.0.0.dar",
                "target/scenario1");

        Assert.assertEquals(2, modulesResult.keySet().size());

        List<Module> result = modulesResult.get("Carbon");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("UpgradeCarbonCertProposal", result.get(0).getName());
        Assert.assertEquals("UpgradeCarbonCert", result.get(1).getName());

        List<Module> intro = modulesResult.get("Intro.Iou");
        Assert.assertEquals(1, intro.size());
        Assert.assertEquals("UpgradeIou", intro.get(0).getName());
    }

    @Test
    public void testSameDarFileProducesNoChange()
    {
        Map<String, List<Module>> result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
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

        Map<String, List<Module>> result = Upgrader.createUpgrades(darPath1, darPath2, "target");

        Assert.assertEquals(0, result.size());
    }
}
