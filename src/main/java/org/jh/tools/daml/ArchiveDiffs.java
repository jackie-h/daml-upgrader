package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;
import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Reader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ArchiveDiffs
{
    private static final Logger LOGGER = Logger.getLogger(ArchiveDiffs.class.getName());

    private final Map<String, Map<String, FieldsDiffs>> moduleDataTypes = new HashMap<>();
    private final Map<String, Map<String, List<String>>> moduleTemplateSignatories;
    private final Map<String, Map<String, UpgradeDecision>> templateUpgradeDecision = new HashMap<>();

    ArchiveDiffs()
    {
        this.moduleTemplateSignatories = new HashMap<>();
    }

    private ArchiveDiffs(Map<String, Map<String, List<String>>> moduleTemplateSignatories)
    {
        this.moduleTemplateSignatories = moduleTemplateSignatories;
    }

    public Set<String> modules()
    {
        return this.templateUpgradeDecision.keySet();
    }

    public long templateCount()
    {
        return templateUpgradeDecision.values().stream().flatMap(stringTemplateDetailsMap -> stringTemplateDetailsMap.values().stream()).count();
    }

    public long upgradableTemplateCount()
    {
        return templateUpgradeDecision.values().stream().flatMap(stringTemplateDetailsMap -> stringTemplateDetailsMap.values().stream())
                .filter(t -> t == UpgradeDecision.YES).count();
    }

    public List<String> upgradableTemplates(String moduleName)
    {
        return this.templateUpgradeDecision.get(moduleName).entrySet().stream()
                .filter(e -> e.getValue() == UpgradeDecision.YES)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public FieldConstructors getFieldsInBothCopyConstructor(String moduleName, String templateName)
    {
        return this.moduleDataTypes.get(moduleName).get(templateName).getFieldsInBothCopyConstructor(this.moduleDataTypes);
    }


    public Iterable<String> getAdditionalOptionalFields(String moduleName, String templateName)
    {
        return this.moduleDataTypes.get(moduleName).get(templateName).getAdditionalOptionalFields();
    }

    public List<String> getSignatories(String moduleName, String templateName)
    {
        return this.moduleTemplateSignatories.get(moduleName).get(templateName);
    }

    public boolean isUnilateral(String moduleName, String templateName)
    {
        //Check that there is one and that it is not a collection type
        List<String> signatories = getSignatories(moduleName, templateName);
        return signatories.size() == 1 && this.moduleDataTypes.get(moduleName).get(templateName).fieldIsPartyType(signatories.get(0));
    }

    public boolean isBilateral(String moduleName, String templateName)
    {
        FieldsDiffs fieldsDiffs = this.moduleDataTypes.get(moduleName).get(templateName);
        List<String> signatories = getSignatories(moduleName, templateName);
        return signatories.size() == 2 && fieldsDiffs.fieldIsPartyType(signatories.get(0)) &&
                fieldsDiffs.fieldIsPartyType(signatories.get(1));
    }

    public static ArchiveDiffs create(Dar darFrom, Dar darTo)
    {
        DamlLf.Archive archiveFrom = darFrom.getMainDamlLf();
        DamlLf.Archive archiveTo = darTo.getMainDamlLf();
        LOGGER.info(archiveFrom.getHash());
        LOGGER.info(archiveTo.getHash());

        if (archiveFrom.getHash().equals(archiveTo.getHash()))
        {
            LOGGER.info("Contents identical nothing to do");
            return new ArchiveDiffs();
        }

        ArchivePayload payloadFrom = Reader.readArchive(archiveFrom).right().get();
        ArchivePayload payloadTo = Reader.readArchive(archiveTo).right().get();
        LOGGER.info(payloadFrom.pkgId());
        LOGGER.info(payloadTo.pkgId());

        DamlLf.ArchivePayload damlLfArchiveFrom = payloadFrom.proto();
        DamlLf.ArchivePayload damlLfArchiveTo = payloadTo.proto();

        ArchiveDiffs archiveDiffs = new ArchiveDiffs(DamlLfProtoUtils.findSignatories(damlLfArchiveFrom));

        Map<String, ModuleIndex> modulesOne = buildModuleIndex(damlLfArchiveFrom.getDamlLf1());
        Map<String, ModuleIndex> modulesTwo = buildModuleIndex(damlLfArchiveTo.getDamlLf1());

        computeDataTypeDifferences(damlLfArchiveFrom, damlLfArchiveTo, archiveDiffs, modulesOne, modulesTwo);
        computeUpgradeDecision(archiveDiffs, modulesOne, modulesTwo);

        return archiveDiffs;
    }

    private static void computeDataTypeDifferences(DamlLf.ArchivePayload archiveFrom, DamlLf.ArchivePayload archiveTo, ArchiveDiffs archiveDiffs, Map<String, ModuleIndex> modulesOne, Map<String, ModuleIndex> modulesTwo)
    {
        for(String moduleName: modulesOne.keySet())
        {
            ModuleIndex moduleIndexOne = modulesOne.get(moduleName);
            ModuleIndex moduleIndexTwo = modulesTwo.get(moduleName);

            Map<String, FieldsDiffs> dataTypes = new HashMap<>();

            if (moduleIndexTwo != null)
            {
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
            archiveDiffs.moduleDataTypes.put(moduleName, dataTypes);
        }
    }

    private static void computeUpgradeDecision(ArchiveDiffs archiveDiffs, Map<String, ModuleIndex> modulesOne, Map<String, ModuleIndex> modulesTwo)
    {
        for(String moduleName: modulesOne.keySet())
        {
            ModuleIndex moduleIndexOne = modulesOne.get(moduleName);
            ModuleIndex moduleIndexTwo = modulesTwo.get(moduleName);

            Map<String, UpgradeDecision> templateUpgradeDecision = new HashMap<>();

            if (moduleIndexTwo != null)
            {
                for (String templateName : moduleIndexOne.templateNames)
                {
                    if (!moduleIndexTwo.templateNames.contains(templateName))
                    {
                        templateUpgradeDecision.put(templateName, UpgradeDecision.NO_TEMPLATE_REMOVED);
                    }
                    else
                    {
                        templateUpgradeDecision.put(templateName,
                                archiveDiffs.computeUpgradeDecision(moduleName, templateName));
                    }
                }

            }
            archiveDiffs.templateUpgradeDecision.put(moduleName, templateUpgradeDecision);
        }
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
            Map<String, UpgradeDecision> templates = this.templateUpgradeDecision.get(module);

            for (String template : templates.keySet())
            {
                builder.append(String.format(rowFormat, module, template,
                        templates.get(template).getMessage()));
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
            Map<String, UpgradeDecision> templates = this.templateUpgradeDecision.get(key);
            for(String templateName: templates.keySet())
            {
                maxLengths[1] = Math.max(maxLengths[1], templateName.length());
                maxLengths[2] = Math.max(maxLengths[2], templates.get(templateName).getMessage().length());
            }
        }
        return maxLengths;
    }

    private UpgradeDecision computeUpgradeDecision(String moduleName, String templateName)
    {
        UpgradeDecision upgradeDecision;
        if (!(this.isUnilateral(moduleName, templateName) || this.isBilateral(moduleName, templateName)))
        {
            upgradeDecision = UpgradeDecision.NO_MULTI_PARTY;
        }
        else {
            FieldsDiffs fieldsDiffs = this.moduleDataTypes.get(moduleName).get(templateName);
            if (!fieldsDiffs.hasUpgradableFields(this.moduleDataTypes))
            {
                upgradeDecision = UpgradeDecision.NO_UNSUPPORTED_TYPES;
            }
            else if (!fieldsDiffs.isSchemaUpgradable(this.moduleDataTypes))
            {
                upgradeDecision = UpgradeDecision.NO_SCHEMA_CHANGE;
            }
            else
            {
                upgradeDecision = UpgradeDecision.YES;
            }
        }
        return upgradeDecision;
    }

    private static Map<String,ModuleIndex> buildModuleIndex(DamlLf1.Package _package)
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
