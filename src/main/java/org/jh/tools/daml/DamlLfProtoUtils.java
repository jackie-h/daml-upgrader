package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;
import com.google.protobuf.ProtocolStringList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DamlLfProtoUtils
{
    private static final Logger LOGGER =  Logger.getLogger(DamlLfProtoUtils.class.getName());

    public static List<String> collectTemplateNames(DamlLf.ArchivePayload input)
    {
        List<String> templates = new ArrayList<>();
        DamlLf1.Package _package = input.getDamlLf1();

        for(DamlLf1.Module module : _package.getModulesList())
        {
            String moduleName = DamlLfProtoUtils.getModuleName(_package, module);
            for(DamlLf1.DefTemplate template: module.getTemplatesList())
            {
                String templateName = DamlLfProtoUtils.getTemplateName(_package, template);
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
                    String name = getName(_package, value.getNameWithType());
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

                                    if (bound.hasRecProj())
                                    {
                                        DamlLf1.Expr.RecProj recProj = bound.getRecProj();
                                        String signatoryName = _package.getInternedStrings(recProj.getFieldInternedStr());

                                        if (recProj.hasTycon())
                                        {
                                            DamlLf1.TypeConName typeConName = recProj.getTycon().getTycon();
                                            String certName = getName(_package, typeConName);
                                            String moduleName = getModuleName(_package, typeConName.getModule());

                                            Map<String, List<String>> templateSigs = templateSignatories.computeIfAbsent(moduleName, k -> new HashMap<>());
                                            List<String> signatories = templateSigs.computeIfAbsent(certName, k -> new ArrayList<>());
                                            signatories.add(signatoryName);
                                        }
                                        else
                                        {
                                            LOGGER.warning("Don't know how to extract signatory");
                                        }
                                    }
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
            throw new RuntimeException("Don't know how to find data type name");
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

    public static String getModuleName(DamlLf1.Package _package, DamlLf1.ModuleRef module)
    {
        if(module.hasModuleNameInternedDname())
        {
            return getName(_package, module.getModuleNameInternedDname());
        }
        else if(module.hasModuleNameDname())
        {
            return getName(module.getModuleNameDname());
        }
        else
        {
            throw new RuntimeException("Don't know how to find module name");
        }
    }

    public static String getTemplateName(DamlLf1.Package _package, DamlLf1.DefTemplate template)
    {
        if(template.hasTyconInternedDname())
        {
            return getName(_package, template.getTyconInternedDname());
        }
        else if(template.hasTyconDname())
        {
            return getName(template.getTyconDname());
        }
        else
        {
            throw new RuntimeException("Don't know how to find template name");
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
            throw new RuntimeException("Don't know how to find name");
        }
    }

    public static String getName(DamlLf1.Package _package, DamlLf1.DefValue.NameWithType nameWithType)
    {
        if(nameWithType.getNameDnameCount() > 0)
        {
            ProtocolStringList segments = nameWithType.getNameDnameList();
            return String.join(".", segments);
        }
        else
        {
            return getName(_package, nameWithType.getNameInternedDname());
        }
    }

    public static String getName(DamlLf1.Package _package, DamlLf1.TypeConName typeConName)
    {
        if(typeConName.hasNameInternedDname())
        {
            return getName(_package, typeConName.getNameInternedDname());
        }
        else if(typeConName.hasNameDname())
        {
            return getName(typeConName.getNameDname());
        }
        else
        {
            throw new RuntimeException("Don't know how to find name");
        }
    }

    public static String getName(DamlLf1.Package _package, DamlLf1.ValName valName)
    {
        if(valName.getNameDnameCount() > 0)
        {
            ProtocolStringList segments = valName.getNameDnameList();
            return String.join(".", segments);
        }
        else
        {
            return getName(_package, valName.getNameInternedDname());
        }
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

    private static String getName(DamlLf1.DottedName dottedName)
    {
        ProtocolStringList segments = dottedName.getSegmentsList();
        return String.join(".", segments);
    }

}
