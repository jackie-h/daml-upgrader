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
    public static Map<String,List<String>> findTemplatesThatAreInOneAndInTwo(DamlLf.ArchivePayload one, DamlLf.ArchivePayload two)
    {
        Map<String,List<String>> moduleTemplates = new HashMap<>();

        Map<String, Map<String, DamlLf1.DefTemplate>> moduleTemplatesOne = collectTemplates(one.getDamlLf1());
        Map<String, Map<String, DamlLf1.DefTemplate>> moduleTemplatesTwo = collectTemplates(two.getDamlLf1());

        for(String moduleName: moduleTemplatesOne.keySet())
        {
            Map<String, DamlLf1.DefTemplate> templatesOne = moduleTemplatesOne.get(moduleName);
            Map<String, DamlLf1.DefTemplate> templatesTwo = moduleTemplatesTwo.get(moduleName);
            List<String> templates = new ArrayList<>();

            if (templatesTwo != null)
            {
                for (String templateName : templatesOne.keySet())
                {
                    DamlLf1.DefTemplate template2 = templatesTwo.get(templateName);
                    if (template2 != null)
                    {
                        templates.add(templateName);
                    }
                }
            }
            if(!templates.isEmpty())
            {
                moduleTemplates.put(moduleName, templates);
            }
        }

        return moduleTemplates;
    }

    public static Map<String,List<String>> findTemplatesThatAreInOneButDifferentInTwo(DamlLf.ArchivePayload one, DamlLf.ArchivePayload two)
    {
        Map<String,List<String>> moduleTemplatesChanged = new HashMap<>();

        Map<String, Map<String, DamlLf1.DefTemplate>> moduleTemplatesOne = collectTemplates(one.getDamlLf1());
        Map<String, Map<String, DamlLf1.DefTemplate>> moduleTemplatesTwo = collectTemplates(two.getDamlLf1());

        for(String moduleName: moduleTemplatesOne.keySet())
        {
            Map<String, DamlLf1.DefTemplate> templatesOne = moduleTemplatesOne.get(moduleName);
            Map<String, DamlLf1.DefTemplate> templatesTwo = moduleTemplatesTwo.get(moduleName);
            List<String> changed = new ArrayList<>();

            if (templatesTwo != null)
            {
                for (String templateName : templatesOne.keySet())
                {
                    DamlLf1.DefTemplate template1 = templatesOne.get(templateName);
                    DamlLf1.DefTemplate template2 = templatesTwo.get(templateName);

                    if (template2 != null)
                    {
                        if (!templatesEqualIgnoreLocation(template1, template2))
                        {
                            changed.add(templateName);
                        }
                    }
                }
            }
            if(!changed.isEmpty())
            {
                moduleTemplatesChanged.put(moduleName, changed);
            }
        }

        return moduleTemplatesChanged;
    }

    public static Map<String,Map<String, DamlLf1.DefTemplate>> collectTemplates(DamlLf1.Package _package)
    {
        Map<String,Map<String, DamlLf1.DefTemplate>> moduleTemplates = new HashMap<>();
        for(DamlLf1.Module module: _package.getModulesList())
        {
            Map<String, DamlLf1.DefTemplate> result = new HashMap<>();
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                String name = getName(_package, template.getTyconInternedDname());
                result.put(name, template);
            }
            String moduleName = getName(_package, module.getNameInternedDname());
            moduleTemplates.put(moduleName, result);
        }
        return moduleTemplates;
    }

    private static String getName(DamlLf1.Package _package, int internedDname)
    {
        DamlLf1.InternedDottedName iName = _package.getInternedDottedNames(internedDname);
        List<Integer> segments = iName.getSegmentsInternedStrList();
        List<String> names = new ArrayList<>();
        for(Integer segment: segments)
        {
            names.add(_package.getInternedStrings(segment));
        }
        return String.join(".", names);
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
