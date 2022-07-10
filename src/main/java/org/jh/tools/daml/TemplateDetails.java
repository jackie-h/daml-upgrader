package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TemplateDetails
{
    private static final Logger LOGGER =  Logger.getLogger(TemplateDetails.class.getName());

    private final String name;

    private FieldsDiffs fieldsDiffs = null;

    private final DamlLf1.Package _package;

    private TemplateDifferenceType differenceType = null;

    private UpgradeDecision upgradeDecision = null;

    private List<String> signatories = new ArrayList<>();

    public TemplateDetails(String name, DamlLf1.Package _package)
    {
        this.name = name;
        this._package = _package;
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
        return this.fieldsDiffs.getFieldNames();
    }

    public List<String> getSignatories()
    {
        return signatories;
    }

    public boolean isUnilateral()
    {
        //Check that there is one and that it is not a collection type
        return signatories.size() == 1 && this.fieldsDiffs.fieldIsPartyType(signatories.get(0));
    }

    public boolean isBilateral()
    {
        return signatories.size() == 2 && this.fieldsDiffs.fieldIsPartyType(signatories.get(0)) &&
                this.fieldsDiffs.fieldIsPartyType(signatories.get(1));
    }

    public UpgradeDecision getUpgradeDecision()
    {
        return upgradeDecision;
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
        else if (!this.fieldsDiffs.hasUpgradableFields(this._package))
        {
            this.upgradeDecision = UpgradeDecision.NO_NON_PRIMITIVE_TYPES;
        }
        else
        {
            this.upgradeDecision = UpgradeDecision.YES;
        }

        if(this.upgradeDecision != UpgradeDecision.YES)
        {
            LOGGER.warning("Unable to upgrade " + this.name + " because: " + this.upgradeDecision.getMessage());
        }
    }

    public void setDifferenceType(TemplateDifferenceType differenceType)
    {
        this.differenceType = differenceType;
    }

    public void setFieldsDiffs(FieldsDiffs fieldsDiffs)
    {
        this.fieldsDiffs = fieldsDiffs;
    }
}
