package org.jh.tools.daml;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Map;

public class UpgradeTemplateTest
{

    @Test
    public void testUpgradeTemplate()
    {
        Map<String, String> result = UpgradeTemplate.createUpgradeTemplatesContent("Carbon", Lists.newArrayList("CarbonAgreement"));



    }
}
