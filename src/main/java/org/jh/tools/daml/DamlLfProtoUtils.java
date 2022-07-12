package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DamlLfProtoUtils
{
    public static Map<String,Map<String,TemplateDetails>> findTemplatesThatAreInOneAndInTwo(DamlLf.ArchivePayload archiveFrom,
                                                                                            DamlLf.ArchivePayload archiveTo)
    {
        Map<String,Map<String,TemplateDetails>> moduleTemplates = new HashMap<>();

        Map<String, ModuleIndex> moduleTemplatesOne = collectTemplates(archiveFrom.getDamlLf1());
        Map<String, ModuleIndex> moduleTemplatesTwo = collectTemplates(archiveTo.getDamlLf1());

        Map<String,Map<String,List<String>>> signatoriesMap = findSignatories(archiveFrom);

        for(String moduleName: moduleTemplatesOne.keySet())
        {
            ModuleIndex templatesOne = moduleTemplatesOne.get(moduleName);
            ModuleIndex templatesTwo = moduleTemplatesTwo.get(moduleName);
            Map<String, TemplateDetails> templates = new HashMap<>();

            if (templatesTwo != null)
            {
                for (String templateName : templatesOne.templateNames)
                {
                    TemplateDetails templateDetails = new TemplateDetails(templateName, archiveFrom.getDamlLf1());
                    if (templatesTwo.templateNames.contains(templateName))
                    {

                        List<String> signatories = signatoriesMap.getOrDefault(moduleName, new HashMap<>())
                                .getOrDefault(templateName, new ArrayList<>());

                        templateDetails.setSignatories(signatories);

                        DamlLf1.DefDataType dataType1 = templatesOne.dataTypes.get(templateName);
                        DamlLf1.DefDataType dataType2 = templatesTwo.dataTypes.get(templateName);
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
            }
            moduleTemplates.put(moduleName, templates);
        }

        return moduleTemplates;
    }

    public static List<String> collectTemplateNames(DamlLf.ArchivePayload input)
    {
        List<String> templates = new ArrayList<>();
        DamlLf1.Package _package = input.getDamlLf1();

        for(DamlLf1.Module module : _package.getModulesList())
        {
            String moduleName = DamlLfProtoUtils.getName(_package, module.getNameInternedDname());
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                String templateName = DamlLfProtoUtils.getName(_package, template.getTyconInternedDname());
                templates.add(moduleName + "[" + templateName + "]");
            }
        }

        Collections.sort(templates);
        return templates;
    }

    public static DamlLf1.Type resolveType(DamlLf1.Type type, DamlLf1.Package _package)
    {
        if(type.hasInterned())
        {
            type = _package.getInternedTypes(type.getInterned());
        }
        return type;
    }

    public static boolean isOptional(DamlLf1.Type type)
    {
        if(type.hasPrim())
        {
            return "OPTIONAL".equals(type.getPrim().getPrim().getValueDescriptor().getName());
        }
        return false;
    }
    
    public static Map<String,Map<String,List<String>>> findSignatories(DamlLf.ArchivePayload payload)
    {
        Map<String,Map<String,List<String>>> templateSignatories = new HashMap<>();

        DamlLf1.Package _package = payload.getDamlLf1();
        for(DamlLf1.Module module : _package.getModulesList())
        {
            for(DamlLf1.DefValue value: module.getValuesList())
            {
                if(value.hasNameWithType())
                {
                    String name = getName(_package, value.getNameWithType().getNameInternedDname());
                    if(name.startsWith("$$csignatory"))
                    {
                        if(value.hasExpr())
                        {
                            DamlLf1.Expr expr = value.getExpr();
                            if(expr.hasAbs())
                            {
                                DamlLf1.Expr.Abs abs = expr.getAbs();
                                DamlLf1.Expr body = abs.getBody();

                                DamlLf1.Block block = body.getLet();
                                for(DamlLf1.Binding binding: block.getBindingsList())
                                {
                                    DamlLf1.Expr bound = binding.getBound();
                                    DamlLf1.Expr.RecProj recProj = bound.getRecProj();
                                    String signatoryName = _package.getInternedStrings(recProj.getFieldInternedStr());
                                    DamlLf1.TypeConName typeConName = recProj.getTycon().getTycon();
                                    String certName = getName(_package, typeConName.getNameInternedDname());
                                    String moduleName = getName(_package, typeConName.getModule().getModuleNameInternedDname());

                                    Map<String, List<String>> templateSigs = templateSignatories.computeIfAbsent(moduleName, k -> new HashMap<>());
                                    List<String> signatories = templateSigs.computeIfAbsent(certName, k -> new ArrayList<>());
                                    signatories.add(signatoryName);
                                }
                            }
                        }
                    }
                }
            }
        }
        return templateSignatories;
    }

    private static Map<String,ModuleIndex> collectTemplates(DamlLf1.Package _package)
    {
        Map<String,ModuleIndex> moduleTemplates = new HashMap<>();
        for(DamlLf1.Module module: _package.getModulesList())
        {
            String moduleName = getName(_package, module.getNameInternedDname());
            moduleTemplates.put(moduleName, ModuleIndex.create(module,_package));
        }
        return moduleTemplates;
    }

    public static String getName(DamlLf1.Package _package, int internedDname)
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

    private static class ModuleIndex
    {
        private final Set<String> templateNames = new HashSet<>();
        private final Map<String,DamlLf1.DefDataType> dataTypes = new HashMap<>();

        static ModuleIndex create(DamlLf1.Module module, DamlLf1.Package _package)
        {
            ModuleIndex moduleIndex = new ModuleIndex();
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                String name = getName(_package, template.getTyconInternedDname());
                moduleIndex.templateNames.add(name);
            }
            for(DamlLf1.DefDataType dataType: module.getDataTypesList())
            {
                String name = getName(_package, dataType.getNameInternedDname());
                moduleIndex.dataTypes.put(name,dataType);
            }

            return moduleIndex;
        }
    }
}
