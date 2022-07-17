package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;
import com.google.protobuf.ProtocolStringList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamlLfProtoUtils
{

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

    public static String getDataTypeName(DamlLf1.Package _package, DamlLf1.DefDataType dataType)
    {
        if(dataType.hasNameInternedDname())
        {
            return getName(_package, dataType.getNameInternedDname());
        }
        else if(dataType.hasNameDname())
        {
            return getName(dataType.getNameDname());
        }
        else
        {
            throw new RuntimeException("Don't know how to find module name");
        }
    }

    public static String getModuleName(DamlLf1.Package _package, DamlLf1.Module module)
    {
        if(module.hasNameInternedDname())
        {
            return getName(_package, module.getNameInternedDname());
        }
        else if(module.hasNameDname())
        {
            return getName(module.getNameDname());
        }
        else
        {
            throw new RuntimeException("Don't know how to find module name");
        }
    }

    public static String getFieldWithTypeFieldName(DamlLf1.Package _package, DamlLf1.FieldWithType fieldWithType)
    {
        if(fieldWithType.hasFieldInternedStr())
        {
            return _package.getInternedStrings(fieldWithType.getFieldInternedStr());
        }
        else if(fieldWithType.hasFieldStr())
        {
            return fieldWithType.getFieldStr();
        }
        else
        {
            throw new RuntimeException("Don't know how to find module name");
        }
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

    private static String getName(DamlLf1.DottedName dottedName)
    {
        ProtocolStringList segments = dottedName.getSegmentsList();
        return String.join(".", segments);
    }

}
