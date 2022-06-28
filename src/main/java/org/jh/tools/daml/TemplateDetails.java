package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.List;

public class TemplateDetails
{
    private final String name;

    private final List<String> fieldNames = new ArrayList<>();

    public TemplateDetails(String name)
    {
        this.name = name;
    }

    public TemplateDetails withFieldName(String name)
    {
        this.fieldNames.add(name);
        return this;
    }

    public String name()
    {
        return name;
    }

    public List<String> getFieldNames()
    {
        return fieldNames;
    }

    public static TemplateDetails from(String name, DamlLf1.DefDataType dataType, DamlLf1.DefTemplate damlLfTemplate, DamlLf1.Package _package)
    {
        TemplateDetails templateDetails = new TemplateDetails(name);
        for(DamlLf1.FieldWithType ft: dataType.getRecord().getFieldsList())
        {
            String fieldName = _package.getInternedStrings(ft.getFieldInternedStr());
            templateDetails.withFieldName(fieldName);
        }
        return templateDetails;
    }
}