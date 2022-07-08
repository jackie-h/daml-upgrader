package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateDetails
{
    private final String name;

    private TemplateDifferenceType differenceType = null;

    private UpgradeDecision upgradeDecision = null;

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

    private boolean hasUpgradableFields()
    {
        //todo - handle complex record types and generics are also not complex
        return this.fields.values().stream().allMatch(type -> {
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
        });
    }

    public boolean canAutoUpgrade()
    {
        if (this.upgradeDecision == null)
            computeUpgradeDecision();

        return this.upgradeDecision == UpgradeDecision.YES;
    }

    private void computeUpgradeDecision()
    {
        if (TemplateDifferenceType.IN_BOTH_SCHEMA_CHANGE.equals(this.differenceType))
        {
            this.upgradeDecision = UpgradeDecision.NO_SCHEMA_CHANGE;
        }
        else if (TemplateDifferenceType.TEMPLATE_REMOVED.equals(this.differenceType))
        {
            this.upgradeDecision = UpgradeDecision.NO_TEMPLATE_REMOVED;
        }
        else if (!(this.isUnilateral() || this.isBilateral()))
        {
            this.upgradeDecision = UpgradeDecision.NO_MULTI_PARTY;
        }
        else if (!this.hasUpgradableFields())
        {
            this.upgradeDecision = UpgradeDecision.NO_NON_PRIMITIVE_TYPES;
        }
        else
        {
            this.upgradeDecision = UpgradeDecision.YES;
        }
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

    public void setDifferenceType(TemplateDifferenceType differenceType)
    {
        this.differenceType = differenceType;
    }

    public void addSchemaData(DamlLf1.DefDataType dataType)
    {
        for(DamlLf1.FieldWithType ft: dataType.getRecord().getFieldsList())
        {
            String fieldName = _package.getInternedStrings(ft.getFieldInternedStr());
            DamlLf1.Type type = ft.getType();
            this.addField(fieldName,type);
        }
    }
}
