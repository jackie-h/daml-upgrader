package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ArchiveDiffs
{
    private final Map<String, Map<String,TemplateDetails>> moduleTemplates = new HashMap<>();
    private final Map<String, Map<String,FieldsDiffs>> moduleDataTypes = new HashMap<>();

    public Set<String> modules()
    {
        return this.moduleTemplates.keySet();
    }

    public List<TemplateDetails> upgradableTemplates(String moduleName)
    {
        return this.moduleTemplates.get(moduleName).values().stream()
                .filter(TemplateDetails::canAutoUpgrade)
                .collect(Collectors.toList());
    }

    public long templateCount()
    {
        return moduleTemplates.values().stream().flatMap(stringTemplateDetailsMap -> stringTemplateDetailsMap.values().stream()).count();
    }

    public long upgradableTemplateCount()
    {
        return moduleTemplates.values().stream().flatMap(stringTemplateDetailsMap -> stringTemplateDetailsMap.values().stream())
                .filter(TemplateDetails::canAutoUpgrade).count();
    }

    public static ArchiveDiffs create(DamlLf.ArchivePayload archiveFrom,
                         DamlLf.ArchivePayload archiveTo)
    {
        ArchiveDiffs archiveDiffs = new ArchiveDiffs();

        Map<String, ModuleIndex> moduleTemplatesOne = collectTemplates(archiveFrom.getDamlLf1());
        Map<String, ModuleIndex> moduleTemplatesTwo = collectTemplates(archiveTo.getDamlLf1());

        Map<String,Map<String, List<String>>> signatoriesMap = DamlLfProtoUtils.findSignatories(archiveFrom);

        for(String moduleName: moduleTemplatesOne.keySet())
        {
            ModuleIndex moduleIndexOne = moduleTemplatesOne.get(moduleName);
            ModuleIndex moduleIndexTwo = moduleTemplatesTwo.get(moduleName);
            Map<String, TemplateDetails> templates = new HashMap<>();
            Map<String, FieldsDiffs> dataTypes = new HashMap<>();

            if (moduleIndexTwo != null)
            {
                for (String templateName : moduleIndexOne.templateNames)
                {
                    TemplateDetails templateDetails = new TemplateDetails(templateName, archiveFrom.getDamlLf1());
                    if (moduleIndexTwo.templateNames.contains(templateName))
                    {

                        List<String> signatories = signatoriesMap.getOrDefault(moduleName, new HashMap<>())
                                .getOrDefault(templateName, new ArrayList<>());

                        templateDetails.setSignatories(signatories);

                        DamlLf1.DefDataType dataType1 = moduleIndexOne.dataTypes.get(templateName);
                        DamlLf1.DefDataType dataType2 = moduleIndexTwo.dataTypes.get(templateName);
                        FieldsDiffs fieldsDiffs = FieldsDiffs.create(
                                dataType1.getRecord(), archiveFrom.getDamlLf1(),
                                dataType2.getRecord(), archiveTo.getDamlLf1()
                        );
                        templateDetails.setFieldsDiffs(fieldsDiffs);
                    }
                    else
                    {
                        templateDetails.setTemplateRemoved();
                    }
                    templates.put(templateName, templateDetails);
                }

                for(String dataTypeName : moduleIndexOne.dataTypes.keySet())
                {
                    DamlLf1.DefDataType dataType1 = moduleIndexOne.dataTypes.get(dataTypeName);
                    DamlLf1.DefDataType dataType2 = moduleIndexTwo.dataTypes.get(dataTypeName);
                    FieldsDiffs fieldsDiffs = new FieldsDiffs();

                    if(dataType2 != null)
                    {
                        fieldsDiffs = FieldsDiffs.create(
                                dataType1.getRecord(), archiveFrom.getDamlLf1(),
                                dataType2.getRecord(), archiveTo.getDamlLf1()
                        );
                    }
                    dataTypes.put(dataTypeName, fieldsDiffs);
                }
            }
            archiveDiffs.moduleTemplates.put(moduleName, templates);
            archiveDiffs.moduleDataTypes.put(moduleName, dataTypes);
        }
        return archiveDiffs;
    }

    public String report()
    {
        int[] maxLengths = maxLengths();
        StringBuilder builder = new StringBuilder();
        String spacer = "-".repeat(maxLengths[0] + maxLengths[1] + maxLengths[2] + 10);
        String rowFormat = "| %-" + maxLengths[0] + "s | %-" + maxLengths[1] + "s | %-" + maxLengths[2] + "s |\n";
        builder.append(spacer).append("\n");
        builder.append(String.format(rowFormat, "Module", "Template", "Result"));
        builder.append(spacer).append("\n");
        for (String module : this.modules())
        {
            Map<String, TemplateDetails> templates = this.moduleTemplates.get(module);

            for (String template : templates.keySet())
            {
                TemplateDetails details = templates.get(template);
                builder.append(String.format(rowFormat, module, template, details.getUpgradeDecision().getMessage()));
            }
        }
        builder.append(spacer).append("\n");
        return builder.toString();
    }

    private int[] maxLengths()
    {
        int[] maxLengths = new int[]{10,10,10};//defaults
        for (String key : this.modules())
        {
            maxLengths[0] = Math.max(maxLengths[0], key.length());
            Map<String, TemplateDetails> templates = this.moduleTemplates.get(key);
            for(String templateName: templates.keySet())
            {
                TemplateDetails details = templates.get(templateName);
                maxLengths[1] = Math.max(maxLengths[1], templateName.length());
                maxLengths[2] = Math.max(maxLengths[2], details.getUpgradeDecision().getMessage().length());
            }
        }
        return maxLengths;
    }

    private static Map<String,ModuleIndex> collectTemplates(DamlLf1.Package _package)
    {
        Map<String,ModuleIndex> moduleTemplates = new HashMap<>();
        for(DamlLf1.Module module: _package.getModulesList())
        {
            String moduleName = DamlLfProtoUtils.getName(_package, module.getNameInternedDname());
            moduleTemplates.put(moduleName, ModuleIndex.create(module,_package));
        }
        return moduleTemplates;
    }

    private static class ModuleIndex
    {
        private final Set<String> templateNames = new HashSet<>();
        private final Map<String,DamlLf1.DefDataType> dataTypes = new HashMap<>();

        static ModuleIndex create(DamlLf1.Module module, DamlLf1.Package _package)
        {
            ModuleIndex moduleIndex = new ModuleIndex();
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                String name = DamlLfProtoUtils.getName(_package, template.getTyconInternedDname());
                moduleIndex.templateNames.add(name);
            }
            for(DamlLf1.DefDataType dataType: module.getDataTypesList())
            {
                String name = DamlLfProtoUtils.getName(_package, dataType.getNameInternedDname());
                moduleIndex.dataTypes.put(name,dataType);
            }

            return moduleIndex;
        }
    }
}
