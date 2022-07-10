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
    public static Map<String,Map<String,TemplateDetails>> findTemplatesThatAreInOneAndInTwo(DamlLf.ArchivePayload one, DamlLf.ArchivePayload two)
    {
        Map<String,Map<String,TemplateDetails>> moduleTemplates = new HashMap<>();

        Map<String, Map<String, TemplateWithData>> moduleTemplatesOne = collectTemplates(one.getDamlLf1());
        Map<String, Map<String, TemplateWithData>> moduleTemplatesTwo = collectTemplates(two.getDamlLf1());

        Map<String,Map<String,List<String>>> signatoriesMap = findSignatories(one);

        for(String moduleName: moduleTemplatesOne.keySet())
        {
            Map<String, TemplateWithData> templatesOne = moduleTemplatesOne.get(moduleName);
            Map<String, TemplateWithData> templatesTwo = moduleTemplatesTwo.get(moduleName);
            Map<String, TemplateDetails> templates = new HashMap<>();

            if (templatesTwo != null)
            {
                for (String templateName : templatesOne.keySet())
                {
                    TemplateDetails templateDetails = new TemplateDetails(templateName, one.getDamlLf1());
                    TemplateWithData template2 = templatesTwo.get(templateName);
                    if (template2 != null)
                    {
                        TemplateWithData templateWithData = templatesOne.get(templateName);
                        List<String> signatories = signatoriesMap.getOrDefault(moduleName, new HashMap<>())
                                .getOrDefault(templateName, new ArrayList<>());

                        templateDetails.setSignatories(signatories);
                        FieldsDiffs fieldsDiffs = FieldsDiffs.create(
                                templateWithData.dataType.getRecord(), one.getDamlLf1(),
                                template2.dataType.getRecord(), two.getDamlLf1()
                        );
                        templateDetails.setFieldsDiffs(fieldsDiffs);

                        if(fieldsDiffs.isSchemaSame())
                        {
                            templateDetails.setDifferenceType(TemplateDifferenceType.IN_BOTH_CONTENTS_ONLY_CHANGE);
                        }
                        else
                        {
                            templateDetails.setDifferenceType(TemplateDifferenceType.IN_BOTH_SCHEMA_CHANGE);
                        }
                    }
                    else
                    {
                        templateDetails.setDifferenceType(TemplateDifferenceType.TEMPLATE_REMOVED);
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

    private static Map<String,Map<String, TemplateWithData>> collectTemplates(DamlLf1.Package _package)
    {
        Map<String,Map<String, TemplateWithData>> moduleTemplates = new HashMap<>();
        for(DamlLf1.Module module: _package.getModulesList())
        {
            Map<String, DamlLf1.DefDataType> dataTypes = new HashMap<>();
            for(DamlLf1.DefDataType dataType: module.getDataTypesList())
            {
                String name = getName(_package, dataType.getNameInternedDname());
                dataTypes.put(name,dataType);
            }
            Map<String, TemplateWithData> result = new HashMap<>();
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                String name = getName(_package, template.getTyconInternedDname());
                result.put(name, new TemplateWithData(dataTypes.get(name), template));
            }
            String moduleName = getName(_package, module.getNameInternedDname());
            moduleTemplates.put(moduleName, result);
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

    private static class TemplateWithData
    {
        private final DamlLf1.DefDataType dataType;
        private final DamlLf1.DefTemplate template;

        TemplateWithData(DamlLf1.DefDataType dataType, DamlLf1.DefTemplate template)
        {
            this.dataType = dataType;
            this.template = template;
        }
    }
}
