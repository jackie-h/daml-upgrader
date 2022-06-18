package org.jh.tools.daml;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

public class UpgradeTemplateTest
{

    @Test
    public void testUpgradeTemplate()
    {
        List<Module> result = UpgradeTemplate.createUpgradeTemplatesContent("Carbon", Lists.newArrayList("CarbonAgreement"));



    }
}
