package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;
import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Reader;

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

                        if(isSchemaSame(templateWithData.dataType, one.getDamlLf1(), template2.dataType, two.getDamlLf1()))
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

    public static boolean isOptional(DamlLf1.Type type, DamlLf1.Package _package)
    {
        return false;
    }

    public static boolean isSchemaSame(DamlLf1.DefDataType dataTypeOne, DamlLf1.Package _packageOne,
                                       DamlLf1.DefDataType dataTypeTwo, DamlLf1.Package _packageTwo)
    {
        if(dataTypeOne.getRecord().getFieldsList().size() != dataTypeTwo.getRecord().getFieldsList().size())
        {
            return false;
        }
        else
        {
            Map<String,String> fieldsOne = getFieldNamesAndTypes(dataTypeOne, _packageOne);
            Map<String,String> fieldsTwo = getFieldNamesAndTypes(dataTypeTwo, _packageTwo);

            for(String fieldName : fieldsOne.keySet())
            {
                String type1 = fieldsOne.get(fieldName);
                String type2 = fieldsTwo.get(fieldName);
                if(type2 == null)
                {
                    return false;
                }
                if(!type1.equals(type2))
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static Map<String,String> getFieldNamesAndTypes(DamlLf1.DefDataType dataType, DamlLf1.Package _package)
    {
        Map<String,String> fieldMap = new HashMap<>();
        for(DamlLf1.FieldWithType ft: dataType.getRecord().getFieldsList())
        {
            String fieldName = _package.getInternedStrings(ft.getFieldInternedStr());
            StringBuilder builder = new StringBuilder();
            DamlLfPrinter.print(builder, "", ft.getType(), _package);
            fieldMap.put(fieldName,builder.toString());
        }
        return fieldMap;
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

    public static Map<String,List<String>> findTemplatesThatAreInOneButDifferentInTwo(DamlLf.ArchivePayload one, DamlLf.ArchivePayload two)
    {
        Map<String,List<String>> moduleTemplatesChanged = new HashMap<>();

        Map<String, Map<String, TemplateWithData>> moduleTemplatesOne = collectTemplates(one.getDamlLf1());
        Map<String, Map<String, TemplateWithData>> moduleTemplatesTwo = collectTemplates(two.getDamlLf1());

        for(String moduleName: moduleTemplatesOne.keySet())
        {
            Map<String, TemplateWithData> templatesOne = moduleTemplatesOne.get(moduleName);
            Map<String, TemplateWithData> templatesTwo = moduleTemplatesTwo.get(moduleName);
            List<String> changed = new ArrayList<>();

            if (templatesTwo != null)
            {
                for (String templateName : templatesOne.keySet())
                {
                    TemplateWithData template1 = templatesOne.get(templateName);
                    TemplateWithData template2 = templatesTwo.get(templateName);

                    if (template2 != null)
                    {
                        if (!templatesEqualIgnoreLocation(template1.template, template2.template))
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
