package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamlLfProtoUtils
{

    public static List<String> findTemplatesThatAreInOneButDifferentInTwo(DamlLf.ArchivePayload one, DamlLf.ArchivePayload two)
    {
        List<String> changed = new ArrayList<>();

        Map<String, DamlLf1.DefTemplate> templatesOne = collectTemplates(one.getDamlLf1());
        Map<String, DamlLf1.DefTemplate> templatesTwo = collectTemplates(two.getDamlLf1());

        for(String templateName: templatesOne.keySet())
        {
            DamlLf1.DefTemplate template1 = templatesOne.get(templateName);
            DamlLf1.DefTemplate template2 = templatesTwo.get(templateName);

            if(template2 != null)
            {
                if(!templatesEqualIgnoreLocation(template1, template2))
                {
                    changed.add(templateName);
                }
            }
        }

        return changed;
    }

    public static Map<String, DamlLf1.DefTemplate> collectTemplates(DamlLf1.Package _package)
    {
        Map<String, DamlLf1.DefTemplate> result = new HashMap<>();
        for(DamlLf1.Module module: _package.getModulesList())
        {
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                List<Integer> internedStrs = _package.getInternedDottedNames(template.getTyconInternedDname()).getSegmentsInternedStrList();
                if(internedStrs.size() != 1) throw new RuntimeException("Not handled yet");
                String name = _package.getInternedStrings(internedStrs.get(0));
                result.put(name, template);
            }
        }
        return result;
    }

    private static boolean templatesEqualIgnoreLocation(DamlLf1.DefTemplate one, DamlLf1.DefTemplate two)
    {
        //Check if the templates are the same, ignore the location in the file which could be different
        return one.getAgreement().equals(two.getAgreement())
                && listsEqual(one.getChoicesList(), two.getChoicesList())
                && one.getSignatories().equals(two.getSignatories());
    }

    private static boolean listsEqual(List<?> one, List<?> two)
    {
        if(one.size() != two.size())
        {
            return false;
        }

        for(Object o: one)
        {
            if(Collections.frequency(one, o) != Collections.frequency(two, o))
                return false;
        }

        return true;
    }
}
