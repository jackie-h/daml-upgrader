package org.jh.tools.daml;

import org.junit.Test;

public class UpgraderTest
{
    @Test
    public void testScenario1()
    {
        Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v2/.daml/dist/carbon-2.0.0.dar",
                "target");


    }
}
