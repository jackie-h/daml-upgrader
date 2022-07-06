package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TemplateDetails
{
    private final String name;

    private final Map<String,DamlLf1.Type> fields = new LinkedHashMap<>();

    private List<String> signatories = new ArrayList<>();

    private final DamlLf1.Package _package;

    public TemplateDetails(String name, DamlLf1.Package _package)
    {
        this.name = name;
        this._package = _package;
    }

    public void addField(String name, DamlLf1.Type type)
    {
        this.fields.put(name, type);
    }

    public void setSignatories(List<String> signatories)
    {
        this.signatories = signatories;
    }

    public String name()
    {
        return name;
    }

    public Iterable<String> getFieldNames()
    {
        return fields.keySet();
    }

    public List<String> getSignatories()
    {
        return signatories;
    }

    public boolean isUnilateral()
    {
        //Check that there is one and that it is not a collection type
        return signatories.size() == 1 && fieldIsPartyType(signatories.get(0));
    }

    public boolean isBilateral()
    {
        return signatories.size() == 2 && fieldIsPartyType(signatories.get(0)) && fieldIsPartyType(signatories.get(1));
    }

    public boolean hasUpgradableFields()
    {
        //todo - handle complex record types and generics are also not complex
        return this.fields.values().stream().allMatch(new Predicate<DamlLf1.Type>()
        {
            @Override
            public boolean test(DamlLf1.Type type)
            {
                if(type.hasInterned())
                {
                    type = _package.getInternedTypes(type.getInterned());
                }
                if(type.hasPrim() && type.getPrim().getArgsCount() > 0)
                {
                    for(DamlLf1.Type argType : type.getPrim().getArgsList())
                    {
                        if(argType.hasInterned())
                        {
                            argType = _package.getInternedTypes(argType.getInterned());
                            if(!argType.hasPrim() && !argType.hasNat()) //decimal types have natural args
                                return false;
                        }
                    }
                }
                return type.hasPrim();
            }
        });
    }

    private boolean fieldIsPartyType(String fieldName)
    {
        DamlLf1.Type type = this.fields.get(fieldName);
        if(type.hasInterned())
        {
            type = _package.getInternedTypes(type.getInterned());
        }
        return type.hasPrim() && type.getPrim().getPrim().getValueDescriptor().getName().equals("PARTY");
    }

    public static TemplateDetails from(String name, List<String> signatories, DamlLf1.DefDataType dataType, DamlLf1.Package _package)
    {
        TemplateDetails templateDetails = new TemplateDetails(name, _package);
        for(DamlLf1.FieldWithType ft: dataType.getRecord().getFieldsList())
        {
            String fieldName = _package.getInternedStrings(ft.getFieldInternedStr());
            DamlLf1.Type type = ft.getType();
            templateDetails.addField(fieldName,type);
            templateDetails.setSignatories(signatories);
        }
        return templateDetails;
    }
}
